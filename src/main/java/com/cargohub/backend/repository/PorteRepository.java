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

    // --- 1. MARKETPLACE (Escaparate para conductores) ---
    // Muestra viajes en estado "PENDIENTE" (o BUSCANDO_CONDUCTOR) ordenados por urgencia.
    // Los más próximos a recoger salen primero.
    List<Porte> findByEstadoOrderByFechaRecogidaAsc(EstadoPorte estado);

    // --- 2. PANELES DE USUARIO ---

    // Para la App del Conductor: "Mis Viajes" (Historial y Activos)
    List<Porte> findByConductorId(Long conductorId);

    // Para la Web del Cliente: "Mis Envíos"
    List<Porte> findByClienteId(Long clienteId);

    // --- 3. VALIDACIÓN DE SOLAPAMIENTO (Evitar Doblete) ---
    // Devuelve TRUE si el conductor ya tiene un viaje ACTIVO (Asignado o En Ruta)
    // que coincida con las fechas del nuevo porte que quiere coger.
    @Query("SELECT COUNT(p) > 0 FROM Porte p " +
            "WHERE p.conductor.id = :conductorId " +
            "AND p.estado IN ('ASIGNADO', 'EN_TRANSITO') " +
            "AND p.fechaRecogida < :nuevoFin " +
            "AND p.fechaEntrega > :nuevoInicio")
    boolean tieneViajeEnFecha(@Param("conductorId") Long conductorId,
                              @Param("nuevoInicio") LocalDateTime nuevoInicio,
                              @Param("nuevoFin") LocalDateTime nuevoFin);
}