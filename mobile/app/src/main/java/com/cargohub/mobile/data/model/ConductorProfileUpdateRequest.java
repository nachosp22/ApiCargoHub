package com.cargohub.mobile.data.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class ConductorProfileUpdateRequest {

    @Nullable
    @SerializedName("nombre")
    private String nombre;
    @Nullable
    @SerializedName("apellidos")
    private String apellidos;
    @Nullable
    @SerializedName("telefono")
    private String telefono;
    @Nullable
    @SerializedName("dni")
    private String dni;
    @Nullable
    @SerializedName("ciudadBase")
    private String ciudadBase;
    @Nullable
    @SerializedName("radioAccionKm")
    private Integer radioAccionKm;

    public ConductorProfileUpdateRequest(@Nullable String nombre,
                                         @Nullable String apellidos,
                                         @Nullable String telefono,
                                         @Nullable String dni,
                                         @Nullable String ciudadBase,
                                         @Nullable Integer radioAccionKm) {
        this.nombre = clean(nombre);
        this.apellidos = clean(apellidos);
        this.telefono = clean(telefono);
        this.dni = clean(dni);
        this.ciudadBase = clean(ciudadBase);
        this.radioAccionKm = radioAccionKm;
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
