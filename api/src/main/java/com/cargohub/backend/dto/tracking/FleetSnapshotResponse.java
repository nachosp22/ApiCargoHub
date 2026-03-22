package com.cargohub.backend.dto.tracking;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class FleetSnapshotResponse {

    private OffsetDateTime snapshotAt;
    private List<DriverLocationPoint> drivers = new ArrayList<>();
    private FleetSnapshotMeta meta;

    public OffsetDateTime getSnapshotAt() {
        return snapshotAt;
    }

    public void setSnapshotAt(OffsetDateTime snapshotAt) {
        this.snapshotAt = snapshotAt;
    }

    public List<DriverLocationPoint> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<DriverLocationPoint> drivers) {
        this.drivers = drivers;
    }

    public FleetSnapshotMeta getMeta() {
        return meta;
    }

    public void setMeta(FleetSnapshotMeta meta) {
        this.meta = meta;
    }
}
