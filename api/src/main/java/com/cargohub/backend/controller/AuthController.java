package com.cargohub.backend.controller;

import com.cargohub.backend.config.JwtService;
import com.cargohub.backend.dto.LoginResponse;
import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.service.ClienteService;
import com.cargohub.backend.service.ConductorService;
import com.cargohub.backend.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private ConductorService conductorService;
    @Autowired private ClienteService clienteService;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtService jwtService;

    // --- EL REGISTRO NO CAMBIA (El servicio ya encripta) ---
    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestParam String email,
                                       @RequestParam String password,
                                       @RequestParam RolUsuario rol) {
        try {
            Usuario nuevoUsuario = usuarioService.registrarUsuario(email, password, rol);

            // ... (Lógica de perfiles Conductor/Cliente se mantiene igual) ...
            if (rol == RolUsuario.CONDUCTOR) {
                Conductor c = new Conductor();
                c.setUsuario(nuevoUsuario);
                c.setNombre("Conductor " + email.split("@")[0]);
                c.setDni("TEMP-" + UUID.randomUUID().toString().substring(0, 8));
                c.setTelefono("000000000");
                c.setCiudadBase("Madrid");
                c.setLatitudBase(40.416);
                c.setLongitudBase(-3.703);
                conductorService.guardarOActualizar(c);
            }
            else if (rol == RolUsuario.CLIENTE) {
                Cliente cli = new Cliente();
                cli.setUsuario(nuevoUsuario);
                cli.setNombreEmpresa("Empresa " + email.split("@")[0]);
                cli.setCif("TEMP-" + UUID.randomUUID().toString().substring(0, 8));
                cli.setEmailContacto(email);
                cli.setDireccionFiscal("Dirección pendiente");
                clienteService.guardarCliente(cli);
            }

            return ResponseEntity.ok(nuevoUsuario);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- LOGIN JWT ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        // Normalize email to lowercase for login
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        Optional<Usuario> userOpt = usuarioService.buscarPorEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        }

        Usuario usuario = userOpt.get();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, password)
            );

            String accessToken = jwtService.generateAccessToken(usuario, authentication);

            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setExpiresIn(jwtService.getExpirationMs() / 1000);
            response.setExpiresAt(Instant.now().plusMillis(jwtService.getExpirationMs()));
            response.setId(usuario.getId());
            response.setEmail(usuario.getEmail());
            response.setRol(usuario.getRol());

            if (usuario.getRol() == RolUsuario.CONDUCTOR) {
                try {
                    Conductor c = conductorService.obtenerPorEmailUsuario(usuario.getEmail());
                    response.setConductorId(c.getId());
                    response.setNombre(c.getNombre());
                } catch (Exception ignored) {
                }
            }
            if (usuario.getRol() == RolUsuario.CLIENTE) {
                try {
                    Cliente cli = clienteService.obtenerPorEmailUsuario(usuario.getEmail());
                    response.setClienteId(cli.getId());
                    response.setNombreEmpresa(cli.getNombreEmpresa());
                } catch (Exception ignored) {
                }
            }

            return ResponseEntity.ok(response);
        } catch (DisabledException e) {
            return ResponseEntity.status(401).body("Usuario inactivo");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("No autorizado");
        }
    }
}
