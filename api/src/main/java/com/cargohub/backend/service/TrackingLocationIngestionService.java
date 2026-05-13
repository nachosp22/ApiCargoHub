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

/**
 * Servicio encargado de ingerir muestras de ubicación GPS provenientes de los conductores.
 * Cada muestra se valida, se asocia a una sesión de seguimiento (activa o explícita)
 * y se persiste junto con la actualización de la última posición conocida del conductor.
 */
@Service
public class TrackingLocationIngestionService {

    /** Tolerancia máxima en minutos para que una marca de tiempo no se considere "en el futuro". */
    private static final long FUTURE_TOLERANCE_MINUTES = 10L;

    private final ConductorService conductorService;
    private final ConductorRepository conductorRepository;
    private final LocationSampleRepository locationSampleRepository;
    private final TrackingSessionRepository trackingSessionRepository;

    /**
     * Construye el servicio con sus dependencias de repositorios y servicio de conductor.
     *
     * @param conductorService            servicio para actualizar la ubicación del conductor.
     * @param conductorRepository         repositorio de conductores.
     * @param locationSampleRepository    repositorio de muestras de ubicación.
     * @param trackingSessionRepository   repositorio de sesiones de seguimiento.
     */
    public TrackingLocationIngestionService(ConductorService conductorService,
                                            ConductorRepository conductorRepository,
                                            LocationSampleRepository locationSampleRepository,
                                            TrackingSessionRepository trackingSessionRepository) {
        this.conductorService = conductorService;
        this.conductorRepository = conductorRepository;
        this.locationSampleRepository = locationSampleRepository;
        this.trackingSessionRepository = trackingSessionRepository;
    }

    /**
     * Ingiere una muestra de ubicación GPS enviada por un conductor.
     * <p>
     * El flujo consiste en:
     * <ol>
     *   <li>Validar que la marca de tiempo {@code recordedAt} no esté demasiado en el futuro.</li>
     *   <li>Actualizar la última posición conocida del conductor.</li>
     *   <li>Resolver la sesión de seguimiento asociada (explícita o activa).</li>
     *   <li>Crear y persistir la muestra de ubicación.</li>
     *   <li>Actualizar la última marca de tiempo de la sesión si corresponde.</li>
     * </ol>
     *
     * @param driverId identificador único del conductor que envía la muestra.
     * @param request  DTO con los datos de la ubicación (latitud, longitud, velocidad,
     *                 rumbo, marca de tiempo y opcionalmente el ID de sesión).
     * @throws ResponseStatusException si el conductor no se encuentra (404),
     *                                 si la sesión de seguimiento no existe (404),
     *                                 si la sesión no pertenece al conductor (400),
     *                                 o si la marca de tiempo está demasiado en el futuro (400).
     */
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

    /**
     * Resuelve la sesión de seguimiento asociada a la muestra.
     * Si se proporciona un {@code sessionId} explícito, lo busca y valida que pertenezca al conductor.
     * De lo contrario, retorna la sesión activa más reciente del conductor, o {@code null} si no existe.
     *
     * @param driverId  identificador del conductor.
     * @param sessionId identificador explícito de la sesión (puede ser {@code null}).
     * @return la sesión de seguimiento resuelta, o {@code null} si no hay sesión activa.
     * @throws ResponseStatusException si la sesión explícita no existe (404)
     *                                 o no pertenece al conductor indicado (400).
     */
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

    /**
     * Valida que la marca de tiempo de registro no supere la tolerancia configurada hacia el futuro.
     *
     * @param recordedAt marca de tiempo a validar (puede ser {@code null}, en cuyo caso se acepta).
     * @throws ResponseStatusException si la fecha está más de {@value #FUTURE_TOLERANCE_MINUTES} minutos en el futuro (400).
     */
    private void validateRecordedAt(LocalDateTime recordedAt) {
        if (recordedAt == null) {
            return;
        }
        if (recordedAt.isAfter(LocalDateTime.now().plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recordedAt no puede estar demasiado en el futuro");
        }
    }

    /**
     * Convierte un {@link OffsetDateTime} a {@link LocalDateTime}, descartando el desplazamiento horario.
     *
     * @param value valor con zona horaria a convertir (puede ser {@code null}).
     * @return el {@link LocalDateTime} equivalente, o {@code null} si la entrada es {@code null}.
     */
    private LocalDateTime toLocalDateTime(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toLocalDateTime();
    }
}
