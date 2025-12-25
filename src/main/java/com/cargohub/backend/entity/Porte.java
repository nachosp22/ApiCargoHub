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
    private Double latitudOrigen;
    private Double longitudOrigen;
    private Double latitudDestino;
    private Double longitudDestino;

    private Double distanciaKm;
    private boolean distanciaEstimada = true;

    // --- ECONOMÍA ---
    private Double precio;
    private Double ajustePrecio = 0.0;
    private String motivoAjuste;

    // --- CARGA ---
    @Column(columnDefinition = "TEXT")
    private String descripcionCliente;

    private Double pesoTotalKg;
    private Double volumenTotalM3;
    private Double largoMaxPaquete;

    @Enumerated(EnumType.STRING)
    private TipoVehiculo tipoVehiculoRequerido;

    // --- CONTROL ---
    private boolean revisionManual = false;
    private String motivoRevision;

    @Enumerated(EnumType.STRING)
    private EstadoPorte estado = EstadoPorte.PENDIENTE;

    @Version
    private Integer version;

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

    public Double getPrecioFinal() {
        Double p = this.precio != null ? this.precio : 0.0;
        Double a = this.ajustePrecio != null ? this.ajustePrecio : 0.0;
        return p + a;
    }
}