package com.cargohub.backend.service;

import com.cargohub.backend.dto.CargoAnalysisResponse;
import com.cargohub.backend.entity.CargoAnalysisLog;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.CargoAnalysisLogRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class GeminiCargaService {

    private static final String USER_FACING_FALLBACK_REASON =
            "No se ha podido analizar la carga. El porte sera revisado manualmente por uno de nuestros agentes.";

    private static final Set<String> ALLOWED_VEHICLE_TYPES = Set.of("FURGONETA", "RIGIDO", "TRAILER");
    private static final Set<String> REQUIRED_JSON_FIELDS = Set.of(
            "pesoTotalKg",
            "volumenTotalM3",
            "largoMaxPaquete",
            "anchoMaxPaquete",
            "altoMaxPaquete",
            "tipoVehiculoRequerido",
            "revisionManual",
            "motivoRevision"
    );

    private static final String SYSTEM_PROMPT = """
            Rol: sos un experto en logística y gestión de flota para ApiCargoHub.
            Analizá el mensaje del cliente y devolvé SOLO un JSON válido, puro, sin markdown ni texto adicional.

            Contrato de salida obligatorio:
            {
              "pesoTotalKg": number o 0.0,
              "volumenTotalM3": number o 0.0,
              "largoMaxPaquete": number en metros o 0.0,
              "anchoMaxPaquete": number en metros o 0.0,
              "altoMaxPaquete": number en metros o 0.0,
              "tipoVehiculoRequerido": "FURGONETA" | "RIGIDO" | "TRAILER",
              "revisionManual": boolean,
              "motivoRevision": string o null
            }

            Reglas de selección de vehículo:
            - FURGONETA: hasta 1200 kg y largo máximo de paquete hasta 3 m.
            - RIGIDO: entre 1200 y 8000 kg, o largo de paquete entre 3 y 7 m.
            - TRAILER: más de 8000 kg o largo de paquete mayor a 7 m.
            - Si el cliente habla de palets europeos estándar sin medidas, usá 1.2 m x 0.8 m como base de cada palet.
            - Si el cliente dice "X cajas/bultos/palets de Y kg", pesoTotalKg debe ser X * Y kg.
            - Si el cliente dice "peso total Y kg", pesoTotalKg debe ser Y kg, no multiplicarlo por la cantidad.

            Reglas de revisión manual:
            - revisionManual debe ser true si NO se puede calcular o deducir claramente alguno de estos: peso total, volumen total (m3), largo máximo.
            - Si falta volumen, aunque exista el peso, revisionManual debe ser true y explicar el motivo.
            - Si el cliente no especifica ancho o alto, inferí valores estándar según el tipo de vehículo:
              * FURGONETA: ancho 1.7 m, alto 1.8 m.
              * RIGIDO: ancho 2.45 m, alto 2.5 m.
              * TRAILER: ancho 2.45 m, alto 2.7 m.
            - revisionManual puede ser false si peso, volumen, largo y tipo de vehículo están claros, aunque ancho/alto no estén explícitos.

            Si no hay datos suficientes, devolvé 0.0 en los campos numéricos y explicá el motivo en motivoRevision.
            """;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model.id:gemini-2.0-flash}")
    private String modelId;

    private final CargoAnalysisLogRepository analysisLogRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiCargaService(CargoAnalysisLogRepository cargoAnalysisLogRepository, ObjectMapper objectMapper) {
        this.analysisLogRepository = cargoAnalysisLogRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    /**
     * Checks if the Gemini API is properly configured and available.
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Calls Google Gemini to parse cargo description into structured dimensions.
     * Maintains the current response contract used by the porte flow.
     */
    public CargoAnalysisResponse calcularDimensiones(String descripcionCliente, Porte porte) {
        if (descripcionCliente == null || descripcionCliente.isBlank()) {
            String errorMsg = "Descripción de carga vacía";
            saveAnalysisLog(descripcionCliente, null, false, errorMsg, porte);
            return createDefaultResponse(USER_FACING_FALLBACK_REASON);
        }

        if (!isAvailable()) {
            String errorMsg = "Gemini API key no configurada";
            saveAnalysisLog(descripcionCliente, null, false, errorMsg, porte);
            return createDefaultResponse(USER_FACING_FALLBACK_REASON);
        }

        CargoAnalysisLog analysisLog = new CargoAnalysisLog();
        analysisLog.setRequestTimestamp(LocalDateTime.now());
        analysisLog.setRequestData(descripcionCliente);
        analysisLog.setPorte(porte);

        try {
            String userPrompt = "Mensaje del cliente para analizar: " + descripcionCliente;

            // Build Gemini REST API request body
            Map<String, Object> requestBody = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                    ),
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", userPrompt)))
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "responseSchema", responseSchema(),
                            "temperature", 0
                    )
            );

            String url = String.format(GEMINI_API_URL, modelId, apiKey);

            String responseBody = restClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // Parse the Gemini response
            JsonNode root = objectMapper.readTree(responseBody);
            String responseText = extractCandidateText(root);
            CargoAnalysisResponse response = parseAndValidateStrictJson(responseText);
            applyBusinessRules(response);

            // Log success
            analysisLog.setResponseTimestamp(LocalDateTime.now());
            analysisLog.setResponseData(responseText);
            analysisLog.setSuccess(true);
            analysisLog.setPesoTotalKg(response.getPesoTotalKg());
            analysisLog.setVolumenTotalM3(response.getVolumenTotalM3());
            analysisLog.setLargoMaxPaquete(response.getLargoMaxPaquete());
            analysisLog.setTipoVehiculoRequerido(response.getTipoVehiculoRequerido());
            analysisLog.setRevisionManual(response.getRevisionManual());
            analysisLogRepository.save(analysisLog);

            return response;

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            String errorMsg = "Error al conectar con Gemini: " + e.getMessage();
            analysisLog.setSuccess(false);
            analysisLog.setErrorMessage(errorMsg);
            analysisLog.setResponseTimestamp(LocalDateTime.now());
            analysisLogRepository.save(analysisLog);
            return createDefaultResponse(USER_FACING_FALLBACK_REASON);
        }
    }

    private CargoAnalysisResponse createDefaultResponse(String motivo) {
        CargoAnalysisResponse response = new CargoAnalysisResponse();
        response.setPesoTotalKg(0.0);
        response.setVolumenTotalM3(0.0);
        response.setLargoMaxPaquete(0.0);
        response.setAnchoMaxPaquete(0.0);
        response.setAltoMaxPaquete(0.0);
        response.setTipoVehiculoRequerido(null);
        response.setRevisionManual(true);
        response.setMotivoRevision(motivo);
        return response;
    }

    private Map<String, Object> responseSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pesoTotalKg", Map.of("type", "number"),
                        "volumenTotalM3", Map.of("type", "number"),
                        "largoMaxPaquete", Map.of("type", "number"),
                        "anchoMaxPaquete", Map.of("type", "number"),
                        "altoMaxPaquete", Map.of("type", "number"),
                        "tipoVehiculoRequerido", Map.of(
                                "type", "string",
                                "enum", List.of("FURGONETA", "RIGIDO", "TRAILER")
                        ),
                        "revisionManual", Map.of("type", "boolean"),
                        "motivoRevision", Map.of("type", "string", "nullable", true)
                ),
                "required", List.of(
                        "pesoTotalKg",
                        "volumenTotalM3",
                        "largoMaxPaquete",
                        "anchoMaxPaquete",
                        "altoMaxPaquete",
                        "tipoVehiculoRequerido",
                        "revisionManual",
                        "motivoRevision"
                )
        );
    }

    private String extractCandidateText(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalArgumentException("Gemini response without candidates");
        }

        JsonNode firstCandidate = candidates.get(0);
        JsonNode parts = firstCandidate.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalArgumentException("Gemini response without content parts");
        }

        String text = parts.get(0).path("text").asText(null);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Gemini response text is empty");
        }

        return text.trim();
    }

    private CargoAnalysisResponse parseAndValidateStrictJson(String responseText) throws Exception {
        JsonNode json = objectMapper.readTree(extractJsonObject(responseText));
        if (!json.isObject()) {
            throw new IllegalArgumentException("Gemini response is not a JSON object");
        }

        validateRequiredFields(json);

        double peso = readRequiredNumber(json, "pesoTotalKg");
        double volumen = readRequiredNumber(json, "volumenTotalM3");
        double largo = readRequiredNumber(json, "largoMaxPaquete");
        double ancho = readOptionalNumber(json, "anchoMaxPaquete");
        double alto = readOptionalNumber(json, "altoMaxPaquete");
        String tipoVehiculo = readRequiredText(json, "tipoVehiculoRequerido");
        if (!ALLOWED_VEHICLE_TYPES.contains(tipoVehiculo)) {
            throw new IllegalArgumentException("Unsupported vehicle type: " + tipoVehiculo);
        }

        JsonNode revisionManualNode = json.get("revisionManual");
        if (revisionManualNode == null || !revisionManualNode.isBoolean()) {
            throw new IllegalArgumentException("revisionManual must be boolean");
        }

        JsonNode motivoNode = json.get("motivoRevision");
        if (motivoNode == null || (!motivoNode.isTextual() && !motivoNode.isNull())) {
            throw new IllegalArgumentException("motivoRevision must be string or null");
        }

        CargoAnalysisResponse response = new CargoAnalysisResponse();
        response.setPesoTotalKg(peso);
        response.setVolumenTotalM3(volumen);
        response.setLargoMaxPaquete(largo);
        response.setAnchoMaxPaquete(ancho);
        response.setAltoMaxPaquete(alto);
        response.setTipoVehiculoRequerido(tipoVehiculo);
        response.setRevisionManual(revisionManualNode.asBoolean());
        response.setMotivoRevision(motivoNode.isNull() ? null : motivoNode.asText());
        return response;
    }

    private String extractJsonObject(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            throw new IllegalArgumentException("Gemini response text is empty");
        }

        String trimmed = responseText.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        throw new IllegalArgumentException("Gemini response does not contain a JSON object");
    }

    private void validateRequiredFields(JsonNode json) {
        for (String field : REQUIRED_JSON_FIELDS) {
            if (json.get(field) == null) {
                throw new IllegalArgumentException("Gemini response missing required field: " + field);
            }
        }
    }

    private double readRequiredNumber(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isNumber()) {
            throw new IllegalArgumentException(field + " must be numeric");
        }
        return value.asDouble();
    }

    private double readOptionalNumber(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isNumber()) {
            return 0.0;
        }
        return value.asDouble();
    }

    private String readRequiredText(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " must be non-empty string");
        }
        return value.asText().trim();
    }

    private void applyBusinessRules(CargoAnalysisResponse response) {
        double peso = defaultIfNull(response.getPesoTotalKg());
        double volumen = defaultIfNull(response.getVolumenTotalM3());
        double largo = defaultIfNull(response.getLargoMaxPaquete());
        double ancho = defaultIfNull(response.getAnchoMaxPaquete());
        double alto = defaultIfNull(response.getAltoMaxPaquete());

        // Solo calculamos fallback si Gemini no devolvió un tipo válido.
        // Gemini puede inferir necesidades por volumen, tipo de carga, fragilidad,
        // cantidad de palets, etc., que nosotros no vemos solo con peso/largo.
        String geminiTipo = response.getTipoVehiculoRequerido();
        if (geminiTipo == null || !ALLOWED_VEHICLE_TYPES.contains(geminiTipo)) {
            response.setTipoVehiculoRequerido(resolveVehicleType(peso, volumen, largo));
        }
        // Si Gemini devolvió un tipo válido, lo respetamos como autoridad.
        // Ej: muchos palets livianos (alto volumen, bajo peso) → RIGIDO/TRAILER

        // Inferir ancho/alto faltantes desde el tipo de vehículo
        String tipoVehiculo = response.getTipoVehiculoRequerido();
        if (ancho <= 0 && tipoVehiculo != null) {
            ancho = inferAnchoFromVehicleType(tipoVehiculo);
            response.setAnchoMaxPaquete(ancho);
        }
        if (alto <= 0 && tipoVehiculo != null) {
            alto = inferAltoFromVehicleType(tipoVehiculo);
            response.setAltoMaxPaquete(alto);
        }

        List<String> missing = new ArrayList<>();
        if (peso <= 0) {
            missing.add("peso total");
        }
        if (volumen <= 0) {
            missing.add("volumen total");
        }
        if (largo <= 0) {
            missing.add("largo maximo");
        }

        boolean revisionManual = !missing.isEmpty();
        response.setRevisionManual(revisionManual);
        if (revisionManual) {
            if (volumen <= 0) {
                response.setMotivoRevision(USER_FACING_FALLBACK_REASON);
            } else {
                response.setMotivoRevision(USER_FACING_FALLBACK_REASON);
            }
        } else {
            response.setMotivoRevision(null);
        }
    }

    /**
     * Fallback heurístico cuando Gemini no devuelve tipo de vehículo.
     * Considera peso, volumen y largo máximo.
     */
    private String resolveVehicleType(double pesoTotalKg, double volumenTotalM3, double largoMaxPaquete) {
        if (pesoTotalKg > 8000 || largoMaxPaquete > 7 || volumenTotalM3 > 40) {
            return "TRAILER";
        }
        if ((pesoTotalKg >= 1200 && pesoTotalKg <= 8000)
                || (largoMaxPaquete >= 3 && largoMaxPaquete <= 7)
                || (volumenTotalM3 >= 10 && volumenTotalM3 <= 40)) {
            return "RIGIDO";
        }
        return "FURGONETA";
    }

    private double defaultIfNull(Double value) {
        return value == null ? 0.0 : value;
    }

    private double inferAnchoFromVehicleType(String tipo) {
        return switch (tipo) {
            case "FURGONETA" -> 1.7;
            case "RIGIDO" -> 2.45;
            case "TRAILER" -> 2.45;
            default -> 1.7;
        };
    }

    private double inferAltoFromVehicleType(String tipo) {
        return switch (tipo) {
            case "FURGONETA" -> 1.8;
            case "RIGIDO" -> 2.5;
            case "TRAILER" -> 2.7;
            default -> 1.8;
        };
    }

    private void saveAnalysisLog(String requestData, String responseData, boolean success, String errorMessage, Porte porte) {
        CargoAnalysisLog analysisLog = new CargoAnalysisLog();
        analysisLog.setRequestTimestamp(LocalDateTime.now());
        analysisLog.setResponseTimestamp(LocalDateTime.now());
        analysisLog.setRequestData(requestData);
        analysisLog.setResponseData(responseData);
        analysisLog.setSuccess(success);
        analysisLog.setErrorMessage(errorMessage);
        analysisLog.setPorte(porte);
        analysisLogRepository.save(analysisLog);
    }
}
