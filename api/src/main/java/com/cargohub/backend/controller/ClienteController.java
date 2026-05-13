package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.dto.ClienteCreateRequest;
import com.cargohub.backend.dto.ClienteUpdateRequest;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/clientes", "/clientes"})
public class ClienteController {

    @Autowired private ClienteService clienteService;
    @Autowired private PorteRepository porteRepository; // Acceso directo para lectura rápida

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<Cliente>> listarTodos() {
        return ResponseEntity.ok(clienteService.listarTodos());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Cliente> crearCliente(@Valid @RequestBody ClienteCreateRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombreEmpresa(request.getNombreEmpresa());
        cliente.setCif(request.getCif());
        cliente.setEmailContacto(request.getEmailContacto());
        cliente.setTelefono(request.getTelefono());
        cliente.setDireccionFiscal(request.getDireccionFiscal());
        return ResponseEntity.status(201).body(clienteService.guardarCliente(cliente));
    }

    // 1. Ver mi perfil
    @GetMapping("/{id}")
    @PreAuthorize("@ownership.canAccessCliente(authentication, #id)")
    public ResponseEntity<Cliente> verPerfil(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    // 2. Actualizar datos (Empresa, teléfono, dirección...)
    @PutMapping("/{id}")
    @PreAuthorize("@ownership.canAccessCliente(authentication, #id)")
    public ResponseEntity<Cliente> actualizarPerfil(@PathVariable Long id, @Valid @RequestBody ClienteUpdateRequest datosNuevos) {
        Cliente cliente = clienteService.obtenerPorId(id);

        if (datosNuevos.getNombreEmpresa() != null) cliente.setNombreEmpresa(datosNuevos.getNombreEmpresa());
        if (datosNuevos.getCif() != null) cliente.setCif(datosNuevos.getCif());
        if (datosNuevos.getDireccionFiscal() != null) cliente.setDireccionFiscal(datosNuevos.getDireccionFiscal());
        if (datosNuevos.getTelefono() != null) cliente.setTelefono(datosNuevos.getTelefono());
        if (datosNuevos.getEmailContacto() != null) cliente.setEmailContacto(datosNuevos.getEmailContacto());

        return ResponseEntity.ok(clienteService.guardarCliente(cliente));
    }

    // 3. Mis Envíos (Historial)
    @GetMapping("/{id}/portes")
    @PreAuthorize("@ownership.canAccessCliente(authentication, #id)")
    public ResponseEntity<List<Porte>> misEnvios(@PathVariable Long id) {
        List<Porte> misPortes = porteRepository.findByClienteId(id);
        return ResponseEntity.ok(misPortes);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> deshabilitarCliente(@PathVariable Long id) {
        clienteService.deshabilitarCliente(id);
        return ResponseEntity.noContent().build();
    }
}
