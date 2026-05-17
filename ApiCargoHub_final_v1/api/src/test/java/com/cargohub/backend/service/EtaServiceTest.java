package com.cargohub.backend.service;

import com.cargohub.backend.config.FleetRealtimeProperties;
import com.cargohub.backend.dto.tracking.EtaMethod;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.observability.FleetRealtimeMetrics;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EtaServiceTest {

    @Mock
    private ConductorRepository conductorRepository;
    @Mock
    private PorteRepository porteRepository;
    @Mock
    private MapaService mapaService;

    private FleetRealtimeProperties properties;
    private FleetRealtimeMetrics metrics;
    private Clock clock;

    @BeforeEach
    void setUp() {
        properties = new FleetRealtimeProperties();
        properties.setEnabled(true);
        metrics = new FleetRealtimeMetrics();
        clock = Clock.fixed(Instant.parse("2026-03-16T10:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    void estimate_usesRouteProviderWhenAvailable() {
        Conductor conductor = new Conductor();
        conductor.setId(1L);
        conductor.setLatitudActual(40.4);
        conductor.setLongitudActual(-3.7);

        Porte porte = new Porte();
        porte.setId(2L);
        porte.setEstado(EstadoPorte.ASIGNADO);
        porte.setLatitudOrigen(41.0);
        porte.setLongitudOrigen(-4.0);

        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));
        when(porteRepository.findById(2L)).thenReturn(Optional.of(porte));
        when(mapaService.obtenerDistanciaMetros(40.4, -3.7, 41.0, -4.0)).thenReturn(10000.0);

        EtaService service = new EtaService(conductorRepository, porteRepository, mapaService, properties, metrics, clock);

        var response = service.estimate(1L, 2L);
        assertEquals(EtaMethod.ROUTE_PROVIDER, response.getMethod());
        assertEquals(12, response.getEtaMinutes());
    }

    @Test
    void estimate_fallsBackToHaversineWhenProviderFails() {
        Conductor conductor = new Conductor();
        conductor.setId(1L);
        conductor.setLatitudActual(40.4);
        conductor.setLongitudActual(-3.7);

        Porte porte = new Porte();
        porte.setId(2L);
        porte.setEstado(EstadoPorte.ASIGNADO);
        porte.setLatitudOrigen(41.0);
        porte.setLongitudOrigen(-4.0);

        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));
        when(porteRepository.findById(2L)).thenReturn(Optional.of(porte));
        when(mapaService.obtenerDistanciaMetros(40.4, -3.7, 41.0, -4.0)).thenReturn(null);

        EtaService service = new EtaService(conductorRepository, porteRepository, mapaService, properties, metrics, clock);

        var response = service.estimate(1L, 2L);
        assertEquals(EtaMethod.HAVERSINE_FALLBACK, response.getMethod());
    }

    @Test
    void estimate_whenPorteEnTransito_targetsDestination() {
        Conductor conductor = new Conductor();
        conductor.setId(1L);
        conductor.setLatitudActual(40.4);
        conductor.setLongitudActual(-3.7);

        Porte porte = new Porte();
        porte.setId(2L);
        porte.setEstado(EstadoPorte.EN_TRANSITO);
        porte.setLatitudOrigen(41.0);
        porte.setLongitudOrigen(-4.0);
        porte.setLatitudDestino(42.0);
        porte.setLongitudDestino(-5.0);

        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));
        when(porteRepository.findById(2L)).thenReturn(Optional.of(porte));
        when(mapaService.obtenerDistanciaMetros(40.4, -3.7, 42.0, -5.0)).thenReturn(10000.0);

        EtaService service = new EtaService(conductorRepository, porteRepository, mapaService, properties, metrics, clock);

        var response = service.estimate(1L, 2L);
        assertEquals(EtaMethod.ROUTE_PROVIDER, response.getMethod());
        assertEquals(12, response.getEtaMinutes());
    }

    @Test
    void estimate_whenPorteClosed_returnsNoActiveEta() {
        Conductor conductor = new Conductor();
        conductor.setId(1L);
        conductor.setLatitudActual(40.4);
        conductor.setLongitudActual(-3.7);

        Porte porte = new Porte();
        porte.setId(2L);
        porte.setEstado(EstadoPorte.ENTREGADO);

        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));
        when(porteRepository.findById(2L)).thenReturn(Optional.of(porte));

        EtaService service = new EtaService(conductorRepository, porteRepository, mapaService, properties, metrics, clock);

        var response = service.estimate(1L, 2L);
        assertNull(response.getEtaMinutes());
        assertNull(response.getMethod());
    }
}
