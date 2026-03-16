package com.cargohub.backend.controller;

import com.cargohub.backend.dto.CrearIncidenciaRequest;
import com.cargohub.backend.dto.IncidenciaEventoResponse;
import com.cargohub.backend.dto.IncidenciaMapper;
import com.cargohub.backend.dto.IncidenciaResponse;
import com.cargohub.backend.dto.ResolverIncidenciaRequest;
import com.cargohub.backend.service.IncidenciaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
@CrossOrigin(origins = "*")
public class IncidenciaController {

    @Autowired private IncidenciaService incidenciaService;

    // 1. Reportar: POST /api/incidencias?porteId=1
    @PostMapping
    public ResponseEntity<IncidenciaResponse> crear(@RequestParam Long porteId,
                                                    Authentication authentication,
                                                    @Valid @RequestBody CrearIncidenciaRequest request) {
        return ResponseEntity.ok(IncidenciaMapper.toResponse(
                incidenciaService.reportarIncidencia(
                        porteId,
                        request.getTitulo(),
                        request.getDescripcion(),
                        request.getSeveridad(),
                        request.getPrioridad(),
                        authentication
                )
        ));
    }

    // 2. Resolver: PUT /api/incidencias/5/resolver
    @PutMapping("/{id}/resolver")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<IncidenciaResponse> resolver(@PathVariable Long id,
                                                       Authentication authentication,
                                                       @Valid @RequestBody ResolverIncidenciaRequest request) {
        return ResponseEntity.ok(IncidenciaMapper.toResponse(
                incidenciaService.resolverIncidencia(id, authentication, request.getResolucion(), request.getEstadoFinal())
        ));
    }

    // 3. Listar Pendientes
    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<IncidenciaResponse>> listarPendientes() {
        return ResponseEntity.ok(incidenciaService.listarPendientes().stream().map(IncidenciaMapper::toResponse).toList());
    }

    // 4. Listar Todas
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<IncidenciaResponse>> listarTodas() {
        return ResponseEntity.ok(incidenciaService.listarTodas().stream().map(IncidenciaMapper::toResponse).toList());
    }

    // 5. Obtener por ID
    @GetMapping("/{id}")
    @PreAuthorize("@ownership.canAccessIncidencia(authentication, #id)")
    public ResponseEntity<IncidenciaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(IncidenciaMapper.toResponse(incidenciaService.obtenerPorId(id)));
    }

    // 6. Listar por Porte
    @GetMapping("/porte/{porteId}")
    @PreAuthorize("@ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<List<IncidenciaResponse>> listarPorPorte(@PathVariable Long porteId) {
        return ResponseEntity.ok(incidenciaService.listarPorPorte(porteId).stream().map(IncidenciaMapper::toResponse).toList());
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("@ownership.canAccessIncidencia(authentication, #id)")
    public ResponseEntity<List<IncidenciaEventoResponse>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(incidenciaService.listarHistorial(id).stream().map(IncidenciaMapper::toResponse).toList());
    }

    @GetMapping("/vencidas-sla")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<IncidenciaResponse>> listarVencidasSla() {
        return ResponseEntity.ok(incidenciaService.listarVencidasSla().stream().map(IncidenciaMapper::toResponse).toList());
    }
}
