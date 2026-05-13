package com.cargohub.backend.service;

import com.cargohub.backend.dto.tracking.DriverLocationUpsertRequest;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.LocationSample;
import com.cargohub.backend.entity.TrackingSession;
import com.cargohub.backend.entity.enums.TrackingSessionStatus;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.LocationSampleRepository;
import com.cargohub.backend.repository.TrackingSessionRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrackingLocationIngestionService {

    private static final long FUTURE_TOLERANCE_MINUTES = 10L;

    private final ConductorService conductorService;
    private final ConductorRepository conductorRepository;
    private final LocationSampleRepository locationSampleRepository;
    private final TrackingSessionRepository trackingSessionRepository;

    public TrackingLocationIngestionService(ConductorService conductorService,
                                            ConductorRepository conductorRepository,
                                            LocationSampleRepository locationSampleRepository,
                                            TrackingSessionRepository trackingSessionRepository) {
        this.conductorService = conductorService;
        this.conductorRepository = conductorRepository;
        this.locationSampleRepository = locationSampleRepository;
        this.trackingSessionRepository = trackingSessionRepository;
    }

    @Transactional
    public void ingest(Long driverId, DriverLocationUpsertRequest request) {
        LocalDateTime recordedAt = toLocalDateTime(request.getRecordedAt());
        validateRecordedAt(recordedAt);

        conductorService.actualizarUbicacion(
                driverId,
                request.getLat(),
                request.getLon(),
                recordedAt,
                request.getSpeedKph(),
                request.getHeadingDeg()
        );

        Conductor conductor = conductorRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conductor no encontrado"));

        TrackingSession session = resolveSession(driverId, request.getSessionId());

        LocationSample sample = new LocationSample();
        sample.setConductor(conductor);
        sample.setSession(session);
        sample.setPorte(session != null ? session.getPorte() : null);
        sample.setLat(request.getLat());
        sample.setLon(request.getLon());
        sample.setRecordedAt(recordedAt != null ? recordedAt : LocalDateTime.now());
        sample.setReceivedAt(LocalDateTime.now());
        sample.setSpeedKph(request.getSpeedKph());
        sample.setHeadingDeg(request.getHeadingDeg());
        locationSampleRepository.save(sample);

        if (session != null) {
            session.setLastSampleAt(sample.getRecordedAt());
            trackingSessionRepository.save(session);
        }
    }

    private TrackingSession resolveSession(Long driverId, Long sessionId) {
        if (sessionId != null) {
            TrackingSession session = trackingSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking session no encontrada"));
            if (session.getConductor() == null || !driverId.equals(session.getConductor().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La session no pertenece al conductor");
            }
            return session;
        }
        return trackingSessionRepository
                .findFirstByConductorIdAndStatusOrderByStartedAtDesc(driverId, TrackingSessionStatus.ACTIVE)
                .orElse(null);
    }

    private void validateRecordedAt(LocalDateTime recordedAt) {
        if (recordedAt == null) {
            return;
        }
        if (recordedAt.isAfter(LocalDateTime.now().plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recordedAt no puede estar demasiado en el futuro");
        }
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toLocalDateTime();
    }
}
