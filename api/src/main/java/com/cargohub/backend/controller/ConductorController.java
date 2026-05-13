package com.cargohub.backend.controller;

import com.cargohub.backend.dto.ActualizarConductorRequest;
import com.cargohub.backend.dto.ConductorCreateRequest;
import com.cargohub.backend.entity.BloqueoAgenda;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.service.BloqueoRecurrenteService;
import com.cargohub.backend.service.ConductorMatchingService;
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
public class ConductorController {

    @Autowired
    private ConductorService conductorService;

    @Autowired
    private VehiculoService vehiculoService;

    @Autowired
    private BloqueoRecurrenteService bloqueoRecurrenteService;

    @Autowired
    private ConductorMatchingService conductorMatchingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Conductor>> listarTodos() {
        return ResponseEntity.ok(conductorService.listarTodos());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Conductor> crearConductor(@Valid @RequestBody ConductorCreateRequest request) {
        Conductor conductor = conductorService.crearConductorAdmin(
                request.getNombre(),
                request.getApellidos(),
                request.getEmail(),
                request.getPassword(),
                request.getDni(),
                request.getTelefono(),
                request.getCiudadBase()
        );
        return ResponseEntity.status(201).body(conductor);
    }

    // --- APROBACIÓN DE CONDUCTORES ---

    @GetMapping("/pendientes-aprobacion")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Conductor>> listarPendientesAprobacion() {
        return ResponseEntity.ok(conductorService.listarPendientesAprobacion());
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Conductor> aprobarConductor(@PathVariable Long id) {
        return ResponseEntity.ok(conductorService.aprobarConductor(id));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> rechazarConductor(@PathVariable Long id) {
        conductorService.rechazarConductor(id);
        return ResponseEntity.ok("Conductor rechazado y eliminado");
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
    public ResponseEntity<Conductor> actualizarPerfil(@PathVariable Long id, @RequestBody ActualizarConductorRequest dto) {
        Conductor conductor = conductorService.obtenerPorId(id);

        if (dto.getNombre() != null) conductor.setNombre(dto.getNombre());
        if (dto.getApellidos() != null) conductor.setApellidos(dto.getApellidos());
        if (dto.getTelefono() != null) conductor.setTelefono(dto.getTelefono());
        if (dto.getDni() != null) conductor.setDni(dto.getDni());
        if (dto.getCiudadBase() != null) conductor.setCiudadBase(dto.getCiudadBase());
        if (dto.getRadioAccionKm() != null) conductor.setRadioAccionKm(dto.getRadioAccionKm());
        if (dto.getDisponible() != null) {
            boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getAuthorities()
                    .stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPERADMIN".equals(a.getAuthority()));
            if (!isAdmin) {
                return ResponseEntity.status(403).build();
            }
            conductor.setDisponible(dto.getDisponible());
        }

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

    @PutMapping("/{conductorId}/vehiculos/{vehiculoId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','CONDUCTOR') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<Vehiculo> actualizarVehiculo(@PathVariable Long conductorId,
                                                        @PathVariable Long vehiculoId,
                                                        @Valid @RequestBody Vehiculo request) {
        return ResponseEntity.ok(vehiculoService.actualizarVehiculo(vehiculoId, conductorId, request));
    }

    // --- BLOQUEOS RECURRENTES ---

    @GetMapping("/{id}/bloqueos-recurrentes")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<List<BloqueoRecurrenteService.BloqueoRecurrenteResponse>> getBloqueoRecurrentes(
            @PathVariable Long id) {
        return ResponseEntity.ok(bloqueoRecurrenteService.getByConductor(id));
    }

    @PutMapping("/{id}/bloqueos-recurrentes")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<List<BloqueoRecurrenteService.BloqueoRecurrenteResponse>> setBloqueoRecurrentes(
            @PathVariable Long id, @RequestBody List<Integer> diasBloqueados) {
        return ResponseEntity.ok(bloqueoRecurrenteService.setBulk(id, diasBloqueados));
    }

    @GetMapping("/{id}/dias-laborables")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<List<Integer>> getDiasLaborables(@PathVariable Long id) {
        return ResponseEntity.ok(conductorService.obtenerDiasLaborables(id));
    }

    @PutMapping("/{id}/dias-laborables")
    @PreAuthorize("@ownership.canAccessConductor(authentication, #id)")
    public ResponseEntity<List<Integer>> setDiasLaborables(@PathVariable Long id,
                                                            @RequestBody List<Integer> diasLaborables) {
        return ResponseEntity.ok(conductorService.actualizarDiasLaborables(id, diasLaborables));
    }

    // --- CONDUCTOR MATCHING ---

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Conductor>> buscarDisponibles(
            @RequestParam String fecha,
            @RequestParam(required = false) String tipoVehiculo,
            @RequestParam(required = false) String ciudad) {
        LocalDateTime fechaParsed = LocalDateTime.parse(fecha);
        TipoVehiculo tipo = tipoVehiculo != null ? TipoVehiculo.valueOf(tipoVehiculo) : null;
        return ResponseEntity.ok(conductorMatchingService.buscarDisponibles(fechaParsed, tipo, ciudad));
    }
}
