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

    private ConductorMatchingService service;

    @BeforeEach
    void setUp() {
        service = new ConductorMatchingService(conductorRepository, bloqueoAgendaRepository,
                bloqueoRecurrenteRepository, vehiculoRepository);
    }

    private Conductor createConductor(Long id, String ciudad, Double rating) {
        Conductor c = new Conductor();
        c.setId(id);
        c.setCiudadBase(ciudad);
        c.setRating(rating);
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

        Conductor c1 = createConductor(1L, "Madrid", 4.5);
        Conductor c2 = createConductor(2L, "Barcelona", 4.0);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1, c2)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());

        List<Conductor> result = service.buscarDisponibles(fecha, null, null);

        assertEquals(2, result.size());
        // Sorted by rating desc
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    // --- ciudad filter ---

    @Test
    void buscarDisponibles_filtersByCiudad() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid", 4.0);
        Conductor c2 = createConductor(2L, "Barcelona", 4.0);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1, c2)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());

        List<Conductor> result = service.buscarDisponibles(fecha, null, "Madrid");

        assertEquals(1, result.size());
        assertEquals("Madrid", result.get(0).getCiudadBase());
    }

    // --- bloqueo agenda filter ---

    @Test
    void buscarDisponibles_excludesBlockedByAgenda() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid", 4.0);
        Conductor c2 = createConductor(2L, "Madrid", 4.5);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1, c2)));
        // c1 is blocked
        when(bloqueoAgendaRepository.estaBloqueado(eq(1L), any(), any())).thenReturn(true);
        when(bloqueoAgendaRepository.estaBloqueado(eq(2L), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());

        List<Conductor> result = service.buscarDisponibles(fecha, null, null);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    // --- bloqueo recurrente filter ---

    @Test
    void buscarDisponibles_excludesBlockedByRecurrente() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid", 4.0);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);

        BloqueoRecurrente bloqueo = new BloqueoRecurrente();
        bloqueo.setActivo(true);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(1L, 3)).thenReturn(Optional.of(bloqueo));

        List<Conductor> result = service.buscarDisponibles(fecha, null, null);

        assertTrue(result.isEmpty());
    }

    // --- vehicle type filter ---

    @Test
    void buscarDisponibles_filtersByVehicleType() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid", 4.0);
        Conductor c2 = createConductor(2L, "Madrid", 4.5);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1, c2)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());

        // c1 has FURGONETA, c2 has TRAILER
        Vehiculo v1 = new Vehiculo();
        v1.setTipo(TipoVehiculo.FURGONETA);
        v1.setEstado(EstadoVehiculo.DISPONIBLE);
        when(vehiculoRepository.findByConductorId(1L)).thenReturn(List.of(v1));

        Vehiculo v2 = new Vehiculo();
        v2.setTipo(TipoVehiculo.TRAILER);
        v2.setEstado(EstadoVehiculo.DISPONIBLE);
        when(vehiculoRepository.findByConductorId(2L)).thenReturn(List.of(v2));

        List<Conductor> result = service.buscarDisponibles(fecha, TipoVehiculo.FURGONETA, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    // --- inactive user filter ---

    @Test
    void buscarDisponibles_excludesInactiveUsers() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid", 4.0);
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

    // --- sorting by rating ---

    @Test
    void buscarDisponibles_sortsByRatingDescending() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 15, 10, 0);

        Conductor c1 = createConductor(1L, "Madrid", 3.0);
        Conductor c2 = createConductor(2L, "Madrid", 5.0);
        Conductor c3 = createConductor(3L, "Madrid", 4.0);

        when(conductorRepository.findCandidatosDisponibles("3")).thenReturn(new ArrayList<>(List.of(c1, c2, c3)));
        when(bloqueoAgendaRepository.estaBloqueado(anyLong(), any(), any())).thenReturn(false);
        when(bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(anyLong(), eq(3))).thenReturn(Optional.empty());

        List<Conductor> result = service.buscarDisponibles(fecha, null, null);

        assertEquals(3, result.size());
        assertEquals(2L, result.get(0).getId()); // 5.0
        assertEquals(3L, result.get(1).getId()); // 4.0
        assertEquals(1L, result.get(2).getId()); // 3.0
    }
}
