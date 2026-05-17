package com.cargohub.mobile.data.model;

import androidx.annotation.Nullable;

public class TrackingSessionResponse {

    private Long id;
    private String status;
    @com.google.gson.annotations.SerializedName("currentPhase")
    private String phase;
    private Long porteId;
    private String startedAt;
    private String updatedAt;
    private String endedAt;

    @Nullable
    public Long getId() {
        return id;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    @Nullable
    public String getPhase() {
        return phase;
    }

    @Nullable
    public Long getPorteId() {
        return porteId;
    }

    @Nullable
    public String getStartedAt() {
        return startedAt;
    }

    @Nullable
    public String getUpdatedAt() {
        return updatedAt;
    }

    @Nullable
    public String getEndedAt() {
        return endedAt;
    }
}
