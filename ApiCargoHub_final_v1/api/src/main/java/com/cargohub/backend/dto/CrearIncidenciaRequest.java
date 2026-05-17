package com.cargohub.backend.dto;

import com.cargohub.backend.entity.enums.PrioridadIncidencia;
import com.cargohub.backend.entity.enums.SeveridadIncidencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearIncidenciaRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar 150 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 4000, message = "La descripción no puede superar 4000 caracteres")
    private String descripcion;

    private SeveridadIncidencia severidad;

    private PrioridadIncidencia prioridad;
}
