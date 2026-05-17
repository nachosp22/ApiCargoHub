package com.cargohub.mobile.data.model;

public enum TipoVehiculo {
    FURGONETA("Furgoneta"),
    RIGIDO("Rígido"),
    TRAILER("Tráiler"),
    ESPECIAL("Especial");

    private final String displayName;

    TipoVehiculo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
