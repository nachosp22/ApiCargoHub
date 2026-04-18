package com.cargohub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FotoPerfilRequest {

    @NotBlank(message = "La imagen base64 es obligatoria")
    private String imagen;
}
