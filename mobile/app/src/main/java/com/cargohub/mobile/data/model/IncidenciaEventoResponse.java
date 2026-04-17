package com.cargohub.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class IncidenciaEventoResponse {

    private Long id;
    private Long incidenciaId;
    private Long actorId;
    private EstadoIncidencia estadoAnterior;
    private EstadoIncidencia estadoNuevo;

    @SerializedName("fecha")
    private String fecha;

    private String accion;
    private String comentario;

    public Long getId() {
        return id;
    }

    public Long getIncidenciaId() {
        return incidenciaId;
    }

    public Long getActorId() {
        return actorId;
    }

    public EstadoIncidencia getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoIncidencia getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getFecha() {
        return fecha;
    }

    public String getAccion() {
        return accion;
    }

    public String getComentario() {
        return comentario;
    }
}
