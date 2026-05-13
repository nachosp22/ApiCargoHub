package com.cargohub.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
    @Column(columnDefinition = "TEXT")
    private String origen;
    @Column(columnDefinition = "TEXT")
    private String destino;
    @Column(nullable = true)
    private String ciudadOrigen;
    @Column(nullable = true)
    private String ciudadDestino;
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
    private Double anchoMaxPaquete;
    private Double altoMaxPaquete;

    @Enumerated(EnumType.STRING)
    private TipoVehiculo tipoVehiculoRequerido;

    // --- CONTROL ---
    private boolean revisionManual = false;
    @Column(columnDefinition = "TEXT")
    private String motivoRevision;

    @Enumerated(EnumType.STRING)
    private EstadoPorte estado = EstadoPorte.PENDIENTE;

    @Version
    private Integer version;

    private LocalDateTime fechaCreacion = LocalDateTime.now();
    private LocalDateTime fechaRecogida;
    private LocalDateTime fechaEntrega;

    @Column(columnDefinition = "TEXT")
    private String firmaEntregaBase64;

    private String firmaEntregaFirmadoPor;

    private LocalDateTime firmaEntregaFecha;

    // --- RELACIONES ---
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "conductor_id")
    private Conductor conductor;

    @JsonIgnore
    @ElementCollection
    @CollectionTable(name = "porte_rechazos", joinColumns = @JoinColumn(name = "porte_id"))
    @Column(name = "conductor_id", nullable = false)
    private Set<Long> conductoresRechazados = new HashSet<>();

    public Double getPrecioFinal() {
        Double p = this.precio != null ? this.precio : 0.0;
        Double a = this.ajustePrecio != null ? this.ajustePrecio : 0.0;
        return p + a;
    }
}
