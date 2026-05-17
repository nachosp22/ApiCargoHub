package com.cargohub.mobile.data.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

public enum TipoBloqueoAgenda {
    @SerializedName("VACACIONES")
    VACACIONES("Vacaciones"),

    @SerializedName("BAJA_MEDICA")
    BAJA_MEDICA("Baja médica"),

    @SerializedName("ASUNTOS_PROPIOS")
    ASUNTOS_PROPIOS("Asuntos propios"),

    @SerializedName(value = "DESCANSO_SEMANAL", alternate = {"DESCANSO"})
    DESCANSO_SEMANAL("Descanso semanal"),

    @SerializedName(value = "OTROS", alternate = {"OTRO"})
    OTROS("Otros");

    private final String displayName;

    TipoBloqueoAgenda(@NonNull String displayName) {
        this.displayName = displayName;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public static TipoBloqueoAgenda fromRawValue(String rawValue) {
        if (rawValue == null) {
            return OTROS;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return OTROS;
        }
        switch (normalized) {
            case "DESCANSO":
                return DESCANSO_SEMANAL;
            case "OTRO":
                return OTROS;
            default:
                try {
                    return TipoBloqueoAgenda.valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    return OTROS;
                }
        }
    }
}
