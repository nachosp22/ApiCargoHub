package com.cargohub. backend.controller;

import com. cargohub.backend.dto.CrearPorteRequest;
import com.cargohub. backend.entity.Factura;
import com.cargohub.backend.entity. Porte;
import com.cargohub.backend.entity. enums.EstadoPorte;
import com.cargohub.backend.service.PorteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework. web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portes")
@CrossOrigin(origins = "*")
public class PorteController {

    @Autowired
    private PorteService porteService;

    // Listado general para panel administrativo
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Porte>> listarTodos() {
        return ResponseEntity.ok(porteService.listarTodos());
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
}
