package com.cargohub.backend.service;

import com.cargohub.backend.entity.Incidencia;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.EstadoIncidencia;
import com.cargohub.backend.repository.IncidenciaRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private PorteRepository porteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private IncidenciaService incidenciaService;

    private Porte porte;
    private Usuario admin;
    private Incidencia incidencia;

    @BeforeEach
    void setUp() {
        porte = new Porte();
        porte.setId(1L);
        porte.setOrigen("Madrid");
        porte.setDestino("Barcelona");

        admin = new Usuario();
        admin.setId(1L);
        admin.setEmail("admin@test.com");

        incidencia = new Incidencia();
        incidencia.setId(1L);
        incidencia.setTitulo("Retraso en la entrega");
        incidencia.setDescripcion("El camión llegó 2 horas tarde");
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setPorte(porte);
    }

    @Test
    void testReportarIncidencia() {
        // Given
        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incidencia);

        // When
        Incidencia resultado = incidenciaService.reportarIncidencia(1L, "Retraso en la entrega", "El camión llegó 2 horas tarde");

        // Then
        assertNotNull(resultado);
        assertEquals(EstadoIncidencia.ABIERTA, resultado.getEstado());
        assertNotNull(resultado.getFechaReporte());
        verify(incidenciaRepository, times(1)).save(any(Incidencia.class));
    }

    @Test
    void testReportarIncidencia_PorteNoEncontrado() {
        // Given
        when(porteRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            incidenciaService.reportarIncidencia(999L, "Titulo", "Descripcion");
        });
        assertEquals("Porte no encontrado", exception.getMessage());
    }

    @Test
    void testResolverIncidencia() {
        // Given
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incidencia);

        // When
        Incidencia resultado = incidenciaService.resolverIncidencia(1L, 1L, "Se ha compensado al cliente", EstadoIncidencia.RESUELTA);

        // Then
        assertNotNull(resultado);
        assertEquals(EstadoIncidencia.RESUELTA, resultado.getEstado());
        assertEquals("Se ha compensado al cliente", resultado.getResolucion());
        assertEquals(admin, resultado.getAdmin());
        assertNotNull(resultado.getFechaResolucion());
        verify(incidenciaRepository, times(1)).save(incidencia);
    }

    @Test
    void testListarPendientes() {
        // Given
        List<Incidencia> incidencias = Arrays.asList(incidencia);
        when(incidenciaRepository.findByEstado(EstadoIncidencia.ABIERTA)).thenReturn(incidencias);

        // When
        List<Incidencia> resultado = incidenciaService.listarPendientes();

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Retraso en la entrega", resultado.get(0).getTitulo());
        verify(incidenciaRepository, times(1)).findByEstado(EstadoIncidencia.ABIERTA);
    }

    @Test
    void testListarPorPorte() {
        // Given
        List<Incidencia> incidencias = Arrays.asList(incidencia);
        when(incidenciaRepository.findByPorteId(1L)).thenReturn(incidencias);

        // When
        List<Incidencia> resultado = incidenciaService.listarPorPorte(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(incidenciaRepository, times(1)).findByPorteId(1L);
    }

    @Test
    void testListarTodas() {
        // Given
        List<Incidencia> incidencias = Arrays.asList(incidencia);
        when(incidenciaRepository.findAll()).thenReturn(incidencias);

        // When
        List<Incidencia> resultado = incidenciaService.listarTodas();

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(incidenciaRepository, times(1)).findAll();
    }

    @Test
    void testObtenerPorId() {
        // Given
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));

        // When
        Incidencia resultado = incidenciaService.obtenerPorId(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Retraso en la entrega", resultado.getTitulo());
        verify(incidenciaRepository, times(1)).findById(1L);
    }

    @Test
    void testObtenerPorId_NoEncontrada() {
        // Given
        when(incidenciaRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            incidenciaService.obtenerPorId(999L);
        });
        assertEquals("Incidencia no encontrada", exception.getMessage());
    }
}
