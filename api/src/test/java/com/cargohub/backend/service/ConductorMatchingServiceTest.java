package com.cargohub.backend.service;

import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.entity.BloqueoRecurrente;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.BloqueoRecurrenteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConductorMatchingServiceTest {

    @Mock
    private ConductorRepository conductorRepository;

    @Mock
    private BloqueoAgendaRepository bloqueoAgendaRepository;

    @Mock
    private BloqueoRecurrenteRepository bloqueoRecurrenteRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private PorteRepository porteRepository;

    private ConductorMatchingService service;

    @BeforeEach
    void setUp() {
        service = new ConductorMatchingService(conductorRepository, bloqueoAgendaRepository,
                bloqueoRecurrenteRepository, vehiculoRepository, porteRepository);
    }

    private Conductor createConductor(Long id, String ciudad) {
        Conductor c = new Conductor();
        c.setId(id);
        c.setCiudadBase(ciudad);
        c.setDisponible(true);
        // Active user
        Usuario u = new Usuario();
        u.setActivo(true);
        c.setUsuario(u);
        return c;
    }

    // --- null fecha ---

    @Test
    void buscarDisponibles_returnsEmpty_whenFechaNull() {
        List<Conductor> result = service.buscarDisponibles(null, null, null);
        assertTrue(result.isEmpty());
    }

    // --- basic filtering ---

    @Test
    void buscarDisponibles_returnsAllCandidates_whenNoFilters() {
        // Wednesday = day 3
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0); // Wednesday

        Conductor c1 = createConductor(1L, "Madrid");
        Conductor c2 = createConductor(2L, "Barcelona");

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1, c2)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());
        when(porteRepository.tieneViajeEnFecha(anyLong(), any(), any())).thenReturn(false);

        List<Conductor> result = service.buscarDisponibles(fecha, null, null);

        assertEquals(2, result.size());
        // Deterministic order by id
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    // --- ciudad filter ---

    @Test
    void buscarDisponibles_noFiltraPorCiudadEnMvpActual() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid");
        Conductor c2 = createConductor(2L, "Barcelona");

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1, c2)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());
        when(porteRepository.tieneViajeEnFecha(anyLong(), any(), any())).thenReturn(false);

        List<Conductor> result = service.buscarDisponibles(fecha, null, "Madrid");

        assertEquals(2, result.size());
    }

    // --- bloqueo agenda filter ---

    @Test
    void buscarDisponibles_excludesBlockedByAgenda() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid");

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());
        when(porteRepository.tieneViajeEnFecha(anyLong(), any(), any())).thenReturn(false);

        // c1 has FURGONETA, c2 has TRAILER
        Vehiculo v1 = new Vehiculo();
        v1.setTipo(TipoVehiculo.FURGONETA);
        v1.setEstado(EstadoVehiculo.DISPONIBLE);
        when(vehiculoRepository.findByConductorId(1L)).thenReturn(List.of(v1));

        List<Conductor> result = service.buscarDisponibles(fecha, TipoVehiculo.FURGONETA, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    // --- inactive user filter ---

    @Test
    void buscarDisponibles_excludesInactiveUsers() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid");
        Usuario inactiveUser = new Usuario();
        inactiveUser.setActivo(false);
        c1.setUsuario(inactiveUser);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));

        List<Conductor> result = service.buscarDisponibles(fecha, null, null);

        assertTrue(result.isEmpty());
    }

    // --- empty results ---

    @Test
    void buscarDisponibles_returnsEmptyList_whenNoCandidates() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);
        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>());

        List<Conductor> result = service.buscarDisponibles(fecha, TipoVehiculo.RIGIDO, "Sevilla");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- sorting ---

    @Test
    void buscarDisponibles_sortsByIdAscending() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid");
        Conductor c2 = createConductor(2L, "Madrid");
        Conductor c3 = createConductor(3L, "Madrid");

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1, c2, c3)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());
        when(porteRepository.tieneViajeEnFecha(anyLong(), any(), any())).thenReturn(false);

        List<Conductor> result = service.buscarDisponibles(fecha, null, null);

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(3L, result.get(2).getId());
    }

    // --- MVP strict distance/radio filters ---

    @Test
    void buscarDisponibles_excludesConductorWithNullRadio() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid");
        c1.setRadioAccionKm(null);
        c1.setLatitudBase(40.0);
        c1.setLongitudBase(-3.0);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());
        when(porteRepository.tieneViajeEnFecha(anyLong(), any(), any())).thenReturn(false);

        List<Conductor> result = service.buscarDisponibles(fecha, null, null, 40.0, -3.0);

        assertTrue(result.isEmpty());
    }

    @Test
    void buscarDisponibles_excludesConductorWithZeroRadio() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid");
        c1.setRadioAccionKm(0);
        c1.setLatitudBase(40.0);
        c1.setLongitudBase(-3.0);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());
        when(porteRepository.tieneViajeEnFecha(anyLong(), any(), any())).thenReturn(false);

        List<Conductor> result = service.buscarDisponibles(fecha, null, null, 40.0, -3.0);

        assertTrue(result.isEmpty());
    }

    @Test
    void buscarDisponibles_excludesConductorWithMissingBaseCoords() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid");
        c1.setRadioAccionKm(50);
        c1.setLatitudBase(null);
        c1.setLongitudBase(null);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());
        when(porteRepository.tieneViajeEnFecha(anyLong(), any(), any())).thenReturn(false);

        List<Conductor> result = service.buscarDisponibles(fecha, null, null, 40.0, -3.0);

        assertTrue(result.isEmpty());
    }

    @Test
    void buscarDisponibles_includesConductorWithinRadio() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid");
        c1.setRadioAccionKm(100);
        c1.setLatitudBase(40.4168);
        c1.setLongitudBase(-3.7038);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());
        when(porteRepository.tieneViajeEnFecha(anyLong(), any(), any())).thenReturn(false);

        // Madrid origin ~0 km from base
        List<Conductor> result = service.buscarDisponibles(fecha, null, null, 40.4168, -3.7038);

        assertEquals(1, result.size());
    }

    // --- range-based filters ---

    @Test
    void buscarDisponibles_excludesBlockedByAgendaInRange() {
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 15, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 4, 17, 18, 0);

        Conductor c1 = createConductor(1L, "Madrid");

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));
        when(bloqueoAgendaRepository.estaBloqueado(eq(1L), eq(inicio), eq(fin))).thenReturn(true);

        List<Conductor> result = service.buscarDisponibles(inicio, fin, null, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void buscarDisponibles_excludesBlockedByRecurrenteAnyDayInRange() {
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 15, 10, 0); // Wednesday (3)
        LocalDateTime fin = LocalDateTime.of(2026, 4, 17, 18, 0);    // Friday (5)

        Conductor c1 = createConductor(1L, "Madrid");

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(1L, 3)).thenReturn(Optional.empty());
        // Thursday (4) has recurrent block
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(1L, 4)).thenReturn(Optional.of(new BloqueoRecurrente() {{ setActivo(true); }}));

        List<Conductor> result = service.buscarDisponibles(inicio, fin, null, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void buscarDisponibles_excludesConductorWithOverlappingPorte() {
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 15, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 4, 16, 18, 0);

        Conductor c1 = createConductor(1L, "Madrid");

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), anyInt())).thenReturn(Optional.empty());
        when(porteRepository.tieneViajeEnFecha(1L, inicio, fin)).thenReturn(true);

        List<Conductor> result = service.buscarDisponibles(inicio, fin, null, null, null, null);

        assertTrue(result.isEmpty());
    }
}
