package com.cargohub.backend.service;

import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.BloqueoRecurrenteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
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
    private final PorteRepository porteRepository;

    public ConductorMatchingService(ConductorRepository conductorRepository,
                                     BloqueoAgendaRepository bloqueoAgendaRepository,
                                     BloqueoRecurrenteRepository bloqueoRecurrenteRepository,
                                     VehiculoRepository vehiculoRepository,
                                     PorteRepository porteRepository) {
        this.conductorRepository = conductorRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.bloqueoRecurrenteRepository = bloqueoRecurrenteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.porteRepository = porteRepository;
    }

    /**
     * Finds available conductors for a given date and optional vehicle type filter.
     * Backward-compat overload: delegates to range-based search with a single-day range.
     */
    public List<Conductor> buscarDisponibles(LocalDateTime fecha, TipoVehiculo tipoVehiculo, String ciudad) {
        return buscarDisponibles(fecha, fecha, tipoVehiculo, ciudad, null, null);
    }

    /**
     * Overload that accepts origin coordinates for distance filtering.
     * Backward-compat: delegates to range-based search with a single-day range.
     */
    public List<Conductor> buscarDisponibles(LocalDateTime fecha, TipoVehiculo tipoVehiculo, String ciudad,
                                             Double latitudOrigen, Double longitudOrigen) {
        return buscarDisponibles(fecha, fecha, tipoVehiculo, ciudad, latitudOrigen, longitudOrigen);
    }

    /**
     * Finds available conductors for a date range and optional vehicle type filter.
     * MVP rules:
     * 1. Conductor is disponible=true and user is activo
     * 2. Conductor works on the start day of the week (diasLaborables)
     * 3. No BloqueoAgenda overlapping the FULL date range
     * 4. No BloqueoRecurrente active for ANY day in the range
     * 5. No overlapping assigned/in-transit porte in the FULL date range (tieneViajeEnFecha)
     * 6. Has at least one DISPONIBLE vehicle of the requested type (if specified)
     * 7. Origin coordinates are within conductor's radioAccionKm.
     *    - If radioAccionKm is null or <= 0 => REJECT.
     *    - If conductor base coords missing => REJECT.
     *    - If origin coords missing => REJECT.
     */
    public List<Conductor> buscarDisponibles(LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                             TipoVehiculo tipoVehiculo, String ciudad,
                                             Double latitudOrigen, Double longitudOrigen) {
        if (fechaInicio == null) {
            return List.of();
        }
        final LocalDateTime fin = fechaFin != null ? fechaFin : fechaInicio;

        // Day of week: 1=Monday .. 7=Sunday (matches BloqueoRecurrente.diaSemana and diasLaborables format)
        int diaSemanaInicio = fechaInicio.getDayOfWeek().getValue();
        String diaSemanaStr = String.valueOf(diaSemanaInicio);

        // Paso 1: Obtener candidatos disponibles y que trabajen el día de inicio de la semana
        List<Conductor> candidates = conductorRepository.findCandidatosDisponibles(diaSemanaStr);

        // Paso 2: Filtrar por usuario activo
        candidates = candidates.stream()
                .filter(c -> c.getUsuario() == null || c.getUsuario().isActivo())
                .collect(Collectors.toList());

        // MVP: la ciudad textual solo es contexto de visualización. El emparejamiento por área
        // se hace con coordenadas base del conductor vs coordenadas de origen del porte y radioAccionKm.

        // Paso 4: Descartar conductores bloqueados por BloqueoAgenda en CUALQUIER fecha del rango
        candidates = candidates.stream()
                .filter(c -> !bloqueoAgendaRepository.estaBloqueado(c.getId(), fechaInicio, fin))
                .collect(Collectors.toList());

        // Paso 5: Descartar conductores con BloqueoRecurrente activo en CUALQUIER día del rango
        candidates = candidates.stream()
                .filter(c -> {
                    LocalDateTime current = fechaInicio;
                    while (!current.isAfter(fin)) {
                        int dia = current.getDayOfWeek().getValue();
                        var bloqueo = bloqueoRecurrenteRepository.findByConductorIdAndDiaSemana(c.getId(), dia);
                        if (bloqueo.isPresent() && bloqueo.get().isActivo()) {
                            return false;
                        }
                        current = current.plusDays(1);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Paso 6: Descartar conductores ya asignados a otro porte en el rango de fechas
        candidates = candidates.stream()
                .filter(c -> !porteRepository.tieneViajeEnFecha(c.getId(), fechaInicio, fin))
                .collect(Collectors.toList());

        // Paso 7: Filtrar por distancia desde origen (radioAccionKm) — reglas estrictas MVP
        if (latitudOrigen != null && longitudOrigen != null) {
            candidates = candidates.stream()
                    .filter(c -> {
                        Integer radio = c.getRadioAccionKm();
                        // MVP: null o <= 0 => NO elegible para emparejamiento automático
                        if (radio == null || radio <= 0) return false;
                        Double latBase = c.getLatitudBase();
                        Double lonBase = c.getLongitudBase();
                        // MVP: sin coordenadas base => NO elegible
                        if (latBase == null || lonBase == null) return false;
                        double distanciaKm = haversine(latitudOrigen, longitudOrigen, latBase, lonBase);
                        return distanciaKm <= radio;
                    })
                    .collect(Collectors.toList());
        } else {
            // MVP: si faltan coordenadas de origen, no se debe hacer emparejamiento automático.
            // Este método también lo usa el controlador admin sin coordenadas (búsqueda manual),
            // por lo que solo aplicamos filtro estricto de distancia cuando hay coordenadas.
            // Quien invoca (PorteService) debe validar coordenadas antes de llamar a auto-matching.
        }

        // Paso 8: Filtrar por tipo de vehículo si se especificó
        if (tipoVehiculo != null) {
            candidates = candidates.stream()
                    .filter(c -> {
                        List<Vehiculo> vehiculos = vehiculoRepository.findByConductorId(c.getId());
                        return vehiculos.stream().anyMatch(v ->
                                v.getTipo() == tipoVehiculo && v.getEstado() == EstadoVehiculo.DISPONIBLE);
                    })
                    .collect(Collectors.toList());
        }

        // Keep deterministic order without using rating.
        candidates.sort((a, b) -> Long.compare(a.getId(), b.getId()));

        log.info("ConductorMatching: fechaInicio={}, fechaFin={}, tipo={}, ciudad={}, lat={}, lon={} → {} conductores disponibles",
                fechaInicio, fechaFin, tipoVehiculo, ciudad, latitudOrigen, longitudOrigen, candidates.size());

        return candidates;
    }

    /**
     * Haversine formula to calculate distance between two points in kilometers.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
