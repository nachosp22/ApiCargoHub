package com.cargohub.backend.service;

import com.cargohub.backend.config.FleetRealtimeProperties;
import com.cargohub.backend.dto.tracking.DriverLocationPoint;
import com.cargohub.backend.dto.tracking.DriverState;
import com.cargohub.backend.dto.tracking.FleetSnapshotMeta;
import com.cargohub.backend.dto.tracking.FleetSnapshotResponse;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.observability.FleetRealtimeMetrics;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio encargado del seguimiento en tiempo real de la flota.
 * Gestiona las instantáneas de ubicación de los conductores y su estado de conectividad
 * (ONLINE, STALE, OFFLINE) según los TTL configurados.
 */
@Service
public class FleetTrackingService {

    private static final Logger log = LoggerFactory.getLogger(FleetTrackingService.class);

    private final ConductorRepository conductorRepository;
    private final PorteRepository porteRepository;
    private final FleetRealtimeProperties properties;
    private final FleetRealtimeMetrics metrics;
    private final Clock clock;

    private volatile FleetSnapshotResponse lastSnapshot;
    private volatile Instant lastSnapshotBuiltAt;

    /**
     * Construye el servicio utilizando el reloj UTC del sistema por defecto.
     *
     * @param conductorRepository repositorio para consultar datos de los conductores
     * @param porteRepository     repositorio para consultar portes activos
     * @param properties          configuración de propiedades del módulo fleet realtime
     * @param metrics             métricas de observabilidad del módulo
     */
    @Autowired
    public FleetTrackingService(ConductorRepository conductorRepository,
                                PorteRepository porteRepository,
                                 FleetRealtimeProperties properties,
                                 FleetRealtimeMetrics metrics) {
        this(conductorRepository, porteRepository, properties, metrics, Clock.systemUTC());
    }

