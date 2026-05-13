package com.cargohub.backend.controller;

import com.cargohub.backend.dto.EstadisticasGlobalesResponse;
import com.cargohub.backend.service.EstadisticasGlobalesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    @Autowired
    private EstadisticasGlobalesService estadisticasGlobalesService;

    @GetMapping("/globales")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<EstadisticasGlobalesResponse> getEstadisticasGlobales() {
        return ResponseEntity.ok(estadisticasGlobalesService.getEstadisticasGlobales());
    }
}
