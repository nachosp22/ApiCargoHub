package com.cargohub.backend.dto.tracking;

import java.time.OffsetDateTime;

public class DriverLocationPoint {

    private String driverId;
    private Double lat;
    private Double lon;
    private OffsetDateTime recordedAt;
    private Double speedKph;
    private Integer headingDeg;
    private DriverState state;
    private String driverName;
    private String driverLastName;
    private Long activePorteId;
    private String activePorteDestination;
    private String activePorteStatus;

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Double getSpeedKph() {
        return speedKph;
    }

    public void setSpeedKph(Double speedKph) {
        this.speedKph = speedKph;
    }

    public Integer getHeadingDeg() {
        return headingDeg;
    }

    public void setHeadingDeg(Integer headingDeg) {
        this.headingDeg = headingDeg;
    }

    public DriverState getState() {
        return state;
    }

    public void setState(DriverState state) {
        this.state = state;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverLastName() {
        return driverLastName;
    }

    public void setDriverLastName(String driverLastName) {
        this.driverLastName = driverLastName;
    }

    public Long getActivePorteId() {
        return activePorteId;
    }

    public void setActivePorteId(Long activePorteId) {
        this.activePorteId = activePorteId;
    }

    public String getActivePorteDestination() {
        return activePorteDestination;
    }

    public void setActivePorteDestination(String activePorteDestination) {
        this.activePorteDestination = activePorteDestination;
    }

    public String getActivePorteStatus() {
        return activePorteStatus;
    }

    public void setActivePorteStatus(String activePorteStatus) {
        this.activePorteStatus = activePorteStatus;
    }
}
