package com.cargohub.backend.repository;

import com.cargohub.backend.entity.IncidenciaEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenciaEventoRepository extends JpaRepository<IncidenciaEvento, Long> {
    List<IncidenciaEvento> findByIncidenciaIdOrderByFechaAsc(Long incidenciaId);
}
