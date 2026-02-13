package com.cargohub.backend.service;

import com.cargohub.backend.dto.McpWebhookRequest;
import com.cargohub.backend.dto.McpWebhookResponse;
import com.cargohub.backend.entity.N8nWebhook;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.N8nWebhookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
public class McpWebhookService {

    @Value("${mcp.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate;
    private final N8nWebhookRepository n8nWebhookRepository;

    @Autowired
    public McpWebhookService(RestTemplate restTemplate, N8nWebhookRepository n8nWebhookRepository) {
        this.restTemplate = restTemplate;
        this.n8nWebhookRepository = n8nWebhookRepository;
    }

    /**
     * Calls the MCP n8n webhook to calculate order dimensions
     * @param descripcionCliente The customer's order description
     * @return McpWebhookResponse with calculated dimensions
     */
    public McpWebhookResponse calcularDimensiones(String descripcionCliente) {
        return calcularDimensiones(descripcionCliente, null);
    }

    /**
     * Calls the MCP n8n webhook to calculate order dimensions and saves execution history
     * @param descripcionCliente The customer's order description
     * @param porte Optional porte to associate with this webhook call
     * @return McpWebhookResponse with calculated dimensions
     */
    public McpWebhookResponse calcularDimensiones(String descripcionCliente, Porte porte) {
        N8nWebhook webhookLog = new N8nWebhook();
        webhookLog.setRequestTimestamp(LocalDateTime.now());
        webhookLog.setPorte(porte);

        // If webhook URL is not configured or description is empty, return null
        if (webhookUrl == null || webhookUrl.isBlank() || descripcionCliente == null || descripcionCliente.isBlank()) {
            String errorMsg = "Webhook no configurado o descripción vacía";
            webhookLog.setRequestData(descripcionCliente);
            webhookLog.setSuccess(false);
            webhookLog.setErrorMessage(errorMsg);
            webhookLog.setResponseTimestamp(LocalDateTime.now());
            n8nWebhookRepository.save(webhookLog);
            return createDefaultResponse(errorMsg);
        }

        try {
            // Prepare request
            McpWebhookRequest request = new McpWebhookRequest(descripcionCliente);
            webhookLog.setRequestData(descripcionCliente);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<McpWebhookRequest> entity = new HttpEntity<>(request, headers);
            
            // Call webhook
            ResponseEntity<McpWebhookResponse> response = restTemplate.postForEntity(
                webhookUrl, 
                entity, 
                McpWebhookResponse.class
            );
            
            webhookLog.setResponseTimestamp(LocalDateTime.now());
            
            // Return response
            if (response.getBody() != null) {
                McpWebhookResponse responseBody = response.getBody();
                webhookLog.setResponseData(formatResponse(responseBody));
                webhookLog.setSuccess(true);
                webhookLog.setPesoTotalKg(responseBody.getPesoTotalKg());
                webhookLog.setVolumenTotalM3(responseBody.getVolumenTotalM3());
                webhookLog.setLargoMaxPaquete(responseBody.getLargoMaxPaquete());
                webhookLog.setTipoVehiculoRequerido(responseBody.getTipoVehiculoRequerido());
                webhookLog.setRevisionManual(responseBody.getRevisionManual());
                n8nWebhookRepository.save(webhookLog);
                return responseBody;
            } else {
                String errorMsg = "Respuesta vacía del webhook";
                webhookLog.setSuccess(false);
                webhookLog.setErrorMessage(errorMsg);
                n8nWebhookRepository.save(webhookLog);
                return createDefaultResponse(errorMsg);
            }
            
        } catch (Exception e) {
            // If webhook fails, return a default response with manual review flag
            log.error("Error calling MCP webhook: {}", e.getMessage(), e);
            webhookLog.setSuccess(false);
            webhookLog.setErrorMessage("Error al conectar con el webhook: " + e.getMessage());
            webhookLog.setResponseTimestamp(LocalDateTime.now());
            n8nWebhookRepository.save(webhookLog);
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
     * Formats the response data as a simple string for logging
     */
    private String formatResponse(McpWebhookResponse response) {
        return String.format("peso=%s kg, volumen=%s m3, largo=%s, vehiculo=%s, revision=%s",
            response.getPesoTotalKg(),
            response.getVolumenTotalM3(),
            response.getLargoMaxPaquete(),
            response.getTipoVehiculoRequerido(),
            response.getRevisionManual());
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
