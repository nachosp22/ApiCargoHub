package com.cargohub.mobile.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.data.model.Vehiculo;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class UiFormatters {

    private static final DateTimeFormatter INPUT_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter OUTPUT_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", new Locale("es", "ES"));
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

    private UiFormatters() {
    }

    @NonNull
    static String formatPorteTitle(@Nullable Porte porte) {
        if (porte == null) {
            return "Porte";
        }
        Long porteId = porte.getId();
        String origin = valueOrFallback(porte.getOrigen(), "Origen pendiente");
        String destination = valueOrFallback(porte.getDestino(), "Destino pendiente");
        if (porteId == null || porteId <= 0) {
            return origin + " -> " + destination;
        }
        return "Porte #" + porteId + " - " + origin + " -> " + destination;
    }

    @NonNull
    static String formatPorteSchedule(@Nullable Porte porte) {
        if (porte == null) {
            return "Sin horario programado";
        }
        return "Recogida: " + formatDateTime(porte.getFechaRecogida())
                + "\nEntrega: " + formatDateTime(porte.getFechaEntrega());
    }

    @NonNull
    static String formatPorteCargo(@Nullable Porte porte) {
        if (porte == null) {
            return "Mercancia no disponible";
        }
        String cargo = valueOrFallback(porte.getDescripcionMercancia(), "Carga pendiente de sincronizar");
        String coldChain = Boolean.TRUE.equals(porte.getRequiereFrio()) ? "Si" : "No";
        String distance = porte.getDistanciaKm() != null
                ? String.format(Locale.US, "%.1f km", porte.getDistanciaKm())
                : "Distancia pendiente";
        return cargo + "\nRequiere frio: " + coldChain + " | Distancia: " + distance;
    }

    @NonNull
    static String formatPortePrice(@Nullable Porte porte) {
        if (porte == null || porte.getPrecio() == null) {
            return "Precio pendiente";
        }
        return CURRENCY.format(porte.getPrecio());
    }

    @NonNull
    static String formatPorteState(@Nullable Porte porte) {
        return formatPorteState(porte != null ? porte.getEstadoPorte() : null);
    }

    @NonNull
    static String formatPorteState(@Nullable EstadoPorte estado) {
        if (estado == null) {
            return "Estado pendiente";
        }
        switch (estado) {
            case PENDIENTE:
                return "Pendiente";
            case ASIGNADO:
                return "Asignado";
            case EN_TRANSITO:
                return "En transito";
            case ENTREGADO:
                return "Entregado";
            default:
                return estado.name();
        }
    }

    @NonNull
    static String formatProfileSummary(@NonNull ConductorProfileResponse profile) {
        return "Telefono: " + valueOrFallback(profile.getTelefono(), "No disponible")
                + "\nDNI: " + valueOrFallback(profile.getDni(), "No disponible")
                + "\nBase: " + valueOrFallback(profile.getCiudadBase(), "No disponible");
    }

    @NonNull
    static String formatAgendaRange(@Nullable AgendaBloqueo bloqueo) {
        if (bloqueo == null) {
            return "Sin fechas";
        }
        return formatDateTime(bloqueo.getFechaInicio()) + " - " + formatDateTime(bloqueo.getFechaFin());
    }

    @NonNull
    static String formatVehiculoSummary(@Nullable Vehiculo vehiculo) {
        if (vehiculo == null) {
            return "Sin especificaciones";
        }
        String type = vehiculo.getTipo() != null ? vehiculo.getTipo().name() : "Tipo pendiente";
        String status = vehiculo.getEstado() != null ? vehiculo.getEstado().name() : "Estado pendiente";
        String capacity = vehiculo.getCapacidadCargaKg() != null ? vehiculo.getCapacidadCargaKg() + " kg" : "capacidad pendiente";
        return type + " | " + status + "\nCapacidad: " + capacity;
    }

    @NonNull
    static String formatVehiculoDimensions(@Nullable Vehiculo vehiculo) {
        if (vehiculo == null) {
            return "Dimensiones no disponibles";
        }
        String largo = vehiculo.getLargoUtilMm() != null ? vehiculo.getLargoUtilMm() + " mm" : "-";
        String ancho = vehiculo.getAnchoUtilMm() != null ? vehiculo.getAnchoUtilMm() + " mm" : "-";
        String alto = vehiculo.getAltoUtilMm() != null ? vehiculo.getAltoUtilMm() + " mm" : "-";
        String trampilla = vehiculo.isTrampillaElevadora() ? "Con trampilla" : "Sin trampilla";
        return "Largo: " + largo + " | Ancho: " + ancho + " | Alto: " + alto + "\n" + trampilla;
    }

    @NonNull
    static String formatDateTime(@Nullable String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "Pendiente";
        }
        try {
            return LocalDateTime.parse(rawValue.trim(), INPUT_DATE_TIME).format(OUTPUT_DATE_TIME);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(rawValue.trim()).toLocalDateTime().format(OUTPUT_DATE_TIME);
        } catch (Exception ignored) {
            return rawValue.trim();
        }
    }

    @NonNull
    static String valueOrFallback(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
