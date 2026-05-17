package com.cargohub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasConductorResponse {
    private Long portesCompletados;
    private Long portesCancelados;
    private Long portesEnCurso;
    private Long portesPendientes;
    private Double kmRecorridos;
    private Double ingresoTotal;
    private Double mediaPorPorte;
    private List<IngresoMensual> ingresoPorMes;
    private Map<String, Long> portesPorEstado;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngresoMensual {
        private String mes;
        private Double total;
        private Long portes;
    }
}
