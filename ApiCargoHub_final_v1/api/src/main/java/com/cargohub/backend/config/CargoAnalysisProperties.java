package com.cargohub.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cargo.analysis")
public class CargoAnalysisProperties {

    private final DevFallback devFallback = new DevFallback();

    public DevFallback getDevFallback() {
        return devFallback;
    }

    public static class DevFallback {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
