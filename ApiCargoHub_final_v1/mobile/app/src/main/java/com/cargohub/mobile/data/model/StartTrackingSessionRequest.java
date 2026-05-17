package com.cargohub.mobile.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StartTrackingSessionRequest {

    private final Long driverId;
    private final Long porteId;
    private final String status;
    private final String currentPhase;

    public StartTrackingSessionRequest(@NonNull Long driverId,
                                       @Nullable Long porteId,
                                       @NonNull String status,
                                       @NonNull String currentPhase) {
        this.driverId = driverId;
        this.porteId = porteId;
        this.status = status;
        this.currentPhase = currentPhase;
    }

    @NonNull
    public Long getDriverId() {
        return driverId;
    }

    @Nullable
    public Long getPorteId() {
        return porteId;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getCurrentPhase() {
        return currentPhase;
    }
}
