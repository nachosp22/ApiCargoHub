package com.cargohub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConductorCandidatoResponse {
    private Long id;
    private String nombre;
    private String apellidos;
    private String telefono;
    private String ciudadBase;
    private String vehiculoInfo;
    private Double score;
}
