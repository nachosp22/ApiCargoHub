package com.cargohub.backend.service;

import com.cargohub.backend.dto.McpWebhookResponse;
import com.cargohub.backend.entity.N8nWebhook;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.N8nWebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class McpWebhookServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private N8nWebhookRepository n8nWebhookRepository;

    private McpWebhookService mcpWebhookService;

    @BeforeEach
    void setUp() {
        mcpWebhookService = new McpWebhookService(restTemplate, n8nWebhookRepository);
    }

    @Test
    void testCalcularDimensiones_SinWebhookUrl() {
        // Given
        ReflectionTestUtils.setField(mcpWebhookService, "webhookUrl", "");
        String descripcion = "10 cajas de 50kg cada una";

        // When
        McpWebhookResponse response = mcpWebhookService.calcularDimensiones(descripcion);

        // Then
        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertNotNull(response.getMotivoRevision());
        assertEquals(0.0, response.getPesoTotalKg());
    }

    @Test
    void testCalcularDimensiones_DescripcionVacia() {
        // Given
        ReflectionTestUtils.setField(mcpWebhookService, "webhookUrl", "http://test.com");
        String descripcion = "";

        // When
        McpWebhookResponse response = mcpWebhookService.calcularDimensiones(descripcion);

        // Then
        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertEquals(0.0, response.getPesoTotalKg());
    }

    @Test
    void testCalcularDimensiones_DescripcionNull() {
        // Given
        ReflectionTestUtils.setField(mcpWebhookService, "webhookUrl", "http://test.com");

        // When
        McpWebhookResponse response = mcpWebhookService.calcularDimensiones(null);

        // Then
        assertNotNull(response);
        assertTrue(response.getRevisionManual());
    }

    @Test
    void testConvertirTipoVehiculo_Furgoneta() {
        // When
        TipoVehiculo tipo = mcpWebhookService.convertirTipoVehiculo("FURGONETA");

        // Then
        assertEquals(TipoVehiculo.FURGONETA, tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Rigido() {
        // When
        TipoVehiculo tipo = mcpWebhookService.convertirTipoVehiculo("RIGIDO");

        // Then
        assertEquals(TipoVehiculo.RIGIDO, tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Trailer() {
        // When
        TipoVehiculo tipo = mcpWebhookService.convertirTipoVehiculo("TRAILER");

        // Then
        assertEquals(TipoVehiculo.TRAILER, tipo);
    }

    @Test
    void testConvertirTipoVehiculo_LowerCase() {
        // When
        TipoVehiculo tipo = mcpWebhookService.convertirTipoVehiculo("furgoneta");

        // Then
        assertEquals(TipoVehiculo.FURGONETA, tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Null() {
        // When
        TipoVehiculo tipo = mcpWebhookService.convertirTipoVehiculo(null);

        // Then
        assertNull(tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Invalido() {
        // When
        TipoVehiculo tipo = mcpWebhookService.convertirTipoVehiculo("INVALIDO");

        // Then
        assertNull(tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Vacio() {
        // When
        TipoVehiculo tipo = mcpWebhookService.convertirTipoVehiculo("");

        // Then
        assertNull(tipo);
    }

    @Test
    void testCalcularDimensiones_GuardaHistorial() {
        // Given
        ReflectionTestUtils.setField(mcpWebhookService, "webhookUrl", "");
        String descripcion = "10 cajas de 50kg cada una";

        // When
        mcpWebhookService.calcularDimensiones(descripcion);

        // Then - Verify that the webhook execution was saved
        ArgumentCaptor<N8nWebhook> webhookCaptor = ArgumentCaptor.forClass(N8nWebhook.class);
        verify(n8nWebhookRepository, times(1)).save(webhookCaptor.capture());
        
        N8nWebhook savedWebhook = webhookCaptor.getValue();
        assertNotNull(savedWebhook);
        assertFalse(savedWebhook.getSuccess());
        assertNotNull(savedWebhook.getErrorMessage());
        assertNotNull(savedWebhook.getRequestTimestamp());
    }

    @Test
    void testCalcularDimensiones_ConPorte_GuardaHistorial() {
        // Given
        ReflectionTestUtils.setField(mcpWebhookService, "webhookUrl", "");
        String descripcion = "5 palets";

        // When
        mcpWebhookService.calcularDimensiones(descripcion, null);

        // Then - Verify that the webhook execution was saved
        verify(n8nWebhookRepository, times(1)).save(any(N8nWebhook.class));
    }
}
