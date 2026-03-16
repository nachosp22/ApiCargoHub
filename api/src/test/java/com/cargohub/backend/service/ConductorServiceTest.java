package com.cargohub.backend.service;

import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import com.cargohub.backend.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConductorServiceTest {

    @Mock
    private ConductorRepository conductorRepository;

    @Mock
    private BloqueoAgendaRepository bloqueoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private ConductorService conductorService;

    private Conductor conductor;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("conductor@test.com");
        usuario.setActivo(true);

        conductor = new Conductor();
        conductor.setId(1L);
        conductor.setNombre("Test Conductor");
        conductor.setDni("12345678A");
        conductor.setUsuario(usuario);
        conductor.setDisponible(true);
    }

    @Test
    void testObtenerPorId() {
        // Given
        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));

        // When
        Conductor resultado = conductorService.obtenerPorId(1L);

        // Then
        assertNotNull(resultado);
        assertEquals("Test Conductor", resultado.getNombre());
        verify(conductorRepository, times(1)).findById(1L);
    }

    @Test
    void testObtenerPorId_NoEncontrado() {
        // Given
        when(conductorRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            conductorService.obtenerPorId(999L);
        });
        assertEquals("Conductor no encontrado", exception.getMessage());
    }

    @Test
    void testActualizarUbicacion() {
        // Given
        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));
        when(conductorRepository.save(any(Conductor.class))).thenReturn(conductor);

        // When
        conductorService.actualizarUbicacion(1L, 40.416, -3.703);

        // Then
        verify(conductorRepository, times(1)).save(conductor);
        assertEquals(40.416, conductor.getLatitudActual());
        assertEquals(-3.703, conductor.getLongitudActual());
        assertNotNull(conductor.getUltimaActualizacionUbicacion());
        assertTrue(conductor.isDisponible());
    }

    @Test
    void testDarDeBajaConductor() {
        // Given
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(1L);
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);

        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));
        when(conductorRepository.save(any(Conductor.class))).thenReturn(conductor);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(vehiculoRepository.findByConductorId(1L)).thenReturn(Arrays.asList(vehiculo));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenReturn(vehiculo);

        // When
        conductorService.darDeBajaConductor(1L);

        // Then
        verify(conductorRepository, times(1)).save(conductor);
        verify(usuarioRepository, times(1)).save(usuario);
        verify(vehiculoRepository, times(1)).save(vehiculo);
        assertFalse(conductor.isDisponible());
        assertFalse(usuario.isActivo());
        assertEquals(EstadoVehiculo.BAJA, vehiculo.getEstado());
    }

    @Test
    void testGuardarOActualizar() {
        // Given
        when(conductorRepository.save(any(Conductor.class))).thenReturn(conductor);

        // When
        Conductor resultado = conductorService.guardarOActualizar(conductor);

        // Then
        assertNotNull(resultado);
        assertEquals("Test Conductor", resultado.getNombre());
        verify(conductorRepository, times(1)).save(conductor);
    }

    @Test
    void testCambiarDisponibilidad() {
        // Given
        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));
        when(conductorRepository.save(any(Conductor.class))).thenReturn(conductor);

        // When
        conductorService.cambiarDisponibilidad(1L, false);

        // Then
        verify(conductorRepository, times(1)).save(conductor);
        assertFalse(conductor.isDisponible());
    }
}
