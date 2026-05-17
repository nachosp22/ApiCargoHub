package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PorteRepository extends JpaRepository<Porte, Long> {

    // 1. Marketplace
    List<Porte> findByEstadoOrderByFechaRecogidaAsc(EstadoPorte estado);

    @Query("""
            SELECT p FROM Porte p
            WHERE p.estado = :estado
              AND p.revisionManual = false
              AND p.conductor IS NULL
              AND :conductorId NOT MEMBER OF p.conductoresRechazados
            ORDER BY p.fechaRecogida ASC
            """)
    List<Porte> findDriverOffers(@Param("estado") EstadoPorte estado,
                                 @Param("conductorId") Long conductorId);

    List<Porte> findByRevisionManualTrue();

    // 2. Mis Viajes (Búsqueda directa por Conductor)
    List<Porte> findByConductorId(Long conductorId);

    Optional<Porte> findFirstByConductorIdAndEstadoInOrderByFechaCreacionDesc(Long conductorId, List<EstadoPorte> estados);

    List<Porte> findByClienteId(Long clienteId);

    boolean existsByIdAndClienteId(Long id, Long clienteId);

    boolean existsByIdAndConductorId(Long id, Long conductorId);

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

    long countByFechaRecogidaBetween(LocalDateTime start, LocalDateTime end);

    long countByFechaRecogidaBetweenAndEstadoIn(LocalDateTime start, LocalDateTime end, List<EstadoPorte> estados);

    @Query("SELECT COUNT(p) FROM Porte p WHERE YEAR(p.fechaRecogida) = :anio AND MONTH(p.fechaRecogida) = :mes")
    long countByYearAndMonth(@Param("anio") int anio, @Param("mes") int mes);

    @Query("SELECT COUNT(p) FROM Porte p WHERE YEAR(p.fechaRecogida) = :anio AND MONTH(p.fechaRecogida) = :mes AND p.estado IN :estados")
    long countByYearAndMonthAndEstadoIn(@Param("anio") int anio, @Param("mes") int mes, @Param("estados") List<EstadoPorte> estados);

    // Estadísticas para conductor
    @Query("SELECT p FROM Porte p WHERE p.conductor.id = :conductorId" +
           " AND (:desde IS NULL OR p.fechaCreacion >= :desde)" +
           " AND (:hasta IS NULL OR p.fechaCreacion <= :hasta)")
    List<Porte> findByConductorIdAndFechas(@Param("conductorId") Long conductorId,
                                            @Param("desde") LocalDateTime desde,
                                            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT p FROM Porte p
            WHERE p.revisionManual = true
               OR (
                     p.estado = com.cargohub.backend.entity.enums.EstadoPorte.PENDIENTE
                 AND p.conductor IS NULL
                 AND p.revisionManual = false
                 AND p.motivoRevision IS NOT NULL
                 AND p.motivoRevision <> :mensajeEsperando
                 AND p.motivoRevision NOT LIKE CONCAT(:rematchingPrefix, '%')
                )
            ORDER BY p.fechaCreacion DESC
            """)
    List<Porte> findPendientesAdminReview(@Param("mensajeEsperando") String mensajeEsperando,
                                          @Param("rematchingPrefix") String rematchingPrefix);
}
