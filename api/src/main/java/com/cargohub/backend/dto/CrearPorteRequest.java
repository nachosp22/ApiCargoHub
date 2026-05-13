package com.cargohub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearPorteRequest {

    // --- CLIENTE ---
    @NotNull
    private Long clienteId;

    // --- RUTA ---
    @NotBlank
    private String origen;
    @NotBlank
    private String destino;
    private Double latitudOrigen;
    private Double longitudOrigen;
    private Double latitudDestino;
    private Double longitudDestino;

    // --- CARGA (MENSAJE PARA N8N) ---
    private String descripcionCliente;

    // --- FECHAS ---
    private LocalDateTime fechaRecogida;
    private LocalDateTime fechaEntrega;
}
