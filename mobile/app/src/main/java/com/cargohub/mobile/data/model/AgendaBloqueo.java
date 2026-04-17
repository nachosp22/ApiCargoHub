package com.cargohub.mobile.data.model;

public class AgendaBloqueo {

    private Long id;
    private String fechaInicio;
    private String fechaFin;
    private TipoBloqueoAgenda tipo;
    private String titulo;

    public Long getId() {
        return id;
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
