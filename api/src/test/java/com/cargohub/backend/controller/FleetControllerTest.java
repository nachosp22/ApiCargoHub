package com.cargohub.backend.controller;

import com.cargohub.backend.config.JwtAuthenticationFilter;
import com.cargohub.backend.config.SecurityConfig;
import com.cargohub.backend.dto.tracking.DriverLocationPoint;
import com.cargohub.backend.dto.tracking.DriverState;
import com.cargohub.backend.dto.tracking.EtaConfidence;
import com.cargohub.backend.dto.tracking.EtaEstimateResponse;
import com.cargohub.backend.dto.tracking.EtaMethod;
import com.cargohub.backend.dto.tracking.FleetSnapshotMeta;
import com.cargohub.backend.dto.tracking.FleetSnapshotResponse;
import com.cargohub.backend.observability.FleetRealtimeMetrics;
import com.cargohub.backend.service.EtaService;
import com.cargohub.backend.service.FleetTrackingService;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FleetController.class)
@Import(SecurityConfig.class)
class FleetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private FleetTrackingService fleetTrackingService;

    @MockitoBean
    private EtaService etaService;

    @MockitoBean
    private FleetRealtimeMetrics metrics;

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
    void snapshot_adminRole_returnsContractShape() throws Exception {
        FleetSnapshotResponse response = new FleetSnapshotResponse();
        response.setSnapshotAt(OffsetDateTime.of(2026, 3, 16, 10, 0, 0, 0, ZoneOffset.UTC));

        DriverLocationPoint point = new DriverLocationPoint();
        point.setDriverId("7");
        point.setLat(40.416);
        point.setLon(-3.703);
        point.setRecordedAt(OffsetDateTime.of(2026, 3, 16, 9, 59, 30, 0, ZoneOffset.UTC));
        point.setState(DriverState.ONLINE);
        response.setDrivers(List.of(point));

        FleetSnapshotMeta meta = new FleetSnapshotMeta();
        meta.setPollingSuggestedSec(10);
        meta.setDegraded(false);
        response.setMeta(meta);

        when(fleetTrackingService.buildSnapshot()).thenReturn(response);

        mockMvc.perform(get("/api/v1/fleet/snapshot")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotAt").value("2026-03-16T10:00:00Z"))
                .andExpect(jsonPath("$.drivers[0].driverId").value("7"))
                .andExpect(jsonPath("$.drivers[0].state").value("ONLINE"))
                .andExpect(jsonPath("$.meta.pollingSuggestedSec").value(10))
                .andExpect(jsonPath("$.meta.degraded").value(false));
    }

    @Test
    void snapshot_conductorRole_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/snapshot")
                        .with(user("conductor@test.com").roles("CONDUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void eta_adminRole_returnsContractShape() throws Exception {
        EtaEstimateResponse response = new EtaEstimateResponse();
        response.setEtaMinutes(12);
        response.setMethod(EtaMethod.ROUTE_PROVIDER);
        response.setConfidence(EtaConfidence.MEDIUM);
        response.setEstimatedAt(OffsetDateTime.of(2026, 3, 16, 10, 0, 0, 0, ZoneOffset.UTC));
        when(etaService.estimate(7L, 30L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/eta/estimate")
                        .param("driverId", "7")
                        .param("jobId", "30")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etaMinutes").value(12))
                .andExpect(jsonPath("$.method").value("ROUTE_PROVIDER"))
                .andExpect(jsonPath("$.confidence").value("MEDIUM"))
                .andExpect(jsonPath("$.estimatedAt").value("2026-03-16T10:00:00Z"));
    }

    @Test
    void eta_providerFailure_returnsFallbackWithout5xx() throws Exception {
        EtaEstimateResponse fallback = new EtaEstimateResponse();
        fallback.setEtaMinutes(18);
        fallback.setMethod(EtaMethod.HAVERSINE_FALLBACK);
        fallback.setConfidence(EtaConfidence.LOW);
        fallback.setEstimatedAt(OffsetDateTime.of(2026, 3, 16, 10, 1, 0, 0, ZoneOffset.UTC));
        when(etaService.estimate(9L, 40L)).thenReturn(fallback);

        mockMvc.perform(get("/api/v1/eta/estimate")
                        .param("driverId", "9")
                        .param("jobId", "40")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("HAVERSINE_FALLBACK"))
                .andExpect(jsonPath("$.etaMinutes").value(18))
                .andExpect(jsonPath("$.confidence").value("LOW"));
    }
}
