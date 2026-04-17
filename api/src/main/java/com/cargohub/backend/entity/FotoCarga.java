package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.TipoFotoCarga;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fotos_carga")
public class FotoCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "porte_id", nullable = false)
    @JsonIgnore
    private Porte porte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoFotoCarga tipo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String fotoBase64;

    private String descripcion;

    private LocalDateTime fechaCaptura = LocalDateTime.now();
}
