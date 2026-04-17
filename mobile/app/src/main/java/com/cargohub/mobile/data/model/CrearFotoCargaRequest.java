package com.cargohub.mobile.data.model;

public class CrearFotoCargaRequest {

    private String tipo;
    private String fotoBase64;
    private String descripcion;

    public CrearFotoCargaRequest(String tipo, String fotoBase64, String descripcion) {
        this.tipo = tipo;
        this.fotoBase64 = fotoBase64;
        this.descripcion = descripcion;
    }

    public String getTipo() { return tipo; }
    public String getFotoBase64() { return fotoBase64; }
    public String getDescripcion() { return descripcion; }
}
