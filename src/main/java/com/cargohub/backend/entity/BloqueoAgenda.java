package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.TipoBloqueoAgenda;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agenda_bloqueos")
public class BloqueoAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false)
    private LocalDateTime fechaFin;

    // CAMBIO A ENUM (Mucho más seguro)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoBloqueoAgenda tipo;

    private String titulo;

    @ManyToOne
    @JoinColumn(name = "conductor_id", nullable = false)
    private Conductor conductor;
}