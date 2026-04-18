package com.cargohub.backend.controller;

import com.cargohub.backend.dto.FotoPerfilRequest;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.repository.UsuarioRepository;
import com.cargohub.backend.service.FotoPerfilService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios/me/foto")
@CrossOrigin(origins = "*")
public class FotoPerfilController {

    @Autowired
    private FotoPerfilService fotoPerfilService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<?> subirFoto(Authentication authentication,
                                       @Valid @RequestBody FotoPerfilRequest request) {
        Long usuarioId = resolveUsuarioId(authentication);
        String url = fotoPerfilService.subirFoto(usuarioId, request.getImagen());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminarFoto(Authentication authentication) {
        Long usuarioId = resolveUsuarioId(authentication);
        fotoPerfilService.eliminarFoto(usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<?> obtenerFoto(Authentication authentication) {
        Long usuarioId = resolveUsuarioId(authentication);
        String url = fotoPerfilService.obtenerFotoUrl(usuarioId);
        if (url == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("url", url));
    }

    private Long resolveUsuarioId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("No se pudo resolver el usuario autenticado");
        }
        String email = authentication.getName().toLowerCase();
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
