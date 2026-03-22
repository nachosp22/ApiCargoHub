package com.cargohub.backend.dto.tracking;

import java.time.OffsetDateTime;

public class EtaEstimateResponse {

    private Integer etaMinutes;
    private EtaMethod method;
    private OffsetDateTime estimatedAt;
    private EtaConfidence confidence;

    public Integer getEtaMinutes() {
        return etaMinutes;
    }

    public void setEtaMinutes(Integer etaMinutes) {
        this.etaMinutes = etaMinutes;
    }

    public EtaMethod getMethod() {
        return method;
    }

    public void setMethod(EtaMethod method) {
        this.method = method;
    }

    public OffsetDateTime getEstimatedAt() {
        return estimatedAt;
    }

    public void setEstimatedAt(OffsetDateTime estimatedAt) {
        this.estimatedAt = estimatedAt;
    }

    public EtaConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(EtaConfidence confidence) {
        this.confidence = confidence;
    }
}
