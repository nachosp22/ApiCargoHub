package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Buscar el perfil de cliente a través del email de su usuario asociado
    Optional<Cliente> findByUsuarioEmail(String email);
}