package com.cargohub.backend.dto.tracking;

import com.cargohub.backend.entity.enums.TrackingSessionPhase;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class StartTrackingSessionRequest {

    @NotNull
    private Long driverId;

    private Long porteId;

    private TrackingSessionPhase currentPhase;

    private OffsetDateTime startedAt;

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Long getPorteId() {
        return porteId;
    }

    public void setPorteId(Long porteId) {
        this.porteId = porteId;
    }

    public TrackingSessionPhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(TrackingSessionPhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }
}
