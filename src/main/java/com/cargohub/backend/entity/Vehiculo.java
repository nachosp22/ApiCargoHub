package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.EstadoVehiculo; // <-- Importamos el nuevo Enum
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

    private Integer capacidadCargaKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVehiculo tipo; // FURGONETA, TRAILER...

    // CAMBIO IMPORTANTE: Usamos el Enum en lugar de boolean
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVehiculo estado = EstadoVehiculo.DISPONIBLE;

    @ManyToOne
    @JoinColumn(name = "conductor_id", nullable = false)
    private Conductor conductor;
}
