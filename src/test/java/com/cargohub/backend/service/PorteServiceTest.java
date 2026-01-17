package com.cargohub.backend.service;

import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PorteServiceTest {

    @Mock
    private PorteRepository porteRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private CalculadoraPrecioService calculadoraPrecio;

    @Mock
    private FacturaService facturaService;

    @Mock
    private ConductorRepository conductorRepository;

    @Mock
    private McpWebhookService mcpWebhookService;

    @InjectMocks
    private PorteService porteService;

    private Porte porte;
    private Conductor conductor;

    @BeforeEach
    void setUp() {
        porte = new Porte();
        porte.setId(1L);
        porte.setLatitudOrigen(40.416);
        porte.setLongitudOrigen(-3.703);
        porte.setLatitudDestino(41.387);
        porte.setLongitudDestino(2.170);
        porte.setPesoTotalKg(1000.0);
        porte.setTipoVehiculoRequerido(TipoVehiculo.FURGONETA);
        porte.setEstado(EstadoPorte.PENDIENTE);

        conductor = new Conductor();
        conductor.setId(1L);
        conductor.setNombre("Test Conductor");
    }

    @Test
    void testCambiarEstado() {
        // Given
        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
        when(porteRepository.save(any(Porte.class))).thenReturn(porte);

        // When
        Porte resultado = porteService.cambiarEstado(1L, EstadoPorte.ASIGNADO);

        // Then
        assertNotNull(resultado);
        assertEquals(EstadoPorte.ASIGNADO, resultado.getEstado());
        verify(porteRepository, times(1)).save(porte);
    }

    @Test
    void testCambiarEstado_PorteNoEncontrado() {
        // Given
        when(porteRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            porteService.cambiarEstado(999L, EstadoPorte.ASIGNADO);
        });
        assertEquals("Porte no encontrado", exception.getMessage());
    }

    @Test
    void testAceptarPorte() {
        // Given
        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));
        when(porteRepository.save(any(Porte.class))).thenReturn(porte);

        // When
        Porte resultado = porteService.aceptarPorte(1L, 1L);

        // Then
        assertNotNull(resultado);
        assertEquals(EstadoPorte.ASIGNADO, resultado.getEstado());
        assertEquals(conductor, resultado.getConductor());
        verify(porteRepository, times(1)).save(porte);
    }

    @Test
    void testAceptarPorte_PorteYaAsignado() {
        // Given
        porte.setEstado(EstadoPorte.ASIGNADO);
        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            porteService.aceptarPorte(1L, 1L);
        });
        assertEquals("Este viaje ya no está disponible.", exception.getMessage());
    }

    @Test
    void testAgregarAjusteManual() {
        // Given
        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
        when(porteRepository.save(any(Porte.class))).thenReturn(porte);

        // When
        Porte resultado = porteService.agregarAjusteManual(1L, -20.0, "Penalización por retraso");

        // Then
        assertNotNull(resultado);
        assertEquals(-20.0, resultado.getAjustePrecio());
        assertEquals("Penalización por retraso", resultado.getMotivoAjuste());
        verify(porteRepository, times(1)).save(porte);
    }

    @Test
    void testObtenerPorId() {
        // Given
        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));

        // When
        Porte resultado = porteService.obtenerPorId(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        verify(porteRepository, times(1)).findById(1L);
    }

    @Test
    void testObtenerPorId_PorteNoEncontrado() {
        // Given
        when(porteRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            porteService.obtenerPorId(999L);
        });
        assertEquals("Porte no encontrado", exception.getMessage());
    }

    @Test
    void testCrearPorte_ConMcpWebhook() {
        // Given
        porte.setDescripcionCliente("10 cajas de 50kg cada una, medidas 1x1x1 metros");
        
        com.cargohub.backend.dto.McpWebhookResponse mcpResponse = new com.cargohub.backend.dto.McpWebhookResponse();
        mcpResponse.setPesoTotalKg(525.0); // 500kg + 5% margen
        mcpResponse.setVolumenTotalM3(10.5); // 10m3 + 5% margen
        mcpResponse.setLargoMaxPaquete(1.05); // 1m + 5% margen
        mcpResponse.setTipoVehiculoRequerido("FURGONETA");
        mcpResponse.setRevisionManual(false);
        mcpResponse.setMotivoRevision(null);
        
        when(mcpWebhookService.calcularDimensiones(anyString())).thenReturn(mcpResponse);
        when(mcpWebhookService.convertirTipoVehiculo("FURGONETA")).thenReturn(TipoVehiculo.FURGONETA);
        when(calculadoraPrecio.calcularPrecioTotal(any(Porte.class))).thenReturn(150.0);
        when(porteRepository.save(any(Porte.class))).thenReturn(porte);
        
        // When
        Porte resultado = porteService.crearPorte(porte);
        
        // Then
        assertNotNull(resultado);
        assertEquals(525.0, resultado.getPesoTotalKg());
        assertEquals(10.5, resultado.getVolumenTotalM3());
        assertEquals(1.05, resultado.getLargoMaxPaquete());
        assertEquals(TipoVehiculo.FURGONETA, resultado.getTipoVehiculoRequerido());
        assertFalse(resultado.isRevisionManual());
        verify(mcpWebhookService, times(1)).calcularDimensiones(anyString());
        verify(porteRepository, times(1)).save(any(Porte.class));
    }

    @Test
    void testCrearPorte_ConMcpWebhook_RevisionManual() {
        // Given
        porte.setDescripcionCliente("Carga con medidas no claras");
        
        com.cargohub.backend.dto.McpWebhookResponse mcpResponse = new com.cargohub.backend.dto.McpWebhookResponse();
        mcpResponse.setPesoTotalKg(0.0);
        mcpResponse.setVolumenTotalM3(0.0);
        mcpResponse.setLargoMaxPaquete(0.0);
        mcpResponse.setTipoVehiculoRequerido(null);
        mcpResponse.setRevisionManual(true);
        mcpResponse.setMotivoRevision("Medidas no claras en la descripción");
        
        when(mcpWebhookService.calcularDimensiones(anyString())).thenReturn(mcpResponse);
        when(calculadoraPrecio.calcularPrecioTotal(any(Porte.class))).thenReturn(150.0);
        when(porteRepository.save(any(Porte.class))).thenReturn(porte);
        
        // When
        Porte resultado = porteService.crearPorte(porte);
        
        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isRevisionManual());
        assertEquals("Medidas no claras en la descripción", resultado.getMotivoRevision());
        verify(mcpWebhookService, times(1)).calcularDimensiones(anyString());
        verify(porteRepository, times(1)).save(any(Porte.class));
    }
}
