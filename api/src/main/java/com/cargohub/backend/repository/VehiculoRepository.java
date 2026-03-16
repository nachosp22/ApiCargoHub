package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo; // <--- Importante
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    // Gestión de flota (Ver camiones de un conductor)
    List<Vehiculo> findByConductorId(Long conductorId);

    // --- FILTRO PARA EL MATCHING AUTOMÁTICO ---
    // Busca un vehículo que esté DISPONIBLE, sea del TIPO correcto,
    // aguante el PESO y quepa el LARGO del paquete.
    @Query("SELECT v FROM Vehiculo v " +
            "WHERE v.estado = 'DISPONIBLE' " +
            "AND v.tipo = :tipo " +
            "AND v.capacidadCargaKg >= :peso " +
            "AND v.largoUtilMm >= :largoMm")
    List<Vehiculo> findCandidatos(@Param("tipo") TipoVehiculo tipo,
                                  @Param("peso") Double peso,
                                  @Param("largoMm") Integer largoMm);
}