package com.cargohub. backend.controller;

import com. cargohub.backend.dto.CrearPorteRequest;
import com.cargohub.backend.dto.SolicitudPorteRequest;
import com.cargohub.backend.dto.ActualizarDimensionesRequest;
import com.cargohub.backend.dto.ConductorCandidatoResponse;
import com.cargohub. backend.entity.Factura;
import com.cargohub.backend.entity. Porte;
import com.cargohub.backend.entity. enums.EstadoPorte;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.PorteService;
import com.cargohub.backend.service.PorteTrackingService;
import com.cargohub.backend.dto.tracking.PorteTrackingResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework. web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portes")
@CrossOrigin(origins = "*")
public class PorteController {

    @Autowired
    private PorteService porteService;

    @Autowired
    private PorteTrackingService porteTrackingService;

    @Autowired
    private OwnershipSecurityService ownershipSecurityService;

    // Listado general para panel administrativo
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Porte>> listarTodos() {
        return ResponseEntity.ok(porteService.listarTodos());
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> getResumen(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(porteService.getResumen(anio, mes));
    }

    // 1. Crear Porte (Admin/IA) - MODIFICADO PARA USAR DTO
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> crearPorte(@RequestBody CrearPorteRequest request) {
        return ResponseEntity.ok(porteService.crearPorteDesdeRequest(request));
    }

    // 2. Ver Ofertas (Conductor)
    @GetMapping("/ofertas/{conductorId}")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<List<Porte>> verOfertas(@PathVariable Long conductorId) {
        return ResponseEntity.ok(porteService. listarOfertasParaConductor(conductorId));
    }

    // 3. Aceptar Porte (Conductor)
    @PostMapping("/{porteId}/aceptar")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<? > aceptarPorte(@PathVariable Long porteId,
                                           @RequestParam Long conductorId) {
        try {
            return ResponseEntity.ok(porteService.aceptarPorte(porteId, conductorId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{porteId}/rechazar")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<?> rechazarPorte(@PathVariable Long porteId,
                                           @RequestParam Long conductorId) {
        try {
            porteService.rechazarPorte(porteId, conductorId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 4. Cambiar Estado (Entregar, En Tránsito)
    @PutMapping("/{porteId}/estado")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<Porte> cambiarEstado(@PathVariable Long porteId,
                                               @RequestParam EstadoPorte nuevo) {
        return ResponseEntity. ok(porteService.cambiarEstado(porteId, nuevo));
    }

    // 5. Ajuste Manual de Precio (Admin)
    @PostMapping("/{porteId}/ajuste")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> agregarAjuste(@PathVariable Long porteId,
                                               @RequestParam Double cantidad,
                                               @RequestParam String concepto) {
        return ResponseEntity.ok(porteService.agregarAjusteManual(porteId, cantidad, concepto));
    }

    // 6. Facturar (Admin)
    @PostMapping("/{porteId}/facturar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<? > generarFactura(@PathVariable Long porteId) {
        try {
            return ResponseEntity.ok(porteService.facturarManualmente(porteId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 7. Obtener Porte por ID (Admin/Cliente/Conductor)
    @GetMapping("/{porteId}")
    @PreAuthorize("@ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<? > obtenerPorte(@PathVariable Long porteId) {
        return ResponseEntity.ok(porteService.obtenerPorId(porteId));
    }

    // 8. Listar Portes por Conductor (Conductor)
    @GetMapping("/conductor/{conductorId}")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<List<Porte>> listarPortesConductor(@PathVariable Long conductorId) {
        return ResponseEntity.ok(porteService.listarPortesPorConductor(conductorId));
    }

    // 9. Solicitud de porte desde portal web (Cliente)
    @PostMapping("/solicitud")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<?> crearSolicitud(@Valid @RequestBody SolicitudPorteRequest request,
                                            Authentication authentication) {
        try {
            Long clienteId = ownershipSecurityService.resolveClienteIdFromAuth(authentication);
            if (clienteId == null) {
                return ResponseEntity.status(403).body("No se encontró cliente asociado al usuario");
            }
            Porte porte = porteService.crearPorteDesdeSolicitud(request, clienteId);
            return ResponseEntity.ok(porte);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 10. Listar Portes del cliente autenticado
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','SUPERADMIN') and @ownership.canAccessCliente(authentication, #clienteId)")
    public ResponseEntity<List<Porte>> listarPortesCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(porteService.listarPortesPorCliente(clienteId));
    }

    // 11. Listar portes pendientes de revisión manual (ADMIN)
    @GetMapping("/pendientes-revision")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Porte>> listarPendientesRevision() {
        return ResponseEntity.ok(porteService.listarPendientesRevision());
    }

    // 12. Actualizar dimensiones de carga (ADMIN)
    @PutMapping("/{porteId}/dimensiones")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> actualizarDimensiones(@PathVariable Long porteId,
                                                        @RequestBody ActualizarDimensionesRequest request) {
        return ResponseEntity.ok(porteService.actualizarDimensiones(porteId, request));
    }

    // 13. Buscar conductores candidatos para un porte (ADMIN)
    @PostMapping("/{porteId}/buscar-conductores")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<ConductorCandidatoResponse>> buscarConductores(@PathVariable Long porteId) {
        return ResponseEntity.ok(porteService.buscarConductoresParaPorte(porteId));
    }

    // 14. Asignar conductor manualmente (ADMIN)
    @PostMapping("/{porteId}/asignar-conductor")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> asignarConductor(@PathVariable Long porteId,
                                                   @RequestParam Long conductorId) {
        return ResponseEntity.ok(porteService.asignarConductorManualmente(porteId, conductorId));
    }

    // 15. Tracking en tiempo real para clientes
    @GetMapping("/{porteId}/tracking")
    @PreAuthorize("@ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<PorteTrackingResponse> getTracking(@PathVariable Long porteId) {
        return ResponseEntity.ok(porteTrackingService.getTracking(porteId));
    }
}
