package com.cargohub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conductores")
public class Conductor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private Usuario usuario;

    @Column(nullable = false)
    private String nombre;
    private String apellidos;

    @Column(unique = true, nullable = false)
    private String dni;
    private String telefono;

    // --- 1. UBICACIÓN "CASA" (ESTÁTICA) ---
    // Se rellena al registrarse. Sirve para calcular retornos a casa.
    private String ciudadBase;       // Para mostrar: "Madrid"
    private Double latitudBase;      // Para el algoritmo: 40.416
    private Double longitudBase;     // Para el algoritmo: -3.703

    private Integer radioAccionKm = 0; // Cuánto se aleja de casa

    // --- 2. UBICACIÓN "GPS" (DINÁMICA) ---
    // Se actualiza cada 5 min con la App. Sirve para asignaciones en ruta.
    private Double latitudActual;
    private Double longitudActual;
    private LocalDateTime ultimaActualizacionUbicacion;

    // --- PREFERENCIAS Y ESTADO ---
    private boolean buscarRetorno = true;
    private String diasLaborables = "1,2,3,4,5";
    private boolean disponible = true;

    // --- RATINGS ---
    private Double rating = 4.0;
    private Integer numeroValoraciones = 10;
    private Double sumaPuntuaciones = 40.0;

    public void recibirValoracion(int estrellas) {
        this.sumaPuntuaciones += estrellas;
        this.numeroValoraciones++;
        this.rating = Math.round((this.sumaPuntuaciones / this.numeroValoraciones) * 100.0) / 100.0;
    }
}