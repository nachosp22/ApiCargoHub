package com.cargohub.backend.dto;

import lombok.Data;

@Data
public class ActualizarDimensionesRequest {
    private Double pesoTotalKg;
    private Double volumenTotalM3;
    private Double largoMaxPaquete;
    private Double anchoMaxPaquete;
    private Double altoMaxPaquete;
    private String tipoVehiculoRequerido;
}
