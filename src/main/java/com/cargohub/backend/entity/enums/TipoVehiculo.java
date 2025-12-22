package com.cargohub.backend.entity.enums;

import lombok.Getter;

@Getter
public enum TipoVehiculo {

    // --- LIGEROS ---
    FURGONETA("Furgoneta", false),

    // --- RÍGIDOS ---
    RIGIDO("Rígido)", false),
    RIGIDO_FRIGO("Rígido Frigorífico", true),

    // --- TRÁILERS ---
    TRAILER("Tráiler)", false),
    TRAILER_FRIGO("Tráiler Frigorífico", true),
    TRAILER_MEGA("Megatráiler (Gran Volumen)", false),

    // --- ESPECIALES ---
    ESPECIAL("Transporte Especial / Otros", false);

    private final String descripcion;
    private final boolean refrigerado;

    TipoVehiculo(String descripcion, boolean refrigerado) {
        this.descripcion = descripcion;
        this.refrigerado = refrigerado;
    }
}
