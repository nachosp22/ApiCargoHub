package com.cargohub.backend.dto;

import com.cargohub.backend.entity.enums.EstadoIncidencia;
import com.cargohub.backend.entity.enums.PrioridadIncidencia;
import com.cargohub.backend.entity.enums.SeveridadIncidencia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaResponse {

    private Long id;
    private Long porteId;
    private String titulo;
    private String descripcion;
    private EstadoIncidencia estado;
    private SeveridadIncidencia severidad;
    private PrioridadIncidencia prioridad;
    private LocalDateTime fechaReporte;
    private LocalDateTime fechaLimiteSla;
    private String resolucion;
    private LocalDateTime fechaResolucion;
    private Long adminId;
}
