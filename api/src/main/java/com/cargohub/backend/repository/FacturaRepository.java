package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    // Buscar la última factura para saber por qué número vamos
    Optional<Factura> findTopByOrderByIdDesc();

    // Buscar factura por su porte asociado
    Optional<Factura> findByPorteId(Long porteId);
}