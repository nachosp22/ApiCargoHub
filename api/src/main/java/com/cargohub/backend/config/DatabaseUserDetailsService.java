package com.cargohub.backend.config;

import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public DatabaseUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email != null ? email.toLowerCase() : null;

        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + normalizedEmail));

        return User
                .withUsername(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())))
                .disabled(!usuario.isActivo())
                .build();
    }
}
