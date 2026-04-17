package com.cargohub.backend.dto;

import com.cargohub.backend.entity.enums.TipoFotoCarga;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearFotoCargaRequest {

    @NotNull(message = "El tipo de foto es obligatorio")
    private TipoFotoCarga tipo;

    @NotBlank(message = "La foto en base64 es obligatoria")
    private String fotoBase64;

    private String descripcion;
}
