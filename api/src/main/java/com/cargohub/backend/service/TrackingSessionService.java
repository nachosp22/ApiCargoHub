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

/**
 * Servicio encargado de gestionar el ciclo de vida de las sesiones de seguimiento (tracking).
 * Permite iniciar, actualizar, pausar y finalizar sesiones, así como registrar pausas
 * asociadas a cada sesión.
 */
@Service
public class TrackingSessionService {

    private final TrackingSessionRepository trackingSessionRepository;
    private final TrackingPauseRepository trackingPauseRepository;
    private final ConductorRepository conductorRepository;
    private final PorteRepository porteRepository;

    /**
     * Construye el servicio con los repositorios necesarios.
     */
    public TrackingSessionService(TrackingSessionRepository trackingSessionRepository,
                                  TrackingPauseRepository trackingPauseRepository,
                                  ConductorRepository conductorRepository,
                                  PorteRepository porteRepository) {
        this.trackingSessionRepository = trackingSessionRepository;
        this.trackingPauseRepository = trackingPauseRepository;
        this.conductorRepository = conductorRepository;
        this.porteRepository = porteRepository;
    }

    /**
     * Inicia una nueva sesión de seguimiento para un conductor.
     * Crea la sesión con el estado {@code ACTIVE} y la fase indicada en la solicitud
     * (o {@code IDLE} por defecto). Si se proporciona un {@code porteId}, se asocia
     * a la sesión.
     *
     * @param request datos de entrada con el identificador del conductor, fase actual
     *                y opcionalmente el identificador del porte y la fecha de inicio.
     * @return respuesta con los datos de la sesión recién creada.
     * @throws ResponseStatusException si el conductor no existe (404).
     */
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

    /**
     * Actualiza una sesión de seguimiento existente. Permite modificar el estado,
     * la fase actual, la fecha de finalización y el porte asociado.
     * Si el estado cambia a {@code ENDED} o se proporciona una fecha de fin,
     * se establece automáticamente {@code endedAt} y el estado se marca como finalizado.
     * Valida que la transición de estado sea válida antes de aplicarla.
     *
     * @param sessionId identificador de la sesión a actualizar.
     * @param request   datos con los campos a modificar (estado, fase, fecha de fin, porte).
     * @return respuesta con los datos actualizados de la sesión.
     * @throws ResponseStatusException si la sesión no existe (404) o la transición
     *                                 de estado no es válida (409).
     */
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

    /**
     * Obtiene una sesión de seguimiento por su identificador.
     *
     * @param sessionId identificador de la sesión a buscar.
     * @return la entidad {@link TrackingSession} encontrada.
     * @throws ResponseStatusException si la sesión no existe (404).
     */
    @Transactional(readOnly = true)
    public TrackingSession getSession(Long sessionId) {
        return trackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking session no encontrada"));
    }

    /**
     * Registra una pausa en una sesión de seguimiento o finaliza una pausa activa.
     * Si se proporciona {@code endedAt} sin un identificador de pausa, se cierra la
     * pausa activa de la sesión. Si no hay pausa activa, retorna una respuesta idempotente.
     * Si no se proporciona {@code endedAt}, se crea una nueva pausa con el motivo indicado.
     *
     * @param sessionId identificador de la sesión a la que pertenece la pausa.
     * @param request   datos de la pausa: motivo obligatorio, nota opcional y
     *                  opcionalmente la fecha de inicio o fin.
     * @return respuesta con los datos de la pausa registrada o finalizada.
     * @throws ResponseStatusException si la sesión no existe (404) o el motivo
     *                                 de la pausa está vacío (400).
     */
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

    /**
     * Convierte una entidad {@link TrackingPause} en su DTO de respuesta.
     */
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

    /**
     * Resuelve un {@link Porte} a partir de su identificador. Retorna {@code null} si el ID es nulo.
     */
    private Porte resolvePorte(Long porteId) {
        if (porteId == null) {
            return null;
        }
        return porteRepository.findById(porteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Porte no encontrado"));
    }

    /**
     * Convierte un {@link OffsetDateTime} a {@link LocalDateTime}, usando un valor por defecto si es nulo.
     */
    private LocalDateTime toLocalDateTime(OffsetDateTime value, LocalDateTime fallback) {
        if (value == null) {
            return fallback;
        }
        return value.toLocalDateTime();
    }

    /**
     * Convierte una entidad {@link TrackingSession} en su DTO de respuesta.
     */
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

    /**
     * Convierte un {@link LocalDateTime} a {@link OffsetDateTime} en zona UTC. Retorna {@code null} si la entrada es nula.
     */
    private OffsetDateTime toOffset(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC);
    }

    /**
     * Valida que la transición entre estados de una sesión sea permitida.
     * Una sesión {@code ENDED} no puede volver a un estado activo.
     */
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
