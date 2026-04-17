package com.cargohub.mobile.data.model;

import androidx.annotation.Nullable;

public class ConductorProfileUpdateRequest {

    @Nullable
    private final String nombre;
    @Nullable
    private final String apellidos;
    @Nullable
    private final String telefono;
    @Nullable
    private final String ciudadBase;

    public ConductorProfileUpdateRequest(@Nullable String nombre,
                                         @Nullable String apellidos,
                                         @Nullable String telefono,
                                         @Nullable String ciudadBase) {
        this.nombre = clean(nombre);
        this.apellidos = clean(apellidos);
        this.telefono = clean(telefono);
        this.ciudadBase = clean(ciudadBase);
    }

    @Nullable
    private String clean(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
