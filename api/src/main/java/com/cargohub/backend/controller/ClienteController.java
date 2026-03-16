package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired private ClienteService clienteService;
    @Autowired private PorteRepository porteRepository; // Acceso directo para lectura rápida

    // 1. Ver mi perfil
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> verPerfil(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    // 2. Actualizar datos (Empresa, teléfono, dirección...)
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarPerfil(@PathVariable Long id, @RequestBody Cliente datosNuevos) {
        Cliente cliente = clienteService.obtenerPorId(id);

        // Actualizamos solo lo editable
        cliente.setNombreEmpresa(datosNuevos.getNombreEmpresa());
        cliente.setDireccionFiscal(datosNuevos.getDireccionFiscal());
        cliente.setTelefono(datosNuevos.getTelefono());
        cliente.setEmailContacto(datosNuevos.getEmailContacto());

        return ResponseEntity.ok(clienteService.guardarCliente(cliente));
    }

    // 3. Mis Envíos (Historial)
    @GetMapping("/{id}/portes")
    public ResponseEntity<List<Porte>> misEnvios(@PathVariable Long id) {
        List<Porte> misPortes = porteRepository.findByClienteId(id);
        return ResponseEntity.ok(misPortes);
    }
}