package com.cargohub.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "valoraciones",
       uniqueConstraints = @UniqueConstraint(columnNames = {"porte_id", "cliente_id"}))
public class Valoracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "porte_id", nullable = false)
    private Porte porte;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "conductor_id", nullable = false)
    private Conductor conductor;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int puntuacion;

    @Size(max = 500)
    @Column(length = 500)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
