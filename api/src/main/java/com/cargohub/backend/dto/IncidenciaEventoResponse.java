package com.cargohub.backend.dto;

import com.cargohub.backend.entity.enums.EstadoIncidencia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaEventoResponse {

    private Long id;
    private Long incidenciaId;
    private Long actorId;
    private EstadoIncidencia estadoAnterior;
    private EstadoIncidencia estadoNuevo;
    private LocalDateTime fecha;
    private String accion;
    private String comentario;
}
