package com.cargohub.mobile.data.model;

public enum EstadoVehiculo {
    DISPONIBLE("Disponible"),
    EN_MANTENIMIENTO("En mantenimiento"),
    BAJA("Baja");

    private final String displayName;

    EstadoVehiculo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
