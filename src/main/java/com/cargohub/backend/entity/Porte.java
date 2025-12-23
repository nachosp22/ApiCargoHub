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

    // --- RUTA ---
    private String origen;
    private String destino;

    // ESTOS SON LOS CAMPOS QUE TE FALTAN Y CAUSAN EL ERROR
    private Double latitudOrigen;
    private Double longitudOrigen;
    private Double latitudDestino;
    private Double longitudDestino;

    private Double distanciaKm;
    private boolean distanciaEstimada = false;

    // --- ECONOMÍA ---
    private Double precio;
    private Double ajustePrecio = 0.0; // <-- Campo para penalizaciones
    private String motivoAjuste;       // <-- Motivo del ajuste

    // --- CARGA ---
    @Column(columnDefinition = "TEXT")
    private String descripcionCliente;
    private Double pesoTotalKg;
    private Double volumenTotalM3;
    private Double largoMaxPaquete;

    @Enumerated(EnumType.STRING)
    private TipoVehiculo tipoVehiculoRequerido;
    private boolean requiereFrio = false;

    // --- SEGURIDAD ---
    private boolean revisionManual = false;
    private String motivoRevision;

    @Enumerated(EnumType.STRING)
    private EstadoPorte estado = EstadoPorte.PENDIENTE;

    @Version
    private Integer version;

    private LocalDateTime fechaCreacion = LocalDateTime.now();
    private LocalDateTime fechaRecogida;
    private LocalDateTime fechaEntrega;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "conductor_id")
    private Conductor conductor;

    // El vehículo es opcional, así que no da error si falta
    @ManyToOne
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    // Helper para el precio final
    public Double getPrecioFinal() {
        Double p = this.precio != null ? this.precio : 0.0;
        Double a = this.ajustePrecio != null ? this.ajustePrecio : 0.0;
        return p + a;
    }
}