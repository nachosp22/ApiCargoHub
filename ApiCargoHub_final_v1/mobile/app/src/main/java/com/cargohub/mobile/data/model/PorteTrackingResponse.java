package com.cargohub.mobile.data.model;

import androidx.annotation.Nullable;

public class PorteTrackingResponse {

    @Nullable
    private Double driverLat;
    @Nullable
    private Double driverLng;
    @Nullable
    private Double speedKph;
    @Nullable
    private Integer headingDeg;
    @Nullable
    private Double originLat;
    @Nullable
    private Double originLng;
    @Nullable
    private String originName;
    @Nullable
    private Double destinationLat;
    @Nullable
    private Double destinationLng;
    @Nullable
    private String destinationName;
    @Nullable
    private Integer etaMinutes;
    @Nullable
    private String etaConfidence;
    @Nullable
    private String status;
    @Nullable
    private String driverName;
    @Nullable
    private String vehicleInfo;

    @Nullable
    public Double getDriverLat() { return driverLat; }
    public void setDriverLat(@Nullable Double driverLat) { this.driverLat = driverLat; }

    @Nullable
    public Double getDriverLng() { return driverLng; }
    public void setDriverLng(@Nullable Double driverLng) { this.driverLng = driverLng; }

    @Nullable
    public Double getSpeedKph() { return speedKph; }
    public void setSpeedKph(@Nullable Double speedKph) { this.speedKph = speedKph; }

    @Nullable
    public Integer getHeadingDeg() { return headingDeg; }
    public void setHeadingDeg(@Nullable Integer headingDeg) { this.headingDeg = headingDeg; }

    @Nullable
    public Double getOriginLat() { return originLat; }
    public void setOriginLat(@Nullable Double originLat) { this.originLat = originLat; }

    @Nullable
    public Double getOriginLng() { return originLng; }
    public void setOriginLng(@Nullable Double originLng) { this.originLng = originLng; }

    @Nullable
    public String getOriginName() { return originName; }
    public void setOriginName(@Nullable String originName) { this.originName = originName; }

    @Nullable
    public Double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(@Nullable Double destinationLat) { this.destinationLat = destinationLat; }

    @Nullable
    public Double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(@Nullable Double destinationLng) { this.destinationLng = destinationLng; }

    @Nullable
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(@Nullable String destinationName) { this.destinationName = destinationName; }

    @Nullable
    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(@Nullable Integer etaMinutes) { this.etaMinutes = etaMinutes; }

    @Nullable
    public String getEtaConfidence() { return etaConfidence; }
    public void setEtaConfidence(@Nullable String etaConfidence) { this.etaConfidence = etaConfidence; }

    @Nullable
    public String getStatus() { return status; }
    public void setStatus(@Nullable String status) { this.status = status; }

    @Nullable
    public String getDriverName() { return driverName; }
    public void setDriverName(@Nullable String driverName) { this.driverName = driverName; }

    @Nullable
    public String getVehicleInfo() { return vehicleInfo; }
    public void setVehicleInfo(@Nullable String vehicleInfo) { this.vehicleInfo = vehicleInfo; }
}
