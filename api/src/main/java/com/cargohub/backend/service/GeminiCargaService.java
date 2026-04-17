package com.cargohub.backend.service;

import com.cargohub.backend.dto.McpWebhookResponse;
import com.cargohub.backend.entity.N8nWebhook;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.N8nWebhookRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiCargaService {

    private static final String SYSTEM_PROMPT = """
            Eres un experto en logística de transporte de mercancías en España.
            Dado una descripción de carga del cliente, debes estimar las dimensiones y peso del envío.
            
            Responde EXCLUSIVAMENTE con un JSON válido (sin markdown, sin texto extra) con estos campos:
            {
              "pesoTotalKg": <número decimal>,
              "volumenTotalM3": <número decimal>,
              "largoMaxPaquete": <número decimal en metros>,
              "anchoMaxPaquete": <número decimal en metros>,
              "altoMaxPaquete": <número decimal en metros>,
              "tipoVehiculoRequerido": "<FURGONETA|RIGIDO|TRAILER|ESPECIAL>",
              "revisionManual": <true|false>,
              "motivoRevision": "<motivo si revisionManual es true, null si false>"
            }
            
            Reglas:
            - Si la descripción es ambigua, pon revisionManual=true con el motivo.
            - largoMaxPaquete, anchoMaxPaquete y altoMaxPaquete representan la dimensión máxima
              del paquete más grande del envío en cada eje (largo, ancho, alto) en metros.
            - tipoVehiculoRequerido se basa en peso y volumen:
              * FURGONETA: hasta 1500kg y 8m³
              * RIGIDO: hasta 10000kg y 40m³
              * TRAILER: hasta 24000kg y 80m³
              * ESPECIAL: cargas fuera de rango o peligrosas
            - Si no puedes estimar, devuelve valores 0 con revisionManual=true.
            """;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model.id:gemini-2.0-flash}")
    private String modelId;

    private final N8nWebhookRepository n8nWebhookRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiCargaService(N8nWebhookRepository n8nWebhookRepository, ObjectMapper objectMapper) {
        this.n8nWebhookRepository = n8nWebhookRepository;
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
     * Maintains the same McpWebhookResponse contract as the n8n webhook.
     */
    public McpWebhookResponse calcularDimensiones(String descripcionCliente, Porte porte) {
        if (descripcionCliente == null || descripcionCliente.isBlank()) {
            String errorMsg = "Descripción de carga vacía";
            saveLog(descripcionCliente, null, false, errorMsg, porte);
            return createDefaultResponse(errorMsg);
        }

        if (!isAvailable()) {
            String errorMsg = "Gemini API key no configurada";
            saveLog(descripcionCliente, null, false, errorMsg, porte);
            return createDefaultResponse(errorMsg);
        }

        N8nWebhook webhookLog = new N8nWebhook();
        webhookLog.setRequestTimestamp(LocalDateTime.now());
        webhookLog.setRequestData(descripcionCliente);
        webhookLog.setPorte(porte);

        try {
            String userPrompt = "Descripción del cliente: " + descripcionCliente;

            // Build Gemini REST API request body
            Map<String, Object> requestBody = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                    ),
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", userPrompt)))
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
            String responseText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText().trim();

            // Strip markdown code fences if present
            if (responseText.startsWith("```")) {
                responseText = responseText.replaceAll("^```(?:json)?\\s*", "")
                        .replaceAll("\\s*```$", "");
            }

            // Parse JSON response
            JsonNode json = objectMapper.readTree(responseText);
            McpWebhookResponse response = new McpWebhookResponse();
            response.setPesoTotalKg(json.path("pesoTotalKg").asDouble(0.0));
            response.setVolumenTotalM3(json.path("volumenTotalM3").asDouble(0.0));
            response.setLargoMaxPaquete(json.path("largoMaxPaquete").asDouble(0.0));
            response.setAnchoMaxPaquete(json.path("anchoMaxPaquete").asDouble(0.0));
            response.setAltoMaxPaquete(json.path("altoMaxPaquete").asDouble(0.0));
            response.setTipoVehiculoRequerido(json.path("tipoVehiculoRequerido").asText(null));
            response.setRevisionManual(json.path("revisionManual").asBoolean(true));
            response.setMotivoRevision(json.path("motivoRevision").isNull() ? null : json.path("motivoRevision").asText(null));

            // Log success
            webhookLog.setResponseTimestamp(LocalDateTime.now());
            webhookLog.setResponseData(responseText);
            webhookLog.setSuccess(true);
            webhookLog.setPesoTotalKg(response.getPesoTotalKg());
            webhookLog.setVolumenTotalM3(response.getVolumenTotalM3());
            webhookLog.setLargoMaxPaquete(response.getLargoMaxPaquete());
            webhookLog.setTipoVehiculoRequerido(response.getTipoVehiculoRequerido());
            webhookLog.setRevisionManual(response.getRevisionManual());
            n8nWebhookRepository.save(webhookLog);

            return response;

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            String errorMsg = "Error al conectar con Gemini: " + e.getMessage();
            webhookLog.setSuccess(false);
            webhookLog.setErrorMessage(errorMsg);
            webhookLog.setResponseTimestamp(LocalDateTime.now());
            n8nWebhookRepository.save(webhookLog);
            return createDefaultResponse(errorMsg);
        }
    }

    private McpWebhookResponse createDefaultResponse(String motivo) {
        McpWebhookResponse response = new McpWebhookResponse();
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

    private void saveLog(String requestData, String responseData, boolean success, String errorMessage, Porte porte) {
        N8nWebhook webhookLog = new N8nWebhook();
        webhookLog.setRequestTimestamp(LocalDateTime.now());
        webhookLog.setResponseTimestamp(LocalDateTime.now());
        webhookLog.setRequestData(requestData);
        webhookLog.setResponseData(responseData);
        webhookLog.setSuccess(success);
        webhookLog.setErrorMessage(errorMessage);
        webhookLog.setPorte(porte);
        n8nWebhookRepository.save(webhookLog);
    }
}
