package com.cargohub.backend.service;

import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.VehiculoRepository;
import com.cargohub.backend.util.CalculadoraDistancia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PorteService {

    @Autowired private PorteRepository porteRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private CalculadoraPrecioService calculadoraPrecio;

    // --- NUEVAS DEPENDENCIAS NECESARIAS ---
    @Autowired private FacturaService facturaService;
    @Autowired private ConductorRepository conductorRepository;

    @Transactional
    public Porte crearPorte(Porte porte) {
        // 1. Distancia
        double km = CalculadoraDistancia.calcularKm(
                porte.getLatitudOrigen(), porte.getLongitudOrigen(),
                porte.getLatitudDestino(), porte.getLongitudDestino());
        porte.setDistanciaKm(km * 1.2);
        porte.setDistanciaEstimada(true);

        // 2. Precio
        porte.setPrecio(calculadoraPrecio.calcularPrecioTotal(porte));
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

        if (!candidatos.isEmpty()) {
            // MATCH: Encontramos vehículo compatible -> Asignamos a SU CONDUCTOR
            Vehiculo v = candidatos.get(0);
            porte.setConductor(v.getConductor());
            porte.setEstado(EstadoPorte.ASIGNADO);
            System.out.println("Asignado a conductor: " + v.getConductor().getNombre());
        } else {
            // NO MATCH: Se queda pendiente y marcamos revisión
            porte.setRevisionManual(true);
            porte.setMotivoRevision("No hay vehículo compatible (Peso/Largo)");
        }

        return porteRepository.save(porte);
    }

    // ==========================================
    //      MÉTODOS AÑADIDOS QUE FALTABAN
    // ==========================================

    // 2. Ver Ofertas (Para que el conductor vea viajes disponibles)
    public List<Porte> listarOfertasParaConductor(Long conductorId) {
        // Devuelve todos los pendientes. Podrías filtrar por cercanía si quisieras.
        return porteRepository.findByEstadoOrderByFechaRecogidaAsc(EstadoPorte.PENDIENTE);
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

        // Si se entrega, registramos la fecha real
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

        if (porte.getEstado() != EstadoPorte.ENTREGADO) {
            throw new RuntimeException("Solo se pueden facturar portes que ya han sido ENTREGADOS.");
        }

        // Llamamos al servicio de facturas para que haga los cálculos
        Factura nuevaFactura = facturaService.generarFacturaParaPorte(porteId);

        // Actualizamos el estado del porte para cerrarlo definitivamente
        porte.setEstado(EstadoPorte.FACTURADO);
        porteRepository.save(porte);

        return nuevaFactura;
    }

    // 7. Obtener Porte por ID
    public Porte obtenerPorId(Long porteId) {
        return porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));
    }

    // 8. Listar Portes por Conductor
    public List<Porte> listarPortesPorConductor(Long conductorId) {
        return porteRepository.findByConductorId(conductorId);
    }
}