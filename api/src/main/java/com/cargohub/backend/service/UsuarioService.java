package com.cargohub.backend.service;

import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario registrarUsuario(String email, String password, RolUsuario rol) {
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        
        if (usuarioRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("El email " + normalizedEmail + " ya está registrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(normalizedEmail);

        usuario.setPassword(passwordEncoder.encode(password));

        usuario.setRol(rol);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        return usuarioRepository.findByEmail(normalizedEmail);
    }

    @Transactional
    public Usuario guardar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizarPerfil(Long id, String nombre, String currentPassword, String newPassword) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (nombre != null && !nombre.isBlank()) {
            usuario.setNombre(nombre);
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, usuario.getPassword())) {
                throw new RuntimeException("La contraseña actual no es correcta");
            }
            usuario.setPassword(passwordEncoder.encode(newPassword));
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario crearAdmin(String email, String password, String nombre) {
        Usuario admin = registrarUsuario(email, password, RolUsuario.ADMIN);
        admin.setNombre(nombre);
        return guardar(admin);
    }

    public List<Usuario> listarAdmins() {
        return usuarioRepository.findByRol(RolUsuario.ADMIN);
    }

    @Transactional
    public Usuario toggleActivo(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setActivo(!usuario.isActivo());
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (usuario.getRol() == RolUsuario.SUPERADMIN) {
            throw new RuntimeException("No se puede eliminar a un SUPERADMIN");
        }
        usuarioRepository.delete(usuario);
    }
}
