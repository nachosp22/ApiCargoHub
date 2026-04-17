package com.cargohub.backend.service;

import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.ClienteRepository;
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
class FacturaAutoGenerationTest {

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

    @Mock
    private ConductorMatchingService conductorMatchingService;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private PorteService porteService;

    private Porte porte;

    @BeforeEach
    void setUp() {
        porte = new Porte();
        porte.setId(1L);
        porte.setEstado(EstadoPorte.EN_TRANSITO);
    }

    @Test
    void cambiarEstado_toEntregado_generatesFactura() {
        Factura factura = new Factura();
        factura.setId(100L);

        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
        when(facturaService.generarFacturaParaPorte(1L)).thenReturn(factura);
        when(porteRepository.save(any(Porte.class))).thenAnswer(inv -> inv.getArgument(0));

        Porte result = porteService.cambiarEstado(1L, EstadoPorte.ENTREGADO);

        verify(facturaService).generarFacturaParaPorte(1L);
        assertEquals(EstadoPorte.FACTURADO, result.getEstado());
        assertNotNull(result.getFechaEntrega());
    }

    @Test
    void cambiarEstado_toEntregado_skipsDuplicateFactura() {
        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
        when(facturaService.generarFacturaParaPorte(1L))
                .thenThrow(new RuntimeException("Factura already exists"));
        when(porteRepository.save(any(Porte.class))).thenAnswer(inv -> inv.getArgument(0));

        Porte result = porteService.cambiarEstado(1L, EstadoPorte.ENTREGADO);

        // Should NOT throw — duplicate is caught gracefully
        verify(facturaService).generarFacturaParaPorte(1L);
        // State stays ENTREGADO since factura generation failed
        assertEquals(EstadoPorte.ENTREGADO, result.getEstado());
        assertNotNull(result.getFechaEntrega());
    }

    @Test
    void cambiarEstado_toOtherState_doesNotGenerateFactura() {
        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
        when(porteRepository.save(any(Porte.class))).thenAnswer(inv -> inv.getArgument(0));

        Porte result = porteService.cambiarEstado(1L, EstadoPorte.CANCELADO);

        verify(facturaService, never()).generarFacturaParaPorte(anyLong());
        assertEquals(EstadoPorte.CANCELADO, result.getEstado());
        assertNull(result.getFechaEntrega());
    }
}
