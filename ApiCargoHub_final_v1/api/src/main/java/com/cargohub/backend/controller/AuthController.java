package com.cargohub.backend.controller;

import com.cargohub.backend.config.JwtService;
import com.cargohub.backend.dto.LoginResponse;
import com.cargohub.backend.dto.RegisterRequest;
import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.service.ClienteService;
import com.cargohub.backend.service.ConductorService;
import com.cargohub.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private ConductorService conductorService;
    @Autowired private ClienteService clienteService;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegisterRequest request) {
        try {
            String rolStr = request.getRol();
            RolUsuario rol = RolUsuario.CLIENTE;

            if (rolStr != null && rolStr.equalsIgnoreCase("CONDUCTOR")) {
                rol = RolUsuario.CONDUCTOR;
            }

            if (rol == RolUsuario.CONDUCTOR) {
                return registrarConductor(request);
            } else {
                return registrarCliente(request);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<?> registrarCliente(RegisterRequest request) {
        if (request.getNombreEmpresa() == null || request.getNombreEmpresa().isBlank()) {
            return ResponseEntity.badRequest().body("El nombre de la empresa es obligatorio");
        }
        if (request.getCif() == null || request.getCif().isBlank()) {
            return ResponseEntity.badRequest().body("El CIF/NIF es obligatorio");
        }

        Usuario nuevoUsuario = usuarioService.registrarUsuario(
                request.getEmail(), request.getPassword(), RolUsuario.CLIENTE);

        Cliente cli = new Cliente();
        cli.setUsuario(nuevoUsuario);
        cli.setNombreEmpresa(request.getNombreEmpresa());
        cli.setCif(request.getCif());
        cli.setEmailContacto(
                request.getEmailContacto() != null ? request.getEmailContacto() : request.getEmail());
        cli.setDireccionFiscal(request.getDireccionFiscal());
        cli.setTelefono(request.getTelefono());
        cli.setSector(request.getSector());
        clienteService.guardarCliente(cli);

        return ResponseEntity.ok(Map.of(
                "message", "Registro completado correctamente",
                "pendingApproval", false
        ));
    }

    private ResponseEntity<?> registrarConductor(RegisterRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body("El nombre es obligatorio");
        }
        if (request.getDni() == null || request.getDni().isBlank()) {
            return ResponseEntity.badRequest().body("El DNI/NIE es obligatorio");
        }

        Usuario nuevoUsuario = usuarioService.registrarUsuario(
                request.getEmail(), request.getPassword(), RolUsuario.CONDUCTOR);
        nuevoUsuario.setActivo(false);
        nuevoUsuario = usuarioService.guardar(nuevoUsuario);

        Conductor conductor = new Conductor();
        conductor.setUsuario(nuevoUsuario);
        conductor.setNombre(request.getNombre());
        conductor.setApellidos(request.getApellidos());
        conductor.setDni(request.getDni());
        conductor.setTelefono(request.getTelefono());
        conductor.setCiudadBase(request.getCiudadBase());
        conductor.setDisponible(false);
        conductorService.guardarOActualizar(conductor);

        return ResponseEntity.ok(Map.of(
                "message", "Registro completado. Tu cuenta será revisada por nuestro equipo.",
                "pendingApproval", true
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
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
            response.setNombre(usuario.getNombre());

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
