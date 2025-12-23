package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Incidencia;
import com.cargohub.backend.entity.enums.EstadoIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    // Ver problemas de un viaje específico
    List<Incidencia> findByPorteId(Long porteId);

    // Panel de Admin: Ver todas las incidencias "ABIERTA" o "EN_REVISION"
    List<Incidencia> findByEstado(EstadoIncidencia estado);
}