    /**
     * Construye el servicio con un {@link Clock} inyectable para facilitar pruebas unitarias.
     *
     * @param conductorRepository repositorio para consultar datos de los conductores
     * @param porteRepository     repositorio para consultar portes activos
     * @param properties          configuración de propiedades del módulo fleet realtime
     * @param metrics             métricas de observabilidad del módulo
     * @param clock               reloj utilizado para obtener la marca de tiempo actual
     */
    FleetTrackingService(ConductorRepository conductorRepository,
                         PorteRepository porteRepository,
                         FleetRealtimeProperties properties,
                         FleetRealtimeMetrics metrics,
                         Clock clock) {
        this.conductorRepository = conductorRepository;
        this.porteRepository = porteRepository;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * Construye una instantánea completa del estado actual de la flota.
     * <p>
     * Consulta los conductores desde la base de datos, valida sus coordenadas y
     * clasifica cada uno según su estado de conexión ({@link DriverState#ONLINE},
     * {@link DriverState#STALE} u {@link DriverState#OFFLINE}). Si ocurre un error
     * durante la construcción, intenta devolver una instantánea almacenada en caché
     * como mecanismo de fallback.
     *
     * @return respuesta con la lista de puntos de ubicación de los conductores y
     *         metadatos asociados a la instantánea
     */
    public FleetSnapshotResponse buildSnapshot() {
        assertFeatureEnabled();
        long startedAt = System.currentTimeMillis();
        metrics.incrementSnapshotRequests();

        try {
            Instant now = Instant.now(clock);
            List<ConductorRepository.DriverSnapshotProjection> rawDrivers = conductorRepository.findFleetSnapshot(
                    PageRequest.of(0, Math.max(1, properties.getMaxDrivers()))
            );

            List<DriverLocationPoint> points = new ArrayList<>();
            boolean degraded = false;
            String degradedReason = null;

            for (ConductorRepository.DriverSnapshotProjection raw : rawDrivers) {
                if (!isValidCoordinate(raw.getLatitudActual(), raw.getLongitudActual())) {
                    degraded = true;
                    degradedReason = "invalid_coordinate_excluded";
                    continue;
                }
                if (raw.getUltimaActualizacionUbicacion() == null) {
                    degraded = true;
                    degradedReason = "missing_recorded_at_excluded";
                    continue;
                }

                Instant recorded = raw.getUltimaActualizacionUbicacion().toInstant(ZoneOffset.UTC);

                DriverLocationPoint point = new DriverLocationPoint();
                point.setDriverId(String.valueOf(raw.getId()));
                point.setDriverName(raw.getNombre());
                point.setDriverLastName(raw.getApellidos());
                point.setLat(raw.getLatitudActual());
                point.setLon(raw.getLongitudActual());
                point.setRecordedAt(OffsetDateTime.ofInstant(recorded, ZoneOffset.UTC));
                point.setSpeedKph(raw.getVelocidadKphActual());
                point.setHeadingDeg(raw.getRumboActualDeg());
                point.setState(classifyState(now, recorded));
                resolveActivePorte(raw.getId(), point);
                points.add(point);
            }

            FleetSnapshotResponse response = new FleetSnapshotResponse();
            response.setSnapshotAt(OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
            response.setDrivers(points);

            FleetSnapshotMeta meta = new FleetSnapshotMeta();
            meta.setPollingSuggestedSec(properties.getPollingSuggestedSec());
            meta.setDegraded(degraded);
            meta.setDegradedReason(degraded ? degradedReason : null);
            response.setMeta(meta);

            if (degraded) {
                metrics.incrementSnapshotDegraded();
            }

            long durationMs = System.currentTimeMillis() - startedAt;
            metrics.recordSnapshotLatency(durationMs);
            log.info("fleet.snapshot.service requestId={} driverCount={} degraded={} reason={} durationMs={} avgLatencyMs={} requests={} degradedCount={}",
                    currentRequestId(),
                    points.size(),
                    degraded,
                    degradedReason,
                    durationMs,
                    metrics.getSnapshotLatencyAvgMs(),
                    metrics.getSnapshotRequests(),
                    metrics.getSnapshotDegraded());

            lastSnapshot = response;
            lastSnapshotBuiltAt = now;
            return response;
        } catch (RuntimeException ex) {
            FleetSnapshotResponse cached = tryGetCached();
            if (cached != null) {
                FleetSnapshotMeta meta = cached.getMeta();
                if (meta == null) {
                    meta = new FleetSnapshotMeta();
                    cached.setMeta(meta);
                }
                meta.setPollingSuggestedSec(properties.getPollingSuggestedSec());
                meta.setDegraded(true);
                meta.setDegradedReason("snapshot_cache_fallback");
                metrics.incrementSnapshotDegraded();
                long durationMs = System.currentTimeMillis() - startedAt;
                metrics.recordSnapshotLatency(durationMs);
                log.warn("fleet.snapshot.service.fallback requestId={} degradedReason=snapshot_cache_fallback durationMs={} avgLatencyMs={} requests={} degradedCount={} error={}",
                        currentRequestId(),
                        durationMs,
                        metrics.getSnapshotLatencyAvgMs(),
                        metrics.getSnapshotRequests(),
                        metrics.getSnapshotDegraded(),
                        ex.getMessage());
                return cached;
            }
            long durationMs = System.currentTimeMillis() - startedAt;
            metrics.recordSnapshotLatency(durationMs);
            log.error("fleet.snapshot.service.error requestId={} durationMs={} avgLatencyMs={} requests={} degradedCount={} error={}",
                    currentRequestId(),
                    durationMs,
                    metrics.getSnapshotLatencyAvgMs(),
                    metrics.getSnapshotRequests(),
                    metrics.getSnapshotDegraded(),
                    ex.getMessage());
            throw ex;
        }
    }

    /**
     * Obtiene el identificador de la petición actual desde el contexto MDC de SLF4J.
     *
     * @return el requestId asociado a la petición en curso, o {@code null} si no está definido
     */
    private String currentRequestId() {
        return org.slf4j.MDC.get("requestId");
    }

    /**
     * Intenta devolver la última instantánea construida si aún no superó el tiempo de vida
     * configurado en {@code snapshotCacheSec}.
     *
     * @return la instantánea cacheada si es válida, o {@code null} si no hay caché o expiró
     */
    private FleetSnapshotResponse tryGetCached() {
        if (lastSnapshot == null || lastSnapshotBuiltAt == null) {
            return null;
        }
        long maxAgeSec = Math.max(1, properties.getSnapshotCacheSec());
        Duration age = Duration.between(lastSnapshotBuiltAt, Instant.now(clock));
        if (age.getSeconds() > maxAgeSec) {
            return null;
        }
        return lastSnapshot;
    }

    /**
     * Verifica que la funcionalidad de fleet realtime esté habilitada.
     * Lanza una excepción si la configuración indica que el módulo está desactivado.
     */
    private void assertFeatureEnabled() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Fleet realtime feature is disabled");
        }
    }

    /**
     * Clasifica el estado de conectividad de un conductor según la antigüedad de su
     * última ubicación reportada.
     *
     * @param now        instante actual de referencia
     * @param recordedAt instante en que se registró la última ubicación del conductor
     * @return {@link DriverState#ONLINE} si la antigüedad está dentro del TTL online,
     *         {@link DriverState#STALE} si excede el online pero no el stale,
     *         u {@link DriverState#OFFLINE} en caso contrario
     */
    private DriverState classifyState(Instant now, Instant recordedAt) {
        long ageSec = Duration.between(recordedAt, now).getSeconds();
        if (ageSec <= Math.max(0, properties.getTtlOnlineSec())) {
            return DriverState.ONLINE;
        }
        if (ageSec <= Math.max(properties.getTtlOnlineSec(), properties.getTtlStaleSec())) {
            return DriverState.STALE;
        }
        return DriverState.OFFLINE;
    }

    /**
     * Valida que las coordenadas geográficas estén dentro del rango aceptable.
     *
     * @param lat latitud (debe estar entre -90 y 90)
     * @param lon longitud (debe estar entre -180 y 180)
     * @return {@code true} si ambas coordenadas son no nulas y están dentro del rango válido
     */
    private boolean isValidCoordinate(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return false;
        }
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }

    /**
     * Resuelve el porte activo asociado a un conductor y lo vincula al punto de ubicación.
     * Busca portes en estado {@code EN_TRANSITO} o {@code ASIGNADO}, ordenados por fecha
     * de creación descendente.
     *
     * @param conductorId identificador del conductor
     * @param point       punto de ubicación que se actualizará con los datos del porte
     */
    private void resolveActivePorte(Long conductorId, DriverLocationPoint point) {
        porteRepository.findFirstByConductorIdAndEstadoInOrderByFechaCreacionDesc(
                        conductorId,
                        Arrays.asList(EstadoPorte.EN_TRANSITO, EstadoPorte.ASIGNADO)
                )
                .ifPresent(porte -> applyActivePorte(point, porte));
    }

    /**
     * Aplica los datos del porte activo sobre el punto de ubicación del conductor.
     *
     * @param point punto de ubicación que recibirá los datos del porte
     * @param porte entidad del porte activo a aplicar
     */
    private void applyActivePorte(DriverLocationPoint point, Porte porte) {
        point.setActivePorteId(porte.getId());
        point.setActivePorteDestination(porte.getDestino());
        point.setActivePorteStatus(porte.getEstado() != null ? porte.getEstado().name() : null);
    }
}
