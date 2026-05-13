package com.cargohub.backend.controller;

import com.cargohub.backend.dto.VehiculoUpsertRequest;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class VehiculoController {

    @Autowired
    private VehiculoService vehiculoService;

    // 1. Ver toda la flota
    @GetMapping
    public ResponseEntity<List<Vehiculo>> listarFlota() {
        return ResponseEntity.ok(vehiculoService.listarFlota());
    }

    // 2. Dar de alta un camión nuevo
    @PostMapping
    public ResponseEntity<Vehiculo> guardar(@Valid @RequestBody VehiculoUpsertRequest request) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(request.getId());
        vehiculo.setMatricula(request.getMatricula());
        vehiculo.setMarca(request.getMarca());
        vehiculo.setModelo(request.getModelo());
        if (request.getTipo() != null) vehiculo.setTipo(request.getTipo());
        if (request.getEstado() != null) vehiculo.setEstado(request.getEstado());
        vehiculo.setCapacidadCargaKg(request.getCapacidadCargaKg());
        vehiculo.setLargoUtilMm(request.getLargoUtilMm());
        vehiculo.setAnchoUtilMm(request.getAnchoUtilMm());
        vehiculo.setAltoUtilMm(request.getAltoUtilMm());
        if (request.getConductor() != null && request.getConductor().getId() != null) {
            Conductor conductor = new Conductor();
            conductor.setId(request.getConductor().getId());
            vehiculo.setConductor(conductor);
        } else {
            vehiculo.setConductor(null);
        }
        return ResponseEntity.ok(vehiculoService.guardar(vehiculo));
    }

    // 3. Dar de baja (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> darDeBaja(@PathVariable Long id) {
        vehiculoService.darDeBajaVehiculo(id);
        return ResponseEntity.ok("Vehículo dado de baja (Estado: BAJA)");
    }

    // 4. Reactivar (Si vuelve del taller)
    @PutMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Long id) {
        vehiculoService.reactivarVehiculo(id);
        return ResponseEntity.ok("Vehículo reactivado y disponible");
    }
}
