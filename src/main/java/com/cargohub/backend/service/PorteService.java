package com.cargohub.backend.service;

import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoPorte;
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
            porte.setConductor(v.getConductor()); // <--- Aquí usamos tu clase Conductor
            porte.setEstado(EstadoPorte.ASIGNADO);
            System.out.println("Asignado a conductor: " + v.getConductor().getNombre());
        } else {
            // NO MATCH: Se queda pendiente y marcamos revisión
            porte.setRevisionManual(true);
            porte.setMotivoRevision("No hay vehículo compatible (Peso/Largo)");
        }

        return porteRepository.save(porte);
    }
}
