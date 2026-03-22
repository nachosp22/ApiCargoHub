package com.cargohub.mobile.data.model;

public class Porte {

    private Long id;
    private String origen;
    private String destino;
    private String estado;

    public Long getId() {
        return id;
    }

    public String getOrigen() {
        return origen;
    }

    public String getDestino() {
        return destino;
    }

    public String getEstado() {
        return estado;
    }

    @Override
    public String toString() {
        if (origen != null && destino != null) {
            return origen + " → " + destino;
        }
        if (origen != null) {
            return origen;
        }
        if (destino != null) {
            return destino;
        }
        return "Porte #" + id;
    }
}
