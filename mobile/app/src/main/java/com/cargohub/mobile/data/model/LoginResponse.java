package com.cargohub.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    private String token;

    private String rol;

    private Long id;

    private Long conductorId;

    @SerializedName("accessToken")
    private String accessToken;

    private String refreshToken;

    public String getToken() {
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            return accessToken;
        }
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getRol() {
        return rol;
    }

    public Long getId() {
        return id;
    }

    public Long getConductorId() {
        return conductorId;
    }
}
