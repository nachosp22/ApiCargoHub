package com.cargohub.backend.controller;

import com.cargohub.backend.dto.tracking.DriverLocationUpsertRequest;
import com.cargohub.backend.observability.FleetRealtimeMetrics;
import com.cargohub.backend.service.TrackingLocationIngestionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracking")
@CrossOrigin(origins = "*")
public class TrackingController {

    private static final Logger log = LoggerFactory.getLogger(TrackingController.class);

    private final TrackingLocationIngestionService trackingLocationIngestionService;
    private final FleetRealtimeMetrics metrics;

    public TrackingController(TrackingLocationIngestionService trackingLocationIngestionService,
                              FleetRealtimeMetrics metrics) {
        this.trackingLocationIngestionService = trackingLocationIngestionService;
        this.metrics = metrics;
    }

    @PostMapping("/drivers/{driverId}/locations")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or @ownership.canAccessConductor(authentication, #driverId)")
    public ResponseEntity<?> upsertLocation(@PathVariable Long driverId,
                                            @Valid @RequestBody DriverLocationUpsertRequest request) {
        String requestId = ensureRequestId();
        long startedAt = System.currentTimeMillis();
        try {
            trackingLocationIngestionService.ingest(driverId, request);
            metrics.incrementTrackingWrites();
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("fleet.tracking.endpoint requestId={} driverId={} durationMs={} trackingWrites={}",
                    requestId,
                    driverId,
                    durationMs,
                    metrics.getTrackingWrites());
            return ResponseEntity.ok().build();
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
