package com.cargohub.backend.controller;

import com.cargohub.backend.entity.BloqueoAgenda;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.service.ConductorService;
import com.cargohub.backend.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/conductores")
@CrossOrigin(origins = "*")
public class ConductorController {

    @Autowired
    private ConductorService conductorService;

    @Autowired
    private VehiculoService vehiculoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Conductor>> listarTodos() {
        return ResponseEntity.ok(conductorService.listarTodos());
    }

    // Reportar GPS: POST /api/conductores/1/ubicacion?lat=40.4&lon=-3.7
    @PostMapping("/{id}/ubicacion")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<?> reportarUbicacion(@PathVariable Long id,
                                               @RequestParam Double lat,
                                               @RequestParam Double lon) {
        conductorService.actualizarUbicacion(id, lat, lon);
        return ResponseEntity.ok("Ubicación recibida");
    }

    // Ver Agenda: GET /api/conductores/1/agenda?desde=2025-01-01&hasta=2025-01-31
    @GetMapping("/{id}/agenda")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<List<BloqueoAgenda>> verAgenda(@PathVariable Long id,
                                                         @RequestParam String desde,
                                                         @RequestParam String hasta) {
        LocalDateTime fechaDesde = LocalDateTime.parse(desde + "T00:00:00");
        LocalDateTime fechaHasta = LocalDateTime.parse(hasta + "T23:59:59");
        return ResponseEntity.ok(conductorService.obtenerAgenda(id, fechaDesde, fechaHasta));
    }

    // Crear Vacaciones: POST /api/conductores/1/agenda
    @PostMapping("/{id}/agenda")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<BloqueoAgenda> crearBloqueo(@PathVariable Long id,
                                                      @RequestBody BloqueoAgenda bloqueo) {
        return ResponseEntity.ok(conductorService.agregarBloqueo(id, bloqueo));
    }

    @DeleteMapping("/agenda/{bloqueoId}")
    @PreAuthorize("@ownership.canDeleteBloqueo(authentication, #bloqueoId)")
    public ResponseEntity<?> eliminarBloqueo(@PathVariable Long bloqueoId) {
        conductorService.eliminarBloqueo(bloqueoId);
        return ResponseEntity.ok("Bloqueo de agenda eliminado correctamente");
    }

    // Perfil completo
    @GetMapping("/{id}")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<Conductor> verPerfil(@PathVariable Long id) {
        return ResponseEntity.ok(conductorService.obtenerPorId(id));
    }

    // Actualizar perfil
    @PutMapping("/{id}")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<Conductor> actualizarPerfil(@PathVariable Long id, @RequestBody Conductor datosNuevos) {
        Conductor conductor = conductorService.obtenerPorId(id);

        // Actualizamos solo lo editable
        if (datosNuevos.getNombre() != null) conductor.setNombre(datosNuevos.getNombre());
        if (datosNuevos.getApellidos() != null) conductor.setApellidos(datosNuevos.getApellidos());
        if (datosNuevos.getTelefono() != null) conductor.setTelefono(datosNuevos.getTelefono());
        if (datosNuevos.getCiudadBase() != null) conductor.setCiudadBase(datosNuevos.getCiudadBase());
        if (datosNuevos.getLatitudBase() != null) conductor.setLatitudBase(datosNuevos.getLatitudBase());
        if (datosNuevos.getLongitudBase() != null) conductor.setLongitudBase(datosNuevos.getLongitudBase());
        if (datosNuevos.getRadioAccionKm() != null) conductor.setRadioAccionKm(datosNuevos.getRadioAccionKm());
        if (datosNuevos.getDiasLaborables() != null) conductor.setDiasLaborables(datosNuevos.getDiasLaborables());

        return ResponseEntity.ok(conductorService.guardarOActualizar(conductor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<?> darDeBaja(@PathVariable Long id) {
        conductorService.darDeBajaConductor(id);
        return ResponseEntity.ok("Conductor dado de baja (Historial conservado)");
    }

    @PostMapping("/{conductorId}/vehiculos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','CONDUCTOR') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<Vehiculo> crearVehiculo(@PathVariable Long conductorId, @Valid @RequestBody Vehiculo vehiculo) {
        Conductor conductor = conductorService.obtenerPorId(conductorId);
        vehiculo.setConductor(conductor);
        vehiculo.setEstado(com.cargohub.backend.entity.enums.EstadoVehiculo.BAJA);
        return ResponseEntity.ok(vehiculoService.guardar(vehiculo));
    }

    @PutMapping("/{conductorId}/vehiculos/{vehiculoId}/activar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','CONDUCTOR') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<Void> activarVehiculo(@PathVariable Long conductorId, @PathVariable Long vehiculoId) {
        vehiculoService.activarVehiculo(vehiculoId, conductorId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{conductorId}/vehiculos/{vehiculoId}/desactivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','CONDUCTOR') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<Void> desactivarVehiculo(@PathVariable Long conductorId, @PathVariable Long vehiculoId) {
        vehiculoService.desactivarVehiculo(vehiculoId, conductorId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{conductorId}/vehiculos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','CONDUCTOR') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<List<Vehiculo>> getMisVehiculos(@PathVariable Long conductorId) {
        return ResponseEntity.ok(vehiculoService.listarPorConductor(conductorId));
    }
}
