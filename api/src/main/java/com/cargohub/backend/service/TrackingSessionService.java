package com.cargohub.backend.service;

import com.cargohub.backend.dto.tracking.StartTrackingSessionRequest;
import com.cargohub.backend.dto.tracking.TrackingPauseRequest;
import com.cargohub.backend.dto.tracking.TrackingPauseResponse;
import com.cargohub.backend.dto.tracking.TrackingSessionResponse;
import com.cargohub.backend.dto.tracking.UpdateTrackingSessionRequest;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.TrackingPause;
import com.cargohub.backend.entity.TrackingSession;
import com.cargohub.backend.entity.enums.TrackingSessionPhase;
import com.cargohub.backend.entity.enums.TrackingSessionStatus;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.TrackingPauseRepository;
import com.cargohub.backend.repository.TrackingSessionRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrackingSessionService {

    private final TrackingSessionRepository trackingSessionRepository;
    private final TrackingPauseRepository trackingPauseRepository;
    private final ConductorRepository conductorRepository;
    private final PorteRepository porteRepository;

    public TrackingSessionService(TrackingSessionRepository trackingSessionRepository,
                                  TrackingPauseRepository trackingPauseRepository,
                                  ConductorRepository conductorRepository,
                                  PorteRepository porteRepository) {
        this.trackingSessionRepository = trackingSessionRepository;
        this.trackingPauseRepository = trackingPauseRepository;
        this.conductorRepository = conductorRepository;
        this.porteRepository = porteRepository;
    }

    @Transactional
    public TrackingSessionResponse startSession(StartTrackingSessionRequest request) {
        Conductor conductor = conductorRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conductor no encontrado"));

        TrackingSession session = new TrackingSession();
        session.setConductor(conductor);
        session.setPorte(resolvePorte(request.getPorteId()));
        session.setStatus(TrackingSessionStatus.ACTIVE);
        session.setCurrentPhase(request.getCurrentPhase() != null ? request.getCurrentPhase() : TrackingSessionPhase.IDLE);
        session.setStartedAt(toLocalDateTime(request.getStartedAt(), LocalDateTime.now()));

        TrackingSession saved = trackingSessionRepository.save(session);
        return toResponse(saved);
    }

    @Transactional
    public TrackingSessionResponse updateSession(Long sessionId, UpdateTrackingSessionRequest request) {
        TrackingSession session = getSession(sessionId);

        if (request.getStatus() != null) {
            validateStatusTransition(session.getStatus(), request.getStatus());
            session.setStatus(request.getStatus());
            if (request.getStatus() == TrackingSessionStatus.ENDED && session.getEndedAt() == null) {
                session.setEndedAt(LocalDateTime.now());
            }
        }
        if (request.getCurrentPhase() != null) {
            session.setCurrentPhase(request.getCurrentPhase());
        }
        if (request.getEndedAt() != null) {
            session.setEndedAt(request.getEndedAt().toLocalDateTime());
            if (session.getStatus() == null || session.getStatus() != TrackingSessionStatus.ENDED) {
                session.setStatus(TrackingSessionStatus.ENDED);
            }
        }
        if (request.getPorteId() != null) {
            session.setPorte(resolvePorte(request.getPorteId()));
        }

        return toResponse(trackingSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public TrackingSession getSession(Long sessionId) {
        return trackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking session no encontrada"));
    }

    @Transactional
    public TrackingPauseResponse recordPause(Long sessionId, TrackingPauseRequest request) {
        TrackingSession session = getSession(sessionId);

        // If endedAt is provided without a specific pause id, close the active pause for this session
        if (request.getEndedAt() != null) {
            Optional<TrackingPause> activeOpt = trackingPauseRepository
                    .findTopBySessionIdAndEndedAtIsNullOrderByStartedAtDesc(sessionId);
            if (activeOpt.isPresent()) {
                TrackingPause active = activeOpt.get();
                active.setEndedAt(request.getEndedAt().toLocalDateTime());
                return toPauseResponse(trackingPauseRepository.save(active));
            }
            // No active pause to close — idempotent, return synthetic response
            TrackingPauseResponse resp = new TrackingPauseResponse();
            resp.setSessionId(sessionId);
            resp.setEndedAt(request.getEndedAt());
            return resp;
        }

        // Create a new pause
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo de pausa es obligatorio");
        }
        TrackingPause pause = new TrackingPause();
        pause.setSession(session);
        pause.setMotivo(request.getMotivo());
        pause.setNota(request.getNota());
        pause.setStartedAt(toLocalDateTime(request.getStartedAt(), LocalDateTime.now()));

        TrackingPause saved = trackingPauseRepository.save(pause);
        return toPauseResponse(saved);
    }

    private TrackingPauseResponse toPauseResponse(TrackingPause pause) {
        TrackingPauseResponse resp = new TrackingPauseResponse();
        resp.setId(pause.getId());
        resp.setSessionId(pause.getSession() != null ? pause.getSession().getId() : null);
        resp.setMotivo(pause.getMotivo());
        resp.setNota(pause.getNota());
        resp.setStartedAt(toOffset(pause.getStartedAt()));
        resp.setEndedAt(toOffset(pause.getEndedAt()));
        return resp;
    }

    private Porte resolvePorte(Long porteId) {
        if (porteId == null) {
            return null;
        }
        return porteRepository.findById(porteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Porte no encontrado"));
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value, LocalDateTime fallback) {
        if (value == null) {
            return fallback;
        }
        return value.toLocalDateTime();
    }

    private TrackingSessionResponse toResponse(TrackingSession session) {
        TrackingSessionResponse response = new TrackingSessionResponse();
        response.setId(session.getId());
        response.setDriverId(session.getConductor() != null ? session.getConductor().getId() : null);
        response.setPorteId(session.getPorte() != null ? session.getPorte().getId() : null);
        response.setStatus(session.getStatus());
        response.setCurrentPhase(session.getCurrentPhase());
        response.setStartedAt(toOffset(session.getStartedAt()));
        response.setEndedAt(toOffset(session.getEndedAt()));
        response.setLastSampleAt(toOffset(session.getLastSampleAt()));
        return response;
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC);
    }

    private void validateStatusTransition(TrackingSessionStatus current, TrackingSessionStatus target) {
        if (current == null || target == null || current == target) {
            return;
        }

        if (current == TrackingSessionStatus.ENDED && target != TrackingSessionStatus.ENDED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transición de estado inválida: ENDED no puede volver a estado activo"
            );
        }

        boolean validTransition = switch (current) {
            case ACTIVE -> target == TrackingSessionStatus.PAUSED
                    || target == TrackingSessionStatus.BACKGROUND
                    || target == TrackingSessionStatus.ENDED;
            case PAUSED -> target == TrackingSessionStatus.ACTIVE
                    || target == TrackingSessionStatus.BACKGROUND
                    || target == TrackingSessionStatus.ENDED;
            case BACKGROUND -> target == TrackingSessionStatus.ACTIVE
                    || target == TrackingSessionStatus.PAUSED
                    || target == TrackingSessionStatus.ENDED;
            case ENDED -> target == TrackingSessionStatus.ENDED;
        };

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transición de estado inválida: " + current + " -> " + target
            );
        }
    }
}
