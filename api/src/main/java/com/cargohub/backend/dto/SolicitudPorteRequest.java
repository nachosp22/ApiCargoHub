package com.cargohub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SolicitudPorteRequest {

    @NotBlank(message = "El origen es obligatorio")
    private String origen;

    @NotBlank(message = "El destino es obligatorio")
    private String destino;

    private String ciudadOrigen;
    private String ciudadDestino;

    private Double latitudOrigen;
    private Double longitudOrigen;
    private Double latitudDestino;
    private Double longitudDestino;

    @NotBlank(message = "La descripción de la carga es obligatoria")
    private String descripcionCliente;

    @NotNull(message = "La fecha de recogida es obligatoria")
    private LocalDateTime fechaRecogida;
}
