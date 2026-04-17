package com.cargohub.backend.controller;

import com.cargohub.backend.config.JwtAuthenticationFilter;
import com.cargohub.backend.config.SecurityConfig;
import com.cargohub.backend.dto.tracking.PorteTrackingResponse;
import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.IncidenciaRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.PorteService;
import com.cargohub.backend.service.PorteTrackingService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = PorteController.class)
@Import({SecurityConfig.class, OwnershipSecurityService.class})
class PorteTrackingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private PorteService porteService;

    @MockitoBean
    private PorteTrackingService porteTrackingService;

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
            filterChain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void getTracking_clienteOwner_returns200() throws Exception {
        // Setup: cliente owns the porte
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@test.com");
        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(usuario));

        Cliente cliente = new Cliente();
        cliente.setId(5L);
        when(clienteRepository.findByUsuarioEmail("cliente@test.com")).thenReturn(Optional.of(cliente));

        when(porteRepository.existsByIdAndClienteId(10L, 5L)).thenReturn(true);

        PorteTrackingResponse trackingResponse = new PorteTrackingResponse();
        trackingResponse.setStatus(EstadoPorte.EN_TRANSITO);
        trackingResponse.setOriginName("Madrid");
        trackingResponse.setDestinationName("Barcelona");
        when(porteTrackingService.getTracking(10L)).thenReturn(trackingResponse);

        mockMvc.perform(get("/api/portes/10/tracking")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_TRANSITO"))
                .andExpect(jsonPath("$.originName").value("Madrid"));
    }

    @Test
    void getTracking_clienteNonOwner_returns403() throws Exception {
        // Setup: cliente does NOT own the porte
        Usuario usuario = new Usuario();
        usuario.setEmail("other@test.com");
        when(usuarioRepository.findByEmail("other@test.com")).thenReturn(Optional.of(usuario));

        Cliente otherCliente = new Cliente();
        otherCliente.setId(99L);
        when(clienteRepository.findByUsuarioEmail("other@test.com")).thenReturn(Optional.of(otherCliente));

        when(porteRepository.existsByIdAndClienteId(10L, 99L)).thenReturn(false);

        mockMvc.perform(get("/api/portes/10/tracking")
                        .with(user("other@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTracking_admin_returns200() throws Exception {
        PorteTrackingResponse trackingResponse = new PorteTrackingResponse();
        trackingResponse.setStatus(EstadoPorte.PENDIENTE);
        when(porteTrackingService.getTracking(10L)).thenReturn(trackingResponse);

        mockMvc.perform(get("/api/portes/10/tracking")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void getTracking_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/portes/10/tracking"))
                .andExpect(status().isUnauthorized());
    }
}
