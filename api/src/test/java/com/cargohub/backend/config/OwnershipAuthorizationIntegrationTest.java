package com.cargohub.backend.config;

import com.cargohub.backend.controller.ClienteController;
import com.cargohub.backend.controller.ConductorController;
import com.cargohub.backend.controller.IncidenciaController;
import com.cargohub.backend.controller.PorteController;
import com.cargohub.backend.entity.Incidencia;
import com.cargohub.backend.entity.IncidenciaEvento;
import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.EstadoIncidencia;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.exception.GlobalExceptionHandler;
import com.cargohub.backend.exception.IncidenciaTransitionException;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.IncidenciaRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.ClienteService;
import com.cargohub.backend.service.ConductorService;
import com.cargohub.backend.service.IncidenciaService;
import com.cargohub.backend.service.PorteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ClienteController.class, ConductorController.class, PorteController.class, IncidenciaController.class})
@Import({SecurityConfig.class, OwnershipSecurityService.class, GlobalExceptionHandler.class})
class OwnershipAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private ConductorService conductorService;

    @MockitoBean
    private PorteService porteService;

    @MockitoBean
    private IncidenciaService incidenciaService;

    @MockitoBean
    private PorteRepository porteRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private ConductorRepository conductorRepository;

    @MockitoBean
    private BloqueoAgendaRepository bloqueoAgendaRepository;

    @MockitoBean
    private IncidenciaRepository incidenciaRepository;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain filterChain = invocation.getArgument(2);
            try {
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        when(usuarioRepository.findByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0, String.class);
            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            return Optional.of(usuario);
        });

        when(bloqueoAgendaRepository.existsByIdAndConductorId(99L, 7L)).thenReturn(true);
    }

    @Test
    void cliente_canAccessOwnClienteResource_butNotOthers() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        when(clienteRepository.findByUsuarioEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(clienteService.obtenerPorId(1L)).thenReturn(cliente);

        mockMvc.perform(get("/api/clientes/1")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/clientes/2")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void conductor_canAccessOwnConductorResource_butNotOthers() throws Exception {
        Conductor conductor = new Conductor();
        conductor.setId(7L);
        when(conductorRepository.findByUsuarioEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(conductorService.obtenerAgenda(7L, java.time.LocalDateTime.parse("2026-01-01T00:00:00"), java.time.LocalDateTime.parse("2026-01-31T23:59:59")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/conductores/7/agenda")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/conductores/8/agenda")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cliente_canReadOwnPorte_butNotOthers() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        when(clienteRepository.findByUsuarioEmail("cliente@test.com")).thenReturn(Optional.of(cliente));
        when(porteRepository.existsByIdAndClienteId(11L, 1L)).thenReturn(true);
        when(porteRepository.existsByIdAndClienteId(12L, 1L)).thenReturn(false);
        when(porteService.obtenerPorId(11L)).thenReturn(new Porte());

        mockMvc.perform(get("/api/portes/11")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portes/12")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void conductor_canChangeEstadoOnlyForOwnAssignedPorte() throws Exception {
        Conductor conductor = new Conductor();
        conductor.setId(7L);
        when(conductorRepository.findByUsuarioEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(porteRepository.existsByIdAndConductorId(21L, 7L)).thenReturn(true);
        when(porteRepository.existsByIdAndConductorId(22L, 7L)).thenReturn(false);
        when(porteService.cambiarEstado(21L, EstadoPorte.EN_TRANSITO)).thenReturn(new Porte());

        mockMvc.perform(put("/api/portes/21/estado")
                        .param("nuevo", "EN_TRANSITO")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/portes/22/estado")
                        .param("nuevo", "EN_TRANSITO")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_bypassOwnershipChecks() throws Exception {
        when(clienteService.obtenerPorId(999L)).thenReturn(new Cliente());

        mockMvc.perform(get("/api/clientes/999")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void conductor_canDeleteOwnAgendaBloqueoOnly() throws Exception {
        Conductor conductor = new Conductor();
        conductor.setId(7L);
        when(conductorRepository.findByUsuarioEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(bloqueoAgendaRepository.existsByIdAndConductorId(100L, 7L)).thenReturn(false);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/conductores/agenda/99")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/conductores/agenda/100")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void superadmin_bypassOwnershipChecks() throws Exception {
        when(porteService.obtenerPorId(500L)).thenReturn(new Porte());

        mockMvc.perform(get("/api/portes/500")
                        .with(user("super@test.com").roles("SUPERADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void incidencias_getById_allowsOwnerAndAdmin_butForbidsNonOwner() throws Exception {
        when(incidenciaRepository.existsByIdAndPorteClienteId(10L, 1L)).thenReturn(true);
        when(incidenciaRepository.existsByIdAndPorteClienteId(11L, 1L)).thenReturn(false);
        when(incidenciaService.obtenerPorId(10L)).thenReturn(new Incidencia());
        when(incidenciaService.obtenerPorId(999L)).thenReturn(new Incidencia());

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        when(clienteRepository.findByUsuarioEmail("cliente@test.com")).thenReturn(Optional.of(cliente));

        mockMvc.perform(get("/api/incidencias/10")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/incidencias/11")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/incidencias/999")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void incidencias_getById_returnsDtoShape_withoutNestedEntities() throws Exception {
        Porte porte = new Porte();
        porte.setId(123L);

        Usuario admin = new Usuario();
        admin.setId(77L);
        admin.setEmail("admin@test.com");

        Incidencia incidencia = new Incidencia();
        incidencia.setId(10L);
        incidencia.setTitulo("Retraso");
        incidencia.setDescripcion("Retraso de carga");
        incidencia.setEstado(EstadoIncidencia.EN_REVISION);
        incidencia.setPorte(porte);
        incidencia.setAdmin(admin);

        when(incidenciaService.obtenerPorId(10L)).thenReturn(incidencia);

        mockMvc.perform(get("/api/incidencias/10")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.porteId").value(123))
                .andExpect(jsonPath("$.adminId").value(77))
                .andExpect(jsonPath("$.porte").doesNotExist())
                .andExpect(jsonPath("$.admin").doesNotExist());
    }

    @Test
    void incidencias_getByPorte_allowsOwnerAndAdmin_butForbidsNonOwner() throws Exception {
        when(porteRepository.existsByIdAndClienteId(30L, 1L)).thenReturn(true);
        when(porteRepository.existsByIdAndClienteId(31L, 1L)).thenReturn(false);
        when(incidenciaService.listarPorPorte(30L)).thenReturn(List.of());
        when(incidenciaService.listarPorPorte(500L)).thenReturn(List.of());

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        when(clienteRepository.findByUsuarioEmail("cliente@test.com")).thenReturn(Optional.of(cliente));

        mockMvc.perform(get("/api/incidencias/porte/30")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/incidencias/porte/31")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/incidencias/porte/500")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void incidencias_historial_allowsOwnerAndAdmin_butForbidsNonOwner() throws Exception {
        when(incidenciaRepository.existsByIdAndPorteClienteId(44L, 1L)).thenReturn(true);
        when(incidenciaRepository.existsByIdAndPorteClienteId(45L, 1L)).thenReturn(false);
        when(incidenciaService.listarHistorial(44L)).thenReturn(List.of(new IncidenciaEvento()));
        when(incidenciaService.listarHistorial(999L)).thenReturn(List.of(new IncidenciaEvento()));

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        when(clienteRepository.findByUsuarioEmail("cliente@test.com")).thenReturn(Optional.of(cliente));

        mockMvc.perform(get("/api/incidencias/44/historial")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/incidencias/45/historial")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/incidencias/999/historial")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void incidencias_historial_returnsDtoShape_withoutNestedEntities() throws Exception {
        Incidencia incidencia = new Incidencia();
        incidencia.setId(44L);

        Usuario actor = new Usuario();
        actor.setId(22L);
        IncidenciaEvento evento = new IncidenciaEvento();
        evento.setId(99L);
        evento.setIncidencia(incidencia);
        evento.setActor(actor);
        evento.setEstadoAnterior(EstadoIncidencia.ABIERTA);
        evento.setEstadoNuevo(EstadoIncidencia.EN_REVISION);
        evento.setAccion("TRANSICION_ESTADO");
        evento.setComentario("En análisis");

        when(incidenciaService.listarHistorial(44L)).thenReturn(List.of(evento));

        mockMvc.perform(get("/api/incidencias/44/historial")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(99))
                .andExpect(jsonPath("$[0].incidenciaId").value(44))
                .andExpect(jsonPath("$[0].actorId").value(22))
                .andExpect(jsonPath("$[0].actorEmail").doesNotExist())
                .andExpect(jsonPath("$[0].incidencia").doesNotExist())
                .andExpect(jsonPath("$[0].actor").doesNotExist());
    }

    @Test
    void incidencias_vencidasSla_isAdminOnly() throws Exception {
        when(incidenciaService.listarVencidasSla()).thenReturn(List.of());

        mockMvc.perform(get("/api/incidencias/vencidas-sla")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/incidencias/vencidas-sla")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void incidencias_listAll_isAdminOnly() throws Exception {
        when(incidenciaService.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/api/incidencias")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/incidencias")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void incidencias_resolver_usesAuthenticatedAdminPrincipal_notRequestAdminId() throws Exception {
        Incidencia incidencia = new Incidencia();
        incidencia.setEstado(EstadoIncidencia.RESUELTA);
        when(incidenciaService.resolverIncidencia(eq(5L), any(), eq("resuelta"), eq(EstadoIncidencia.RESUELTA)))
                .thenReturn(incidencia);

        mockMvc.perform(put("/api/incidencias/5/resolver")
                        .contentType("application/json")
                        .content("{\"resolucion\":\"resuelta\",\"estadoFinal\":\"RESUELTA\"}")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(incidenciaService).resolverIncidencia(
                eq(5L),
                org.mockito.ArgumentMatchers.argThat(authentication -> authentication != null && "admin@test.com".equals(authentication.getName())),
                eq("resuelta"),
                eq(EstadoIncidencia.RESUELTA)
        );
    }

    @Test
    void incidencias_create_rejectsInvalidPayload_with400() throws Exception {
        mockMvc.perform(post("/api/incidencias")
                        .param("porteId", "1")
                        .contentType("application/json")
                        .content("{\"titulo\":\"\",\"descripcion\":\"\"}")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void incidencias_resolver_rejectsMissingEstadoFinal_with400() throws Exception {
        mockMvc.perform(put("/api/incidencias/5/resolver")
                        .contentType("application/json")
                        .content("{\"resolucion\":\"ok\"}")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void incidencias_resolver_invalidTransition_returns409() throws Exception {
        when(incidenciaService.resolverIncidencia(eq(5L), any(), eq("reabrir"), eq(EstadoIncidencia.EN_REVISION)))
                .thenThrow(new IncidenciaTransitionException("Transición no permitida: RESUELTA -> EN_REVISION"));

        mockMvc.perform(put("/api/incidencias/5/resolver")
                        .contentType("application/json")
                        .content("{\"resolucion\":\"reabrir\",\"estadoFinal\":\"EN_REVISION\"}")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isConflict());
    }
}
