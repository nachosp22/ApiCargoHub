package com.cargohub.backend.controller;

import com.cargohub.backend.entity.BloqueoAgenda;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.service.ConductorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/conductores")
@CrossOrigin(origins = "*")
public class ConductorController {

    @Autowired
    private ConductorService conductorService;

    // Reportar GPS: POST /api/conductores/1/ubicacion?lat=40.4&lon=-3.7
    @PostMapping("/{id}/ubicacion")
    public ResponseEntity<?> reportarUbicacion(@PathVariable Long id,
                                               @RequestParam Double lat,
                                               @RequestParam Double lon) {
        conductorService.actualizarUbicacion(id, lat, lon);
        return ResponseEntity.ok("Ubicación recibida");
    }

    // Ver Agenda: GET /api/conductores/1/agenda?desde=2025-01-01&hasta=2025-01-31
    @GetMapping("/{id}/agenda")
    public ResponseEntity<List<BloqueoAgenda>> verAgenda(@PathVariable Long id,
                                                         @RequestParam String desde,
                                                         @RequestParam String hasta) {
        LocalDateTime fechaDesde = LocalDateTime.parse(desde + "T00:00:00");
        LocalDateTime fechaHasta = LocalDateTime.parse(hasta + "T23:59:59");
        return ResponseEntity.ok(conductorService.obtenerAgenda(id, fechaDesde, fechaHasta));
    }

    // Crear Vacaciones: POST /api/conductores/1/agenda
    @PostMapping("/{id}/agenda")
    public ResponseEntity<BloqueoAgenda> crearBloqueo(@PathVariable Long id,
                                                      @RequestBody BloqueoAgenda bloqueo) {
        return ResponseEntity.ok(conductorService.agregarBloqueo(id, bloqueo));
    }

    @DeleteMapping("/agenda/{bloqueoId}")
    public ResponseEntity<?> eliminarBloqueo(@PathVariable Long bloqueoId) {
        conductorService.eliminarBloqueo(bloqueoId);
        return ResponseEntity.ok("Bloqueo de agenda eliminado correctamente");
    }

    // Perfil completo
    @GetMapping("/{id}")
    public ResponseEntity<Conductor> verPerfil(@PathVariable Long id) {
        return ResponseEntity.ok(conductorService.obtenerPorId(id));
    }

    // Actualizar perfil
    @PutMapping("/{id}")
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
    public ResponseEntity<?> darDeBaja(@PathVariable Long id) {
        conductorService.darDeBajaConductor(id);
        return ResponseEntity.ok("Conductor dado de baja (Historial conservado)");
    }
}