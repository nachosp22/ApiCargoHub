package com.cargohub.backend.service;

import com.cargohub.backend.dto.CargoAnalysisResponse;
import com.cargohub.backend.entity.CargoAnalysisLog;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.CargoAnalysisLogRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeminiCargaServiceTest {

    private static final String USER_FACING_FALLBACK_REASON =
            "No se pudo analizar la carga automaticamente. Tu solicitud se creo correctamente y quedo pendiente de revision manual.";

    @Mock
    private CargoAnalysisLogRepository cargoAnalysisLogRepository;

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
        geminiCargaService = new GeminiCargaService(cargoAnalysisLogRepository, objectMapper);
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
        CargoAnalysisResponse response = geminiCargaService.calcularDimensiones(null, null);

        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertEquals(0.0, response.getPesoTotalKg());
        verify(cargoAnalysisLogRepository).save(any(CargoAnalysisLog.class));
    }

    @Test
    void calcularDimensiones_returnsDefault_whenDescripcionBlank() {
        CargoAnalysisResponse response = geminiCargaService.calcularDimensiones("  ", null);

        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertNotNull(response.getMotivoRevision());
    }

    // --- calcularDimensiones: API key not configured ---

    @Test
    void calcularDimensiones_returnsDefault_whenApiKeyNotConfigured() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "");

        CargoAnalysisResponse response = geminiCargaService.calcularDimensiones("10 cajas de 50kg", null);

        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertEquals(USER_FACING_FALLBACK_REASON, response.getMotivoRevision());
        verify(cargoAnalysisLogRepository).save(any(CargoAnalysisLog.class));
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

        CargoAnalysisResponse response = geminiCargaService.calcularDimensiones("10 cajas de 50kg", new Porte());

        assertNotNull(response);
        assertEquals(500.0, response.getPesoTotalKg());
        assertEquals(2.5, response.getVolumenTotalM3());
        assertEquals(1.2, response.getLargoMaxPaquete());
        assertEquals(0.0, response.getAnchoMaxPaquete());
        assertEquals(0.0, response.getAltoMaxPaquete());
        assertEquals("FURGONETA", response.getTipoVehiculoRequerido());
        assertFalse(response.getRevisionManual());
        assertNull(response.getMotivoRevision());

        // Verify log was saved
        ArgumentCaptor<CargoAnalysisLog> captor = ArgumentCaptor.forClass(CargoAnalysisLog.class);
        verify(cargoAnalysisLogRepository).save(captor.capture());
        assertTrue(captor.getValue().getSuccess());
    }

    // --- calcularDimensiones: request enforces JSON output ---

    @Test
    @SuppressWarnings("unchecked")
    void calcularDimensiones_setsResponseMimeTypeApplicationJson() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiCargaService, "modelId", "gemini-2.0-flash");

        String geminiResponse = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{
                        "text": "{\\"pesoTotalKg\\": 200.0, \\"volumenTotalM3\\": 1.0, \\"largoMaxPaquete\\": 2.0, \\"tipoVehiculoRequerido\\": \\"FURGONETA\\", \\"revisionManual\\": false, \\"motivoRevision\\": null}"
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

        geminiCargaService.calcularDimensiones("5 palets", null);

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodySpec).body(bodyCaptor.capture());

        assertTrue(bodyCaptor.getValue() instanceof Map);
        Map<String, Object> requestBody = (Map<String, Object>) bodyCaptor.getValue();
        Map<String, Object> generationConfig = (Map<String, Object>) requestBody.get("generationConfig");
        assertEquals("application/json", generationConfig.get("responseMimeType"));
        assertEquals(0, generationConfig.get("temperature"));
    }

    // --- calcularDimensiones: markdown is rejected in strict JSON mode ---

    @Test
    void calcularDimensiones_returnsDefault_whenGeminiReturnsMarkdown() {
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

        CargoAnalysisResponse response = geminiCargaService.calcularDimensiones("5 palets", null);

        assertEquals(0.0, response.getPesoTotalKg());
        assertTrue(response.getRevisionManual());
        assertEquals(USER_FACING_FALLBACK_REASON, response.getMotivoRevision());
    }

    @Test
    void calcularDimensiones_forcesManualReview_whenVolumeIsMissing() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiCargaService, "modelId", "gemini-2.0-flash");

        String geminiResponse = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{
                        "text": "{\\"pesoTotalKg\\": 300.0, \\"volumenTotalM3\\": 0.0, \\"largoMaxPaquete\\": 2.4, \\"tipoVehiculoRequerido\\": \\"FURGONETA\\", \\"revisionManual\\": false, \\"motivoRevision\\": null}"
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

        CargoAnalysisResponse response = geminiCargaService.calcularDimensiones("carga sin volumen", null);

        assertEquals("FURGONETA", response.getTipoVehiculoRequerido());
        assertTrue(response.getRevisionManual());
        assertEquals("Falta volumen total (m3), se requiere revision manual.", response.getMotivoRevision());
    }

    @Test
    void calcularDimensiones_resolvesVehicleTypeUsingWeightAndLengthRules() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiCargaService, "modelId", "gemini-2.0-flash");

        String geminiResponse = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{
                        "text": "{\\"pesoTotalKg\\": 9500.0, \\"volumenTotalM3\\": 12.0, \\"largoMaxPaquete\\": 2.5, \\"tipoVehiculoRequerido\\": \\"FURGONETA\\", \\"revisionManual\\": false, \\"motivoRevision\\": null}"
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

        CargoAnalysisResponse response = geminiCargaService.calcularDimensiones("carga pesada", null);

        assertEquals("TRAILER", response.getTipoVehiculoRequerido());
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

        CargoAnalysisResponse response = geminiCargaService.calcularDimensiones("carga test", new Porte());

        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertEquals(0.0, response.getPesoTotalKg());
        assertEquals(USER_FACING_FALLBACK_REASON, response.getMotivoRevision());

        // Verify error log saved
        ArgumentCaptor<CargoAnalysisLog> captor = ArgumentCaptor.forClass(CargoAnalysisLog.class);
        verify(cargoAnalysisLogRepository).save(captor.capture());
        assertFalse(captor.getValue().getSuccess());
    }

    // --- calcularDimensiones: saves log with porte reference ---

    @Test
    void calcularDimensiones_savesLogWithPorteReference() {
        ReflectionTestUtils.setField(geminiCargaService, "apiKey", "");
        Porte porte = new Porte();
        porte.setId(42L);

        geminiCargaService.calcularDimensiones("test", porte);

        ArgumentCaptor<CargoAnalysisLog> captor = ArgumentCaptor.forClass(CargoAnalysisLog.class);
        verify(cargoAnalysisLogRepository).save(captor.capture());
        assertEquals(porte, captor.getValue().getPorte());
    }
}
