package com.cargohub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpWebhookResponse {
    private Double pesoTotalKg;
    private Double volumenTotalM3;
    private Double largoMaxPaquete;
    private String tipoVehiculoRequerido;
    private Boolean revisionManual;
    private String motivoRevision;
}
