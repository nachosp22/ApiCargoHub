package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.FacturaPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaController {

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private OwnershipSecurityService ownershipSecurityService;

    @Autowired
    private FacturaPdfService facturaPdfService;

    // 1. Listar todas las facturas (Panel de Admin)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Factura>> listarTodas() {
        return ResponseEntity.ok(facturaRepository.findAll());
    }

    // 2. Ver una factura concreta por ID (Admin)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','CLIENTE','CONDUCTOR')")
    public ResponseEntity<?> verDetalle(@PathVariable Long id, Authentication authentication) {
        return facturaRepository.findById(id)
                .map(factura -> {
                    // Ownership check: CLIENTE can only see their own invoices
                    if (!ownershipSecurityService.canAccessPorte(authentication, factura.getPorte().getId())) {
                        return ResponseEntity.status(403).body("Acceso denegado a esta factura");
                    }
                    return ResponseEntity.ok(factura);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Obtener factura por ID de Porte (útil para el cliente)
    @GetMapping("/porte/{porteId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or @ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<Factura> buscarPorPorte(@PathVariable Long porteId) {
        return facturaRepository.findByPorteId(porteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. Mis facturas (Cliente autenticado)
    @GetMapping("/mis-facturas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<?> misFacturas(Authentication authentication) {
        Long clienteId = ownershipSecurityService.resolveClienteIdFromAuth(authentication);
        if (clienteId == null) {
            return ResponseEntity.status(403).body("No se encontró cliente asociado al usuario");
        }
        return ResponseEntity.ok(facturaRepository.findByClienteId(clienteId));
    }

    // 5. Descargar PDF de una factura
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','CLIENTE')")
    public ResponseEntity<?> descargarPdf(@PathVariable Long id, Authentication authentication) {
        return facturaRepository.findById(id)
                .map(factura -> {
                    if (!ownershipSecurityService.canAccessPorte(authentication, factura.getPorte().getId())) {
                        return ResponseEntity.status(403).body((Object) "Acceso denegado a esta factura");
                    }
                    byte[] pdf = facturaPdfService.generatePdf(factura);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", factura.getNumeroSerie() + ".pdf");
                    headers.setContentLength(pdf.length);
                    return ResponseEntity.ok().headers(headers).body((Object) pdf);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
