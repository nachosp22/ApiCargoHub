package com.cargohub.backend.service;

import com.cargohub.backend.config.FleetRealtimeProperties;
import com.cargohub.backend.dto.tracking.DriverState;
import com.cargohub.backend.dto.tracking.FleetSnapshotResponse;
import com.cargohub.backend.observability.FleetRealtimeMetrics;
import com.cargohub.backend.repository.ConductorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FleetTrackingServiceTest {

    @Mock
    private ConductorRepository conductorRepository;

    private FleetRealtimeProperties properties;
    private FleetRealtimeMetrics metrics;
    private Clock clock;

    @BeforeEach
    void setUp() {
        properties = new FleetRealtimeProperties();
        properties.setEnabled(true);
        properties.setTtlOnlineSec(30);
        properties.setTtlStaleSec(180);
        properties.setPollingSuggestedSec(10);
        properties.setSnapshotCacheSec(60);
        properties.setMaxDrivers(300);
        metrics = new FleetRealtimeMetrics();
        clock = Clock.fixed(Instant.parse("2026-03-16T10:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    void buildSnapshot_classifiesOnlineStaleOffline() {
        when(conductorRepository.findFleetSnapshot(any(Pageable.class))).thenReturn(List.of(
                projection(1L, 40.4, -3.7, LocalDateTime.of(2026, 3, 16, 9, 59, 40), null, null),
                projection(2L, 41.4, -4.7, LocalDateTime.of(2026, 3, 16, 9, 58, 20), null, null),
                projection(3L, 42.4, -5.7, LocalDateTime.of(2026, 3, 16, 9, 55, 0), null, null)
        ));

        FleetTrackingService service = new FleetTrackingService(conductorRepository, properties, metrics, clock);
        FleetSnapshotResponse response = service.buildSnapshot();

        assertEquals(3, response.getDrivers().size());
        assertEquals(DriverState.ONLINE, response.getDrivers().get(0).getState());
        assertEquals(DriverState.STALE, response.getDrivers().get(1).getState());
        assertEquals(DriverState.OFFLINE, response.getDrivers().get(2).getState());
        assertFalse(response.getMeta().isDegraded());
    }

    @Test
    void buildSnapshot_excludesInvalidCoordinatesAndSetsDegraded() {
        when(conductorRepository.findFleetSnapshot(any(Pageable.class))).thenReturn(List.of(
                projection(1L, 95.0, -3.7, LocalDateTime.of(2026, 3, 16, 9, 59, 40), null, null),
                projection(2L, 40.4, -3.7, LocalDateTime.of(2026, 3, 16, 9, 59, 40), null, null)
        ));

        FleetTrackingService service = new FleetTrackingService(conductorRepository, properties, metrics, clock);
        FleetSnapshotResponse response = service.buildSnapshot();

        assertEquals(1, response.getDrivers().size());
        assertTrue(response.getMeta().isDegraded());
        assertEquals("invalid_coordinate_excluded", response.getMeta().getDegradedReason());
    }

    @Test
    void buildSnapshot_returnsCachedSnapshotOnFailureWithinCacheWindow() {
        when(conductorRepository.findFleetSnapshot(any(Pageable.class)))
                .thenReturn(List.of(projection(2L, 40.4, -3.7, LocalDateTime.of(2026, 3, 16, 9, 59, 40), null, null)))
                .thenThrow(new RuntimeException("db down"));

        FleetTrackingService service = new FleetTrackingService(conductorRepository, properties, metrics, clock);
        FleetSnapshotResponse first = service.buildSnapshot();
        FleetSnapshotResponse second = service.buildSnapshot();

        assertEquals(1, first.getDrivers().size());
        assertEquals(1, second.getDrivers().size());
        assertTrue(second.getMeta().isDegraded());
        assertEquals("snapshot_cache_fallback", second.getMeta().getDegradedReason());
    }

    @Test
    void buildSnapshot_p95Evidence_for300Drivers_underNfrTarget() {
        List<ConductorRepository.DriverSnapshotProjection> drivers = new ArrayList<>();
        for (long i = 1; i <= 300; i++) {
            drivers.add(projection(
                    i,
                    40.0 + (i * 0.001),
                    -3.0 - (i * 0.001),
                    LocalDateTime.of(2026, 3, 16, 9, 59, 40),
                    50.0,
                    180
            ));
        }
        when(conductorRepository.findFleetSnapshot(any(Pageable.class))).thenReturn(drivers);

        FleetTrackingService service = new FleetTrackingService(conductorRepository, properties, metrics, clock);

        List<Long> samplesMs = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            long start = System.nanoTime();
            FleetSnapshotResponse response = service.buildSnapshot();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertEquals(300, response.getDrivers().size());
            samplesMs.add(elapsedMs);
        }

        samplesMs.sort(Long::compareTo);
        long p95 = samplesMs.get((int) Math.ceil(samplesMs.size() * 0.95) - 1);
        System.out.println("Fleet snapshot performance evidence (300 drivers) p95=" + p95 + "ms");
        assertTrue(p95 <= 800, "Snapshot p95 should be <= 800ms for 300 drivers");
    }

    private ConductorRepository.DriverSnapshotProjection projection(Long id,
                                                                    Double lat,
                                                                    Double lon,
                                                                    LocalDateTime recordedAt,
                                                                    Double speed,
                                                                    Integer heading) {
        return new ConductorRepository.DriverSnapshotProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public Double getLatitudActual() {
                return lat;
            }

            @Override
            public Double getLongitudActual() {
                return lon;
            }

            @Override
            public LocalDateTime getUltimaActualizacionUbicacion() {
                return recordedAt;
            }

            @Override
            public Double getVelocidadKphActual() {
                return speed;
            }

            @Override
            public Integer getRumboActualDeg() {
                return heading;
            }
        };
    }
}
