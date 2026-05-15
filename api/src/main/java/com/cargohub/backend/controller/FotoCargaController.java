package com.cargohub.backend.controller;

import com.cargohub.backend.dto.CrearFotoCargaRequest;
import com.cargohub.backend.dto.FotoCargaResponse;
import com.cargohub.backend.entity.FotoCarga;
import com.cargohub.backend.service.FotoCargaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portes/{porteId}/fotos")
public class FotoCargaController {

    @Autowired
    private FotoCargaService fotoCargaService;

    // Subir foto — CONDUCTOR (owner del porte)
    @PostMapping
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<FotoCargaResponse> subirFoto(@PathVariable Long porteId,
                                                        @Valid @RequestBody CrearFotoCargaRequest request) {
        FotoCarga foto = new FotoCarga();
        foto.setTipo(request.getTipo());
        foto.setDescripcion(request.getDescripcion());

        FotoCarga guardada = fotoCargaService.subirFoto(porteId, foto, request.getFotoBase64());
        return ResponseEntity.ok(FotoCargaResponse.fromEntity(guardada));
    }

    // Listar fotos — ADMIN, CONDUCTOR (owner), CLIENTE (owner)
    @GetMapping
    @PreAuthorize("@ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<List<FotoCargaResponse>> listarFotos(@PathVariable Long porteId) {
        List<FotoCargaResponse> fotos = fotoCargaService.listarFotosPorPorte(porteId)
                .stream()
                .map(FotoCargaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(fotos);
    }

    // Eliminar foto — CONDUCTOR owner del porte
    @DeleteMapping("/{fotoId}")
    @PreAuthorize("hasAnyRole('CONDUCTOR','ADMIN','SUPERADMIN') and @ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Long porteId,
                                              @PathVariable Long fotoId) {
        fotoCargaService.eliminarFoto(porteId, fotoId);
        return ResponseEntity.noContent().build();
    }
}
