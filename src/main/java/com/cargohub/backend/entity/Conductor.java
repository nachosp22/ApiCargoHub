package com.cargohub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false)
    private String apellidos;

    @Column(unique = true, nullable = false)
    private String dni;

    private String telefono;

    private boolean disponible = true;

    // --- SISTEMA DE RATING SUAVIZADO ---

    // Nota visible (0.0 a 5.0)
    // Inicializamos en 4.0 como pediste
    private Double rating = 4.0;

    // Campos internos para el cálculo matemático
    // Inicializamos como si ya hubiera recibido 10 votos de 4 estrellas
    @Column(nullable = false)
    private Integer numeroValoraciones = 10; // Los "votos fantasma"

    @Column(nullable = false)
    private Double sumaPuntuaciones = 40.0; // 10 votos * 4 puntos = 40


    // --- LÓGICA DE NEGOCIO (Helper Method) ---
    // Este método lo puedes llamar desde tu Service cuando llegue una review
    public void recibirValoracion(int estrellas) {
        // 1. Sumamos la nueva puntuación al acumulado
        this.sumaPuntuaciones += estrellas;

        // 2. Incrementamos el contador de votos
        this.numeroValoraciones++;

        // 3. Recalculamos la media
        this.rating = this.sumaPuntuaciones / this.numeroValoraciones;

        // Opcional: Redondear a 2 decimales para que quede bonito
        this.rating = Math.round(this.rating * 100.0) / 100.0;
    }
}
