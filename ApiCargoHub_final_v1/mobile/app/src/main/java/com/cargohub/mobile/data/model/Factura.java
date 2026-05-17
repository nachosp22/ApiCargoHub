package com.cargohub.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class Factura {

    private Long id;

    @SerializedName("numeroSerie")
    private String numeroSerie;

    private Double baseImponible;
    private Double iva;
    private Double importeTotal;

    @SerializedName("fechaEmision")
    private String fechaEmision;

    private boolean pagada;

    @SerializedName("porte")
    private PorteRef porte;

    public Long getId() { return id; }
    public String getNumeroSerie() { return numeroSerie; }
    public Double getBaseImponible() { return baseImponible; }
    public Double getIva() { return iva; }
    public Double getImporteTotal() { return importeTotal; }
    public String getFechaEmision() { return fechaEmision; }
    public boolean isPagada() { return pagada; }
    public PorteRef getPorte() { return porte; }

    public Long getPorteId() {
        return porte != null ? porte.id : null;
    }

    public static class PorteRef {
        private Long id;
        public Long getId() { return id; }
    }
}
