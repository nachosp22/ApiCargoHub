package com.cargohub.backend.service;

import com.cargohub.backend.dto.CargoAnalysisResponse;
import com.cargohub.backend.entity.CargoAnalysisLog;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.CargoAnalysisLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            Eres un experto en logística y transporte de mercancías por carretera en España.
            Analiza el mensaje que ha escrito un cliente describiendo la carga que necesita transportar
            y devuelve ÚNICAMENTE un JSON válido, sin markdown, sin explicaciones ni texto adicional.

            ── FORMATO DE SALIDA OBLIGATORIO ──
            {
              "pesoTotalKg": <número en kilogramos, 0.0 si no se puede determinar>,
              "volumenTotalM3": <número en metros cúbicos, 0.0 si no se puede determinar>,
              "largoMaxPaquete": <número en metros, 0.0 si no se puede determinar>,
              "anchoMaxPaquete": <número en metros, 0.0 si no se puede determinar>,
              "altoMaxPaquete": <número en metros, 0.0 si no se puede determinar>,
              "tipoVehiculoRequerido": "FURGONETA" | "RIGIDO" | "TRAILER",
              "revisionManual": <true si faltan datos críticos, false si todo está claro>,
              "motivoRevision": <explicación para el agente si revisionManual es true, null en caso contrario>
            }

            ── CÓMO RAZONAR EL PESO ──
            - Si el cliente dice "peso total 2500 kg" o "2500 kilos": pesoTotalKg = 2500.
            - Si dice "5 palets de 500 kg cada uno": pesoTotalKg = 5 × 500 = 2500.
            - Si dice "10 cajas de 20 kg": pesoTotalKg = 10 × 20 = 200.
            - Si menciona toneladas, convierte: 1 tonelada = 1000 kg.
            - Si no hay NINGUNA mención de peso: pesoTotalKg = 0.0 y marca revisión manual.

            ── CÓMO RAZONAR EL VOLUMEN ──
            - Si el cliente da el volumen directamente ("8 m3", "8 metros cúbicos"): usa ese valor.
            - Si da dimensiones por bulto ("cajas de 0.5 x 0.4 x 0.3 m"):
              volumen unitario = 0.5 × 0.4 × 0.3 = 0.06 m3. Si hay 10 cajas → 0.6 m3 total.
            - Si menciona palets europeos sin medidas: cada palet = 1.2 × 0.8 × 1.5 m (alto estimado) = 1.44 m3.
            - Si no hay forma de calcular el volumen (ni directo ni por dimensiones): volumenTotalM3 = 0.0.
            - El volumen total se redondea a 2 decimales.

            ── CÓMO RAZONAR LAS DIMENSIONES MÁXIMAS ──
            - largoMaxPaquete: la mayor longitud de un solo bulto/palet/caja en metros.
            - anchoMaxPaquete y altoMaxPaquete: dimensiones del bulto más grande.
            - Si NO se especifican ancho y alto, infiere valores estándar según el tipo de vehículo
              (ver tabla más abajo). Esto NO debe provocar revisión manual por sí solo.
            - Si se mencionan dimensiones en cm o mm, convierte a metros (1 cm = 0.01 m, 1 mm = 0.001 m).

            ── ELECCIÓN DEL TIPO DE VEHÍCULO ──
            | Tipo       | Peso máximo | Largo máximo de bulto | Volumen máximo |
            | FURGONETA  | 1.200 kg    | 3,0 m                 | 15 m3          |
            | RÍGIDO     | 8.000 kg    | 7,0 m                 | 45 m3          |
            | TRAILER    | 24.000 kg   | 13,6 m                | 90 m3          |

            - Elige el tipo MÁS PEQUEÑO que pueda transportar la carga.
            - Si peso, volumen O largo superan el máximo de un tipo, pasa al siguiente.
            - Si el cliente menciona explícitamente el tipo de vehículo que necesita, respétalo.

            ── VALORES POR DEFECTO PARA ANCHO Y ALTO ──
            Usa estos valores cuando el cliente no especifique ancho o alto:
            - FURGONETA: ancho 1,7 m, alto 1,8 m
            - RÍGIDO:    ancho 2,45 m, alto 2,5 m
            - TRAILER:   ancho 2,45 m, alto 2,7 m

            ── CUÁNDO MARCAR REVISIÓN MANUAL ──
            revisionManual = true si FALTA CUALQUIERA de estos tres datos críticos:
              1. peso total (pesoTotalKg <= 0)
              2. volumen total (volumenTotalM3 <= 0)
              3. largo máximo del bulto más grande (largoMaxPaquete <= 0)
            Basta con que UNO solo sea 0.0 para que el porte necesite revisión manual.
            Solo pon revisionManual = false cuando los TRES tengan valor > 0.

            Si revisionManual = true, escribe en motivoRevision exactamente QUÉ datos faltan y por qué
            no se pudieron inferir, para que el agente humano sepa qué preguntar al cliente.
            Ejemplos de motivoRevision:
            - "El cliente no indica peso ni dimensiones. Imposible estimar."
            - "El cliente menciona 'muebles varios' sin cantidades ni pesos. Datos insuficientes."
            - "Falta el peso total y el volumen. Solo se conoce el largo máximo de 2 m."

            ── EJEMPLOS ──
            Cliente: "10 palets de 500 kg cada uno, 1.2 x 0.8 m"
            → pesoTotalKg: 5000, volumenTotalM3: 14.4, largoMaxPaquete: 1.2,
              anchoMaxPaquete: 0.8, altoMaxPaquete: 1.5, tipoVehiculo: RIGIDO, revisionManual: false

            Cliente: "muebles de oficina, unos 2000 kg en total"
            → pesoTotalKg: 2000, volumenTotalM3: 0.0, largoMaxPaquete: 0.0,
              tipoVehiculo: RIGIDO, revisionManual: true,
              motivoRevision: "Falta el volumen total y las dimensiones de los bultos."

            Cliente: "3 cajas de 50x40x30 cm, 15 kg cada una"
            → pesoTotalKg: 45, volumenTotalM3: 0.18, largoMaxPaquete: 0.5,
              anchoMaxPaquete: 0.4, altoMaxPaquete: 0.3, tipoVehiculo: FURGONETA, revisionManual: false

            ── IMPORTANTE ──
            - NO devuelvas markdown ni texto fuera del JSON.
            - Usa punto como separador decimal (3.5, no 3,5).
            - Todos los campos numéricos deben estar presentes, aunque valgan 0.0.
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

        // Respetamos la decisión de Gemini sobre revisionManual.
        // Gemini ya aplica las reglas del system prompt: si no puede determinar
        // peso, volumen o largo, él mismo pone revisionManual=true.
        // Solo forzamos revisionManual=true como red de seguridad si TODOS los
        // valores críticos son 0 (posible alucinación o error de parseo).
        boolean geminiDijoManual = Boolean.TRUE.equals(response.getRevisionManual());
        boolean todosCeros = (peso <= 0 && volumen <= 0 && largo <= 0);

        if (!geminiDijoManual && todosCeros) {
            // Red de seguridad: Gemini dijo que estaba bien pero no devolvió nada
            response.setRevisionManual(true);
            response.setMotivoRevision(USER_FACING_FALLBACK_REASON);
        }
        // Si Gemini dijo revisionManual=true, respetamos su criterio.
        // Si Gemini dijo false y hay al menos un valor > 0, confiamos.
        // El motivoRevision que puso Gemini se conserva tal cual.
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
