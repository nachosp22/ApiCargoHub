package com.cargohub.backend.service;

import com.cargohub. backend.dto.CrearPorteRequest;
import com.cargohub.backend.dto.CargoAnalysisResponse;
import com.cargohub.backend.dto.SolicitudPorteRequest;
import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity. Porte;
import com.cargohub.backend.entity. Vehiculo;
import com.cargohub.backend.entity. enums.EstadoPorte;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com. cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository. VehiculoRepository;
import com.cargohub.backend.util.CalculadoraDistancia;
import lombok.extern.slf4j. Slf4j;
import org. springframework.beans.factory.annotation. Autowired;
import org. springframework.stereotype.Service;
import org.springframework.transaction.annotation. Transactional;

import com.cargohub.backend.dto.ActualizarDimensionesRequest;
import com.cargohub.backend.dto.ConductorCandidatoResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class PorteService {

    @Autowired private PorteRepository porteRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private CalculadoraPrecioService calculadoraPrecio;
    @Autowired private FacturaService facturaService;
    @Autowired private ConductorRepository conductorRepository;
    @Autowired private CargoAnalysisService cargoAnalysisService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ConductorMatchingService conductorMatchingService;

    /**
     * NUEVO MÉTODO: Crear Porte desde DTO simplificado
     */
    @Transactional
    public Porte crearPorteDesdeRequest(CrearPorteRequest request) {
        // 1. Buscar el cliente
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + request.getClienteId()));

        // 2. Crear la entidad Porte
        Porte porte = new Porte();
        porte.setCliente(cliente);
        porte.setOrigen(request.getOrigen());
        porte.setDestino(request.getDestino());
        porte.setLatitudOrigen(request.getLatitudOrigen());
        porte.setLongitudOrigen(request. getLongitudOrigen());
        porte.setLatitudDestino(request.getLatitudDestino());
        porte.setLongitudDestino(request.getLongitudDestino());
        porte.setDescripcionCliente(request.getDescripcionCliente());
        porte.setFechaRecogida(request.getFechaRecogida());
        porte.setFechaEntrega(request.getFechaEntrega());

        // 3. Llamar al método existente que hace toda la lógica
        return crearPorte(porte);
    }

    /**
     * Crear Porte desde solicitud de cliente (portal web).
     * El clienteId se resuelve desde la autenticación JWT.
     */
    @Transactional
    public Porte crearPorteDesdeSolicitud(SolicitudPorteRequest request, Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));

        Porte porte = new Porte();
        porte.setCliente(cliente);
        porte.setOrigen(request.getOrigen());
        porte.setDestino(request.getDestino());
        porte.setCiudadOrigen(request.getCiudadOrigen());
        porte.setCiudadDestino(request.getCiudadDestino());
        porte.setLatitudOrigen(request.getLatitudOrigen());
        porte.setLongitudOrigen(request.getLongitudOrigen());
        porte.setLatitudDestino(request.getLatitudDestino());
        porte.setLongitudDestino(request.getLongitudDestino());
        porte.setDescripcionCliente(request.getDescripcionCliente());
        porte.setFechaRecogida(request.getFechaRecogida());

        return crearPorte(porte);
    }

    /**
     * Listar portes de un cliente específico.
     */
    public List<Porte> listarPortesPorCliente(Long clienteId) {
        return porteRepository.findByClienteId(clienteId);
    }

    @Transactional
    public Porte crearPorte(Porte porte) {
        // 0. Analizar carga con Gemini para inferir dimensiones
        if (porte.getDescripcionCliente() != null && !porte.getDescripcionCliente().isEmpty()) {
            CargoAnalysisResponse cargoAnalysisResponse = cargoAnalysisService.calcularDimensiones(porte.getDescripcionCliente());

            if (cargoAnalysisResponse != null) {
                // Aplicar dimensiones inferidas por IA
                if (cargoAnalysisResponse.getPesoTotalKg() != null && cargoAnalysisResponse.getPesoTotalKg() > 0) {
                    porte.setPesoTotalKg(cargoAnalysisResponse.getPesoTotalKg());
                }
                if (cargoAnalysisResponse.getVolumenTotalM3() != null && cargoAnalysisResponse.getVolumenTotalM3() > 0) {
                    porte.setVolumenTotalM3(cargoAnalysisResponse.getVolumenTotalM3());
                }
                if (cargoAnalysisResponse. getLargoMaxPaquete() != null && cargoAnalysisResponse.getLargoMaxPaquete() > 0) {
                    porte.setLargoMaxPaquete(cargoAnalysisResponse.getLargoMaxPaquete());
                }
                if (cargoAnalysisResponse.getAnchoMaxPaquete() != null && cargoAnalysisResponse.getAnchoMaxPaquete() > 0) {
                    porte.setAnchoMaxPaquete(cargoAnalysisResponse.getAnchoMaxPaquete());
                }
                if (cargoAnalysisResponse.getAltoMaxPaquete() != null && cargoAnalysisResponse.getAltoMaxPaquete() > 0) {
                    porte.setAltoMaxPaquete(cargoAnalysisResponse.getAltoMaxPaquete());
                }

                // Aplicar tipo de vehículo si fue calculado
                if (cargoAnalysisResponse.getTipoVehiculoRequerido() != null && ! cargoAnalysisResponse.getTipoVehiculoRequerido().isEmpty()) {
                    TipoVehiculo tipoCalculado = cargoAnalysisService.convertirTipoVehiculo(cargoAnalysisResponse.getTipoVehiculoRequerido());
                    if (tipoCalculado != null) {
                        porte.setTipoVehiculoRequerido(tipoCalculado);
                    }
                }

                // Aplicar flags de revisión manual
                if (cargoAnalysisResponse.getRevisionManual() != null && cargoAnalysisResponse.getRevisionManual()) {
                    porte.setRevisionManual(true);
                    porte.setMotivoRevision(cargoAnalysisResponse.getMotivoRevision());
                }
            }
        }

        // 1. Distancia
        if (!hasValidCoordinates(porte)) {
            throw new RuntimeException("Origen y destino deben incluir coordenadas válidas");
        }

        double km = CalculadoraDistancia. calcularKm(
                porte.getLatitudOrigen(), porte.getLongitudOrigen(),
                porte. getLatitudDestino(), porte.getLongitudDestino());
        porte.setDistanciaKm(km * 1.2);
        porte.setDistanciaEstimada(true);

        // 2. Precio
        porte.setPrecio(calculadoraPrecio. calcularPrecioTotal(porte));
        porte.setFechaCreacion(LocalDateTime.now());
        porte.setEstado(EstadoPorte.PENDIENTE);

        if (porte.isRevisionManual()) {
            return porteRepository.save(porte);
        }

        // 3. LOGICA DE ASIGNACIÓN CON SCORING Y CONDUCTOR MATCHING
        Integer largoMm = (porte.getLargoMaxPaquete() != null) ? (int)(porte.getLargoMaxPaquete() * 1000) : 0;
        Integer anchoMm = (porte.getAnchoMaxPaquete() != null) ? (int)(porte.getAnchoMaxPaquete() * 1000) : 0;
        Integer altoMm = (porte.getAltoMaxPaquete() != null) ? (int)(porte.getAltoMaxPaquete() * 1000) : 0;
        Double volumenRequerido = (porte.getVolumenTotalM3() != null) ? porte.getVolumenTotalM3() : 0.0;

        // Buscamos vehículos que cumplan TODAS las dimensiones (peso, largo, ancho, alto, volumen)
        List<Vehiculo> candidatos = vehiculoRepository.findCandidatos(
                porte.getTipoVehiculoRequerido(),
                porte.getPesoTotalKg(),
                largoMm,
                anchoMm,
                altoMm,
                volumenRequerido
        );

        if (!candidatos.isEmpty()) {
            // Usar ConductorMatchingService para filtrar conductores disponibles
            LocalDateTime fechaRecogida = porte.getFechaRecogida() != null ? porte.getFechaRecogida() : LocalDateTime.now();
            String ciudadOrigen = porte.getOrigen();

            List<Conductor> conductoresDisponibles = conductorMatchingService.buscarDisponibles(
                    fechaRecogida,
                    porte.getTipoVehiculoRequerido(),
                    ciudadOrigen
            );

            // Filtrar: solo conductores que tengan uno de los vehículos candidatos
            Set<Long> conductorIdsConVehiculo = candidatos.stream()
                    .filter(v -> v.getConductor() != null)
                    .map(v -> v.getConductor().getId())
                    .collect(java.util.stream.Collectors.toSet());

            List<Conductor> conductoresValidos = conductoresDisponibles.stream()
                    .filter(c -> conductorIdsConVehiculo.contains(c.getId()))
                    .collect(java.util.stream.Collectors.toList());

            if (!conductoresValidos.isEmpty()) {
                porte.setConductor(null);
                porte.setEstado(EstadoPorte.PENDIENTE);
                log.info("Porte {} publicado en ofertas para {} conductores elegibles", porte.getId(), conductoresValidos.size());
            } else {
                // Hay vehículos pero no conductores disponibles
                if (!porte.isRevisionManual()) {
                    porte.setRevisionManual(true);
                    porte.setMotivoRevision("Vehículos compatibles encontrados pero sin conductores disponibles");
                }
            }
        } else {
            // NO MATCH: Se queda pendiente y marcamos revisión (solo si no está ya marcado)
            if (!porte.isRevisionManual()) {
                porte. setRevisionManual(true);
                porte.setMotivoRevision("No hay vehículo compatible (Peso/Volumen/Dimensiones)");
            }
        }

        return porteRepository.save(porte);
    }

    /**
     * Scores a conductor for assignment. Higher is better.
     * Factors: rating (0-5 normalized to 0-50) + capacity efficiency (0-50).
     * Capacity efficiency prefers vehicles closest to the required load (avoids waste).
     */
    private double scoreConductor(Conductor conductor, List<Vehiculo> candidatos, double pesoRequerido) {
        // Rating score: 0-50 points
        double ratingScore = (conductor.getRating() != null ? conductor.getRating() : 0.0) * 10.0;

        // Capacity efficiency: prefer smallest vehicle that fits (0-50 points)
        double efficiencyScore = candidatos.stream()
                .filter(v -> v.getConductor() != null && v.getConductor().getId().equals(conductor.getId()))
                .mapToDouble(v -> {
                    double capacidad = v.getCapacidadCargaKg() != null ? v.getCapacidadCargaKg() : 1.0;
                    if (capacidad <= 0) capacidad = 1.0;
                    // Ratio of how well the vehicle is utilized (1.0 = perfect fit)
                    double ratio = pesoRequerido / capacidad;
                    return Math.min(ratio, 1.0) * 50.0;
                })
                .max()
                .orElse(0.0);

        return ratingScore + efficiencyScore;
    }

    private boolean hasValidCoordinates(Porte porte) {
        return isValidLatitude(porte.getLatitudOrigen())
                && isValidLongitude(porte.getLongitudOrigen())
                && isValidLatitude(porte.getLatitudDestino())
                && isValidLongitude(porte.getLongitudDestino());
    }

    private boolean isValidLatitude(Double latitude) {
        return latitude != null && latitude >= -90 && latitude <= 90;
    }

    private boolean isValidLongitude(Double longitude) {
        return longitude != null && longitude >= -180 && longitude <= 180;
    }

    // ...  resto de métodos sin cambios ...

    // Listado general para panel administrativo
    public List<Porte> listarTodos() {
        return porteRepository.findAll();
    }

    // 2. Ver Ofertas (Para que el conductor vea viajes disponibles)
    public List<Porte> listarOfertasParaConductor(Long conductorId) {
        return porteRepository.findDriverOffers(EstadoPorte.PENDIENTE, conductorId);
    }

    // 3. Aceptar Porte (El conductor acepta manualmente)
    @Transactional
    public Porte aceptarPorte(Long porteId, Long conductorId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        if (porte.getEstado() != EstadoPorte.PENDIENTE) {
            throw new RuntimeException("Este viaje ya no está disponible.");
        }

        if (porte.getConductoresRechazados().contains(conductorId)) {
            throw new RuntimeException("Ya rechazaste esta oferta y ya no está disponible para tu cuenta.");
        }

        porte.setConductor(conductorRepository.findById(conductorId)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado")));

        porte.setEstado(EstadoPorte.ASIGNADO);

        return porteRepository.save(porte);
    }

    @Transactional
    public void rechazarPorte(Long porteId, Long conductorId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        if (porte.getEstado() != EstadoPorte.PENDIENTE) {
            throw new RuntimeException("Este viaje ya no admite rechazo.");
        }

        conductorRepository.findById(conductorId)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));

        if (porte.getConductoresRechazados().contains(conductorId)) {
            return;
        }

        porte.getConductoresRechazados().add(conductorId);
        porteRepository.save(porte);
    }

    // 4. Cambiar Estado (En Tránsito, Entregado...)
    @Transactional
    public Porte cambiarEstado(Long porteId, EstadoPorte nuevoEstado) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        porte.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoPorte.ENTREGADO) {
            porte.setFechaEntrega(LocalDateTime.now());
            // Auto-generate factura on delivery (skip if one already exists)
            try {
                facturaService.generarFacturaParaPorte(porteId);
                porte.setEstado(EstadoPorte.FACTURADO);
                log.info("Factura auto-generada para porte {}", porteId);
            } catch (RuntimeException e) {
                // Already has a factura — ignore duplicate
                log.debug("Factura ya existente para porte {}: {}", porteId, e.getMessage());
            }
        }

        return porteRepository.save(porte);
    }

    // 5. Ajuste Manual de Precio (Admin añade extras/penalizaciones)
    @Transactional
    public Porte agregarAjusteManual(Long porteId, Double cantidad, String concepto) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        porte.setAjustePrecio(cantidad);
        porte.setMotivoAjuste(concepto);

        return porteRepository.save(porte);
    }

    // 6. Facturar Manualmente (Genera la factura y cierra el porte)
    @Transactional
    public Factura facturarManualmente(Long porteId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        if (porte.getEstado() != EstadoPorte. ENTREGADO) {
            throw new RuntimeException("Solo se pueden facturar portes que ya han sido ENTREGADOS.");
        }

        Factura nuevaFactura = facturaService.generarFacturaParaPorte(porteId);

        porte.setEstado(EstadoPorte.FACTURADO);
        porteRepository.save(porte);

        return nuevaFactura;
    }

    // 7. Obtener Porte por ID
    public Porte obtenerPorId(Long porteId) {
        return porteRepository. findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));
    }

    // 8. Listar Portes por Conductor
    public List<Porte> listarPortesPorConductor(Long conductorId) {
        return porteRepository.findByConductorId(conductorId);
    }

    // 9. Resumen de portes para dashboard
    public Map<String, Object> getResumen(Integer anio, Integer mes) {
        int year = anio != null ? anio : LocalDateTime.now().getYear();
        int month = mes != null ? mes : LocalDateTime.now().getMonthValue();

        Set<EstadoPorte> estadosActivos = EnumSet.of(EstadoPorte.PENDIENTE, EstadoPorte.ASIGNADO, EstadoPorte.EN_TRANSITO);

        long portesMes = porteRepository.countByYearAndMonth(year, month);
        long portesActivos = porteRepository.countByYearAndMonthAndEstadoIn(year, month, new java.util.ArrayList<>(estadosActivos));

        LocalDateTime mananaStart = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime mananaEnd = mananaStart.plusDays(1).minusSeconds(1);
        long portesManana = porteRepository.countByFechaRecogidaBetween(mananaStart, mananaEnd);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("portesMes", portesMes);
        resumen.put("portesActivos", portesActivos);
        resumen.put("portesManana", portesManana);
        return resumen;
    }

    // --- REVISION MANUAL ENDPOINTS ---

    /**
     * List portes pending manual review.
     */
    public List<Porte> listarPendientesRevision() {
        return porteRepository.findByRevisionManualTrueOrderByFechaCreacionDesc();
    }

    /**
     * Admin updates cargo dimensions on a porte.
     */
    @Transactional
    public Porte actualizarDimensiones(Long porteId, ActualizarDimensionesRequest request) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        if (request.getPesoTotalKg() != null) porte.setPesoTotalKg(request.getPesoTotalKg());
        if (request.getVolumenTotalM3() != null) porte.setVolumenTotalM3(request.getVolumenTotalM3());
        if (request.getLargoMaxPaquete() != null) porte.setLargoMaxPaquete(request.getLargoMaxPaquete());
        if (request.getAnchoMaxPaquete() != null) porte.setAnchoMaxPaquete(request.getAnchoMaxPaquete());
        if (request.getAltoMaxPaquete() != null) porte.setAltoMaxPaquete(request.getAltoMaxPaquete());

        if (request.getTipoVehiculoRequerido() != null && !request.getTipoVehiculoRequerido().isEmpty()) {
            porte.setTipoVehiculoRequerido(TipoVehiculo.valueOf(request.getTipoVehiculoRequerido()));
        }

        // Recalculate price with updated dimensions
        porte.setPrecio(calculadoraPrecio.calcularPrecioTotal(porte));

        return porteRepository.save(porte);
    }

    /**
     * Search matching conductors for a porte based on its current dimensions.
     * Returns ranked list with scores.
     */
    public List<ConductorCandidatoResponse> buscarConductoresParaPorte(Long porteId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        // Find candidate vehicles
        Integer largoMm = (porte.getLargoMaxPaquete() != null) ? (int)(porte.getLargoMaxPaquete() * 1000) : 0;
        Integer anchoMm = (porte.getAnchoMaxPaquete() != null) ? (int)(porte.getAnchoMaxPaquete() * 1000) : 0;
        Integer altoMm = (porte.getAltoMaxPaquete() != null) ? (int)(porte.getAltoMaxPaquete() * 1000) : 0;
        Double volumenRequerido = (porte.getVolumenTotalM3() != null) ? porte.getVolumenTotalM3() : 0.0;

        List<Vehiculo> candidatos = vehiculoRepository.findCandidatos(
                porte.getTipoVehiculoRequerido(),
                porte.getPesoTotalKg() != null ? porte.getPesoTotalKg() : 0.0,
                largoMm, anchoMm, altoMm, volumenRequerido
        );

        // Find available conductors
        LocalDateTime fechaRecogida = porte.getFechaRecogida() != null ? porte.getFechaRecogida() : LocalDateTime.now();
        List<Conductor> conductoresDisponibles = conductorMatchingService.buscarDisponibles(
                fechaRecogida, porte.getTipoVehiculoRequerido(), porte.getOrigen()
        );

        // Filter: only conductors that have one of the candidate vehicles
        Set<Long> conductorIdsConVehiculo = candidatos.stream()
                .filter(v -> v.getConductor() != null)
                .map(v -> v.getConductor().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<Conductor> conductoresValidos = conductoresDisponibles.stream()
                .filter(c -> conductorIdsConVehiculo.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());

        // Also include conductores disponibles that don't have matching vehicles but are available
        // (admin might want to see all available even without perfect vehicle match)
        List<Conductor> sinVehiculo = conductoresDisponibles.stream()
                .filter(c -> !conductorIdsConVehiculo.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());

        double pesoRequerido = porte.getPesoTotalKg() != null ? porte.getPesoTotalKg() : 0.0;

        // Build response with scores
        List<ConductorCandidatoResponse> result = new ArrayList<>();

        for (Conductor c : conductoresValidos) {
            double score = scoreConductor(c, candidatos, pesoRequerido);
            String vehiculoInfo = candidatos.stream()
                    .filter(v -> v.getConductor() != null && v.getConductor().getId().equals(c.getId()))
                    .findFirst()
                    .map(v -> v.getMarca() + " " + v.getModelo() + " (" + v.getMatricula() + ") - " + v.getCapacidadCargaKg() + "kg")
                    .orElse("—");

            result.add(new ConductorCandidatoResponse(
                    c.getId(), c.getNombre(), c.getApellidos(), c.getTelefono(),
                    c.getCiudadBase(), c.getRating(), c.getNumeroValoraciones(),
                    vehiculoInfo, Math.round(score * 100.0) / 100.0
            ));
        }

        // Add conductors without matching vehicles with score 0
        for (Conductor c : sinVehiculo) {
            result.add(new ConductorCandidatoResponse(
                    c.getId(), c.getNombre(), c.getApellidos(), c.getTelefono(),
                    c.getCiudadBase(), c.getRating(), c.getNumeroValoraciones(),
                    "Sin vehículo compatible", 0.0
            ));
        }

        // Sort by score descending
        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return result;
    }

    /**
     * Admin assigns a conductor to a porte manually.
     */
    @Transactional
    public Porte asignarConductorManualmente(Long porteId, Long conductorId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        Conductor conductor = conductorRepository.findById(conductorId)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));

        porte.setConductor(conductor);
        porte.setEstado(EstadoPorte.ASIGNADO);
        porte.setRevisionManual(false);
        porte.setMotivoRevision(null);

        log.info("Admin asignó conductor {} al porte {}", conductorId, porteId);

        return porteRepository.save(porte);
    }
}
