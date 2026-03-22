package com.cargohub.backend.service;

import com.cargohub.backend.config.FleetRealtimeProperties;
import com.cargohub.backend.dto.tracking.DriverLocationPoint;
import com.cargohub.backend.dto.tracking.DriverState;
import com.cargohub.backend.dto.tracking.FleetSnapshotMeta;
import com.cargohub.backend.dto.tracking.FleetSnapshotResponse;
import com.cargohub.backend.observability.FleetRealtimeMetrics;
import com.cargohub.backend.repository.ConductorRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FleetTrackingService {

    private static final Logger log = LoggerFactory.getLogger(FleetTrackingService.class);

    private final ConductorRepository conductorRepository;
    private final FleetRealtimeProperties properties;
    private final FleetRealtimeMetrics metrics;
    private final Clock clock;

    private volatile FleetSnapshotResponse lastSnapshot;
    private volatile Instant lastSnapshotBuiltAt;

    @Autowired
    public FleetTrackingService(ConductorRepository conductorRepository,
                                FleetRealtimeProperties properties,
                                FleetRealtimeMetrics metrics) {
        this(conductorRepository, properties, metrics, Clock.systemUTC());
    }

    FleetTrackingService(ConductorRepository conductorRepository,
                         FleetRealtimeProperties properties,
                         FleetRealtimeMetrics metrics,
                         Clock clock) {
        this.conductorRepository = conductorRepository;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

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
                point.setLat(raw.getLatitudActual());
                point.setLon(raw.getLongitudActual());
                point.setRecordedAt(OffsetDateTime.ofInstant(recorded, ZoneOffset.UTC));
                point.setSpeedKph(raw.getVelocidadKphActual());
                point.setHeadingDeg(raw.getRumboActualDeg());
                point.setState(classifyState(now, recorded));
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

    private String currentRequestId() {
        return org.slf4j.MDC.get("requestId");
    }

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

    private void assertFeatureEnabled() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Fleet realtime feature is disabled");
        }
    }

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

    private boolean isValidCoordinate(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return false;
        }
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }
}
