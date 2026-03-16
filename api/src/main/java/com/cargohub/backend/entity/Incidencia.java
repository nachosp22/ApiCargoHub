package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.EstadoIncidencia; // <-- Importamos el Enum
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- REPORTE ---
    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private LocalDateTime fechaReporte = LocalDateTime.now();

    // CAMBIO A ENUM
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIncidencia estado = EstadoIncidencia.ABIERTA;

    // --- RESOLUCIÓN ---
    @Column(columnDefinition = "TEXT")
    private String resolucion;

    private LocalDateTime fechaResolucion;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Usuario admin;

    // --- RELACIÓN ---
    @ManyToOne
    @JoinColumn(name = "porte_id", nullable = false)
    private Porte porte;
}