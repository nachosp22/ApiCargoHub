package com.cargohub.backend.service;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    // --- 1. GESTIÓN DE PERFIL ---

    /**
     * Guarda o actualiza los datos de empresa del cliente.
     *
     * @param cliente la entidad {@link Cliente} a persistir o actualizar
     * @return el cliente guardado con los datos confirmados
     */
    @Transactional
    public Cliente guardarCliente(Cliente cliente) {
        // Aquí podrías añadir validaciones (ej: comprobar formato CIF)
        return clienteRepository.save(cliente);
    }

    /**
     * Obtiene un cliente por su identificador único.
     *
     * @param id el identificador del cliente
     * @return la entidad {@link Cliente} correspondiente al ID solicitado
     * @throws RuntimeException si no se encuentra un cliente con el ID proporcionado
     */
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    /**
     * Devuelve la lista completa de clientes registrados en el sistema.
     *
     * @return una lista con todas las entidades {@link Cliente}
     */
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    /**
     * Vital para el Login: Permite saber qué ID de cliente tiene un usuario logueado.
     *
     * @param email la dirección de correo electrónico del usuario asociado al cliente
     * @return la entidad {@link Cliente} vinculada al usuario con ese email
     * @throws RuntimeException si no existe un perfil de cliente para el email indicado
     */
    public Cliente obtenerPorEmailUsuario(String email) {
        // Normalize email to lowercase for search
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        return clienteRepository.findByUsuarioEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No existe perfil de cliente para este usuario"));
    }

    // --- 2. UTILIDADES ---

    /**
     * Verifica si existe un perfil de cliente asociado a un usuario con el email indicado.
     *
     * @param email la dirección de correo electrónico a buscar
     * @return {@code true} si existe un cliente asociado al email, {@code false} en caso contrario
     */
    public boolean existeClienteParaUsuario(String email) {
        // Normalize email to lowercase for search
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        return clienteRepository.findByUsuarioEmail(normalizedEmail).isPresent();
    }

    /**
     * Deshabilita un cliente marcando como inactivo al usuario asociado.
     *
     * @param clienteId el identificador del cliente a deshabilitar
     */
    @Transactional
    public void deshabilitarCliente(Long clienteId) {
        Cliente cliente = obtenerPorId(clienteId);
        Usuario usuario = cliente.getUsuario();
        if (usuario != null) {
            usuario.setActivo(false);
        }
        clienteRepository.save(cliente);
    }
}
