package com.cargohub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroSerie;

    private Double baseImponible;
    private Double iva;
    private Double importeTotal;

    private LocalDate fechaEmision = LocalDate.now();

    private boolean pagada = false;

    @OneToOne
    @JoinColumn(name = "porte_id", nullable = false, unique = true)
    private Porte porte;

    @PrePersist
    public void calcularTotales() {
        if (baseImponible != null) {
            // Calculamos el 21% de IVA
            this.iva = Math.round((baseImponible * 0.21) * 100.0) / 100.0;
            this.importeTotal = Math.round((baseImponible + this.iva) * 100.0) / 100.0;
        }
    }
}