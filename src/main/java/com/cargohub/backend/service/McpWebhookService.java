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
     * Llama al webhook MCP n8n para calcular las dimensiones del pedido
     * @param descripcionCliente La descripción del pedido del cliente
     * @return McpWebhookResponse con las dimensiones calculadas
     */
    public McpWebhookResponse calcularDimensiones(String descripcionCliente) {
        return calcularDimensiones(descripcionCliente, null);
    }

    /**
     * Llama al webhook MCP n8n para calcular las dimensiones del pedido y guarda el historial de ejecución
     * @param descripcionCliente La descripción del pedido del cliente
     * @param porte Porte opcional para asociar con esta llamada al webhook
     * @return McpWebhookResponse con las dimensiones calculadas
     */
    public McpWebhookResponse calcularDimensiones(String descripcionCliente, Porte porte) {
        // Si la URL del webhook no está configurada o la descripción está vacía, devuelve null
        if (webhookUrl == null || webhookUrl.isBlank() || descripcionCliente == null || descripcionCliente.isBlank()) {
            String errorMsg = "Webhook no configurado o descripción vacía";
            saveWebhookLog(null, null, descripcionCliente, null, false, errorMsg, porte);
            return createDefaultResponse(errorMsg);
        }

        N8nWebhook webhookLog = new N8nWebhook();
        webhookLog.setRequestTimestamp(LocalDateTime.now());
        webhookLog.setRequestData(descripcionCliente);
        webhookLog.setPorte(porte);

        try {
            // Preparar solicitud
            McpWebhookRequest request = new McpWebhookRequest(descripcionCliente);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<McpWebhookRequest> entity = new HttpEntity<>(request, headers);
            
            // Llamar al webhook
            ResponseEntity<McpWebhookResponse> response = restTemplate.postForEntity(
                webhookUrl, 
                entity, 
                McpWebhookResponse.class
            );
            
            webhookLog.setResponseTimestamp(LocalDateTime.now());
            
            // Devolver respuesta
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
            // Si el webhook falla, devuelve una respuesta predeterminada con bandera de revisión manual
            log.error("Error calling MCP webhook: {}", e.getMessage(), e);
            webhookLog.setSuccess(false);
            webhookLog.setErrorMessage(buildWebhookErrorMessage(e.getMessage()));
            webhookLog.setResponseTimestamp(LocalDateTime.now());
            n8nWebhookRepository.save(webhookLog);
            return createDefaultResponse(buildWebhookErrorMessage(e.getMessage()));
        }
    }

    /**
     * Crea una respuesta predeterminada cuando el webhook no está disponible o falla
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
     * Formatea los datos de respuesta como una cadena simple para registro
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

    /**
     * Método auxiliar para guardar el registro del webhook
     */
    private void saveWebhookLog(LocalDateTime requestTime, LocalDateTime responseTime, String requestData, 
                                String responseData, boolean success, String errorMessage, Porte porte) {
        N8nWebhook webhookLog = new N8nWebhook();
        webhookLog.setRequestTimestamp(requestTime != null ? requestTime : LocalDateTime.now());
        webhookLog.setResponseTimestamp(responseTime != null ? responseTime : LocalDateTime.now());
        webhookLog.setRequestData(requestData);
        webhookLog.setResponseData(responseData);
        webhookLog.setSuccess(success);
        webhookLog.setErrorMessage(errorMessage);
        webhookLog.setPorte(porte);
        n8nWebhookRepository.save(webhookLog);
    }

    /**
     * Construye el mensaje de error para fallos del webhook
     */
    private String buildWebhookErrorMessage(String details) {
        return "Error al conectar con el webhook: " + details;
    }
}
