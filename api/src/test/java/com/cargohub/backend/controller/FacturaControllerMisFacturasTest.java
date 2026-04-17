package com.cargohub.backend.controller;

import com.cargohub.backend.config.JwtAuthenticationFilter;
import com.cargohub.backend.config.JwtService;
import com.cargohub.backend.config.SecurityConfig;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.FacturaPdfService;
import com.cargohub.backend.entity.Factura;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = FacturaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-for-integration-tests-which-is-long-enough",
        "security.jwt.expiration-ms=60000"
})
class FacturaControllerMisFacturasTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FacturaRepository facturaRepository;

    @MockitoBean
    private OwnershipSecurityService ownershipSecurityService;

    @MockitoBean
    private FacturaPdfService facturaPdfService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void misFacturas_withClienteRole_returns200() throws Exception {
        when(ownershipSecurityService.resolveClienteIdFromAuth(any())).thenReturn(5L);
        when(facturaRepository.findByClienteId(5L)).thenReturn(List.of());

        mockMvc.perform(get("/api/facturas/mis-facturas")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void misFacturas_withAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/facturas/mis-facturas")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void misFacturas_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/facturas/mis-facturas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void misFacturas_whenNoClienteAssociated_returns403() throws Exception {
        when(ownershipSecurityService.resolveClienteIdFromAuth(any())).thenReturn(null);

        mockMvc.perform(get("/api/facturas/mis-facturas")
                        .with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }
}
