package com.cargohub.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "feature.fleet-realtime")
public class FleetRealtimeProperties {

    private boolean enabled;
    private int ttlOnlineSec = 30;
    private int ttlStaleSec = 180;
    private int pollingSuggestedSec = 10;
    private int snapshotCacheSec = 60;
    private int maxDrivers = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTtlOnlineSec() {
        return ttlOnlineSec;
    }

    public void setTtlOnlineSec(int ttlOnlineSec) {
        this.ttlOnlineSec = ttlOnlineSec;
    }

    public int getTtlStaleSec() {
        return ttlStaleSec;
    }

    public void setTtlStaleSec(int ttlStaleSec) {
        this.ttlStaleSec = ttlStaleSec;
    }

    public int getPollingSuggestedSec() {
        return pollingSuggestedSec;
    }

    public void setPollingSuggestedSec(int pollingSuggestedSec) {
        this.pollingSuggestedSec = pollingSuggestedSec;
    }

    public int getSnapshotCacheSec() {
        return snapshotCacheSec;
    }

    public void setSnapshotCacheSec(int snapshotCacheSec) {
        this.snapshotCacheSec = snapshotCacheSec;
    }

    public int getMaxDrivers() {
        return maxDrivers;
    }

    public void setMaxDrivers(int maxDrivers) {
        this.maxDrivers = maxDrivers;
    }
}
