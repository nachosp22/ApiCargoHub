package com.cargohub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResumenResponse {
    private Double totalFacturado;
    private Double totalPagado;
    private Double totalPendiente;
    private Long numeroFacturas;
}
