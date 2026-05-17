package com.cargohub.backend.service;

import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la gestión de vehículos: alta, baja, reactivación,
 * activación/desactivación por conductor y actualización de datos.
 */
@Service
public class VehiculoService {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    /**
     * Lista todos los vehículos que forman parte de la flota,
     * sin filtrar por estado.
     *
     * @return lista completa de vehículos registrados
     */
    public List<Vehiculo> listarFlota() {
        return vehiculoRepository.findAll();
    }

    /**
     * Persiste un vehículo en la base de datos. Si el vehículo ya tiene ID,
     * realiza una actualización; de lo contrario, lo crea.
     *
     * @param vehiculo entidad a guardar o actualizar
     * @return el vehículo persistido con su ID asignado
     */
    @Transactional
    public Vehiculo guardar(Vehiculo vehiculo) {
        return vehiculoRepository.save(vehiculo);
    }

    /**
     * Da de baja un vehículo de forma lógica cambiando su estado a {@code BAJA}.
     * No elimina el registro de la base de datos.
     *
     * @param vehiculoId identificador del vehículo a dar de baja
     * @throws RuntimeException si no se encuentra el vehículo con el ID indicado
     */
    @Transactional
    public void darDeBajaVehiculo(Long vehiculoId) {
        Vehiculo v = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));

        v.setEstado(EstadoVehiculo.BAJA);
        vehiculoRepository.save(v);
    }

    /**
     * Reactiva un vehículo que estaba dado de baja, devolviéndolo
     * al estado {@code DISPONIBLE}.
     *
     * @param vehiculoId identificador del vehículo a reactivar
     * @throws RuntimeException si no se encuentra el vehículo con el ID indicado
     */
    @Transactional
    public void reactivarVehiculo(Long vehiculoId) {
        Vehiculo v = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));

        v.setEstado(EstadoVehiculo.DISPONIBLE);
        vehiculoRepository.save(v);
    }

    /**
     * Activa un vehículo para un conductor específico. Antes de activarlo,
     * desactiva (pone en {@code BAJA}) cualquier otro vehículo {@code DISPONIBLE}
     * del mismo conductor, garantizando que solo haya un vehículo activo a la vez.
     *
     * @param vehiculoId identificador del vehículo a activar
     * @param conductorId identificador del conductor al que pertenece el vehículo
     * @throws RuntimeException si el vehículo no existe o no pertenece al conductor indicado
     */
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

    /**
     * Desactiva un vehículo de un conductor, cambiando su estado a {@code BAJA}.
     * Valida que el vehículo pertenezca al conductor indicado antes de proceder.
     *
     * @param vehiculoId identificador del vehículo a desactivar
     * @param conductorId identificador del conductor propietario del vehículo
     * @throws RuntimeException si el vehículo no existe o no pertenece al conductor indicado
     */
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

    /**
     * Lista todos los vehículos asignados a un conductor determinado.
     *
     * @param conductorId identificador del conductor
     * @return lista de vehículos asociados al conductor
     */
    public List<Vehiculo> listarPorConductor(Long conductorId) {
        return vehiculoRepository.findByConductorId(conductorId);
    }

    /**
     * Actualiza de forma parcial los datos de un vehículo. Solo se modifican
     * los campos que vienen con valor no nulo en el objeto {@code updates}.
     * Valida que el vehículo pertenezca al conductor indicado.
     *
     * @param vehiculoId identificador del vehículo a actualizar
     * @param conductorId identificador del conductor propietario
     * @param updates objeto con los campos a actualizar (puede ser parcial)
     * @return el vehículo actualizado y persistido
     * @throws RuntimeException si el vehículo no existe o no pertenece al conductor indicado
     */
    @Transactional
    public Vehiculo actualizarVehiculo(Long vehiculoId, Long conductorId, Vehiculo updates) {
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));

        if (vehiculo.getConductor() == null || !vehiculo.getConductor().getId().equals(conductorId)) {
            throw new RuntimeException("El vehículo no pertenece a este conductor");
        }

        if (updates.getMatricula() != null) vehiculo.setMatricula(updates.getMatricula());
        if (updates.getMarca() != null) vehiculo.setMarca(updates.getMarca());
        if (updates.getModelo() != null) vehiculo.setModelo(updates.getModelo());
        if (updates.getTipo() != null) vehiculo.setTipo(updates.getTipo());
        if (updates.getCapacidadCargaKg() != null) vehiculo.setCapacidadCargaKg(updates.getCapacidadCargaKg());
        if (updates.getLargoUtilMm() != null) vehiculo.setLargoUtilMm(updates.getLargoUtilMm());
        if (updates.getAnchoUtilMm() != null) vehiculo.setAnchoUtilMm(updates.getAnchoUtilMm());
        if (updates.getAltoUtilMm() != null) vehiculo.setAltoUtilMm(updates.getAltoUtilMm());

        return vehiculoRepository.save(vehiculo);
    }
}
