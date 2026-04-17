package com.cargohub.backend.service;

import com.cargohub.backend.dto.McpWebhookResponse;
import com.cargohub.backend.entity.N8nWebhook;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.N8nWebhookRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeminiCargaServiceTest {

    @Mock
    private N8nWebhookRepository n8nWebhookRepository;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GeminiCargaService geminiCargaService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        geminiCargaService = new GeminiCargaService(n8nWebhookRepository, objectMapper);
        // Inject the mocked RestClient
        ReflectionTestUtils.setField(geminiCargaService, "restClient", restClient);
    }

    // --- isAvailable ---

    @Test
    void isAvailable_returnsFalse_whenApiKeyBlank() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "");
        assertFalse(geminiCargaService.isAvailable());
    }

    @Test
    void isAvailable_returnsFalse_whenApiKeyNull() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", null);
        assertFalse(geminiCargaService.isAvailable());
    }

    @Test
    void isAvailable_returnsTrue_whenApiKeyPresent() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "test-key");
        assertTrue(geminiCargaService.isAvailable());
    }

    // --- calcularDimensiones: null/empty description ---

    @Test
    void calcularDimensiones_returnsDefault_whenDescripcionNull() {
        McpWebhookResponse response = geminiCargaService.calcularDimensiones(null, null);

        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertEquals(0.0, response.getPesoTotalKg());
        verify(n8nWebhookRepository).save(any(N8nWebhook.class));
    }

    @Test
    void calcularDimensiones_returnsDefault_whenDescripcionBlank() {
        McpWebhookResponse response = geminiCargaService.calcularDimensiones("  ", null);

        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertNotNull(response.getMotivoRevision());
    }

    // --- calcularDimensiones: API key not configured ---

    @Test
    void calcularDimensiones_returnsDefault_whenApiKeyNotConfigured() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "");

        McpWebhookResponse response = geminiCargaService.calcularDimensiones("10 cajas de 50kg", null);

        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertEquals("Gemini API key no configurada", response.getMotivoRevision());
        verify(n8nWebhookRepository).save(any(N8nWebhook.class));
    }

    // --- calcularDimensiones: successful Gemini call ---

    @Test
    void calcularDimensiones_parsesGeminiResponse_whenSuccessful() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiCargaService, "modelId", "gemini-2.0-flash");

        String geminiResponse = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{
                        "text": "{\\"pesoTotalKg\\": 500.0, \\"volumenTotalM3\\": 2.5, \\"largoMaxPaquete\\": 1.2, \\"tipoVehiculoRequerido\\": \\"FURGONETA\\", \\"revisionManual\\": false, \\"motivoRevision\\": null}"
                      }]
                    }
                  }]
                }
                """;

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(geminiResponse);

        McpWebhookResponse response = geminiCargaService.calcularDimensiones("10 cajas de 50kg", new Porte());

        assertNotNull(response);
        assertEquals(500.0, response.getPesoTotalKg());
        assertEquals(2.5, response.getVolumenTotalM3());
        assertEquals(1.2, response.getLargoMaxPaquete());
        assertEquals("FURGONETA", response.getTipoVehiculoRequerido());
        assertFalse(response.getRevisionManual());
        assertNull(response.getMotivoRevision());

        // Verify log was saved
        ArgumentCaptor<N8nWebhook> captor = ArgumentCaptor.forClass(N8nWebhook.class);
        verify(n8nWebhookRepository).save(captor.capture());
        assertTrue(captor.getValue().getSuccess());
    }

    // --- calcularDimensiones: Gemini returns markdown-wrapped JSON ---

    @Test
    void calcularDimensiones_stripsMarkdownFences() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiCargaService, "modelId", "gemini-2.0-flash");

        String geminiResponse = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{
                        "text": "```json\\n{\\"pesoTotalKg\\": 100, \\"volumenTotalM3\\": 1.0, \\"largoMaxPaquete\\": 0.5, \\"tipoVehiculoRequerido\\": \\"FURGONETA\\", \\"revisionManual\\": false, \\"motivoRevision\\": null}\\n```"
                      }]
                    }
                  }]
                }
                """;

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(geminiResponse);

        McpWebhookResponse response = geminiCargaService.calcularDimensiones("5 palets", null);

        assertEquals(100.0, response.getPesoTotalKg());
        assertFalse(response.getRevisionManual());
    }

    // --- calcularDimensiones: Gemini API error ---

    @Test
    void calcularDimensiones_returnsDefault_whenGeminiThrows() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiCargaService, "modelId", "gemini-2.0-flash");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        McpWebhookResponse response = geminiCargaService.calcularDimensiones("carga test", new Porte());

        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertEquals(0.0, response.getPesoTotalKg());
        assertTrue(response.getMotivoRevision().contains("Gemini"));

        // Verify error log saved
        ArgumentCaptor<N8nWebhook> captor = ArgumentCaptor.forClass(N8nWebhook.class);
        verify(n8nWebhookRepository).save(captor.capture());
        assertFalse(captor.getValue().getSuccess());
    }

    // --- calcularDimensiones: saves log with porte reference ---

    @Test
    void calcularDimensiones_savesLogWithPorteReference() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "");
        Porte porte = new Porte();
        porte.setId(42L);

        geminiCargaService.calcularDimensiones("test", porte);

        ArgumentCaptor<N8nWebhook> captor = ArgumentCaptor.forClass(N8nWebhook.class);
        verify(n8nWebhookRepository).save(captor.capture());
        assertEquals(porte, captor.getValue().getPorte());
    }
}
