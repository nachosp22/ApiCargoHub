package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portes")
public class Porte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- RUTA Y DINERO ---
    private String origen;
    private String destino;
    private Double distanciaKm;
    private Double precio;

    // --- CARGA (INPUT) ---
    // Lo que escribe el cliente: "Quiero llevar un piano de cola y 3 cajas"
    @Column(columnDefinition = "TEXT")
    private String descripcionCliente;

    // --- CÁLCULOS DE LA IA (OUTPUT) ---
    // Dimensiones estimadas por n8n para hacer el matching
    private Double pesoTotalKg;
    private Double volumenTotalM3;
    private Double largoMaxPaquete; // Vital para ver si cabe de largo

    // Preferencias técnicas
    @Enumerated(EnumType.STRING)
    private TipoVehiculo tipoVehiculoRequerido;
    private boolean requiereFrio;

    // --- SEGURIDAD Y ERRORES (HUMAN-IN-THE-LOOP) ---

    // Si esto es TRUE, el algoritmo SE DETIENE y el porte no se asigna.
    // Aparecerá en el Dashboard del Admin con una alerta roja.
    private boolean revisionManual = false;

    // Aquí guardamos por qué falló:
    // "IA con baja confianza", "Dimensiones imposibles", "Sin vehículos disponibles"
    private String motivoRevision;

    // --- ESTADO Y FECHAS ---
    @Enumerated(EnumType.STRING)
    private EstadoPorte estado = EstadoPorte.PENDIENTE;

    private LocalDateTime fechaCreacion = LocalDateTime.now();
    private LocalDateTime fechaRecogida;
    private LocalDateTime fechaEntrega;

    // --- RELACIONES ---
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "conductor_id")
    private Conductor conductor;

    @ManyToOne
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;
}