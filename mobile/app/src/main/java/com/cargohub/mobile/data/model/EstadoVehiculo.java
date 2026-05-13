package com.cargohub.mobile.data.model;

public enum EstadoVehiculo {
    DISPONIBLE("Disponible"),
    BAJA("Baja"),
    MANTENIMIENTO("Mantenimiento");

    private final String displayName;

    EstadoVehiculo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
