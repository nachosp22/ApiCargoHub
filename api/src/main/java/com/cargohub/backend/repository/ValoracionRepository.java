package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Valoracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    List<Valoracion> findByPorteId(Long porteId);

    List<Valoracion> findByConductorIdOrderByFechaCreacionDesc(Long conductorId);

    boolean existsByPorteIdAndClienteId(Long porteId, Long clienteId);

    Optional<Valoracion> findByPorteIdAndClienteId(Long porteId, Long clienteId);
}
