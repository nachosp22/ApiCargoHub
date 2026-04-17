package com.cargohub.mobile.data.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

public class Porte {

    private Long id;
    private String origen;
    private String destino;
    private String estado;

    @SerializedName("fechaRecogida")
    private String fechaRecogida;

    @SerializedName("fechaEntrega")
    private String fechaEntrega;

    @SerializedName("descripcionMercancia")
    private String descripcionMercancia;

    @SerializedName("descripcionCliente")
    private String descripcionCliente;

    @SerializedName(value = "precio", alternate = {"precioFinal"})
    private Double precio;

    @SerializedName("distanciaKm")
    private Double distanciaKm;

    @SerializedName("origenLat")
    private Double origenLat;

    @SerializedName("origenLon")
    private Double origenLon;

    @SerializedName("destinoLat")
    private Double destinoLat;

    @SerializedName("destinoLon")
    private Double destinoLon;

    @SerializedName("mercanciaPeligrosa")
    private Boolean mercanciaPeligrosa;

    @SerializedName("requiereFrio")
    private Boolean requiereFrio;

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

    @Nullable
    public EstadoPorte getEstadoPorte() {
        if (estado == null || estado.trim().isEmpty()) {
            return null;
        }
        try {
            return EstadoPorte.valueOf(estado.trim().toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String getFechaRecogida() {
        return fechaRecogida;
    }

    public String getFechaEntrega() {
        return fechaEntrega;
    }

    public String getDescripcionMercancia() {
        if (descripcionMercancia != null && !descripcionMercancia.trim().isEmpty()) {
            return descripcionMercancia;
        }
        return descripcionCliente;
    }

    public String getDescripcionCliente() {
        return descripcionCliente;
    }

    public Double getPrecio() {
        return precio;
    }

    public Double getDistanciaKm() {
        return distanciaKm;
    }

    public Double getOrigenLat() {
        return origenLat;
    }

    public Double getOrigenLon() {
        return origenLon;
    }

    public Double getDestinoLat() {
        return destinoLat;
    }

    public Double getDestinoLon() {
        return destinoLon;
    }

    public boolean hasDestinationCoordinates() {
        return destinoLat != null && destinoLon != null;
    }

    public boolean hasOriginCoordinates() {
        return origenLat != null && origenLon != null;
    }

    public Boolean getMercanciaPeligrosa() {
        return mercanciaPeligrosa;
    }

    public Boolean getRequiereFrio() {
        if (requiereFrio != null) {
            return requiereFrio;
        }
        return mercanciaPeligrosa;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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
