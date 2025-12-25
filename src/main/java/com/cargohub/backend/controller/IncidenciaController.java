package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Incidencia;
import com.cargohub.backend.entity.enums.EstadoIncidencia;
import com.cargohub.backend.service.IncidenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
@CrossOrigin(origins = "*")
public class IncidenciaController {

    @Autowired private IncidenciaService incidenciaService;

    // 1. Reportar: POST /api/incidencias?porteId=1
    @PostMapping
    public ResponseEntity<Incidencia> crear(@RequestParam Long porteId,
                                            @RequestBody Incidencia datos) {
        return ResponseEntity.ok(
                incidenciaService.reportarIncidencia(porteId, datos.getTitulo(), datos.getDescripcion())
        );
    }

    // 2. Resolver: PUT /api/incidencias/5/resolver?adminId=1
    @PutMapping("/{id}/resolver")
    public ResponseEntity<Incidencia> resolver(@PathVariable Long id,
                                               @RequestParam Long adminId,
                                               @RequestParam EstadoIncidencia estado,
                                               @RequestBody String resolucion) {
        return ResponseEntity.ok(incidenciaService.resolverIncidencia(id, adminId, resolucion, estado));
    }

    // 3. Listar Pendientes
    @GetMapping("/pendientes")
    public ResponseEntity<List<Incidencia>> listarPendientes() {
        return ResponseEntity.ok(incidenciaService.listarPendientes());
    }
}