package com.cargohub.backend.service;

import com.cargohub. backend.dto.CrearPorteRequest;
import com.cargohub.backend.dto.McpWebhookResponse;
import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity. Porte;
import com.cargohub.backend.entity. Vehiculo;
import com.cargohub.backend.entity. enums.EstadoPorte;
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

import java.time.LocalDateTime;
import java.util. List;

@Slf4j
@Service
public class PorteService {

    @Autowired private PorteRepository porteRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private CalculadoraPrecioService calculadoraPrecio;
    @Autowired private FacturaService facturaService;
    @Autowired private ConductorRepository conductorRepository;
    @Autowired private McpWebhookService mcpWebhookService;
    @Autowired private ClienteRepository clienteRepository;  // AÑADIR ESTA LÍNEA

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

    @Transactional
    public Porte crearPorte(Porte porte) {
        // 0.  LLAMAR AL MCP WEBHOOK PARA CALCULAR DIMENSIONES
        if (porte.getDescripcionCliente() != null && !porte.getDescripcionCliente().isEmpty()) {
            McpWebhookResponse mcpResponse = mcpWebhookService.calcularDimensiones(porte.getDescripcionCliente());

            if (mcpResponse != null) {
                // Aplicar dimensiones calculadas por el MCP
                if (mcpResponse.getPesoTotalKg() != null && mcpResponse.getPesoTotalKg() > 0) {
                    porte.setPesoTotalKg(mcpResponse.getPesoTotalKg());
                }
                if (mcpResponse.getVolumenTotalM3() != null && mcpResponse.getVolumenTotalM3() > 0) {
                    porte.setVolumenTotalM3(mcpResponse.getVolumenTotalM3());
                }
                if (mcpResponse. getLargoMaxPaquete() != null && mcpResponse.getLargoMaxPaquete() > 0) {
                    porte.setLargoMaxPaquete(mcpResponse.getLargoMaxPaquete());
                }

                // Aplicar tipo de vehículo si fue calculado
                if (mcpResponse.getTipoVehiculoRequerido() != null && ! mcpResponse.getTipoVehiculoRequerido().isEmpty()) {
                    TipoVehiculo tipoCalculado = mcpWebhookService.convertirTipoVehiculo(mcpResponse.getTipoVehiculoRequerido());
                    if (tipoCalculado != null) {
                        porte.setTipoVehiculoRequerido(tipoCalculado);
                    }
                }

                // Aplicar flags de revisión manual
                if (mcpResponse.getRevisionManual() != null && mcpResponse.getRevisionManual()) {
                    porte.setRevisionManual(true);
                    porte.setMotivoRevision(mcpResponse.getMotivoRevision());
                }
            }
        }

        // 1. Distancia
        double km = CalculadoraDistancia. calcularKm(
                porte.getLatitudOrigen(), porte.getLongitudOrigen(),
                porte. getLatitudDestino(), porte.getLongitudDestino());
        porte.setDistanciaKm(km * 1.2);
        porte.setDistanciaEstimada(true);

        // 2. Precio
        porte.setPrecio(calculadoraPrecio. calcularPrecioTotal(porte));
        porte.setFechaCreacion(LocalDateTime.now());
        porte.setEstado(EstadoPorte.PENDIENTE);

        // 3. LOGICA DE ASIGNACIÓN (Usa Vehiculo para filtrar, pero guarda Conductor)
        Integer largoMm = (porte.getLargoMaxPaquete() != null) ? (int)(porte.getLargoMaxPaquete() * 1000) : 0;

        // Buscamos un vehículo que cumpla con TIPO, PESO y LARGO
        List<Vehiculo> candidatos = vehiculoRepository.findCandidatos(
                porte.getTipoVehiculoRequerido(),
                porte.getPesoTotalKg(),
                largoMm
        );

        if (! candidatos.isEmpty()) {
            // MATCH: Encontramos vehículo compatible -> Asignamos a SU CONDUCTOR
            Vehiculo v = candidatos.get(0);
            porte.setConductor(v.getConductor());
            porte.setEstado(EstadoPorte. ASIGNADO);
            log.info("Asignado a conductor: {}", v.getConductor().getNombre());
        } else {
            // NO MATCH: Se queda pendiente y marcamos revisión (solo si no está ya marcado)
            if (!porte.isRevisionManual()) {
                porte. setRevisionManual(true);
                porte.setMotivoRevision("No hay vehículo compatible (Peso/Largo)");
            }
        }

        return porteRepository.save(porte);
    }

    // ...  resto de métodos sin cambios ...

    // 2. Ver Ofertas (Para que el conductor vea viajes disponibles)
    public List<Porte> listarOfertasParaConductor(Long conductorId) {
        return porteRepository.findByEstadoOrderByFechaRecogidaAsc(EstadoPorte. PENDIENTE);
    }

    // 3. Aceptar Porte (El conductor acepta manualmente)
    @Transactional
    public Porte aceptarPorte(Long porteId, Long conductorId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        if (porte.getEstado() != EstadoPorte.PENDIENTE) {
            throw new RuntimeException("Este viaje ya no está disponible.");
        }

        porte.setConductor(conductorRepository.findById(conductorId)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado")));

        porte.setEstado(EstadoPorte.ASIGNADO);

        return porteRepository.save(porte);
    }

    // 4. Cambiar Estado (En Tránsito, Entregado...)
    @Transactional
    public Porte cambiarEstado(Long porteId, EstadoPorte nuevoEstado) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        porte.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoPorte.ENTREGADO) {
            porte.setFechaEntrega(LocalDateTime.now());
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
}