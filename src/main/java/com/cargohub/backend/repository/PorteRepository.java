package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PorteRepository extends JpaRepository<Porte, Long> {

    // 1. Marketplace
    List<Porte> findByEstadoOrderByFechaRecogidaAsc(EstadoPorte estado);

    // 2. Mis Viajes (Búsqueda directa por Conductor)
    List<Porte> findByConductorId(Long conductorId);

    List<Porte> findByClienteId(Long clienteId);

    // 3. Validación de Solapamiento
    // Comprobamos si EL CONDUCTOR ya tiene viaje (independiente del camión)
    @Query("SELECT COUNT(p) > 0 FROM Porte p " +
            "WHERE p.conductor.id = :conductorId " +
            "AND p.estado IN ('ASIGNADO', 'EN_TRANSITO') " +
            "AND p.fechaRecogida < :nuevoFin " +
            "AND p.fechaEntrega > :nuevoInicio")
    boolean tieneViajeEnFecha(@Param("conductorId") Long conductorId,
                              @Param("nuevoInicio") LocalDateTime nuevoInicio,
                              @Param("nuevoFin") LocalDateTime nuevoFin);
}