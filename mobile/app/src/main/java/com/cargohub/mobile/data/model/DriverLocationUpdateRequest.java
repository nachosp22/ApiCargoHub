package com.cargohub.mobile.data.model;

public class DriverLocationUpdateRequest {

    private final Double lat;
    private final Double lon;
    private final String recordedAt;
    private final Double speedKph;
    private final Integer headingDeg;

    public DriverLocationUpdateRequest(Double lat,
                                       Double lon,
                                       String recordedAt,
                                       Double speedKph,
                                       Integer headingDeg) {
        this.lat = lat;
        this.lon = lon;
        this.recordedAt = recordedAt;
        this.speedKph = speedKph;
        this.headingDeg = headingDeg;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public Double getSpeedKph() {
        return speedKph;
    }

    public Integer getHeadingDeg() {
        return headingDeg;
    }
}
