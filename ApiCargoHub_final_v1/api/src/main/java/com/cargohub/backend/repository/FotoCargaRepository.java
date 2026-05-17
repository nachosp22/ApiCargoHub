package com.cargohub.backend.repository;

import com.cargohub.backend.entity.FotoCarga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FotoCargaRepository extends JpaRepository<FotoCarga, Long> {

    List<FotoCarga> findByPorteIdOrderByFechaCapturaDesc(Long porteId);

    boolean existsByIdAndPorteId(Long id, Long porteId);

    boolean existsByIdAndPorteConductorId(Long id, Long conductorId);
}
