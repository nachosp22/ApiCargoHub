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

    // --- 1. REGISTRO (Ahora crea perfiles automáticos) ---
    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestParam String email,
                                       @RequestParam String password,
                                       @RequestParam RolUsuario rol) {
        try {
            // 1. Crear el Usuario base (Login)
            Usuario nuevoUsuario = usuarioService.registrarUsuario(email, password, rol);

            // 2. Crear el Perfil asociado según el rol (Para evitar errores de SQL)
            if (rol == RolUsuario.CONDUCTOR) {
                Conductor c = new Conductor();
                c.setUsuario(nuevoUsuario);
                c.setNombre("Conductor " + email.split("@")[0]); // Nombre temporal
                c.setDni("TEMP-" + UUID.randomUUID().toString().substring(0, 8)); // DNI temporal único
                c.setTelefono("000000000");
                c.setCiudadBase("Madrid"); // Base por defecto
                c.setLatitudBase(40.416);
                c.setLongitudBase(-3.703);
                conductorService.guardarOActualizar(c);
            }
            else if (rol == RolUsuario.CLIENTE) {
                Cliente cli = new Cliente();
                cli.setUsuario(nuevoUsuario);
                cli.setNombreEmpresa("Empresa " + email.split("@")[0]);
                cli.setCif("TEMP-" + UUID.randomUUID().toString().substring(0, 8)); // CIF temporal
                cli.setEmailContacto(email);
                cli.setDireccionFiscal("Dirección pendiente");
                clienteService.guardarCliente(cli);
            }

            return ResponseEntity.ok(nuevoUsuario);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- 2. LOGIN (Igual que antes) ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        Optional<Usuario> userOpt = usuarioService.buscarPorEmail(email);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            Usuario usuario = userOpt.get();
            if (!usuario.isActivo()) return ResponseEntity.badRequest().body("Usuario inactivo");

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("id", usuario.getId());
            respuesta.put("email", usuario.getEmail());
            respuesta.put("rol", usuario.getRol());

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