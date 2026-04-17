package com.cargohub.backend.controller;

import com.cargohub.backend.config.JwtAuthenticationFilter;
import com.cargohub.backend.config.JwtService;
import com.cargohub.backend.config.SecurityConfig;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.BloqueoRecurrenteService;
import com.cargohub.backend.service.ConductorMatchingService;
import com.cargohub.backend.service.ConductorService;
import com.cargohub.backend.service.VehiculoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = ConductorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-for-integration-tests-which-is-long-enough",
        "security.jwt.expiration-ms=60000"
})
class ConductorAprobacionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConductorService conductorService;

    @MockitoBean
    private VehiculoService vehiculoService;

    @MockitoBean
    private BloqueoRecurrenteService bloqueoRecurrenteService;

    @MockitoBean
    private ConductorMatchingService conductorMatchingService;

    @MockitoBean
    private OwnershipSecurityService ownershipSecurityService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void aprobar_withAdminRole_returns200() throws Exception {
        Conductor conductor = new Conductor();
        conductor.setId(1L);
        conductor.setNombre("Juan");
        when(conductorService.aprobarConductor(1L)).thenReturn(conductor);

        mockMvc.perform(post("/api/conductores/1/aprobar")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Juan"));
    }

    @Test
    void aprobar_withClienteRole_returns403() throws Exception {
        mockMvc.perform(post("/api/conductores/1/aprobar")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprobar_withConductorRole_returns403() throws Exception {
        mockMvc.perform(post("/api/conductores/1/aprobar")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprobar_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/conductores/1/aprobar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazar_withAdminRole_returns200() throws Exception {
        mockMvc.perform(post("/api/conductores/1/rechazar")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazar_withClienteRole_returns403() throws Exception {
        mockMvc.perform(post("/api/conductores/1/rechazar")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazar_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/conductores/1/rechazar"))
                .andExpect(status().isUnauthorized());
    }
}
