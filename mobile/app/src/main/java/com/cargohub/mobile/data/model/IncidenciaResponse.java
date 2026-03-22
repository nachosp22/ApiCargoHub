package com.cargohub.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class IncidenciaResponse {

    private Long id;
    private Long porteId;
    private String titulo;
    private String descripcion;
    private EstadoIncidencia estado;
    private SeveridadIncidencia severidad;
    private PrioridadIncidencia prioridad;

    @SerializedName("fechaReporte")
    private String fechaReporte;

    @SerializedName("fechaLimiteSla")
    private String fechaLimiteSla;

    private String resolucion;

    @SerializedName("fechaResolucion")
    private String fechaResolucion;

    public Long getId() {
        return id;
    }

    public Long getPorteId() {
        return porteId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoIncidencia getEstado() {
        return estado;
    }

    public SeveridadIncidencia getSeveridad() {
        return severidad;
    }

    public PrioridadIncidencia getPrioridad() {
        return prioridad;
    }

    public String getFechaReporte() {
        return fechaReporte;
    }

    public String getFechaLimiteSla() {
        return fechaLimiteSla;
    }

    public String getResolucion() {
        return resolucion;
    }

    public String getFechaResolucion() {
        return fechaResolucion;
    }
}
