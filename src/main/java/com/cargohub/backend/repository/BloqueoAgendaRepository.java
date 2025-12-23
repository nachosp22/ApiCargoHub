package com.cargohub.backend.repository;

import com.cargohub.backend.entity.BloqueoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloqueoAgendaRepository extends JpaRepository<BloqueoAgenda, Long> {

    // Para pintar el calendario visual en la App
    List<BloqueoAgenda> findByConductorIdAndFechaInicioBetween(
            Long conductorId, LocalDateTime inicio, LocalDateTime fin);

    // ESCUDO DE PROTECCIÓN:
    // ¿Hay algún evento (vacaciones, médico) que choque con estas fechas?
    @Query("SELECT COUNT(b) > 0 FROM BloqueoAgenda b " +
            "WHERE b.conductor.id = :conductorId " +
            "AND b.fechaInicio < :finPorte " +
            "AND b.fechaFin > :inicioPorte")
    boolean estaBloqueado(@Param("conductorId") Long conductorId,
                          @Param("inicioPorte") LocalDateTime inicioPorte,
                          @Param("finPorte") LocalDateTime finPorte);
}