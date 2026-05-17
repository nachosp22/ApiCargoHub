package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Update the authenticated user's own profile (name and/or password).
     */
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizarPerfil(Authentication authentication,
                                               @RequestBody Map<String, String> body) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String nombre = body.get("nombre");
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        try {
            Usuario actualizado = usuarioService.actualizarPerfil(
                    usuario.getId(), nombre, currentPassword, newPassword);
            return ResponseEntity.ok(Map.of(
                    "message", "Perfil actualizado",
                    "nombre", actualizado.getNombre() != null ? actualizado.getNombre() : "",
                    "email", actualizado.getEmail()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * SUPERADMIN creates a new ADMIN user account.
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> crearAdmin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String nombre = body.get("nombre");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El email es obligatorio"));
        }
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña es obligatoria"));
        }
        if (nombre == null || nombre.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio"));
        }

        try {
            Usuario admin = usuarioService.crearAdmin(email, password, nombre);
            return ResponseEntity.ok(Map.of(
                    "message", "Administrador creado correctamente",
                    "id", admin.getId(),
                    "email", admin.getEmail(),
                    "nombre", admin.getNombre()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * SUPERADMIN lists all ADMIN accounts for management.
     */
    @GetMapping("/admins")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> listarAdmins() {
        List<Map<String, Object>> admins = usuarioService.listarAdmins().stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("email", u.getEmail());
                    m.put("nombre", u.getNombre());
                    m.put("activo", u.isActivo());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(admins);
    }

    /**
     * SUPERADMIN toggles an admin account active/inactive.
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try {
            Usuario usuario = usuarioService.toggleActivo(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Estado actualizado",
                    "id", usuario.getId(),
                    "activo", usuario.isActivo()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * SUPERADMIN deletes an admin account (not himself, not other SUPERADMINS).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id, Authentication authentication) {
        // Prevent self-deletion
        String email = authentication.getName();
        Usuario currentUser = usuarioService.buscarPorEmail(email).orElse(null);
        if (currentUser != null && currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "No podés eliminar tu propia cuenta"));
        }
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok(Map.of("message", "Usuario eliminado"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
