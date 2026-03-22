package com.cargohub.backend.service;

import com.cargohub.backend.config.FleetRealtimeProperties;
import com.cargohub.backend.dto.tracking.EtaConfidence;
import com.cargohub.backend.dto.tracking.EtaEstimateResponse;
import com.cargohub.backend.dto.tracking.EtaMethod;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
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

    @Autowired
    public EtaService(ConductorRepository conductorRepository,
                      PorteRepository porteRepository,
                      MapaService mapaService,
                      FleetRealtimeProperties properties,
                      FleetRealtimeMetrics metrics) {
        this(conductorRepository, porteRepository, mapaService, properties, metrics, Clock.systemUTC());
    }

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

    public EtaEstimateResponse estimate(Long driverId, Long jobId) {
        assertFeatureEnabled();
        long startedAt = System.currentTimeMillis();

        Conductor conductor = conductorRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Conductor no encontrado"));
        Porte porte = porteRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Porte no encontrado"));

        if (conductor.getLatitudActual() == null || conductor.getLongitudActual() == null
                || porte.getLatitudOrigen() == null || porte.getLongitudOrigen() == null) {
            throw new IllegalArgumentException("Coordenadas insuficientes para calcular ETA");
        }

        EtaEstimateResponse response = new EtaEstimateResponse();
        response.setEstimatedAt(OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));

        Double providerDistanceM = mapaService.obtenerDistanciaMetros(
                conductor.getLatitudActual(),
                conductor.getLongitudActual(),
                porte.getLatitudOrigen(),
                porte.getLongitudOrigen()
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
                porte.getLatitudOrigen(),
                porte.getLongitudOrigen()
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

    private void assertFeatureEnabled() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Fleet realtime feature is disabled");
        }
    }

    private String currentRequestId() {
        return org.slf4j.MDC.get("requestId");
    }
}
