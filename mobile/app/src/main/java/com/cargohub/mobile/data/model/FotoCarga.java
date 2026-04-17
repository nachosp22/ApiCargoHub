package com.cargohub.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class FotoCarga {

    private Long id;
    private Long porteId;
    private String tipo;
    private String fotoBase64;
    private String descripcion;

    @SerializedName("fechaCaptura")
    private String fechaCaptura;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPorteId() { return porteId; }
    public void setPorteId(Long porteId) { this.porteId = porteId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getFotoBase64() { return fotoBase64; }
    public void setFotoBase64(String fotoBase64) { this.fotoBase64 = fotoBase64; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFechaCaptura() { return fechaCaptura; }
    public void setFechaCaptura(String fechaCaptura) { this.fechaCaptura = fechaCaptura; }

    public String getTipoLabel() {
        if (tipo == null) return "";
        switch (tipo) {
            case "CARGA": return "Carga";
            case "DESCARGA": return "Descarga";
            case "DANO": return "Daño";
            default: return tipo;
        }
    }
}
