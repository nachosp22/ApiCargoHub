package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String matricula;
    private String marca;
    private String modelo;

    // Normalize matricula (license plate) to uppercase
    public void setMatricula(String matricula) {
        this.matricula = matricula != null ? matricula.toUpperCase() : null;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVehiculo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVehiculo estado = EstadoVehiculo.DISPONIBLE;

    // Peso en Kg de la carga máxima que puede transportar
    private Integer capacidadCargaKg;

    // Dimensiones de la zona de carga en Milímetros (mm)
    private Integer largoUtilMm;
    private Integer anchoUtilMm;
    private Integer altoUtilMm;
    private Double volumenM3;

    private boolean trampillaElevadora;

    @ManyToOne
    @JoinColumn(name = "conductor_id", nullable = false)
    private Conductor conductor;

    @PrePersist
    @PreUpdate
    public void calcularVolumenAutomatico() {
        if (largoUtilMm != null && anchoUtilMm != null && altoUtilMm != null) {
            this.volumenM3 = (double) (largoUtilMm * anchoUtilMm * altoUtilMm) / 1_000_000_000.0;
            this.volumenM3 = Math.round(this.volumenM3 * 100.0) / 100.0;
        }
    }
}