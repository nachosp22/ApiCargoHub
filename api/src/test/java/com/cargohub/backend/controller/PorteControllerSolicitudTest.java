package com.cargohub.backend.controller;

import com.cargohub.backend.config.JwtAuthenticationFilter;
import com.cargohub.backend.config.JwtService;
import com.cargohub.backend.config.SecurityConfig;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.PorteService;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = PorteController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-for-integration-tests-which-is-long-enough",
        "security.jwt.expiration-ms=60000"
})
class PorteControllerSolicitudTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PorteService porteService;

    @MockitoBean
    private OwnershipSecurityService ownershipSecurityService;

    @MockitoBean
    private com.cargohub.backend.service.PorteTrackingService porteTrackingService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String VALID_SOLICITUD_JSON = """
            {
                "origen": "Madrid",
                "destino": "Barcelona",
                "descripcionCliente": "10 cajas de libros",
                "fechaRecogida": "2026-05-01T10:00:00"
            }
            """;

    @Test
    void solicitud_withClienteRole_returns200() throws Exception {
        Porte porte = new Porte();
        porte.setId(1L);
        porte.setOrigen("Madrid");
        porte.setDestino("Barcelona");
        porte.setEstado(EstadoPorte.PENDIENTE);

        when(ownershipSecurityService.resolveClienteIdFromAuth(any())).thenReturn(10L);
        when(porteService.crearPorteDesdeSolicitud(any(), anyLong())).thenReturn(porte);

        mockMvc.perform(post("/api/portes/solicitud")
                        .with(user("cliente@test.com").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SOLICITUD_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.origen").value("Madrid"));
    }

    @Test
    void solicitud_withAdminRole_returns403() throws Exception {
        mockMvc.perform(post("/api/portes/solicitud")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SOLICITUD_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void solicitud_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/portes/solicitud")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SOLICITUD_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitud_withMissingFields_returns400() throws Exception {
        String invalidJson = """
                {
                    "origen": "Madrid"
                }
                """;

        when(ownershipSecurityService.resolveClienteIdFromAuth(any())).thenReturn(10L);

        mockMvc.perform(post("/api/portes/solicitud")
                        .with(user("cliente@test.com").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitud_whenNoClienteAssociated_returns403() throws Exception {
        when(ownershipSecurityService.resolveClienteIdFromAuth(any())).thenReturn(null);

        mockMvc.perform(post("/api/portes/solicitud")
                        .with(user("cliente@test.com").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SOLICITUD_JSON))
                .andExpect(status().isForbidden());
    }
}
