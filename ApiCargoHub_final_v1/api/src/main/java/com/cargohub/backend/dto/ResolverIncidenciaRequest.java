package com.cargohub.backend.dto;

import com.cargohub.backend.entity.enums.EstadoIncidencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolverIncidenciaRequest {

    @Size(max = 4000, message = "La resolución no puede superar 4000 caracteres")
    private String resolucion;

    @NotNull(message = "El estado final es obligatorio")
    private EstadoIncidencia estadoFinal;
}
