package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo; // <--- Importante
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByMatricula(String matricula);

    // Gestión de flota (Ver camiones de un conductor)
    List<Vehiculo> findByConductorId(Long conductorId);

    // --- FILTRO PARA EL MATCHING AUTOMÁTICO ---
    // Busca vehículos DISPONIBLES del TIPO correcto que cumplan TODAS las dimensiones.
    // Las comprobaciones de dimensión son opcionales: si el valor del vehículo es null, se omite esa restricción.
    @Query("SELECT v FROM Vehiculo v " +
            "WHERE v.estado = 'DISPONIBLE' " +
            "AND v.tipo = :tipo " +
            "AND v.capacidadCargaKg >= :peso " +
            "AND (v.largoUtilMm IS NULL OR v.largoUtilMm >= :largoMm) " +
            "AND (v.anchoUtilMm IS NULL OR v.anchoUtilMm >= :anchoMm) " +
            "AND (v.altoUtilMm IS NULL OR v.altoUtilMm >= :altoMm) " +
            "AND (v.volumenM3 IS NULL OR v.volumenM3 <= 0 OR v.volumenM3 >= :volumenM3) " +
            "ORDER BY v.capacidadCargaKg ASC")
    List<Vehiculo> findCandidatos(@Param("tipo") TipoVehiculo tipo,
                                  @Param("peso") Double peso,
                                  @Param("largoMm") Integer largoMm,
                                  @Param("anchoMm") Integer anchoMm,
                                  @Param("altoMm") Integer altoMm,
                                  @Param("volumenM3") Double volumenM3);

    // --- FILTRO PARA ASIGNACIÓN MANUAL / ADMIN ---
    // SIN filtrar por estado DISPONIBLE. Acepta tipo solicitado o superior.
    // Si las dimensiones son 0 o negativas, se omite ese filtro.
    @Query("SELECT v FROM Vehiculo v " +
            "WHERE v.tipo IN :tipos " +
            "AND v.capacidadCargaKg >= :peso " +
            "AND (:largoMm <= 0 OR v.largoUtilMm IS NULL OR v.largoUtilMm >= :largoMm) " +
            "AND (:anchoMm <= 0 OR v.anchoUtilMm IS NULL OR v.anchoUtilMm >= :anchoMm) " +
            "AND (:altoMm <= 0 OR v.altoUtilMm IS NULL OR v.altoUtilMm >= :altoMm) " +
            "AND (:volumenM3 <= 0 OR v.volumenM3 IS NULL OR v.volumenM3 <= 0 OR v.volumenM3 >= :volumenM3) " +
            "ORDER BY v.capacidadCargaKg ASC")
    List<Vehiculo> findTodosCandidatos(@Param("tipos") List<TipoVehiculo> tipos,
                                       @Param("peso") Double peso,
                                       @Param("largoMm") Integer largoMm,
                                       @Param("anchoMm") Integer anchoMm,
                                       @Param("altoMm") Integer altoMm,
                                       @Param("volumenM3") Double volumenM3);
}
