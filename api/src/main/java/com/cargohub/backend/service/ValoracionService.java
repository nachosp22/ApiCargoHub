package com.cargohub.backend.service;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Valoracion;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.ValoracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ValoracionService {

    private final ValoracionRepository valoracionRepository;
    private final PorteRepository porteRepository;
    private final ClienteRepository clienteRepository;
    private final ConductorRepository conductorRepository;

    public ValoracionService(ValoracionRepository valoracionRepository,
                             PorteRepository porteRepository,
                             ClienteRepository clienteRepository,
                             ConductorRepository conductorRepository) {
        this.valoracionRepository = valoracionRepository;
        this.porteRepository = porteRepository;
        this.clienteRepository = clienteRepository;
        this.conductorRepository = conductorRepository;
    }

    @Transactional
    public Valoracion crearValoracion(Long porteId, Long clienteId, int puntuacion, String comentario) {
        // Validate puntuacion range
        if (puntuacion < 1 || puntuacion > 5) {
            throw new IllegalArgumentException("La puntuación debe estar entre 1 y 5");
        }

        // Validate porte exists
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado con id: " + porteId));

        // Validate porte is in valid state
        if (porte.getEstado() != EstadoPorte.ENTREGADO && porte.getEstado() != EstadoPorte.FACTURADO) {
            throw new RuntimeException("Solo se pueden valorar portes en estado ENTREGADO o FACTURADO");
        }

        // Validate the cliente is the owner of the porte
        if (!porte.getCliente().getId().equals(clienteId)) {
            throw new RuntimeException("Solo el cliente propietario del porte puede valorarlo");
        }

        // Validate conductor assigned
        if (porte.getConductor() == null) {
            throw new RuntimeException("El porte no tiene conductor asignado");
        }

        // Validate no duplicate rating
        if (valoracionRepository.existsByPorteIdAndClienteId(porteId, clienteId)) {
            throw new RuntimeException("Ya existe una valoración para este porte");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + clienteId));

        Conductor conductor = porte.getConductor();

        // Create and save valoracion
        Valoracion valoracion = new Valoracion();
        valoracion.setPorte(porte);
        valoracion.setCliente(cliente);
        valoracion.setConductor(conductor);
        valoracion.setPuntuacion(puntuacion);
        valoracion.setComentario(comentario);
        valoracion.setFechaCreacion(LocalDateTime.now());

        Valoracion saved = valoracionRepository.save(valoracion);

        // Update conductor's average rating
        conductor.recibirValoracion(puntuacion);
        conductorRepository.save(conductor);

        return saved;
    }

    public List<Valoracion> obtenerValoracionesConductor(Long conductorId) {
        return valoracionRepository.findByConductorIdOrderByFechaCreacionDesc(conductorId);
    }

    public List<Valoracion> obtenerValoracionesPorte(Long porteId) {
        return valoracionRepository.findByPorteId(porteId);
    }

    public Valoracion obtenerValoracionClientePorte(Long porteId, Long clienteId) {
        return valoracionRepository.findByPorteIdAndClienteId(porteId, clienteId).orElse(null);
    }
}
