package com.cargohub.backend.dto.tracking;

public class FleetSnapshotMeta {

    private Integer pollingSuggestedSec;
    private boolean degraded;
    private String degradedReason;

    public Integer getPollingSuggestedSec() {
        return pollingSuggestedSec;
    }

    public void setPollingSuggestedSec(Integer pollingSuggestedSec) {
        this.pollingSuggestedSec = pollingSuggestedSec;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public String getDegradedReason() {
        return degradedReason;
    }

    public void setDegradedReason(String degradedReason) {
        this.degradedReason = degradedReason;
    }
}
