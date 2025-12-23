package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    // Ver todos los camiones de un conductor (para la gestión de flota)
    List<Vehiculo> findByConductorId(Long conductorId);

    // --- FILTRO INTELIGENTE (IA) ---
    // Busca vehículos DISPONIBLES que soporten el peso y volumen requeridos.
    @Query("SELECT v FROM Vehiculo v " +
            "WHERE v.estado = 'DISPONIBLE' " +
            "AND v.capacidadCargaKg >= :peso " +
            "AND v.volumenM3 >= :volumen")
    List<Vehiculo> findCandidatosPorCapacidad(@Param("peso") Integer peso,
                                              @Param("volumen") Double volumen);
}