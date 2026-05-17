package com.cargohub.backend.dto.tracking;

import com.cargohub.backend.entity.enums.TrackingSessionPhase;
import com.cargohub.backend.entity.enums.TrackingSessionStatus;
import java.time.OffsetDateTime;

public class TrackingSessionResponse {

    private Long id;
    private Long driverId;
    private Long porteId;
    private TrackingSessionStatus status;
    private TrackingSessionPhase currentPhase;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime lastSampleAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public OffsetDateTime getLastSampleAt() {
        return lastSampleAt;
    }

    public void setLastSampleAt(OffsetDateTime lastSampleAt) {
        this.lastSampleAt = lastSampleAt;
    }
}
