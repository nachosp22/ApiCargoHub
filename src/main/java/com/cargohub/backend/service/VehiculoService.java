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
}