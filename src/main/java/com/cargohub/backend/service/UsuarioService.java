package com.cargohub.backend.service;

import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Registra un nuevo usuario base.
     */
    @Transactional
    public Usuario registrarUsuario(String email, String password, RolUsuario rol) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("El email " + email + " ya está registrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword(password); // Más adelante encriptaremos esto
        usuario.setRol(rol);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
}