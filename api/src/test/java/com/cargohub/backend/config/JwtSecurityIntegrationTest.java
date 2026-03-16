package com.cargohub.backend.config;

import com.cargohub.backend.controller.AuthController;
import com.cargohub.backend.controller.FacturaController;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.service.ClienteService;
import com.cargohub.backend.service.ConductorService;
import com.cargohub.backend.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = {AuthController.class, FacturaController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-for-integration-tests-which-is-long-enough",
        "security.jwt.expiration-ms=60000"
})
class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private ConductorService conductorService;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private FacturaRepository facturaRepository;

    @Test
    void login_returnsJwtAccessTokenPayload() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("admin@test.com");
        usuario.setRol(RolUsuario.ADMIN);
        usuario.setActivo(true);

        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                "admin@test.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(usuarioService.buscarPorEmail("admin@test.com")).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);

        mockMvc.perform(post("/api/auth/login")
                        .param("email", "admin@test.com")
                        .param("password", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(60))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void protectedEndpoint_withoutBearerToken_returns401() throws Exception {
        mockMvc.perform(get("/api/facturas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidAdminBearerToken_returns200() throws Exception {
        when(facturaRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/facturas")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withConductorBearerToken_returns403() throws Exception {
        mockMvc.perform(get("/api/facturas")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isForbidden());
    }
}
