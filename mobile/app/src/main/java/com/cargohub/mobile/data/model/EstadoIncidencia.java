package com.cargohub.mobile.data.model;

public enum EstadoIncidencia {
    ABIERTA("Abierta"),
    EN_REVISION("En revisión"),
    RESUELTA("Resuelta"),
    DESESTIMADA("Desestimada");

    private final String displayName;

    EstadoIncidencia(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
