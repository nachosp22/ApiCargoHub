package com.cargohub.mobile.data.model;

import java.util.List;
import java.util.Map;

public class EstadisticasConductor {

    private Long portesCompletados;
    private Long portesCancelados;
    private Long portesEnCurso;
    private Long portesPendientes;
    private Double kmRecorridos;
    private Double ingresoTotal;
    private Double mediaPorPorte;
    private List<IngresoMensual> ingresoPorMes;
    private Map<String, Long> portesPorEstado;

    public Long getPortesCompletados() { return portesCompletados; }
    public Long getPortesCancelados() { return portesCancelados; }
    public Long getPortesEnCurso() { return portesEnCurso; }
    public Long getPortesPendientes() { return portesPendientes; }
    public Double getKmRecorridos() { return kmRecorridos; }
    public Double getIngresoTotal() { return ingresoTotal; }
    public Double getMediaPorPorte() { return mediaPorPorte; }
    public List<IngresoMensual> getIngresoPorMes() { return ingresoPorMes; }
    public Map<String, Long> getPortesPorEstado() { return portesPorEstado; }

    public static class IngresoMensual {
        private String mes;
        private Double total;
        private Long portes;

        public String getMes() { return mes; }
        public Double getTotal() { return total; }
        public Long getPortes() { return portes; }
    }
}
