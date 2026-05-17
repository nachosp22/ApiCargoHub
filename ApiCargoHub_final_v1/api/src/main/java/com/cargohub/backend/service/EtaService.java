package com.cargohub.backend.service;

import com.cargohub.backend.config.FleetRealtimeProperties;
import com.cargohub.backend.dto.tracking.EtaConfidence;
import com.cargohub.backend.dto.tracking.EtaEstimateResponse;
import com.cargohub.backend.dto.tracking.EtaMethod;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.observability.FleetRealtimeMetrics;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.util.CalculadoraDistancia;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de calcular el Tiempo Estimado de Arribo (ETA) para las entregas.
 * Utiliza la posición actual del conductor y las coordenadas del porte para estimar
 * cuánto tiempo falta hasta la entrega, ya sea mediante un proveedor de rutas o
 * mediante la fórmula de Haversine como fallback.
 */
@Service
public class EtaService {

    private static final Logger log = LoggerFactory.getLogger(EtaService.class);
    private static final double FALLBACK_SPEED_KPH = 50.0;

    private final ConductorRepository conductorRepository;
    private final PorteRepository porteRepository;
    private final MapaService mapaService;
    private final FleetRealtimeProperties properties;
    private final FleetRealtimeMetrics metrics;
    private final Clock clock;

    /**
     * Constructor público inyectado por Spring. Utiliza el reloj del sistema UTC
     * como referencia temporal por defecto.
     *
     * @param conductorRepository repositorio para acceder a los datos del conductor
     * @param porteRepository     repositorio para acceder a los datos del porte
     * @param mapaService         servicio de mapas para obtener distancias reales por ruta
     * @param properties          configuración de la funcionalidad de fleet realtime
     * @param metrics             métricas de observabilidad del servicio
     */
    @Autowired
    public EtaService(ConductorRepository conductorRepository,
                       PorteRepository porteRepository,
                       MapaService mapaService,
                       FleetRealtimeProperties properties,
                       FleetRealtimeMetrics metrics) {
        this(conductorRepository, porteRepository, mapaService, properties, metrics, Clock.systemUTC());
    }

