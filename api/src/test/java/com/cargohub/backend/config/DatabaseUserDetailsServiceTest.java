package com.cargohub.backend.config;

import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private DatabaseUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new DatabaseUserDetailsService(usuarioRepository);
    }

    @Test
    void loadUserByUsername_mapsRoleAndEnabledState() {
        Usuario usuario = new Usuario();
        usuario.setEmail("admin@test.com");
        usuario.setPassword("$2a$10$hash");
        usuario.setRol(RolUsuario.ADMIN);
        usuario.setActivo(true);

        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(usuario));

        UserDetails details = userDetailsService.loadUserByUsername("ADMIN@TEST.COM");

        assertEquals("admin@test.com", details.getUsername());
        assertEquals("$2a$10$hash", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_throwsWhenUserNotFound() {
        when(usuarioRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown@test.com"));
    }

    @Test
    void loadUserByUsername_disablesInactiveUsers() {
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@test.com");
        usuario.setPassword("$2a$10$hash");
        usuario.setRol(RolUsuario.CLIENTE);
        usuario.setActivo(false);

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(usuario));

        UserDetails details = userDetailsService.loadUserByUsername("cliente@test.com");

        assertFalse(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE")));
    }
}
