package com.cargohub.backend.service;

import com.cargohub.backend.entity.*;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.repository.*;
import com.cargohub.backend.util.CalculadoraDistancia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PorteService {

    @Autowired private PorteRepository porteRepository;
    @Autowired private ConductorService conductorService;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private BloqueoAgendaRepository bloqueoRepository;
    @Autowired private FacturaService facturaService;
    @Autowired private MapaService mapaService;
    @Autowired private CalculadoraPrecioService calculadoraPrecio;

    // ... (Mantén crearPorte, listarOfertas, aceptarPorte, agregarAjusteManual IGUAL) ...

    /**
     * OPERATIVA: El conductor (o admin) cambia el estado.
     * Si pasa a ENTREGADO, solo liberamos al conductor, NO facturamos aún.
     */
    @Transactional
    public Porte cambiarEstado(Long porteId, EstadoPorte nuevoEstado) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        porte.setEstado(nuevoEstado);

        // Si se entrega, el conductor queda libre para otro viaje
        if (nuevoEstado == EstadoPorte.ENTREGADO) {
            porte.setFechaEntrega(LocalDateTime.now()); // Guardamos hora real de fin
            if (porte.getConductor() != null) {
                conductorService.cambiarDisponibilidad(porte.getConductor().getId(), true);
            }
        }

        return porteRepository.save(porte);
    }

    /**
     * ADMINISTRACIÓN: Botón final "GENERAR FACTURA".
     * Requisito: El porte debe estar ENTREGADO.
     * Efecto: Crea la factura y pasa el estado a FACTURADO.
     */
    @Transactional
    public Factura facturarManualmente(Long porteId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        // Validaciones de seguridad
        if (porte.getEstado() == EstadoPorte.FACTURADO) {
            throw new RuntimeException("Este porte ya está facturado (F-" + porte.getId() + ")");
        }
        if (porte.getEstado() != EstadoPorte.ENTREGADO) {
            throw new RuntimeException("No puedes facturar un viaje que no se ha entregado todavía.");
        }

        // 1. Generamos la factura (con los precios y ajustes que hayas metido)
        Factura factura = facturaService.generarFacturaParaPorte(porteId);

        // 2. CAMBIO DE ESTADO FINAL
        porte.setEstado(EstadoPorte.FACTURADO);
        porteRepository.save(porte);

        return factura;
    }

    // ... (El resto de métodos igual) ...
    // --- RECORDATORIO: MÉTODOS QUE DEBES TENER (Resumidos) ---
    @Transactional
    public Porte crearPorte(Porte porte) {
        Double metrosReales = mapaService.obtenerDistanciaMetros(
                porte.getLatitudOrigen(), porte.getLongitudOrigen(),
                porte.getLatitudDestino(), porte.getLongitudDestino()
        );
        if (metrosReales != null) {
            double km = Math.round((metrosReales / 1000.0) * 100.0) / 100.0;
            porte.setDistanciaKm(km);
            porte.setDistanciaEstimada(false);
        } else {
            double kmRectos = CalculadoraDistancia.calcularKm(
                    porte.getLatitudOrigen(), porte.getLongitudOrigen(),
                    porte.getLatitudDestino(), porte.getLongitudDestino()
            );
            porte.setDistanciaKm(Math.round(kmRectos * 1.2 * 100.0) / 100.0);
            porte.setDistanciaEstimada(true);
        }
        Double precioFinal = calculadoraPrecio.calcularPrecioTotal(porte);
        porte.setPrecio(precioFinal);
        porte.setFechaCreacion(LocalDateTime.now());
        porte.setEstado(EstadoPorte.PENDIENTE);
        return porteRepository.save(porte);
    }

    public List<Porte> listarOfertasParaConductor(Long conductorId) {
        Conductor c = conductorService.obtenerPorId(conductorId);
        List<Porte> todos = porteRepository.findByEstadoOrderByFechaRecogidaAsc(EstadoPorte.PENDIENTE);
        return todos.stream().filter(p -> esCompatible(c, p)).collect(Collectors.toList());
    }

    private boolean esCompatible(Conductor c, Porte p) {
        if (bloqueoRepository.estaBloqueado(c.getId(), p.getFechaRecogida(), p.getFechaEntrega())) return false;
        if (porteRepository.tieneViajeEnFecha(c.getId(), p.getFechaRecogida(), p.getFechaEntrega())) return false;
        Double latC = c.getLatitudActual() != null ? c.getLatitudActual() : c.getLatitudBase();
        Double lonC = c.getLongitudActual() != null ? c.getLongitudActual() : c.getLongitudBase();
        if (c.getRadioAccionKm() != null && c.getRadioAccionKm() > 0 && latC != null) {
            double dist = CalculadoraDistancia.calcularKm(latC, lonC, p.getLatitudOrigen(), p.getLongitudOrigen());
            if (dist > c.getRadioAccionKm()) return false;
        }
        return true;
    }

    @Transactional
    public Porte aceptarPorte(Long porteId, Long conductorId) {
        Porte porte = porteRepository.findById(porteId).orElseThrow(() -> new RuntimeException("Porte no encontrado"));
        if (porte.getEstado() != EstadoPorte.PENDIENTE) throw new RuntimeException("Ocupado");
        Conductor conductor = conductorService.obtenerPorId(conductorId);
        porte.setConductor(conductor);
        porte.setEstado(EstadoPorte.ASIGNADO);
        List<Vehiculo> vehiculos = vehiculoRepository.findByConductorId(conductorId);
        if (!vehiculos.isEmpty()) porte.setVehiculo(vehiculos.get(0));
        return porteRepository.save(porte);
    }

    @Transactional
    public Porte agregarAjusteManual(Long porteId, Double cantidad, String concepto) {
        Porte porte = porteRepository.findById(porteId).orElseThrow(() -> new RuntimeException("Porte no encontrado"));
        Double ajusteActual = porte.getAjustePrecio() != null ? porte.getAjustePrecio() : 0.0;
        porte.setAjustePrecio(ajusteActual + cantidad);
        String motivoActual = porte.getMotivoAjuste();
        String nuevoMotivo = String.format("(%+.2f€: %s)", cantidad, concepto);
        if (motivoActual == null || motivoActual.isEmpty()) {
            porte.setMotivoAjuste(nuevoMotivo);
        } else {
            porte.setMotivoAjuste(motivoActual + " | " + nuevoMotivo);
        }
        return porteRepository.save(porte);
    }
}