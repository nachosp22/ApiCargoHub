package com.cargohub.backend.service;

import com.cargohub.backend.dto.McpWebhookRequest;
import com.cargohub.backend.dto.McpWebhookResponse;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class McpWebhookService {

    @Value("${mcp.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    public McpWebhookService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Calls the MCP n8n webhook to calculate order dimensions
     * @param descripcionCliente The customer's order description
     * @return McpWebhookResponse with calculated dimensions
     */
    public McpWebhookResponse calcularDimensiones(String descripcionCliente) {
        // If webhook URL is not configured or description is empty, return null
        if (webhookUrl == null || webhookUrl.isEmpty() || descripcionCliente == null || descripcionCliente.isEmpty()) {
            return createDefaultResponse("Webhook no configurado o descripción vacía");
        }

        try {
            // Prepare request
            McpWebhookRequest request = new McpWebhookRequest(descripcionCliente);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<McpWebhookRequest> entity = new HttpEntity<>(request, headers);
            
            // Call webhook
            ResponseEntity<McpWebhookResponse> response = restTemplate.postForEntity(
                webhookUrl, 
                entity, 
                McpWebhookResponse.class
            );
            
            // Return response
            if (response.getBody() != null) {
                return response.getBody();
            } else {
                return createDefaultResponse("Respuesta vacía del webhook");
            }
            
        } catch (Exception e) {
            // If webhook fails, return a default response with manual review flag
            log.error("Error calling MCP webhook: {}", e.getMessage(), e);
            return createDefaultResponse("Error al conectar con el webhook: " + e.getMessage());
        }
    }

    /**
     * Creates a default response when webhook is unavailable or fails
     */
    private McpWebhookResponse createDefaultResponse(String motivo) {
        McpWebhookResponse response = new McpWebhookResponse();
        response.setPesoTotalKg(0.0);
        response.setVolumenTotalM3(0.0);
        response.setLargoMaxPaquete(0.0);
        response.setTipoVehiculoRequerido(null);
        response.setRevisionManual(true);
        response.setMotivoRevision(motivo);
        return response;
    }

    /**
     * Converts string vehicle type to enum
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
