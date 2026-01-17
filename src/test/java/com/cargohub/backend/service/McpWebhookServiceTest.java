package com.cargohub.backend.service;

import com.cargohub.backend.dto.McpWebhookResponse;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class McpWebhookServiceTest {

    private McpWebhookService mcpWebhookService;

    @BeforeEach
    void setUp() {
        mcpWebhookService = new McpWebhookService();
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
}
