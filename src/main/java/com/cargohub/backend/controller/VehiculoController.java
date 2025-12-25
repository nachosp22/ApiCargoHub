package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.service.VehiculoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@CrossOrigin(origins = "*")
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
    public ResponseEntity<Vehiculo> guardar(@RequestBody Vehiculo vehiculo) {
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