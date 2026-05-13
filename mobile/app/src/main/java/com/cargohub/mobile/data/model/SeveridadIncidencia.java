package com.cargohub.mobile.data.model;

public enum SeveridadIncidencia {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta");

    private final String displayName;

    SeveridadIncidencia(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
