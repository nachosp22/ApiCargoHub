package com.cargohub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasGlobalesResponse {

    // --- KPIs principales ---
    private long totalPortes;
    private long portesEsteMes;
    private double portesTendencia; // % vs mes anterior

    private double totalIngresos;
    private double ingresosEsteMes;
    private double ingresosTendencia; // % vs mes anterior

    private long totalConductoresActivos;
    private long totalClientes;

    // --- Portes por estado ---
    private long portesCompletados;
    private long portesPendientes;
    private long portesEnTransito;

    // --- Facturas ---
    private long facturasEmitidas;
    private long facturasPagadas;
    private long facturasPendientes;

    // --- Rankings ---
    private List<TopConductor> topConductores;
    private List<TopCliente> topClientes;

    // --- Series temporales ---
    private List<PorteMensual> portesPorMes;
    private List<PorteEstado> portesPorEstado;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopConductor {
        private String nombre;
        private long portes;
        private double rating;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCliente {
        private String nombreEmpresa;
        private double totalFacturado;
        private long portes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PorteMensual {
        private String mes;
        private long cantidad;
        private double ingresos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PorteEstado {
        private String estado;
        private long cantidad;
    }
}
