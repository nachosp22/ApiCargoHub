package com.cargohub.backend.dto.tracking;

import com.cargohub.backend.entity.enums.EstadoPorte;

import java.time.OffsetDateTime;

public class PorteTrackingResponse {

    // Current driver position (null if no live data)
    private Double driverLat;
    private Double driverLng;
    private OffsetDateTime lastUpdate;
    private Double speedKph;
    private Integer headingDeg;

    // Origin / Destination
    private Double originLat;
    private Double originLng;
    private String originName;
    private Double destinationLat;
    private Double destinationLng;
    private String destinationName;

    // ETA
    private Integer etaMinutes;
    private EtaConfidence etaConfidence;

    // Status
    private EstadoPorte status;

    // Driver info
    private String driverName;
    private String vehicleInfo;

    // Getters & Setters

    public Double getDriverLat() { return driverLat; }
    public void setDriverLat(Double driverLat) { this.driverLat = driverLat; }

    public Double getDriverLng() { return driverLng; }
    public void setDriverLng(Double driverLng) { this.driverLng = driverLng; }

    public OffsetDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(OffsetDateTime lastUpdate) { this.lastUpdate = lastUpdate; }

    public Double getSpeedKph() { return speedKph; }
    public void setSpeedKph(Double speedKph) { this.speedKph = speedKph; }

    public Integer getHeadingDeg() { return headingDeg; }
    public void setHeadingDeg(Integer headingDeg) { this.headingDeg = headingDeg; }

    public Double getOriginLat() { return originLat; }
    public void setOriginLat(Double originLat) { this.originLat = originLat; }

    public Double getOriginLng() { return originLng; }
    public void setOriginLng(Double originLng) { this.originLng = originLng; }

    public String getOriginName() { return originName; }
    public void setOriginName(String originName) { this.originName = originName; }

    public Double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(Double destinationLat) { this.destinationLat = destinationLat; }

    public Double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(Double destinationLng) { this.destinationLng = destinationLng; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }

    public EtaConfidence getEtaConfidence() { return etaConfidence; }
    public void setEtaConfidence(EtaConfidence etaConfidence) { this.etaConfidence = etaConfidence; }

    public EstadoPorte getStatus() { return status; }
    public void setStatus(EstadoPorte status) { this.status = status; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getVehicleInfo() { return vehicleInfo; }
    public void setVehicleInfo(String vehicleInfo) { this.vehicleInfo = vehicleInfo; }
}
