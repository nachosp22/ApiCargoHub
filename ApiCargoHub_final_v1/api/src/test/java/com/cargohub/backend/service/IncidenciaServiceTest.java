package com.cargohub.backend.service;

import com.cargohub.backend.entity.Incidencia;
import com.cargohub.backend.entity.IncidenciaEvento;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.EstadoIncidencia;
import com.cargohub.backend.entity.enums.PrioridadIncidencia;
import com.cargohub.backend.entity.enums.SeveridadIncidencia;
import com.cargohub.backend.repository.IncidenciaEventoRepository;
import com.cargohub.backend.repository.IncidenciaRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private IncidenciaEventoRepository incidenciaEventoRepository;

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
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(incidenciaEventoRepository.save(any(IncidenciaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Incidencia resultado = incidenciaService.reportarIncidencia(1L, "Retraso en la entrega", "El camión llegó 2 horas tarde");

        // Then
        assertNotNull(resultado);
        assertEquals(EstadoIncidencia.ABIERTA, resultado.getEstado());
        assertEquals(SeveridadIncidencia.MEDIA, resultado.getSeveridad());
        assertEquals(PrioridadIncidencia.MEDIA, resultado.getPrioridad());
        assertNotNull(resultado.getFechaReporte());
        assertNotNull(resultado.getFechaLimiteSla());
        verify(incidenciaRepository, times(1)).save(any(Incidencia.class));
        verify(incidenciaEventoRepository, times(1)).save(any(IncidenciaEvento.class));
    }

    @Test
    void testReportarIncidencia_SlaAlta24Horas() {
        when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(incidenciaEventoRepository.save(any(IncidenciaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Incidencia resultado = incidenciaService.reportarIncidencia(
                1L,
                "Caida de servicio",
                "No hay entregas",
                SeveridadIncidencia.ALTA,
                PrioridadIncidencia.BAJA,
                null
        );

        long horas = java.time.Duration.between(resultado.getFechaReporte(), resultado.getFechaLimiteSla()).toHours();
        assertEquals(24, horas);
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
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incidencia);
        when(incidenciaEventoRepository.save(any(IncidenciaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin@test.com", null, List.of());

        // When
        Incidencia resultado = incidenciaService.resolverIncidencia(1L, authentication, "Se ha compensado al cliente", EstadoIncidencia.RESUELTA);

        // Then
        assertNotNull(resultado);
        assertEquals(EstadoIncidencia.RESUELTA, resultado.getEstado());
        assertEquals("Se ha compensado al cliente", resultado.getResolucion());
        assertEquals(admin, resultado.getAdmin());
        assertNotNull(resultado.getFechaResolucion());
        verify(incidenciaRepository, times(1)).save(incidencia);
        verify(incidenciaEventoRepository, times(1)).save(any(IncidenciaEvento.class));
    }

    @Test
    void testResolverIncidencia_TransicionAEnRevisionPermitida() {
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incidencia);
        when(incidenciaEventoRepository.save(any(IncidenciaEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin@test.com", null, List.of());

        Incidencia resultado = incidenciaService.resolverIncidencia(1L, authentication, null, EstadoIncidencia.EN_REVISION);

        assertNotNull(resultado);
        assertEquals(EstadoIncidencia.EN_REVISION, resultado.getEstado());
        assertNull(resultado.getResolucion());
        verify(incidenciaRepository, times(1)).save(incidencia);
        verify(incidenciaEventoRepository, times(1)).save(any(IncidenciaEvento.class));
    }

    @Test
    void testResolverIncidencia_TransicionInvalidaDesdeAbierta() {
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin@test.com", null, List.of());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                incidenciaService.resolverIncidencia(1L, authentication, "comentario", EstadoIncidencia.ABIERTA)
        );

        assertEquals("Estado final no permitido para resolver incidencias: ABIERTA", exception.getMessage());
        verify(incidenciaRepository, never()).save(any(Incidencia.class));
    }

    @Test
    void testResolverIncidencia_NoPermiteReaperturaDesdeTerminal() {
        incidencia.setEstado(EstadoIncidencia.RESUELTA);
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin@test.com", null, List.of());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                incidenciaService.resolverIncidencia(1L, authentication, "reabrir", EstadoIncidencia.EN_REVISION)
        );

        assertEquals("Transición no permitida: RESUELTA -> EN_REVISION", exception.getMessage());
        verify(incidenciaRepository, never()).save(any(Incidencia.class));
    }

    @Test
    void testResolverIncidencia_ResolucionObligatoriaParaEstadoTerminal() {
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin@test.com", null, List.of());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                incidenciaService.resolverIncidencia(1L, authentication, "  ", EstadoIncidencia.RESUELTA)
        );

        assertEquals("La resolución es obligatoria para estados finales RESUELTA/DESESTIMADA", exception.getMessage());
        verify(incidenciaRepository, never()).save(any(Incidencia.class));
    }

    @Test
    void testListarPendientes() {
        // Given
        List<Incidencia> incidencias = Arrays.asList(incidencia);
        when(incidenciaRepository.findByEstadoIn(anyCollection())).thenReturn(incidencias);

        // When
        List<Incidencia> resultado = incidenciaService.listarPendientes();

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Retraso en la entrega", resultado.get(0).getTitulo());
        ArgumentCaptor<Collection<EstadoIncidencia>> estadosCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(incidenciaRepository, times(1)).findByEstadoIn(estadosCaptor.capture());
        Collection<EstadoIncidencia> estadosConsultados = estadosCaptor.getValue();
        assertTrue(estadosConsultados.contains(EstadoIncidencia.ABIERTA));
        assertTrue(estadosConsultados.contains(EstadoIncidencia.EN_REVISION));
    }

    @Test
    void testListarVencidasSla() {
        List<Incidencia> incidencias = Arrays.asList(incidencia);
        when(incidenciaRepository.findByEstadoInAndFechaLimiteSlaBefore(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(incidencias);

        List<Incidencia> resultado = incidenciaService.listarVencidasSla();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        ArgumentCaptor<Collection<EstadoIncidencia>> estadosCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(incidenciaRepository).findByEstadoInAndFechaLimiteSlaBefore(estadosCaptor.capture(), any(LocalDateTime.class));
        Collection<EstadoIncidencia> estadosConsultados = estadosCaptor.getValue();
        assertTrue(estadosConsultados.contains(EstadoIncidencia.ABIERTA));
        assertTrue(estadosConsultados.contains(EstadoIncidencia.EN_REVISION));
    }

    @Test
    void testListarHistorial() {
        when(incidenciaRepository.existsById(1L)).thenReturn(true);
        when(incidenciaEventoRepository.findByIncidenciaIdOrderByFechaAsc(1L)).thenReturn(List.of(new IncidenciaEvento()));

        List<IncidenciaEvento> resultado = incidenciaService.listarHistorial(1L);

        assertEquals(1, resultado.size());
        verify(incidenciaEventoRepository).findByIncidenciaIdOrderByFechaAsc(1L);
    }

    @Test
    void testListarHistorial_IncidenciaNoEncontrada() {
        when(incidenciaRepository.existsById(anyLong())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> incidenciaService.listarHistorial(55L));
        assertEquals("Incidencia no encontrada", exception.getMessage());
        verify(incidenciaEventoRepository, never()).findByIncidenciaIdOrderByFechaAsc(anyLong());
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
