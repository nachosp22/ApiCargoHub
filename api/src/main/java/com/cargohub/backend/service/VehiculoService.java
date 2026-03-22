package com.cargohub.backend.service;

import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehiculoService {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    public List<Vehiculo> listarFlota() {
        return vehiculoRepository.findAll();
    }

    @Transactional
    public Vehiculo guardar(Vehiculo vehiculo) {
        return vehiculoRepository.save(vehiculo);
    }

    // SOFT DELETE: No borra el registro, cambia el estado a BAJA
    @Transactional
    public void darDeBajaVehiculo(Long vehiculoId) {
        Vehiculo v = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));

        v.setEstado(EstadoVehiculo.BAJA);
        vehiculoRepository.save(v);
    }

    // Reactivar si vuelve del taller o se readmite
    @Transactional
    public void reactivarVehiculo(Long vehiculoId) {
        Vehiculo v = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));

        v.setEstado(EstadoVehiculo.DISPONIBLE);
        vehiculoRepository.save(v);
    }

    @Transactional
    public void activarVehiculo(Long vehiculoId, Long conductorId) {
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));

        if (vehiculo.getConductor() == null || !vehiculo.getConductor().getId().equals(conductorId)) {
            throw new RuntimeException("El vehículo no pertenece a este conductor");
        }

        List<Vehiculo> vehiculosConductor = vehiculoRepository.findByConductorId(conductorId);
        for (Vehiculo v : vehiculosConductor) {
            if (!v.getId().equals(vehiculoId) && v.getEstado() == EstadoVehiculo.DISPONIBLE) {
                v.setEstado(EstadoVehiculo.BAJA);
                vehiculoRepository.save(v);
            }
        }

        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        vehiculoRepository.save(vehiculo);
    }

    @Transactional
    public void desactivarVehiculo(Long vehiculoId, Long conductorId) {
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));

        if (vehiculo.getConductor() == null || !vehiculo.getConductor().getId().equals(conductorId)) {
            throw new RuntimeException("El vehículo no pertenece a este conductor");
        }

        vehiculo.setEstado(EstadoVehiculo.BAJA);
        vehiculoRepository.save(vehiculo);
    }

    public List<Vehiculo> listarPorConductor(Long conductorId) {
        return vehiculoRepository.findByConductorId(conductorId);
    }
}