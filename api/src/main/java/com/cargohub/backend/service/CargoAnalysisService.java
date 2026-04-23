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
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(kg|kilos?|kilogramos?|tn|ton|tons?|toneladas?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOLUME_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(m3|m\\^3|metros?\\s*cubicos?|metro\\s*cubico)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LENGTH_PATTERN = Pattern.compile("(?:largo(?:\\s+maximo)?|longitud|max(?:imo)?\\s+largo|de\\s+largo|mide)\\D{0,20}(\\d+(?:[.,]\\d+)?)\\s*(m|metros?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIMENSIONS_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*[xX]\\s*(\\d+(?:[.,]\\d+)?)\\s*[xX]\\s*(\\d+(?:[.,]\\d+)?)\\s*(m|metros?)", Pattern.CASE_INSENSITIVE);

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
        double pesoKg = extractWeightKg(normalized);
        double volumenM3 = extractVolumeM3(normalized);
        double largoM = extractLengthM(normalized);

        if (pesoKg <= 0 || volumenM3 <= 0 || largoM <= 0) {
            return Optional.empty();
        }

        CargoAnalysisResponse response = new CargoAnalysisResponse();
        response.setPesoTotalKg(pesoKg);
        response.setVolumenTotalM3(volumenM3);
        response.setLargoMaxPaquete(largoM);
        response.setAnchoMaxPaquete(0.0);
        response.setAltoMaxPaquete(0.0);
        response.setTipoVehiculoRequerido(resolveVehicleType(normalized, pesoKg, volumenM3, largoM));
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

        Matcher dimensionsMatcher = DIMENSIONS_PATTERN.matcher(text);
        if (dimensionsMatcher.find()) {
            double a = parseNumber(dimensionsMatcher.group(1));
            double b = parseNumber(dimensionsMatcher.group(2));
            double c = parseNumber(dimensionsMatcher.group(3));
            if (a > 0 && b > 0 && c > 0) {
                return round(a * b * c);
            }
        }

        return 0.0;
    }

    private double extractLengthM(String text) {
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

        Matcher dimensionsMatcher = DIMENSIONS_PATTERN.matcher(text);
        if (dimensionsMatcher.find()) {
            double a = parseNumber(dimensionsMatcher.group(1));
            double b = parseNumber(dimensionsMatcher.group(2));
            double c = parseNumber(dimensionsMatcher.group(3));
            return round(Math.max(a, Math.max(b, c)));
        }

        return 0.0;
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
