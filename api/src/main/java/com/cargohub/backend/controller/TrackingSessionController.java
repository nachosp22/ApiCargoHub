package com.cargohub.backend.controller;

import com.cargohub.backend.dto.tracking.StartTrackingSessionRequest;
import com.cargohub.backend.dto.tracking.TrackingPauseRequest;
import com.cargohub.backend.dto.tracking.TrackingPauseResponse;
import com.cargohub.backend.dto.tracking.TrackingSessionResponse;
import com.cargohub.backend.dto.tracking.UpdateTrackingSessionRequest;
import com.cargohub.backend.service.TrackingSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracking/sessions")
public class TrackingSessionController {

    private final TrackingSessionService trackingSessionService;

    public TrackingSessionController(TrackingSessionService trackingSessionService) {
        this.trackingSessionService = trackingSessionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or @ownership.canAccessConductor(authentication, #request.driverId)")
    public ResponseEntity<TrackingSessionResponse> startSession(@Valid @RequestBody StartTrackingSessionRequest request) {
        return ResponseEntity.ok(trackingSessionService.startSession(request));
    }

    @PatchMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or @ownership.canAccessTrackingSession(authentication, #sessionId)")
    public ResponseEntity<TrackingSessionResponse> updateSession(@PathVariable Long sessionId,
                                                                 @RequestBody UpdateTrackingSessionRequest request) {
        return ResponseEntity.ok(trackingSessionService.updateSession(sessionId, request));
    }

    @PostMapping("/{sessionId}/pausas")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or @ownership.canAccessTrackingSession(authentication, #sessionId)")
    public ResponseEntity<TrackingPauseResponse> recordPause(@PathVariable Long sessionId,
                                                             @Valid @RequestBody TrackingPauseRequest request) {
        return ResponseEntity.ok(trackingSessionService.recordPause(sessionId, request));
    }
}
