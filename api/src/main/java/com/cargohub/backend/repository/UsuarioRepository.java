package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Método mágico: Spring crea el SQL automáticamente al leer "findByEmail"
    Optional<Usuario> findByEmail(String email);

    // Para evitar registros duplicados
    boolean existsByEmail(String email);
}