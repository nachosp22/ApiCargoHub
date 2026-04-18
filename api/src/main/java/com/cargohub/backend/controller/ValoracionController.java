package com.cargohub.backend.controller;

import com.cargohub.backend.dto.ValoracionRequest;
import com.cargohub.backend.entity.Valoracion;
import com.cargohub.backend.security.OwnershipSecurityService;
import com.cargohub.backend.service.ValoracionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/valoraciones")
@CrossOrigin(origins = "*")
public class ValoracionController {

    private final ValoracionService valoracionService;
    private final OwnershipSecurityService ownershipSecurityService;

    public ValoracionController(ValoracionService valoracionService,
                                OwnershipSecurityService ownershipSecurityService) {
        this.valoracionService = valoracionService;
        this.ownershipSecurityService = ownershipSecurityService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<?> crearValoracion(@Valid @RequestBody ValoracionRequest request,
                                             Authentication authentication) {
        try {
            Long clienteId = ownershipSecurityService.resolveClienteIdFromAuth(authentication);
            if (clienteId == null) {
                return ResponseEntity.status(403).body("No se encontró cliente asociado al usuario");
            }

            Valoracion valoracion = valoracionService.crearValoracion(
                    request.getPorteId(),
                    clienteId,
                    request.getPuntuacion(),
                    request.getComentario()
            );
            return ResponseEntity.ok(valoracion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/conductor/{conductorId}")
    public ResponseEntity<List<Valoracion>> obtenerValoracionesConductor(@PathVariable Long conductorId) {
        return ResponseEntity.ok(valoracionService.obtenerValoracionesConductor(conductorId));
    }

    @GetMapping("/porte/{porteId}")
    @PreAuthorize("@ownership.canAccessPorte(authentication, #porteId)")
    public ResponseEntity<List<Valoracion>> obtenerValoracionesPorte(@PathVariable Long porteId) {
        return ResponseEntity.ok(valoracionService.obtenerValoracionesPorte(porteId));
    }

    @GetMapping("/porte/{porteId}/mi-valoracion")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<?> obtenerMiValoracion(@PathVariable Long porteId,
                                                  Authentication authentication) {
        Long clienteId = ownershipSecurityService.resolveClienteIdFromAuth(authentication);
        if (clienteId == null) {
            return ResponseEntity.status(403).body("No se encontró cliente asociado al usuario");
        }
        Valoracion valoracion = valoracionService.obtenerValoracionClientePorte(porteId, clienteId);
        if (valoracion == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(valoracion);
    }
}
