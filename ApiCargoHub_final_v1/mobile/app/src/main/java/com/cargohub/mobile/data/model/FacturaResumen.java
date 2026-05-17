package com.cargohub.mobile.data.model;

public class FacturaResumen {

    private Double totalFacturado;
    private Double totalPagado;
    private Double totalPendiente;
    private Long numeroFacturas;

    public Double getTotalFacturado() { return totalFacturado; }
    public Double getTotalPagado() { return totalPagado; }
    public Double getTotalPendiente() { return totalPendiente; }
    public Long getNumeroFacturas() { return numeroFacturas; }
}
