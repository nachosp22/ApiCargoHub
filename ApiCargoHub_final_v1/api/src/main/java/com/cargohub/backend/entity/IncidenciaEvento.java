package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.EstadoIncidencia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incidencia_eventos")
public class IncidenciaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "incidencia_id", nullable = false)
    private Incidencia incidencia;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private Usuario actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoIncidencia estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false)
    private EstadoIncidencia estadoNuevo;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(columnDefinition = "TEXT")
    private String comentario;
}
