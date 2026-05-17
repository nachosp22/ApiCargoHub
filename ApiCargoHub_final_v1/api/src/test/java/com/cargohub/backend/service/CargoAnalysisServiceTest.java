package com.cargohub.backend.service;

import com.cargohub.backend.config.CargoAnalysisProperties;
import com.cargohub.backend.dto.CargoAnalysisResponse;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CargoAnalysisServiceTest {

    @Mock
    private GeminiCargaService geminiCargaService;

    private CargoAnalysisService cargoAnalysisService;

    @BeforeEach
    void setUp() {
        cargoAnalysisService = buildService(false);
    }

    private CargoAnalysisService buildService(boolean devFallbackEnabled) {
        CargoAnalysisProperties cargoAnalysisProperties = new CargoAnalysisProperties();
        cargoAnalysisProperties.getDevFallback().setEnabled(devFallbackEnabled);
        return new CargoAnalysisService(geminiCargaService, cargoAnalysisProperties);
    }

    @Test
    void calcularDimensiones_delegatesToGemini_whenGeminiReturnsParsedJson() {
        CargoAnalysisResponse geminiResponse = new CargoAnalysisResponse();
        geminiResponse.setPesoTotalKg(500.0);
        geminiResponse.setVolumenTotalM3(2.5);
        geminiResponse.setLargoMaxPaquete(1.2);
        geminiResponse.setTipoVehiculoRequerido("FURGONETA");
        geminiResponse.setRevisionManual(false);
        when(geminiCargaService.calcularDimensiones("10 cajas de 50kg", null)).thenReturn(geminiResponse);

        CargoAnalysisResponse response = cargoAnalysisService.calcularDimensiones("10 cajas de 50kg");

        assertNotNull(response);
        assertEquals(500.0, response.getPesoTotalKg());
        assertEquals(2.5, response.getVolumenTotalM3());
        assertFalse(response.getRevisionManual());
        verify(geminiCargaService).calcularDimensiones("10 cajas de 50kg", null);
    }

    @Test
    void testConvertirTipoVehiculo_Furgoneta() {
        TipoVehiculo tipo = cargoAnalysisService.convertirTipoVehiculo("FURGONETA");
        assertEquals(TipoVehiculo.FURGONETA, tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Rigido() {
        TipoVehiculo tipo = cargoAnalysisService.convertirTipoVehiculo("RIGIDO");
        assertEquals(TipoVehiculo.RIGIDO, tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Trailer() {
        TipoVehiculo tipo = cargoAnalysisService.convertirTipoVehiculo("TRAILER");
        assertEquals(TipoVehiculo.TRAILER, tipo);
    }

    @Test
    void testConvertirTipoVehiculo_LowerCase() {
        TipoVehiculo tipo = cargoAnalysisService.convertirTipoVehiculo("furgoneta");
        assertEquals(TipoVehiculo.FURGONETA, tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Null() {
        TipoVehiculo tipo = cargoAnalysisService.convertirTipoVehiculo(null);
        assertNull(tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Invalido() {
        TipoVehiculo tipo = cargoAnalysisService.convertirTipoVehiculo("INVALIDO");
        assertNull(tipo);
    }

    @Test
    void testConvertirTipoVehiculo_Vacio() {
        TipoVehiculo tipo = cargoAnalysisService.convertirTipoVehiculo("");
        assertNull(tipo);
    }

    @Test
    void calcularDimensiones_returnsManualReview_whenGeminiUnavailableOrFails() {
        CargoAnalysisResponse fallback = new CargoAnalysisResponse();
        fallback.setRevisionManual(true);
        fallback.setMotivoRevision("No se pudo analizar la carga automaticamente.");
        fallback.setPesoTotalKg(0.0);
        when(geminiCargaService.calcularDimensiones("carga ambigua", null)).thenReturn(fallback);

        CargoAnalysisResponse response = cargoAnalysisService.calcularDimensiones("carga ambigua");

        assertNotNull(response);
        assertTrue(response.getRevisionManual());
        assertEquals(0.0, response.getPesoTotalKg());
        verify(geminiCargaService).calcularDimensiones("carga ambigua", null);
    }

    @Test
    void calcularDimensiones_keepsManualReview_whenDevFallbackDisabled() {
        cargoAnalysisService = buildService(false);

        CargoAnalysisResponse geminiFailure = new CargoAnalysisResponse();
        geminiFailure.setPesoTotalKg(0.0);
        geminiFailure.setVolumenTotalM3(0.0);
        geminiFailure.setLargoMaxPaquete(0.0);
        geminiFailure.setRevisionManual(true);
        geminiFailure.setMotivoRevision("No se pudo analizar la carga automaticamente.");
        when(geminiCargaService.calcularDimensiones("15 cajas de 100 kg, 12 m3, largo 4 metros", null)).thenReturn(geminiFailure);

        CargoAnalysisResponse response = cargoAnalysisService.calcularDimensiones("15 cajas de 100 kg, 12 m3, largo 4 metros");

        assertTrue(response.getRevisionManual());
        assertEquals(0.0, response.getPesoTotalKg());
        assertEquals(0.0, response.getVolumenTotalM3());
        assertEquals(0.0, response.getLargoMaxPaquete());
    }

    @Test
    void calcularDimensiones_appliesDevFallback_whenEnabledAndDescriptionIsClear() {
        cargoAnalysisService = buildService(true);

        CargoAnalysisResponse geminiFailure = new CargoAnalysisResponse();
        geminiFailure.setPesoTotalKg(0.0);
        geminiFailure.setVolumenTotalM3(0.0);
        geminiFailure.setLargoMaxPaquete(0.0);
        geminiFailure.setRevisionManual(true);
        geminiFailure.setMotivoRevision("No se pudo analizar la carga automaticamente.");
        when(geminiCargaService.calcularDimensiones("Carga de 1.5 toneladas, 12 m3 y largo 4 metros", null)).thenReturn(geminiFailure);

        CargoAnalysisResponse response = cargoAnalysisService.calcularDimensiones("Carga de 1.5 toneladas, 12 m3 y largo 4 metros");

        assertFalse(response.getRevisionManual());
        assertEquals(1500.0, response.getPesoTotalKg());
        assertEquals(12.0, response.getVolumenTotalM3());
        assertEquals(4.0, response.getLargoMaxPaquete());
        assertEquals("RIGIDO", response.getTipoVehiculoRequerido());
        assertNull(response.getMotivoRevision());
    }

    @Test
    void calcularDimensiones_keepsManualReview_whenEnabledButDescriptionIsAmbiguous() {
        cargoAnalysisService = buildService(true);

        CargoAnalysisResponse geminiFailure = new CargoAnalysisResponse();
        geminiFailure.setPesoTotalKg(0.0);
        geminiFailure.setVolumenTotalM3(0.0);
        geminiFailure.setLargoMaxPaquete(0.0);
        geminiFailure.setRevisionManual(true);
        geminiFailure.setMotivoRevision("No se pudo analizar la carga automaticamente.");
        when(geminiCargaService.calcularDimensiones("Necesito mover mercaderia varias", null)).thenReturn(geminiFailure);

        CargoAnalysisResponse response = cargoAnalysisService.calcularDimensiones("Necesito mover mercaderia varias");

        assertTrue(response.getRevisionManual());
        assertEquals(0.0, response.getPesoTotalKg());
        assertEquals(0.0, response.getVolumenTotalM3());
        assertEquals(0.0, response.getLargoMaxPaquete());
    }

    @Test
    void calcularDimensiones_appliesDevFallback_whenDescriptionUsesMillimetersAndPalletCount() {
        cargoAnalysisService = buildService(true);

        CargoAnalysisResponse geminiFailure = new CargoAnalysisResponse();
        geminiFailure.setPesoTotalKg(0.0);
        geminiFailure.setVolumenTotalM3(0.0);
        geminiFailure.setLargoMaxPaquete(0.0);
        geminiFailure.setRevisionManual(true);
        geminiFailure.setMotivoRevision("No se pudo analizar la carga automaticamente.");
        when(geminiCargaService.calcularDimensiones("2 palets de comida variada de 600kg con medidas 1200mm x 800mm x 800mm", null))
                .thenReturn(geminiFailure);

        CargoAnalysisResponse response = cargoAnalysisService.calcularDimensiones(
                "2 palets de comida variada de 600kg con medidas 1200mm x 800mm x 800mm"
        );

        assertFalse(response.getRevisionManual());
        assertEquals(600.0, response.getPesoTotalKg());
        assertEquals(1.54, response.getVolumenTotalM3());
        assertEquals(1.2, response.getLargoMaxPaquete());
        assertEquals("FURGONETA", response.getTipoVehiculoRequerido());
        assertNull(response.getMotivoRevision());
    }
}
