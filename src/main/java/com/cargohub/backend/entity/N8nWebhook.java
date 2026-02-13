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
@Table(name = "n8n_webhooks")
public class N8nWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- REQUEST DATA ---
    @Column(columnDefinition = "TEXT")
    private String requestData;

    @Column(nullable = false)
    private LocalDateTime requestTimestamp;

    // --- RESPONSE DATA ---
    @Column(columnDefinition = "TEXT")
    private String responseData;

    private LocalDateTime responseTimestamp;

    // --- STATUS ---
    @Column(nullable = false)
    private Boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // --- CALCULATED VALUES ---
    private Double pesoTotalKg;
    private Double volumenTotalM3;
    private Double largoMaxPaquete;
    private String tipoVehiculoRequerido;
    private Boolean revisionManual;

    // --- RELATION ---
    @ManyToOne
    @JoinColumn(name = "porte_id")
    private Porte porte;
}
