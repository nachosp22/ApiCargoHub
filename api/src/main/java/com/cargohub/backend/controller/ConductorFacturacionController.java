package com.cargohub.backend.controller;

import com.cargohub.backend.dto.EstadisticasConductorResponse;
import com.cargohub.backend.dto.FacturaResumenResponse;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.service.EstadisticasConductorService;
import com.cargohub.backend.service.FacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/conductores/{conductorId}")
@CrossOrigin(origins = "*")
public class ConductorFacturacionController {

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private EstadisticasConductorService estadisticasService;

    // ── Facturas del conductor ──

    @GetMapping("/facturas")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<Page<Factura>> listarFacturas(
            @PathVariable Long conductorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Boolean pagada,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(facturaService.findByConductorId(conductorId, desde, hasta, pagada, pageable));
    }

    @GetMapping("/facturas/resumen")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<FacturaResumenResponse> resumenFacturas(
            @PathVariable Long conductorId,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(facturaService.getResumenByConductor(conductorId, periodo));
    }

    // ── Estadísticas del conductor ──

    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessConductor(authentication, #conductorId)")
    public ResponseEntity<EstadisticasConductorResponse> estadisticas(
            @PathVariable Long conductorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(estadisticasService.getEstadisticas(conductorId, desde, hasta));
    }
}
