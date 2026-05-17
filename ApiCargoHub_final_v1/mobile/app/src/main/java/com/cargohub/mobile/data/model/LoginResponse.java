package com.cargohub.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    private String token;

    private String rol;

    private Long id;

    private Long conductorId;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("expiresAt")
    private String expiresAt;

    @SerializedName("expiresIn")
    private Long expiresIn;

    private String email;

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

    public String getExpiresAt() {
        return expiresAt;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public String getEmail() {
        return email;
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

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConductorId(Long conductorId) {
        this.conductorId = conductorId;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
