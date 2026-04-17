package com.cargohub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FirmaEntregaRequest {

    @NotBlank(message = "La firma en base64 es obligatoria")
    private String firmaBase64;

    @NotBlank(message = "El nombre del firmante es obligatorio")
    private String firmadoPor;
}
