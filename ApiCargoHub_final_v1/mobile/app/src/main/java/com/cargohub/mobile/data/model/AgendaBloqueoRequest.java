package com.cargohub.mobile.data.model;

public class AgendaBloqueoRequest {

    private final String fechaInicio;
    private final String fechaFin;
    private final TipoBloqueoAgenda tipo;
    private final String titulo;

    public AgendaBloqueoRequest(String fechaInicio, String fechaFin, TipoBloqueoAgenda tipo, String titulo) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.tipo = tipo;
        this.titulo = titulo;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public TipoBloqueoAgenda getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }
}
