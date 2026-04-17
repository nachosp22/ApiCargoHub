package com.cargohub.backend.service;

import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.BloqueoRecurrenteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.VehiculoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConductorMatchingService {

    private final ConductorRepository conductorRepository;
    private final BloqueoAgendaRepository bloqueoAgendaRepository;
    private final BloqueoRecurrenteRepository bloqueoRecurrenteRepository;
    private final VehiculoRepository vehiculoRepository;

    public ConductorMatchingService(ConductorRepository conductorRepository,
                                     BloqueoAgendaRepository bloqueoAgendaRepository,
                                     BloqueoRecurrenteRepository bloqueoRecurrenteRepository,
                                     VehiculoRepository vehiculoRepository) {
        this.conductorRepository = conductorRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.bloqueoRecurrenteRepository = bloqueoRecurrenteRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    /**
     * Finds available conductors for a given date and optional vehicle type filter.
     * Checks:
     * 1. Conductor is disponible=true and user is activo
     * 2. Conductor works on that day of the week (diasLaborables)
     * 3. No BloqueoAgenda overlapping the requested date
     * 4. No BloqueoRecurrente active for that day of the week
     * 5. Has at least one DISPONIBLE vehicle of the requested type (if specified)
     */
    public List<Conductor> buscarDisponibles(LocalDateTime fecha, TipoVehiculo tipoVehiculo, String ciudad) {
        if (fecha == null) {
            return List.of();
        }

        // Day of week: 1=Monday .. 7=Sunday (matches BloqueoRecurrente.diaSemana and diasLaborables format)
        int diaSemana = fecha.getDayOfWeek().getValue();
        String diaSemanaStr = String.valueOf(diaSemana);

        // Step 1: Get candidates that are disponible and work on this day of the week
        List<Conductor> candidates = conductorRepository.findCandidatosDisponibles(diaSemanaStr);

        // Step 2: Filter by active user
        candidates = candidates.stream()
                .filter(c -> c.getUsuario() == null || c.getUsuario().isActivo())
                .collect(Collectors.toList());

        // Step 3: Filter by ciudad if specified
        if (ciudad != null && !ciudad.isBlank()) {
            String ciudadLower = ciudad.toLowerCase();
            candidates = candidates.stream()
                    .filter(c -> c.getCiudadBase() != null && c.getCiudadBase().toLowerCase().contains(ciudadLower))
                    .collect(Collectors.toList());
        }

        // Step 4: Filter out conductors blocked by BloqueoAgenda on that date
        LocalDateTime startOfDay = fecha.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        candidates = candidates.stream()
                .filter(c -> !bloqueoAgendaRepository.estaBloqueado(c.getId(), startOfDay, endOfDay))
                .collect(Collectors.toList());

        // Step 5: Filter out conductors with active BloqueoRecurrente for this day of the week
        candidates = candidates.stream()
                .filter(c -> {
                    var bloqueo = bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(c.getId(), diaSemana);
                    return bloqueo.isEmpty() || !bloqueo.get().isActivo();
                })
                .collect(Collectors.toList());

        // Step 6: Filter by vehicle type if specified
        if (tipoVehiculo != null) {
            candidates = candidates.stream()
                    .filter(c -> {
                        List<Vehiculo> vehiculos = vehiculoRepository.findByConductorId(c.getId());
                        return vehiculos.stream().anyMatch(v ->
                                v.getTipo() == tipoVehiculo && v.getEstado() == EstadoVehiculo.DISPONIBLE);
                    })
                    .collect(Collectors.toList());
        }

        // Sort by rating descending (higher rated conductors first)
        candidates.sort((a, b) -> {
            double ratingA = a.getRating() != null ? a.getRating() : 0.0;
            double ratingB = b.getRating() != null ? b.getRating() : 0.0;
            return Double.compare(ratingB, ratingA);
        });

        log.info("ConductorMatching: fecha={}, tipo={}, ciudad={} → {} conductores disponibles",
                fecha, tipoVehiculo, ciudad, candidates.size());

        return candidates;
    }
}
