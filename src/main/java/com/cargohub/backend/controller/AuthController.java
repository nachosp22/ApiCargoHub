package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.service.ClienteService;
import com.cargohub.backend.service.ConductorService;
import com.cargohub.backend.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder; // <--- Importante
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private ConductorService conductorService;
    @Autowired private ClienteService clienteService;

    @Autowired private PasswordEncoder passwordEncoder; // <--- Inyectamos el codificador

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

    // --- LOGIN SEGURO ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        // Normalize email to lowercase for login
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        Optional<Usuario> userOpt = usuarioService.buscarPorEmail(normalizedEmail);

        // CAMBIO: Usamos .matches(raw, hash) en lugar de .equals()
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {

            Usuario usuario = userOpt.get();
            if (!usuario.isActivo()) return ResponseEntity.badRequest().body("Usuario inactivo");

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("id", usuario.getId());
            respuesta.put("email", usuario.getEmail());
            respuesta.put("rol", usuario.getRol());

            // Rellenar datos extra según rol
            if (usuario.getRol() == RolUsuario.CONDUCTOR) {
                try {
                    Conductor c = conductorService.obtenerPorEmailUsuario(usuario.getEmail());
                    respuesta.put("conductorId", c.getId());
                    respuesta.put("nombre", c.getNombre());
                } catch (Exception e) {}
            }
            if (usuario.getRol() == RolUsuario.CLIENTE) {
                try {
                    Cliente cli = clienteService.obtenerPorEmailUsuario(usuario.getEmail());
                    respuesta.put("clienteId", cli.getId());
                    respuesta.put("nombreEmpresa", cli.getNombreEmpresa());
                } catch (Exception e) {}
            }
            return ResponseEntity.ok(respuesta);
        }
        return ResponseEntity.status(401).body("Credenciales incorrectas");
    }
}