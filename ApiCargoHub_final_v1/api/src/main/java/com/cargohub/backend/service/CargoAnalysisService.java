package com.cargohub.backend.service;

import com.cargohub.backend.dto.CargoAnalysisResponse;
import com.cargohub.backend.config.CargoAnalysisProperties;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CargoAnalysisService {
    private static final Pattern ITEM_COUNT_PATTERN = Pattern.compile("(\\d+)\\s*(palets?|pallets?|pal[eé]s|bultos?|cajas?|paquetes?|ultos?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(kg|kilos?|kilogramos?|tn|ton|tons?|toneladas?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOLUME_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(m3|m\\^3|metros?\\s*cubicos?|metro\\s*cubico)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LENGTH_PATTERN = Pattern.compile("(?:largo(?:\\s+maximo)?|longitud|max(?:imo)?\\s+largo|de\\s+largo|mide)\\D{0,20}(\\d+(?:[.,]\\d+)?)\\s*(m|metros?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WIDTH_PATTERN = Pattern.compile("(?:ancho(?:\\s+maximo)?|anchura|max(?:imo)?\\s+ancho|de\\s+ancho)\\D{0,20}(\\d+(?:[.,]\\d+)?)\\s*(m|metros?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT_PATTERN = Pattern.compile("(?:alto(?:\\s+maximo)?|altura|max(?:imo)?\\s+alto|de\\s+alto)\\D{0,20}(\\d+(?:[.,]\\d+)?)\\s*(m|metros?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIMENSIONS_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(mm|cm|m|metros?)?\\s*[xX]\\s*(\\d+(?:[.,]\\d+)?)\\s*(mm|cm|m|metros?)?\\s*[xX]\\s*(\\d+(?:[.,]\\d+)?)\\s*(mm|cm|m|metros?)", Pattern.CASE_INSENSITIVE);

    private final GeminiCargaService geminiCargaService;
    private final CargoAnalysisProperties cargoAnalysisProperties;

    public CargoAnalysisService(GeminiCargaService geminiCargaService, CargoAnalysisProperties cargoAnalysisProperties) {
        this.geminiCargaService = geminiCargaService;
        this.cargoAnalysisProperties = cargoAnalysisProperties;
    }

    /**
     * Analiza la carga para calcular dimensiones del pedido.
     * @param descripcionCliente La descripción del pedido del cliente
     * @return CargoAnalysisResponse con las dimensiones inferidas
     */
    public CargoAnalysisResponse calcularDimensiones(String descripcionCliente) {
        return calcularDimensiones(descripcionCliente, null);
    }

    /**
     * Calcula dimensiones del pedido usando Gemini directamente.
     * Si Gemini no está configurado o falla, la respuesta se marca para revisión manual
     * dentro del propio flujo de Gemini, sin fallback externo.
     * @param descripcionCliente La descripción del pedido del cliente
     * @param porte Porte opcional para asociar con esta llamada
     * @return CargoAnalysisResponse con las dimensiones calculadas
     */
    public CargoAnalysisResponse calcularDimensiones(String descripcionCliente, Porte porte) {
        log.info("Delegating cargo dimension calculation to Gemini AI");
        CargoAnalysisResponse response = geminiCargaService.calcularDimensiones(descripcionCliente, porte);

        if (!cargoAnalysisProperties.getDevFallback().isEnabled() || !needsFallbackAttempt(response)) {
            return response;
        }

        Optional<CargoAnalysisResponse> fallback = inferirDimensionesDesdeDescripcion(descripcionCliente);
        if (fallback.isPresent()) {
            log.warn("Gemini unavailable or insufficient response. Applying DEV/DEMO heuristic cargo fallback");
            return fallback.get();
        }

        return response;
    }

    private boolean needsFallbackAttempt(CargoAnalysisResponse response) {
        if (response == null) {
            return true;
        }

        return Boolean.TRUE.equals(response.getRevisionManual())
                || response.getPesoTotalKg() == null || response.getPesoTotalKg() <= 0
                || response.getVolumenTotalM3() == null || response.getVolumenTotalM3() <= 0
                || response.getLargoMaxPaquete() == null || response.getLargoMaxPaquete() <= 0;
    }

    private Optional<CargoAnalysisResponse> inferirDimensionesDesdeDescripcion(String descripcionCliente) {
        if (descripcionCliente == null || descripcionCliente.isBlank()) {
            return Optional.empty();
        }

        String normalized = descripcionCliente.toLowerCase(Locale.ROOT);
        ParsedDimensions parsedDimensions = extractDimensions(normalized);
        int itemCount = extractItemCount(normalized);
        double pesoKg = extractWeightKg(normalized);
        double volumenM3 = extractVolumeM3(normalized, parsedDimensions, itemCount);
        double largoM = extractLengthM(normalized, parsedDimensions);
        double anchoM = extractWidthM(normalized, parsedDimensions);
        double altoM = extractHeightM(normalized, parsedDimensions);

        if (pesoKg <= 0 || volumenM3 <= 0 || largoM <= 0) {
            return Optional.empty();
        }

        String tipoVehiculo = resolveVehicleType(normalized, pesoKg, volumenM3, largoM);

        // Inferir ancho/alto faltantes desde el tipo de vehículo
        if (anchoM <= 0) {
            anchoM = inferAnchoFromVehicleType(tipoVehiculo);
        }
        if (altoM <= 0) {
            altoM = inferAltoFromVehicleType(tipoVehiculo);
        }

        CargoAnalysisResponse response = new CargoAnalysisResponse();
        response.setPesoTotalKg(pesoKg);
        response.setVolumenTotalM3(volumenM3);
        response.setLargoMaxPaquete(largoM);
        response.setAnchoMaxPaquete(anchoM);
        response.setAltoMaxPaquete(altoM);
        response.setTipoVehiculoRequerido(tipoVehiculo);
        response.setRevisionManual(false);
        response.setMotivoRevision(null);
        return Optional.of(response);
    }

    private double extractWeightKg(String text) {
        Matcher matcher = WEIGHT_PATTERN.matcher(text);
        double maxWeightKg = 0.0;

        while (matcher.find()) {
            double value = parseNumber(matcher.group(1));
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            if (value <= 0) {
                continue;
            }

            double weightKg = unit.startsWith("kg") || unit.startsWith("kilo") ? value : value * 1000;
            maxWeightKg = Math.max(maxWeightKg, weightKg);
        }

        return round(maxWeightKg);
    }

    private double extractVolumeM3(String text) {
        return extractVolumeM3(text, null, 1);
    }

    private double extractVolumeM3(String text, ParsedDimensions parsedDimensions, int itemCount) {
        Matcher matcher = VOLUME_PATTERN.matcher(text);
        double maxVolume = 0.0;

        while (matcher.find()) {
            double value = parseNumber(matcher.group(1));
            if (value > 0) {
                maxVolume = Math.max(maxVolume, value);
            }
        }

        if (maxVolume > 0) {
            return round(maxVolume);
        }

        ParsedDimensions dimensions = parsedDimensions != null ? parsedDimensions : extractDimensions(text);
        if (dimensions.isPresent()) {
            return round(dimensions.volumeM3() * Math.max(itemCount, 1));
        }

        return 0.0;
    }

    private double extractLengthM(String text) {
        return extractLengthM(text, null);
    }

    private double extractLengthM(String text, ParsedDimensions parsedDimensions) {
        Matcher matcher = LENGTH_PATTERN.matcher(text);
        double maxLength = 0.0;

        while (matcher.find()) {
            double value = parseNumber(matcher.group(1));
            if (value > 0) {
                maxLength = Math.max(maxLength, value);
            }
        }

        if (maxLength > 0) {
            return round(maxLength);
        }

        ParsedDimensions dimensions = parsedDimensions != null ? parsedDimensions : extractDimensions(text);
        if (dimensions.isPresent()) {
            return round(dimensions.maxLengthM());
        }

        return 0.0;
    }

    private double extractWidthM(String text) {
        return extractWidthM(text, null);
    }

    private double extractWidthM(String text, ParsedDimensions parsedDimensions) {
        Matcher matcher = WIDTH_PATTERN.matcher(text);
        double maxWidth = 0.0;

        while (matcher.find()) {
            double value = parseNumber(matcher.group(1));
            if (value > 0) {
                maxWidth = Math.max(maxWidth, value);
            }
        }

        if (maxWidth > 0) {
            return round(maxWidth);
        }

        ParsedDimensions dimensions = parsedDimensions != null ? parsedDimensions : extractDimensions(text);
        if (dimensions.isPresent()) {
            return round(dimensions.middleLengthM());
        }

        return 0.0;
    }

    private double extractHeightM(String text) {
        return extractHeightM(text, null);
    }

    private double extractHeightM(String text, ParsedDimensions parsedDimensions) {
        Matcher matcher = HEIGHT_PATTERN.matcher(text);
        double maxHeight = 0.0;

        while (matcher.find()) {
            double value = parseNumber(matcher.group(1));
            if (value > 0) {
                maxHeight = Math.max(maxHeight, value);
            }
        }

        if (maxHeight > 0) {
            return round(maxHeight);
        }

        ParsedDimensions dimensions = parsedDimensions != null ? parsedDimensions : extractDimensions(text);
        if (dimensions.isPresent()) {
            return round(dimensions.minLengthM());
        }

        return 0.0;
    }

    private int extractItemCount(String text) {
        Matcher matcher = ITEM_COUNT_PATTERN.matcher(text);
        int maxCount = 1;

        while (matcher.find()) {
            try {
                maxCount = Math.max(maxCount, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                // ignore malformed counters and continue with next match
            }
        }

        return maxCount;
    }

    private ParsedDimensions extractDimensions(String text) {
        Matcher matcher = DIMENSIONS_PATTERN.matcher(text);
        if (!matcher.find()) {
            return ParsedDimensions.empty();
        }

        double a = convertToMeters(parseNumber(matcher.group(1)), matcher.group(2));
        double b = convertToMeters(parseNumber(matcher.group(3)), matcher.group(4));
        double c = convertToMeters(parseNumber(matcher.group(5)), matcher.group(6));
        if (a <= 0 || b <= 0 || c <= 0) {
            return ParsedDimensions.empty();
        }

        double max = Math.max(a, Math.max(b, c));
        double min = Math.min(a, Math.min(b, c));
        double middle = a + b + c - max - min;
        return new ParsedDimensions(
                round(a * b * c),
                round(max),
                round(middle),
                round(min)
        );
    }

    private double convertToMeters(double value, String unit) {
        if (value <= 0) {
            return 0.0;
        }

        if (unit == null || unit.isBlank()) {
            return value;
        }

        String normalizedUnit = unit.toLowerCase(Locale.ROOT);
        return switch (normalizedUnit) {
            case "mm" -> value / 1000.0;
            case "cm" -> value / 100.0;
            case "m", "metro", "metros" -> value;
            default -> value;
        };
    }

    private String resolveVehicleType(String text, double pesoKg, double volumenM3, double largoM) {
        if (text.contains("trailer") || text.contains("tracto") || text.contains("semi")) {
            return "TRAILER";
        }
        if (text.contains("rigido") || text.contains("r\u00edgido") || text.contains("camion") || text.contains("cami\u00f3n")) {
            return "RIGIDO";
        }
        if (text.contains("furgoneta") || text.contains("van")) {
            return "FURGONETA";
        }

        if (pesoKg > 8000 || largoM > 7 || volumenM3 > 40) {
            return "TRAILER";
        }
        if (pesoKg >= 1200 || largoM >= 3 || volumenM3 >= 10) {
            return "RIGIDO";
        }
        return "FURGONETA";
    }

    private double parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double inferAnchoFromVehicleType(String tipo) {
        return switch (tipo.toUpperCase()) {
            case "FURGONETA" -> 1.7;
            case "RIGIDO" -> 2.45;
            case "TRAILER" -> 2.45;
            default -> 1.7;
        };
    }

    private double inferAltoFromVehicleType(String tipo) {
        return switch (tipo.toUpperCase()) {
            case "FURGONETA" -> 1.8;
            case "RIGIDO" -> 2.5;
            case "TRAILER" -> 2.7;
            default -> 1.8;
        };
    }

    private record ParsedDimensions(double volumeM3, double maxLengthM, double middleLengthM, double minLengthM) {
        private static ParsedDimensions empty() {
            return new ParsedDimensions(0.0, 0.0, 0.0, 0.0);
        }

        private boolean isPresent() {
            return volumeM3 > 0 && maxLengthM > 0 && middleLengthM > 0 && minLengthM > 0;
        }
    }

    /**
     * Convierte el tipo de vehículo de cadena a enumeración
     */
    public TipoVehiculo convertirTipoVehiculo(String tipoStr) {
        if (tipoStr == null || tipoStr.isEmpty()) {
            return null;
        }
        try {
            return TipoVehiculo.valueOf(tipoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
