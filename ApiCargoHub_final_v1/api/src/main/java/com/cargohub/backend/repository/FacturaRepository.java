package com.cargohub.backend.repository;

import com.cargohub.backend.entity.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    // Buscar la última factura para saber por qué número vamos
    Optional<Factura> findTopByOrderByIdDesc();

    // Buscar factura por su porte asociado
    Optional<Factura> findByPorteId(Long porteId);

    // Facturas de un conductor con filtros opcionales
    @Query("SELECT f FROM Factura f WHERE f.porte.conductor.id = :conductorId" +
           " AND (:desde IS NULL OR f.fechaEmision >= :desde)" +
           " AND (:hasta IS NULL OR f.fechaEmision <= :hasta)" +
           " AND (:pagada IS NULL OR f.pagada = :pagada)" +
           " ORDER BY f.fechaEmision DESC")
    Page<Factura> findByConductorId(@Param("conductorId") Long conductorId,
                                    @Param("desde") LocalDate desde,
                                    @Param("hasta") LocalDate hasta,
                                    @Param("pagada") Boolean pagada,
                                    Pageable pageable);

    // Lista simple (sin paginación) para cálculos de resumen
    @Query("SELECT f FROM Factura f WHERE f.porte.conductor.id = :conductorId" +
           " AND (:desde IS NULL OR f.fechaEmision >= :desde)" +
           " AND (:hasta IS NULL OR f.fechaEmision <= :hasta)")
    List<Factura> findAllByConductorId(@Param("conductorId") Long conductorId,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

    // Facturas de un cliente (via porte.cliente.id)
    @Query("SELECT f FROM Factura f WHERE f.porte.cliente.id = :clienteId ORDER BY f.fechaEmision DESC")
    List<Factura> findByClienteId(@Param("clienteId") Long clienteId);
}
