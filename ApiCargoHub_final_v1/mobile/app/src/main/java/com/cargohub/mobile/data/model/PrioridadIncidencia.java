package com.cargohub.mobile.data.model;

public enum PrioridadIncidencia {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta");

    private final String displayName;

    PrioridadIncidencia(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
