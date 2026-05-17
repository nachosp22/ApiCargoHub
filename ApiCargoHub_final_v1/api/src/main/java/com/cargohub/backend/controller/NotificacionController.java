package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Notificacion;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import com.cargohub.backend.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ConductorRepository conductorRepository;

    /**
     * Get all notifications for the authenticated user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getNotificaciones(Authentication authentication) {
        Long usuarioId = resolveUsuarioId(authentication);
        if (usuarioId == null) {
            return ResponseEntity.status(403).body("No se pudo resolver el usuario.");
        }
        List<Notificacion> notificaciones = notificacionService.getByUsuario(usuarioId);
        return ResponseEntity.ok(notificaciones);
    }

    /**
     * Get unread notification count.
     */
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        Long usuarioId = resolveUsuarioId(authentication);
        if (usuarioId == null) {
            return ResponseEntity.status(403).body("No se pudo resolver el usuario.");
        }
        long count = notificacionService.countUnread(usuarioId);
        return ResponseEntity.ok(Map.of("unread", count));
    }

    /**
     * Mark a specific notification as read.
     */
    @PutMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long usuarioId = resolveUsuarioId(authentication);
        if (usuarioId == null) {
            return ResponseEntity.status(403).body("No se pudo resolver el usuario.");
        }
        try {
            Notificacion notificacion = notificacionService.markAsRead(id);
            // Verify ownership
            if (!notificacion.getUsuarioId().equals(usuarioId)) {
                return ResponseEntity.status(403).body("No tienes acceso a esta notificacion.");
            }
            return ResponseEntity.ok(notificacion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Mark all notifications as read for the authenticated user.
     */
    @PutMapping("/leer-todas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        Long usuarioId = resolveUsuarioId(authentication);
        if (usuarioId == null) {
            return ResponseEntity.status(403).body("No se pudo resolver el usuario.");
        }
        notificacionService.markAllAsRead(usuarioId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUsuarioId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        String email = authentication.getName().toLowerCase();
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getId)
                .orElse(null);
    }
}
