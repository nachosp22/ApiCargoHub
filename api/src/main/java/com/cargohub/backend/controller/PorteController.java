package com.cargohub.backend.controller;

import com.cargohub.backend.dto.CrearPorteRequest;
import com.cargohub.backend.dto.SolicitudPorteRequest;
import com.cargohub.backend.dto.ActualizarDimensionesRequest;
import com.cargohub.backend.dto.ActualizarPorteRequest;
import com.cargohub.backend.dto.ConductorCandidatoResponse;
import com.cargohub.backend.dto.FirmaEntregaRequest;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.AlbaranEntregaPdfService;
import com.cargohub.backend.service.PorteService;
import com.cargohub.backend.service.PorteTrackingService;
import com.cargohub.backend.dto.tracking.PorteTrackingResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portes")
public class PorteController {

    @Autowired
    private PorteService porteService;

    @Autowired
    private PorteTrackingService porteTrackingService;

    @Autowired
    private OwnershipSecurityService ownershipSecurityService;

    @Autowired
    private AlbaranEntregaPdfService albaranEntregaPdfService;

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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> crearPorte(@Valid @RequestBody CrearPorteRequest request) {
        return ResponseEntity.ok(porteService.crearPorteDesdeRequest(request));
    }

    @GetMapping("/ofertas/{conductorId}")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<List<Porte>> verOfertas(@PathVariable Long conductorId) {
        return ResponseEntity.ok(porteService.listarOfertasParaConductor(conductorId));
    }

    @PostMapping("/{porteId}/aceptar")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<?> aceptarPorte(@PathVariable Long porteId,
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

    @PutMapping("/{porteId}/estado")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<Porte> cambiarEstado(@PathVariable Long porteId,
                                               @RequestParam EstadoPorte nuevo) {
        return ResponseEntity.ok(porteService.cambiarEstado(porteId, nuevo));
    }

    @PostMapping("/{porteId}/ajuste")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> agregarAjuste(@PathVariable Long porteId,
                                               @RequestParam Double cantidad,
                                               @RequestParam String concepto) {
        return ResponseEntity.ok(porteService.agregarAjusteManual(porteId, cantidad, concepto));
    }

    @PostMapping("/{porteId}/facturar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<? > generarFactura(@PathVariable Long porteId) {
        try {
            return ResponseEntity.ok(porteService.facturarManualmente(porteId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{porteId}")
    @PreAuthorize("@ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<? > obtenerPorte(@PathVariable Long porteId) {
        return ResponseEntity.ok(porteService.obtenerPorId(porteId));
    }

    @GetMapping("/conductor/{conductorId}")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<List<Porte>> listarPortesConductor(@PathVariable Long conductorId) {
        return ResponseEntity.ok(porteService.listarPortesPorConductor(conductorId));
    }

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

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','SUPERADMIN') and @ownership.canAccessCliente(authentication, #clienteId)")
    public ResponseEntity<List<Porte>> listarPortesCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(porteService.listarPortesPorCliente(clienteId));
    }

    @GetMapping("/pendientes-revision")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Porte>> listarPendientesRevision() {
        return ResponseEntity.ok(porteService.listarPendientesRevision());
    }

    @PutMapping("/{porteId}/dimensiones")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> actualizarDimensiones(@PathVariable Long porteId,
                                                          @Valid @RequestBody ActualizarDimensionesRequest request) {
        return ResponseEntity.ok(porteService.actualizarDimensiones(porteId, request));
    }

    @PutMapping("/{porteId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> actualizarPorte(@PathVariable Long porteId,
                                                 @RequestBody ActualizarPorteRequest request) {
        return ResponseEntity.ok(porteService.actualizarPorte(porteId, request));
    }

    @DeleteMapping("/{porteId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> eliminarPorte(@PathVariable Long porteId) {
        boolean eliminado = porteService.eliminarOCancelarPorte(porteId);
        if (eliminado) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok("Porte cancelado para conservar historial");
    }

    @PostMapping("/{porteId}/buscar-conductores")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<ConductorCandidatoResponse>> buscarConductores(@PathVariable Long porteId) {
        return ResponseEntity.ok(porteService.buscarConductoresParaPorte(porteId));
    }

    @PostMapping("/{porteId}/asignar-conductor")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> asignarConductor(@PathVariable Long porteId,
                                                   @RequestParam Long conductorId) {
        return ResponseEntity.ok(porteService.asignarConductorManualmente(porteId, conductorId));
    }

    @PostMapping("/{porteId}/retry-matching")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Porte> retryMatching(@PathVariable Long porteId) {
        return ResponseEntity.ok(porteService.retryMatching(porteId));
    }

    @GetMapping("/{porteId}/tracking")
    @PreAuthorize("@ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<PorteTrackingResponse> getTracking(@PathVariable Long porteId) {
        return ResponseEntity.ok(porteTrackingService.getTracking(porteId));
    }

    @PostMapping("/{porteId}/firma")
    @PreAuthorize("@ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<?> registrarFirmaEntrega(@PathVariable Long porteId,
                                                   @Valid @RequestBody FirmaEntregaRequest request) {
        try {
            return ResponseEntity.ok(porteService.registrarFirmaEntrega(porteId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{porteId}/albaran/pdf")
    @PreAuthorize("@ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<?> descargarAlbaranPdf(@PathVariable Long porteId) {
        try {
            Porte porte = porteService.obtenerPorId(porteId);
            byte[] pdf = albaranEntregaPdfService.generatePdf(porte);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("albaran-porte-" + porteId + ".pdf")
                            .build()
            );
            headers.setContentLength(pdf.length);

            return ResponseEntity.ok().headers(headers).body((Object) pdf);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
