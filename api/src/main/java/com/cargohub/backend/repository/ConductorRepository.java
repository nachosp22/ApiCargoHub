package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConductorRepository extends JpaRepository<Conductor, Long> {

    // Para el Login desde la App Móvil
    Optional<Conductor> findByUsuarioEmail(String email);

    // Evitar DNI duplicados
    boolean existsByDni(String dni);

    // --- EL CEREBRO DEL MATCHING ---
    // Busca conductores que estén DISPONIBLES y trabajen ese día de la semana.
    // Usamos SQL para buscar dentro del texto "1,2,3,4,5" (diasLaborables).
    @Query("SELECT c FROM Conductor c " +
            "WHERE c.disponible = true " +
            "AND c.diasLaborables LIKE %:diaSemanaString% ")
    List<Conductor> findCandidatosDisponibles(@Param("diaSemanaString") String diaSemanaString);
}
