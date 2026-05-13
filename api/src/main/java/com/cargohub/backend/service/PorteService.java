package com.cargohub.backend.service;

import com.cargohub.backend.dto.CrearPorteRequest;
import com.cargohub.backend.dto.CargoAnalysisResponse;
import com.cargohub.backend.dto.SolicitudPorteRequest;
import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.VehiculoRepository;
import com.cargohub.backend.util.CalculadoraDistancia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cargohub.backend.dto.ActualizarDimensionesRequest;
import com.cargohub.backend.dto.ActualizarPorteRequest;
import com.cargohub.backend.dto.ConductorCandidatoResponse;
import com.cargohub.backend.dto.FirmaEntregaRequest;

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

    public static final String MSG_ESPERANDO_ACEPTACION_CONDUCTOR = "La carga se ha analizado correctamente. Le asignaremos un conductor lo antes posible.";
    public static final String MSG_REVISION_MANUAL_CLIENTE = "No se ha podido analizar la carga. El porte sera revisado manualmente por uno de nuestros agentes.";
    public static final String MSG_SIN_CONDUCTORES_DISPONIBLES = "Porte validado correctamente, pero no hay conductores disponibles que se ajusten a los requisitos en este momento";
    public static final String MSG_SIN_MATCH_VEHICULO_CONDUCTOR = "Porte validado correctamente, pero no hay vehículos o conductores compatibles para estos requisitos en este momento";
    public static final String MSG_REMATCHING_EXITO = "Rematching completado. Se ha enviado oferta a %d conductores.";

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
        porte.setLongitudOrigen(request.getLongitudOrigen());
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
                if (cargoAnalysisResponse.getLargoMaxPaquete() != null && cargoAnalysisResponse.getLargoMaxPaquete() > 0) {
                    porte.setLargoMaxPaquete(cargoAnalysisResponse.getLargoMaxPaquete());
                }
                if (cargoAnalysisResponse.getAnchoMaxPaquete() != null && cargoAnalysisResponse.getAnchoMaxPaquete() > 0) {
                    porte.setAnchoMaxPaquete(cargoAnalysisResponse.getAnchoMaxPaquete());
                }
                if (cargoAnalysisResponse.getAltoMaxPaquete() != null && cargoAnalysisResponse.getAltoMaxPaquete() > 0) {
                    porte.setAltoMaxPaquete(cargoAnalysisResponse.getAltoMaxPaquete());
                }

                // Aplicar tipo de vehículo si fue calculado
                if (cargoAnalysisResponse.getTipoVehiculoRequerido() != null && !cargoAnalysisResponse.getTipoVehiculoRequerido().isEmpty()) {
                    TipoVehiculo tipoCalculado = cargoAnalysisService.convertirTipoVehiculo(cargoAnalysisResponse.getTipoVehiculoRequerido());
                    if (tipoCalculado != null) {
                        porte.setTipoVehiculoRequerido(tipoCalculado);
                    }
                }

                // Aplicar flags de revisión manual desde Gemini
                if (cargoAnalysisResponse.getRevisionManual() != null && cargoAnalysisResponse.getRevisionManual()) {
                    porte.setRevisionManual(true);
                    porte.setMotivoRevision(cargoAnalysisResponse.getMotivoRevision());
                }
            }
        }

        // Inferir ancho/alto faltantes desde el tipo de vehículo antes de validación MVP
        if (porte.getTipoVehiculoRequerido() != null) {
            if (porte.getAnchoMaxPaquete() == null || porte.getAnchoMaxPaquete() <= 0) {
                porte.setAnchoMaxPaquete(inferAnchoFromVehicleType(porte.getTipoVehiculoRequerido().name()));
            }
            if (porte.getAltoMaxPaquete() == null || porte.getAltoMaxPaquete() <= 0) {
                porte.setAltoMaxPaquete(inferAltoFromVehicleType(porte.getTipoVehiculoRequerido().name()));
            }
        }

        validarDatosCriticosParaMatching(porte);

        // 1. Distancia
        if (hasValidCoordinates(porte)) {
            double km = CalculadoraDistancia.calcularKm(
                    porte.getLatitudOrigen(), porte.getLongitudOrigen(),
                    porte.getLatitudDestino(), porte.getLongitudDestino());
            porte.setDistanciaKm(km * 1.2);
            porte.setDistanciaEstimada(true);
        } else {
            porte.setDistanciaKm(0.0);
            porte.setDistanciaEstimada(true);
        }

        // 2. Precio
        porte.setPrecio(calculadoraPrecio.calcularPrecioTotal(porte));
        porte.setFechaCreacion(LocalDateTime.now());
        porte.setEstado(EstadoPorte.PENDIENTE);

        if (porte.isRevisionManual()) {
            return porteRepository.save(porte);
        }

        aplicarResultadoMatchingAutomatico(porte);

        return porteRepository.save(porte);
    }

    @Transactional
    public Porte retryMatching(Long porteId) {
        Porte porte = obtenerPorId(porteId);

        porte.setConductor(null);
        porte.setEstado(EstadoPorte.PENDIENTE);
        porte.setRevisionManual(false);
        porte.setMotivoRevision(null);

        validarDatosCriticosParaMatching(porte);

        if (porte.isRevisionManual()) {
            return porteRepository.save(porte);
        }

        // Admin retry: solo comprueba compatibilidad de vehículo.
        // Ignora disponibilidad, agenda, radio y distancia intencionalmente.
        List<Vehiculo> candidatos = buscarVehiculosCompatiblesAdmin(porte);

        if (!candidatos.isEmpty()) {
            long numConductores = candidatos.stream()
                    .map(Vehiculo::getConductor)
                    .filter(c -> c != null && c.getId() != null)
                    .map(Conductor::getId)
                    .distinct()
                    .count();

            porte.setConductor(null);
            porte.setEstado(EstadoPorte.PENDIENTE);
            porte.setRevisionManual(false);
            porte.setMotivoRevision(String.format(MSG_REMATCHING_EXITO, numConductores));
            log.info("Admin retry-matching porte {}: {} vehículos compatibles, {} conductores notificados",
                    porte.getId(), candidatos.size(), numConductores);
        } else {
            porte.setRevisionManual(false);
            porte.setConductor(null);
            porte.setEstado(EstadoPorte.PENDIENTE);
            porte.setMotivoRevision(MSG_SIN_MATCH_VEHICULO_CONDUCTOR);
            log.info("Admin retry-matching porte {}: ningún vehículo compatible", porte.getId());
        }

        return porteRepository.save(porte);
    }

    private void validarDatosCriticosParaMatching(Porte porte) {
        if (porte.isRevisionManual()) {
            return;
        }

        List<String> motivos = new ArrayList<>();
        if (porte.getTipoVehiculoRequerido() == null) {
            motivos.add("Tipo de vehículo requerido no determinado");
        }
        if (porte.getLatitudOrigen() == null || porte.getLongitudOrigen() == null) {
            motivos.add("Coordenadas de origen faltantes");
        }
        if (porte.getLatitudDestino() == null || porte.getLongitudDestino() == null) {
            motivos.add("Coordenadas de destino faltantes");
        }

        if (!motivos.isEmpty()) {
            porte.setRevisionManual(true);
            porte.setMotivoRevision(MSG_REVISION_MANUAL_CLIENTE);
        }
    }

    private void aplicarResultadoMatchingAutomatico(Porte porte) {
        // 3. LOGICA DE ASIGNACIÓN CON SCORING Y CONDUCTOR MATCHING
        List<Vehiculo> candidatos = buscarVehiculosCompatibles(porte);

        if (!candidatos.isEmpty()) {
            // Usar ConductorMatchingService para filtrar conductores disponibles
            LocalDateTime fechaRecogida = porte.getFechaRecogida() != null ? porte.getFechaRecogida() : LocalDateTime.now();
            LocalDateTime fechaEntrega = porte.getFechaEntrega() != null ? porte.getFechaEntrega() : fechaRecogida;
            String ciudadOrigen = (porte.getCiudadOrigen() != null && !porte.getCiudadOrigen().isBlank())
                    ? porte.getCiudadOrigen()
                    : porte.getOrigen();

            List<Conductor> conductoresDisponibles = conductorMatchingService.buscarDisponibles(
                    fechaRecogida,
                    fechaEntrega,
                    porte.getTipoVehiculoRequerido(),
                    ciudadOrigen,
                    porte.getLatitudOrigen(),
                    porte.getLongitudOrigen()
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
                porte.setRevisionManual(false);
                porte.setMotivoRevision(MSG_ESPERANDO_ACEPTACION_CONDUCTOR);
                log.info("Porte {} publicado en ofertas para {} conductores elegibles", porte.getId(), conductoresValidos.size());
            } else {
                porte.setRevisionManual(false);
                porte.setConductor(null);
                porte.setEstado(EstadoPorte.PENDIENTE);
                porte.setMotivoRevision(MSG_SIN_CONDUCTORES_DISPONIBLES);
            }
        } else {
            porte.setRevisionManual(false);
            porte.setConductor(null);
            porte.setEstado(EstadoPorte.PENDIENTE);
            porte.setMotivoRevision(MSG_SIN_MATCH_VEHICULO_CONDUCTOR);
        }
    }

    private List<Vehiculo> buscarVehiculosCompatibles(Porte porte) {
        if (porte.getTipoVehiculoRequerido() == null) {
            return List.of();
        }

        Integer largoMm = (porte.getLargoMaxPaquete() != null) ? (int)(porte.getLargoMaxPaquete() * 1000) : 0;
        Integer anchoMm = (porte.getAnchoMaxPaquete() != null) ? (int)(porte.getAnchoMaxPaquete() * 1000) : 0;
        Integer altoMm = (porte.getAltoMaxPaquete() != null) ? (int)(porte.getAltoMaxPaquete() * 1000) : 0;
        Double volumenRequerido = (porte.getVolumenTotalM3() != null) ? porte.getVolumenTotalM3() : 0.0;
        Double pesoRequerido = porte.getPesoTotalKg() != null ? porte.getPesoTotalKg() : 0.0;

        return vehiculoRepository.findCandidatos(
                porte.getTipoVehiculoRequerido(),
                pesoRequerido,
                largoMm,
                anchoMm,
                altoMm,
                volumenRequerido
        );
    }

    /**
     * Admin / manual review: busca vehículos compatibles SIN filtrar por estado DISPONIBLE
     * y aceptando el tipo solicitado o cualquier tipo superior (FURGONETA < RIGIDO < TRAILER).
     * Solo comprueba dimensiones/volumen/peso.
     */
    private List<Vehiculo> buscarVehiculosCompatiblesAdmin(Porte porte) {
        if (porte.getTipoVehiculoRequerido() == null) {
            return List.of();
        }

        List<TipoVehiculo> tiposCompatibles = getTiposSuperioresOIguales(porte.getTipoVehiculoRequerido());

        Integer largoMm = (porte.getLargoMaxPaquete() != null) ? (int)(porte.getLargoMaxPaquete() * 1000) : 0;
        Integer anchoMm = (porte.getAnchoMaxPaquete() != null) ? (int)(porte.getAnchoMaxPaquete() * 1000) : 0;
        Integer altoMm = (porte.getAltoMaxPaquete() != null) ? (int)(porte.getAltoMaxPaquete() * 1000) : 0;
        Double volumenRequerido = (porte.getVolumenTotalM3() != null) ? porte.getVolumenTotalM3() : 0.0;
        Double pesoRequerido = porte.getPesoTotalKg() != null ? porte.getPesoTotalKg() : 0.0;

        return vehiculoRepository.findTodosCandidatos(
                tiposCompatibles,
                pesoRequerido,
                largoMm,
                anchoMm,
                altoMm,
                volumenRequerido
        );
    }

    /**
     * Devuelve el tipo solicitado más todos los superiores.
     * Jerarquía: FURGONETA < RIGIDO < TRAILER. ESPECIAL solo se empareja consigo mismo.
     */
    private List<TipoVehiculo> getTiposSuperioresOIguales(TipoVehiculo tipo) {
        return switch (tipo) {
            case FURGONETA -> List.of(TipoVehiculo.FURGONETA, TipoVehiculo.RIGIDO, TipoVehiculo.TRAILER);
            case RIGIDO    -> List.of(TipoVehiculo.RIGIDO, TipoVehiculo.TRAILER);
            case TRAILER   -> List.of(TipoVehiculo.TRAILER);
            default        -> List.of(tipo);
        };
    }

    /**
     * Scores a conductor for assignment. Higher is better.
     * Capacity efficiency prefers vehicles closest to the required load (avoids waste).
     */
    private double scoreConductor(Conductor conductor, List<Vehiculo> candidatos, double pesoRequerido) {
        // Capacity efficiency: prefer smallest vehicle that fits (0-100 points)
        double efficiencyScore = candidatos.stream()
                .filter(v -> v.getConductor() != null && v.getConductor().getId().equals(conductor.getId()))
                .mapToDouble(v -> {
                    double capacidad = v.getCapacidadCargaKg() != null ? v.getCapacidadCargaKg() : 1.0;
                    if (capacidad <= 0) capacidad = 1.0;
                    // Ratio of how well the vehicle is utilized (1.0 = perfect fit)
                    double ratio = pesoRequerido / capacidad;
                    return Math.min(ratio, 1.0) * 100.0;
                })
                .max()
                .orElse(0.0);

        return efficiencyScore;
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

    private double inferAnchoFromVehicleType(String tipo) {
        return switch (tipo) {
            case "FURGONETA" -> 1.7;
            case "RIGIDO" -> 2.45;
            case "TRAILER" -> 2.45;
            default -> 1.7;
        };
    }

    private double inferAltoFromVehicleType(String tipo) {
        return switch (tipo) {
            case "FURGONETA" -> 1.8;
            case "RIGIDO" -> 2.5;
            case "TRAILER" -> 2.7;
            default -> 1.8;
        };
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

        if (porte.getEstado() != EstadoPorte.ENTREGADO) {
            throw new RuntimeException("Solo se pueden facturar portes que ya han sido ENTREGADOS.");
        }

        Factura nuevaFactura = facturaService.generarFacturaParaPorte(porteId);

        porte.setEstado(EstadoPorte.FACTURADO);
        porteRepository.save(porte);

        return nuevaFactura;
    }

    // 7. Obtener Porte por ID
    public Porte obtenerPorId(Long porteId) {
        return porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));
    }

    @Transactional
    public Porte actualizarPorte(Long porteId, ActualizarPorteRequest request) {
        Porte porte = obtenerPorId(porteId);

        if (request.getOrigen() != null) porte.setOrigen(request.getOrigen());
        if (request.getDestino() != null) porte.setDestino(request.getDestino());
        if (request.getDescripcionCliente() != null) porte.setDescripcionCliente(request.getDescripcionCliente());
        if (request.getFechaRecogida() != null) porte.setFechaRecogida(request.getFechaRecogida());
        if (request.getFechaEntrega() != null) porte.setFechaEntrega(request.getFechaEntrega());
        if (request.getEstado() != null) porte.setEstado(request.getEstado());

        return porteRepository.save(porte);
    }

    @Transactional
    public boolean eliminarOCancelarPorte(Long porteId) {
        Porte porte = obtenerPorId(porteId);

        EstadoPorte estado = porte.getEstado();
        if (estado == EstadoPorte.PENDIENTE || estado == EstadoPorte.CANCELADO) {
            porteRepository.delete(porte);
            return true;
        }

        if (estado == EstadoPorte.ASIGNADO || estado == EstadoPorte.EN_TRANSITO) {
            porte.setEstado(EstadoPorte.CANCELADO);
            porteRepository.save(porte);
            return false;
        }

        throw new RuntimeException("No se puede borrar/cancelar un porte en estado " + estado + " sin comprometer historial");
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
     * List portes that need admin attention: either manual data review
     * or operational follow-up because they could not be assigned.
     */
    public List<Porte> listarPendientesRevision() {
        return porteRepository.findPendientesAdminReview(MSG_ESPERANDO_ACEPTACION_CONDUCTOR, "Rematching completado");
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
     * Search manual-assignment candidates for a porte.
     * Manual review only filters by vehicle compatibility (type, dimensions, weight, volume).
     * It intentionally ignores radius, route distance, agenda availability and date overlap;
     * those constraints belong to the automatic matching path.
     */
    public List<ConductorCandidatoResponse> buscarConductoresParaPorte(Long porteId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        List<Vehiculo> candidatos = buscarVehiculosCompatiblesAdmin(porte);
        double pesoRequerido = porte.getPesoTotalKg() != null ? porte.getPesoTotalKg() : 0.0;
        Map<Long, ConductorCandidatoResponse> resultByConductor = new HashMap<>();

        for (Vehiculo vehiculo : candidatos) {
            Conductor c = vehiculo.getConductor();
            if (c == null || c.getId() == null || resultByConductor.containsKey(c.getId())) {
                continue;
            }

            double score = scoreConductor(c, candidatos, pesoRequerido);
            String vehiculoInfo = formatVehiculoInfo(vehiculo);

            resultByConductor.put(c.getId(), new ConductorCandidatoResponse(
                    c.getId(), c.getNombre(), c.getApellidos(), c.getTelefono(),
                    c.getCiudadBase(), vehiculoInfo, Math.round(score * 100.0) / 100.0
            ));
        }

        List<ConductorCandidatoResponse> result = new ArrayList<>(resultByConductor.values());
        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return result;
    }

    private String formatVehiculoInfo(Vehiculo v) {
        String marca = v.getMarca() != null ? v.getMarca() : "";
        String modelo = v.getModelo() != null ? v.getModelo() : "";
        String matricula = v.getMatricula() != null ? v.getMatricula() : "sin matrícula";
        String capacidad = v.getCapacidadCargaKg() != null ? v.getCapacidadCargaKg() + "kg" : "capacidad no informada";
        return (marca + " " + modelo).trim() + " (" + matricula + ") - " + capacidad;
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

        boolean tieneVehiculoCompatible = buscarVehiculosCompatiblesAdmin(porte).stream()
                .anyMatch(v -> v.getConductor() != null && conductorId.equals(v.getConductor().getId()));
        if (!tieneVehiculoCompatible) {
            throw new RuntimeException("El conductor seleccionado no tiene un vehículo compatible con las medidas del porte");
        }

        porte.setConductor(conductor);
        porte.setEstado(EstadoPorte.ASIGNADO);
        porte.setRevisionManual(false);
        porte.setMotivoRevision(null);

        log.info("Admin asignó conductor {} al porte {}", conductorId, porteId);

        return porteRepository.save(porte);
    }

    @Transactional
    public Porte registrarFirmaEntrega(Long porteId, FirmaEntregaRequest request) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        EstadoPorte estadoActual = porte.getEstado();
        if (estadoActual == EstadoPorte.EN_TRANSITO) {
            porte = cambiarEstado(porteId, EstadoPorte.ENTREGADO);
        } else if (estadoActual != EstadoPorte.ENTREGADO && estadoActual != EstadoPorte.FACTURADO) {
            throw new RuntimeException(
                    "No se puede registrar firma de entrega para un porte en estado " + estadoActual
                            + ". Estados válidos: EN_TRANSITO, ENTREGADO o FACTURADO."
            );
        }

        porte.setFirmaEntregaBase64(request.getFirmaBase64());
        porte.setFirmaEntregaFirmadoPor(request.getFirmadoPor());
        porte.setFirmaEntregaFecha(LocalDateTime.now());

        return porteRepository.save(porte);
    }
}
