package com.cargohub.backend.dto.tracking;

import com.cargohub.backend.entity.enums.TrackingSessionPhase;
import com.cargohub.backend.entity.enums.TrackingSessionStatus;
import java.time.OffsetDateTime;

public class UpdateTrackingSessionRequest {

    private TrackingSessionStatus status;

    private TrackingSessionPhase currentPhase;

    private OffsetDateTime endedAt;

    private Long porteId;

    public TrackingSessionStatus getStatus() {
        return status;
    }

    public void setStatus(TrackingSessionStatus status) {
        this.status = status;
    }

    public TrackingSessionPhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(TrackingSessionPhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Long getPorteId() {
        return porteId;
    }

    public void setPorteId(Long porteId) {
        this.porteId = porteId;
    }
}
