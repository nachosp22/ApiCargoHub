package com.cargohub.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Secret used to sign JWT tokens (HS256).
     * Must be at least 32 characters.
     */
    private String secret;

    /**
     * Token expiration in milliseconds.
     */
    private long expirationMs;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
