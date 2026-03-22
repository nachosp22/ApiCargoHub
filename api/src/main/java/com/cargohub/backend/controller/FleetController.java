package com.cargohub.backend.controller;

import com.cargohub.backend.dto.tracking.EtaEstimateResponse;
import com.cargohub.backend.dto.tracking.FleetSnapshotResponse;
import com.cargohub.backend.observability.FleetRealtimeMetrics;
import com.cargohub.backend.service.EtaService;
import com.cargohub.backend.service.FleetTrackingService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class FleetController {

    private static final Logger log = LoggerFactory.getLogger(FleetController.class);

    private final FleetTrackingService fleetTrackingService;
    private final EtaService etaService;
    private final FleetRealtimeMetrics metrics;

    public FleetController(FleetTrackingService fleetTrackingService,
                           EtaService etaService,
                           FleetRealtimeMetrics metrics) {
        this.fleetTrackingService = fleetTrackingService;
        this.etaService = etaService;
        this.metrics = metrics;
    }

    @GetMapping("/fleet/snapshot")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<FleetSnapshotResponse> snapshot() {
        String requestId = ensureRequestId();
        long startedAt = System.currentTimeMillis();
        try {
            FleetSnapshotResponse response = fleetTrackingService.buildSnapshot();
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("fleet.snapshot.endpoint requestId={} durationMs={} driverCount={} degraded={} requests={} degradedCount={} avgLatencyMs={}",
                    requestId,
                    durationMs,
                    response.getDrivers() != null ? response.getDrivers().size() : 0,
                    response.getMeta() != null && response.getMeta().isDegraded(),
                    metrics.getSnapshotRequests(),
                    metrics.getSnapshotDegraded(),
                    metrics.getSnapshotLatencyAvgMs());
            return ResponseEntity.ok(response);
        } finally {
            MDC.remove("requestId");
        }
    }

    @GetMapping("/eta/estimate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<EtaEstimateResponse> estimateEta(@RequestParam Long driverId,
                                                           @RequestParam Long jobId) {
        String requestId = ensureRequestId();
        long startedAt = System.currentTimeMillis();
        try {
            EtaEstimateResponse response = etaService.estimate(driverId, jobId);
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("fleet.eta.endpoint requestId={} driverId={} jobId={} method={} durationMs={} fallbackCount={} avgLatencyMs={}",
                    requestId,
                    driverId,
                    jobId,
                    response.getMethod(),
                    durationMs,
                    metrics.getEtaFallback(),
                    metrics.getEtaLatencyAvgMs());
            return ResponseEntity.ok(response);
        } finally {
            MDC.remove("requestId");
        }
    }

    private String ensureRequestId() {
        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            MDC.put("requestId", requestId);
        }
        return requestId;
    }
}
