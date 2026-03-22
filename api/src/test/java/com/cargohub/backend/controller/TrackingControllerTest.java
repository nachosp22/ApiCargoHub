package com.cargohub.backend.controller;

import com.cargohub.backend.config.JwtAuthenticationFilter;
import com.cargohub.backend.config.SecurityConfig;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.observability.FleetRealtimeMetrics;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.IncidenciaRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.ConductorService;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TrackingController.class)
@Import({SecurityConfig.class, OwnershipSecurityService.class})
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private ConductorService conductorService;

    @MockitoBean
    private FleetRealtimeMetrics metrics;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private ConductorRepository conductorRepository;

    @MockitoBean
    private PorteRepository porteRepository;

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
    }

    @Test
    void upsertLocation_adminRole_allowed() throws Exception {
        mockMvc.perform(post("/api/v1/tracking/drivers/7/locations")
                        .contentType("application/json")
                        .content("{" +
                                "\"lat\":40.416," +
                                "\"lon\":-3.703," +
                                "\"recordedAt\":\"2026-03-16T10:00:00Z\"," +
                                "\"speedKph\":63.5," +
                                "\"headingDeg\":180" +
                                "}")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(conductorService).actualizarUbicacion(
                eq(7L),
                eq(40.416),
                eq(-3.703),
                eq(LocalDateTime.of(2026, 3, 16, 10, 0, 0)),
                eq(63.5),
                eq(180)
        );
    }

    @Test
    void upsertLocation_conductorOwner_allowed() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("conductor@test.com");
        when(usuarioRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(usuario));

        Conductor conductor = new Conductor();
        conductor.setId(7L);
        when(conductorRepository.findByUsuarioEmail("conductor@test.com")).thenReturn(Optional.of(conductor));

        mockMvc.perform(post("/api/v1/tracking/drivers/7/locations")
                        .contentType("application/json")
                        .content("{\"lat\":40.416,\"lon\":-3.703}")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isOk());
    }

    @Test
    void upsertLocation_conductorNonOwner_forbidden() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("conductor@test.com");
        when(usuarioRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(usuario));

        Conductor conductor = new Conductor();
        conductor.setId(8L);
        when(conductorRepository.findByUsuarioEmail("conductor@test.com")).thenReturn(Optional.of(conductor));

        mockMvc.perform(post("/api/v1/tracking/drivers/7/locations")
                        .contentType("application/json")
                        .content("{\"lat\":40.416,\"lon\":-3.703}")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void upsertLocation_invalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tracking/drivers/7/locations")
                        .contentType("application/json")
                        .content("{\"lat\":120.0,\"lon\":-3.703}")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
