package com.cargohub.backend.service;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    // --- 1. GESTIÓN DE PERFIL ---

    /**
     * Guarda o actualiza los datos de empresa del cliente.
     */
    @Transactional
    public Cliente guardarCliente(Cliente cliente) {
        // Aquí podrías añadir validaciones (ej: comprobar formato CIF)
        return clienteRepository.save(cliente);
    }

    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    /**
     * Vital para el Login: Permite saber qué ID de cliente tiene un usuario logueado.
     */
    public Cliente obtenerPorEmailUsuario(String email) {
        // Normalize email to lowercase for search
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        return clienteRepository.findByUsuarioEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No existe perfil de cliente para este usuario"));
    }

    // --- 2. UTILIDADES ---

    public boolean existeClienteParaUsuario(String email) {
        // Normalize email to lowercase for search
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        return clienteRepository.findByUsuarioEmail(normalizedEmail).isPresent();
    }
}