    /**
     * Constructor de paquete que acepta un {@link Clock} personalizado, útil para
     * pruebas unitarias donde se necesita controlar el tiempo.
     *
     * @param conductorRepository repositorio para acceder a los datos del conductor
     * @param porteRepository     repositorio para acceder a los datos del porte
     * @param mapaService         servicio de mapas para obtener distancias reales por ruta
     * @param properties          configuración de la funcionalidad de fleet realtime
     * @param metrics             métricas de observabilidad del servicio
     * @param clock               reloj inyectado para control temporal en tests
     */
    EtaService(ConductorRepository conductorRepository,
               PorteRepository porteRepository,
               MapaService mapaService,
               FleetRealtimeProperties properties,
               FleetRealtimeMetrics metrics,
               Clock clock) {
        this.conductorRepository = conductorRepository;
        this.porteRepository = porteRepository;
        this.mapaService = mapaService;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * Calcula el Tiempo Estimado de Arribo (ETA) para un conductor y un porte específicos.
     *
     * <p>El flujo de cálculo es el siguiente:</p>
     * <ol>
     *   <li>Verifica que la funcionalidad de fleet realtime esté habilitada.</li>
     *   <li>Busca el conductor y el porte en la base de datos.</li>
     *   <li>Si el porte está en un estado cerrado (entregado, facturado o cancelado),
     *       retorna una respuesta sin ETA calculado.</li>
     *   <li>Intenta obtener la distancia real por ruta mediante el proveedor de mapas.</li>
     *   <li>Si el proveedor no devuelve una distancia válida, utiliza la fórmula de
     *       Haversine como fallback con una velocidad estimada de 50 km/h.</li>
     * </ol>
     *
     * @param driverId identificador único del conductor
     * @param jobId    identificador único del porte (trabajo de entrega)
     * @return respuesta con el ETA estimado en minutos, el método utilizado y el nivel de confianza
     * @throws IllegalArgumentException si el conductor o porte no existen, o si faltan coordenadas
     * @throws IllegalStateException    si la funcionalidad de fleet realtime está deshabilitada
     */
    public EtaEstimateResponse estimate(Long driverId, Long jobId) {
        assertFeatureEnabled();
        long startedAt = System.currentTimeMillis();

        Conductor conductor = conductorRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Conductor no encontrado"));
        Porte porte = porteRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Porte no encontrado"));

        EtaEstimateResponse response = new EtaEstimateResponse();
        response.setEstimatedAt(OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));

        if (isClosedState(porte.getEstado())) {
            return response;
        }

        Double targetLat = resolveTargetLat(porte);
        Double targetLon = resolveTargetLon(porte);

        if (conductor.getLatitudActual() == null || conductor.getLongitudActual() == null
                || targetLat == null || targetLon == null) {
            throw new IllegalArgumentException("Coordenadas insuficientes para calcular ETA");
        }

        Double providerDistanceM = mapaService.obtenerDistanciaMetros(
                conductor.getLatitudActual(),
                conductor.getLongitudActual(),
                targetLat,
                targetLon
        );

        if (providerDistanceM != null && providerDistanceM > 0) {
            double km = providerDistanceM / 1000.0;
            response.setEtaMinutes(Math.max(1, (int) Math.ceil((km / FALLBACK_SPEED_KPH) * 60.0)));
            response.setMethod(EtaMethod.ROUTE_PROVIDER);
            response.setConfidence(EtaConfidence.MEDIUM);
            long durationMs = System.currentTimeMillis() - startedAt;
            metrics.recordEtaLatency(durationMs);
            log.info("fleet.eta.service requestId={} driverId={} jobId={} method={} etaMinutes={} durationMs={} avgLatencyMs={}",
                    currentRequestId(),
                    driverId,
                    jobId,
                    EtaMethod.ROUTE_PROVIDER,
                    response.getEtaMinutes(),
                    durationMs,
                    metrics.getEtaLatencyAvgMs());
            return response;
        }

        double fallbackKm = CalculadoraDistancia.calcularKm(
                conductor.getLatitudActual(),
                conductor.getLongitudActual(),
                targetLat,
                targetLon
        );
        response.setEtaMinutes(Math.max(1, (int) Math.ceil((fallbackKm / FALLBACK_SPEED_KPH) * 60.0)));
        response.setMethod(EtaMethod.HAVERSINE_FALLBACK);
        response.setConfidence(EtaConfidence.LOW);
        metrics.incrementEtaFallback();
        long durationMs = System.currentTimeMillis() - startedAt;
        metrics.recordEtaLatency(durationMs);
        log.warn("fleet.eta.service.fallback requestId={} driverId={} jobId={} method={} etaMinutes={} durationMs={} avgLatencyMs={} fallbackCount={}",
                currentRequestId(),
                driverId,
                jobId,
                EtaMethod.HAVERSINE_FALLBACK,
                response.getEtaMinutes(),
                durationMs,
                metrics.getEtaLatencyAvgMs(),
                metrics.getEtaFallback());
        return response;
    }

    /**
     * Verifica que la funcionalidad de fleet realtime esté habilitada en la configuración.
     *
     * @throws IllegalStateException si la funcionalidad está deshabilitada
     */
    private void assertFeatureEnabled() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Fleet realtime feature is disabled");
        }
    }

    /**
     * Obtiene el identificador de la petición actual desde el contexto MDC de SLF4J.
     *
     * @return el requestId asociado a la petición en curso, o null si no está presente
     */
    private String currentRequestId() {
        return org.slf4j.MDC.get("requestId");
    }

    /**
     * Determina si el estado del porte corresponde a un estado cerrado donde no se requiere cálculo de ETA.
     *
     * @param estado el estado actual del porte
     * @return true si el porte está entregado, facturado o cancelado
     */
    private boolean isClosedState(EstadoPorte estado) {
        return estado == EstadoPorte.ENTREGADO
                || estado == EstadoPorte.FACTURADO
                || estado == EstadoPorte.CANCELADO;
    }

    /**
     * Resuelve la latitud de destino según el estado del porte.
     * Si el porte está en tránsito, retorna la latitud de destino; de lo contrario, la de origen.
     *
     * @param porte el porte del cual obtener la coordenada
     * @return la latitud de destino o de origen según corresponda
     */
    private Double resolveTargetLat(Porte porte) {
        if (porte.getEstado() == EstadoPorte.EN_TRANSITO) {
            return porte.getLatitudDestino();
        }
        return porte.getLatitudOrigen();
    }

    /**
     * Resuelve la longitud de destino según el estado del porte.
     * Si el porte está en tránsito, retorna la longitud de destino; de lo contrario, la de origen.
     *
     * @param porte el porte del cual obtener la coordenada
     * @return la longitud de destino o de origen según corresponda
     */
    private Double resolveTargetLon(Porte porte) {
        if (porte.getEstado() == EstadoPorte.EN_TRANSITO) {
            return porte.getLongitudDestino();
        }
        return porte.getLongitudOrigen();
    }
}
