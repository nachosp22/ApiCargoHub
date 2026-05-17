package com.cargohub.mobile.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.EstadoVehiculo;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.data.model.TipoVehiculo;
import com.cargohub.mobile.data.model.Vehiculo;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class UiFormatters {

    private static final DateTimeFormatter INPUT_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter OUTPUT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "ES"));
    private static final DateTimeFormatter OUTPUT_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", new Locale("es", "ES"));
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
    private static final NumberFormat DECIMAL = NumberFormat.getNumberInstance(new Locale("es", "ES"));
    private static final String FALLBACK_STATE_PENDING = "Estado pendiente";
    private static final String FALLBACK_ORIGIN_PENDING = "Origen pendiente";
    private static final String FALLBACK_DESTINATION_PENDING = "Destino pendiente";
    private static final String FALLBACK_CARGO_PENDING = "Carga pendiente de sincronizar";
    private static final String FALLBACK_VEHICLE_TYPE_PENDING = "Tipo pendiente";
    private static final String FALLBACK_PRICE_PENDING = "Precio pendiente";
    private static final String FALLBACK_DISTANCE_PENDING = "Distancia pendiente";
    private static final String FALLBACK_NOT_AVAILABLE = "No disponible";
    private static final String FALLBACK_PENDING = "Pendiente";

    private UiFormatters() {
    }

    @NonNull
    static String formatPorteTitle(@Nullable Porte porte) {
        if (porte == null) {
            return "Porte";
        }
        Long porteId = porte.getId();
        String origin = valueOrFallback(porte.getOrigen(), FALLBACK_ORIGIN_PENDING);
        String destination = valueOrFallback(porte.getDestino(), FALLBACK_DESTINATION_PENDING);
        if (porteId == null || porteId <= 0) {
            return origin + " -> " + destination;
        }
        return "Porte #" + porteId + " - " + origin + " -> " + destination;
    }

    @NonNull
    static String formatPorteShortTitle(@Nullable Porte porte) {
        if (porte == null || porte.getId() == null || porte.getId() <= 0) {
            return "Porte";
        }
        return "Porte #" + porte.getId();
    }

    @NonNull
    static String formatPorteRoute(@Nullable Porte porte) {
        if (porte == null) {
            return FALLBACK_ORIGIN_PENDING + " -> " + FALLBACK_DESTINATION_PENDING;
        }
        return valueOrFallback(porte.getOrigen(), FALLBACK_ORIGIN_PENDING)
                + " -> "
                + valueOrFallback(porte.getDestino(), FALLBACK_DESTINATION_PENDING);
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
            return "Mercancía no disponible";
        }
        String cargo = valueOrFallback(porte.getDescripcionMercancia(), FALLBACK_CARGO_PENDING);
        StringBuilder cargoDetails = new StringBuilder(cargo);

        String weight = formatMeasure(porte.getPesoTotalKg(), "kg");
        if (weight != null) {
            cargoDetails.append("\nPeso: ").append(weight);
        }

        String volume = formatMeasure(porte.getVolumenTotalM3(), "m3");
        if (volume != null) {
            cargoDetails.append(" | Volumen: ").append(volume);
        }

        String dimensions = formatDimensions(porte);
        if (dimensions != null) {
            cargoDetails.append("\nDimensiones max.: ").append(dimensions);
        }

        String requiredVehicle = normalizeText(porte.getTipoVehiculoRequerido());
        if (requiredVehicle != null) {
            cargoDetails.append("\nVehículo requerido: ").append(formatVehicleRequirementLabel(requiredVehicle));
        }

        String distance = porte.getDistanciaKm() != null
                ? String.format(Locale.US, "%.1f km", porte.getDistanciaKm())
                : FALLBACK_DISTANCE_PENDING;
        cargoDetails.append("\nDistancia: ").append(distance);
        return cargoDetails.toString();
    }

    @NonNull
    static String formatPortePrice(@Nullable Porte porte) {
        if (porte == null || porte.getPrecio() == null) {
            return FALLBACK_PRICE_PENDING;
        }
        return CURRENCY.format(porte.getPrecio());
    }

    @NonNull
    static String formatPorteDistance(@Nullable Porte porte) {
        if (porte == null || porte.getDistanciaKm() == null) {
            return FALLBACK_DISTANCE_PENDING;
        }
        return formatDecimal(porte.getDistanciaKm()) + " km";
    }

    @NonNull
    static String formatPorteWeight(@Nullable Porte porte) {
        if (porte == null || porte.getPesoTotalKg() == null) {
            return FALLBACK_NOT_AVAILABLE;
        }
        return formatDecimal(porte.getPesoTotalKg()) + " kg";
    }

    @NonNull
    static String formatPorteVolume(@Nullable Porte porte) {
        if (porte == null || porte.getVolumenTotalM3() == null) {
            return FALLBACK_NOT_AVAILABLE;
        }
        return formatDecimal(porte.getVolumenTotalM3()) + " m³";
    }

    @NonNull
    static String formatPorteDimensions(@Nullable Porte porte) {
        if (porte == null) {
            return FALLBACK_NOT_AVAILABLE;
        }
        StringBuilder text = new StringBuilder();
        appendDimensionValue(text, "L", porte.getLargoMaxPaquete());
        appendDimensionValue(text, "A", porte.getAnchoMaxPaquete());
        appendDimensionValue(text, "A", porte.getAltoMaxPaquete());
        return text.length() > 0 ? text.toString() : FALLBACK_NOT_AVAILABLE;
    }

    @NonNull
    static String formatPorteVehicleRequirement(@Nullable Porte porte) {
        if (porte == null) {
            return FALLBACK_VEHICLE_TYPE_PENDING;
        }
        return formatVehicleRequirementLabel(porte.getTipoVehiculoRequerido());
    }

    @NonNull
    static String formatBooleanYesNo(@Nullable Boolean value) {
        return Boolean.TRUE.equals(value) ? "Sí" : "No";
    }

    @NonNull
    static String formatPorteState(@Nullable Porte porte) {
        if (porte == null) {
            return FALLBACK_STATE_PENDING;
        }
        EstadoPorte estadoPorte = porte.getEstadoPorte();
        if (estadoPorte != null) {
            return formatPorteState(estadoPorte);
        }
        return formatRawPorteState(porte.getEstado());
    }

    @NonNull
    static String formatPorteState(@Nullable EstadoPorte estado) {
        if (estado == null) {
            return FALLBACK_STATE_PENDING;
        }
        switch (estado) {
            case PENDIENTE:
                return "Pendiente";
            case ASIGNADO:
                return "Asignado";
            case EN_RECOGIDA:
                return "En recogida";
            case EN_TRANSITO:
                return "En tránsito";
            case ENTREGADO:
                return "Entregado";
            case FACTURADO:
                return "Facturado";
            default:
                return humanizeTechnicalLabel(estado.name());
        }
    }

    @NonNull
    static String formatRawPorteState(@Nullable String rawState) {
        if (rawState == null || rawState.trim().isEmpty()) {
            return FALLBACK_STATE_PENDING;
        }
        switch (rawState.trim().toUpperCase(Locale.ROOT)) {
            case "PENDIENTE":
                return "Pendiente";
            case "ASIGNADO":
                return "Asignado";
            case "EN_RECOGIDA":
            case "EN RECOGIDA":
                return "En recogida";
            case "EN_TRANSITO":
            case "EN TRANSITO":
                return "En tránsito";
            case "ENTREGADO":
                return "Entregado";
            case "FACTURADO":
                return "Facturado";
            case "CANCELADO":
                return "Cancelado";
            default:
                return humanizeTechnicalLabel(rawState);
        }
    }

    @NonNull
    static String formatTrackingStatusLabel(@Nullable String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return "Sin tracking";
        }
        switch (rawStatus.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE":
                return "En seguimiento";
            case "PAUSED":
                return "Pausado";
            case "ENDED":
                return "Finalizado";
            default:
                return humanizeTechnicalLabel(rawStatus);
        }
    }

    @NonNull
    static String formatTrackingPhaseLabel(@Nullable String rawPhase) {
        if (rawPhase == null || rawPhase.trim().isEmpty()) {
            return "Fase no disponible";
        }
        switch (rawPhase.trim().toUpperCase(Locale.ROOT)) {
            case "TO_PICKUP":
                return "Yendo al punto de recogida";
            case "TO_DROPOFF":
                return "Yendo al punto de entrega";
            case "IDLE":
                return "Sin ruta activa";
            default:
                return humanizeTechnicalLabel(rawPhase);
        }
    }

    @NonNull
    static String formatVehicleRequirementLabel(@Nullable String rawVehicleType) {
        if (rawVehicleType == null || rawVehicleType.trim().isEmpty()) {
            return FALLBACK_VEHICLE_TYPE_PENDING;
        }
        switch (rawVehicleType.trim().toUpperCase(Locale.ROOT)) {
            case "FURGONETA":
            case "FURGON":
                return "Furgoneta";
            case "RIGIDO":
                return "Camión rígido";
            case "TRAILER":
                return "Tráiler";
            case "FRIGORIFICO":
            case "FURGON_FRIGORIFICO":
                return "Frigorífico";
            default:
                return humanizeTechnicalLabel(rawVehicleType);
        }
    }

    @NonNull
    static String formatIncidenciaRawLabel(@Nullable String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "-";
        }
        return humanizeTechnicalLabel(rawValue);
    }

    @NonNull
    static String formatProfileSummary(@NonNull ConductorProfileResponse profile) {
        return "Teléfono: " + valueOrFallback(profile.getTelefono(), FALLBACK_NOT_AVAILABLE)
                + "\nDNI: " + valueOrFallback(profile.getDni(), FALLBACK_NOT_AVAILABLE)
                + "\nBase: " + valueOrFallback(profile.getCiudadBase(), FALLBACK_NOT_AVAILABLE);
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
        String type = vehiculo.getTipo() != null ? vehiculo.getTipo().getDisplayName() : FALLBACK_VEHICLE_TYPE_PENDING;
        String status = vehiculo.getEstado() != null ? vehiculo.getEstado().getDisplayName() : FALLBACK_STATE_PENDING;
        String capacity = vehiculo.getCapacidadCargaKg() != null ? vehiculo.getCapacidadCargaKg() + " kg" : "capacidad pendiente";
        return type + " | " + status + "\nCapacidad: " + capacity;
    }

    @NonNull
    static String formatVehiculoTipo(@Nullable TipoVehiculo tipoVehiculo) {
        return tipoVehiculo != null ? tipoVehiculo.getDisplayName() : FALLBACK_VEHICLE_TYPE_PENDING;
    }

    @NonNull
    static String formatVehiculoEstado(@Nullable EstadoVehiculo estadoVehiculo) {
        return estadoVehiculo != null ? estadoVehiculo.getDisplayName() : FALLBACK_STATE_PENDING;
    }

    @NonNull
    static String formatVehiculoDimensions(@Nullable Vehiculo vehiculo) {
        if (vehiculo == null) {
            return "Dimensiones no disponibles";
        }
        String largo = vehiculo.getLargoUtilMm() != null ? vehiculo.getLargoUtilMm() + " mm" : "-";
        String ancho = vehiculo.getAnchoUtilMm() != null ? vehiculo.getAnchoUtilMm() + " mm" : "-";
        String alto = vehiculo.getAltoUtilMm() != null ? vehiculo.getAltoUtilMm() + " mm" : "-";
        return "Largo: " + largo + " | Ancho: " + ancho + " | Alto: " + alto;
    }

    @NonNull
    static String formatDateTime(@Nullable String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return FALLBACK_PENDING;
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

    @NonNull
    static String formatDateOnly(@Nullable String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return FALLBACK_PENDING;
        }
        try {
            return LocalDate.parse(rawValue.trim()).format(OUTPUT_DATE);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(rawValue.trim(), INPUT_DATE_TIME).toLocalDate().format(OUTPUT_DATE);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(rawValue.trim()).toLocalDate().format(OUTPUT_DATE);
        } catch (Exception ignored) {
            return rawValue.trim();
        }
    }

    @NonNull
    static String normalizeDateFilterValue(@Nullable String rawValue) {
        String dateOnly = formatDateOnly(rawValue);
        return dateOnly.replace('-', '/').trim();
    }

    @Nullable
    private static String formatMeasure(@Nullable Double value, @NonNull String unit) {
        if (value == null || value <= 0d) {
            return null;
        }
        return String.format(Locale.US, "%.1f %s", value, unit);
    }

    @NonNull
    private static String formatDecimal(@NonNull Double value) {
        DECIMAL.setMinimumFractionDigits(value % 1d == 0d ? 0 : 1);
        DECIMAL.setMaximumFractionDigits(2);
        return DECIMAL.format(value);
    }

    @Nullable
    private static String formatDimensions(@NonNull Porte porte) {
        StringBuilder text = new StringBuilder();
        appendDimension(text, "L", porte.getLargoMaxPaquete());
        appendDimension(text, "A", porte.getAnchoMaxPaquete());
        appendDimension(text, "H", porte.getAltoMaxPaquete());
        return text.length() > 0 ? text.toString() : null;
    }

    private static void appendDimension(@NonNull StringBuilder text, @NonNull String label, @Nullable Double value) {
        if (value == null || value <= 0d) {
            return;
        }
        if (text.length() > 0) {
            text.append(" x ");
        }
        text.append(label).append(" ").append(String.format(Locale.US, "%.1f m", value));
    }

    private static void appendDimensionValue(@NonNull StringBuilder text, @NonNull String label, @Nullable Double value) {
        if (value == null || value <= 0d) {
            return;
        }
        if (text.length() > 0) {
            text.append(" x ");
        }
        text.append(label).append(" ").append(formatDecimal(value)).append(" m");
    }

    @Nullable
    private static String normalizeText(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    @NonNull
    private static String humanizeTechnicalLabel(@NonNull String rawValue) {
        String normalized = rawValue.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "-";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
