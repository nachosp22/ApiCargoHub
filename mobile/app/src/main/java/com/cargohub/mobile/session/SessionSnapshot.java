package com.cargohub.mobile.session;

import androidx.annotation.Nullable;

import com.cargohub.mobile.data.model.LoginResponse;

import java.time.Instant;

public final class SessionSnapshot {

    private final String accessToken;
    private final String role;
    private final Long conductorId;
    private final Long expiresAtEpochMs;

    public SessionSnapshot(@Nullable String accessToken,
                           @Nullable String role,
                           @Nullable Long conductorId,
                           @Nullable Long expiresAtEpochMs) {
        this.accessToken = accessToken;
        this.role = role;
        this.conductorId = conductorId;
        this.expiresAtEpochMs = expiresAtEpochMs;
    }

    public static SessionSnapshot fromLoginResponse(LoginResponse loginResponse) {
        Long conductorId = loginResponse.getConductorId() != null
                ? loginResponse.getConductorId()
                : loginResponse.getId();
        return new SessionSnapshot(
                loginResponse.getToken(),
                loginResponse.getRol(),
                conductorId,
                resolveExpiresAtEpochMs(loginResponse)
        );
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getRole() {
        return role;
    }

    @Nullable
    public Long getConductorId() {
        return conductorId;
    }

    @Nullable
    public Long getExpiresAtEpochMs() {
        return expiresAtEpochMs;
    }

    public boolean isActive(long nowEpochMs) {
        return accessToken != null
                && !accessToken.trim().isEmpty()
                && (expiresAtEpochMs == null || expiresAtEpochMs > nowEpochMs);
    }

    @Nullable
    private static Long resolveExpiresAtEpochMs(LoginResponse loginResponse) {
        String expiresAt = loginResponse.getExpiresAt();
        if (expiresAt != null && !expiresAt.trim().isEmpty()) {
            try {
                return Instant.parse(expiresAt.trim()).toEpochMilli();
            } catch (Exception ignored) {
            }
        }
        Long expiresIn = loginResponse.getExpiresIn();
        if (expiresIn != null && expiresIn > 0) {
            return System.currentTimeMillis() + (expiresIn * 1000L);
        }
        return null;
    }
}
