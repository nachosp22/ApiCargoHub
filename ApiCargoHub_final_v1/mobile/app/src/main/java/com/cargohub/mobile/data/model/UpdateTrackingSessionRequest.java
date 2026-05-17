package com.cargohub.mobile.data.model;

import androidx.annotation.NonNull;

public class UpdateTrackingSessionRequest {

    private final String status;
    private final String currentPhase;

    public UpdateTrackingSessionRequest(@NonNull String status,
                                        @NonNull String currentPhase) {
        this.status = status;
        this.currentPhase = currentPhase;
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
