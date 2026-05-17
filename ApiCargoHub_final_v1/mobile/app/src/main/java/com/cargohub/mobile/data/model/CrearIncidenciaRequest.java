package com.cargohub.mobile.data.model;

public class CrearIncidenciaRequest {

    private final String titulo;
    private final String descripcion;
    private final String severidad;
    private final String prioridad;

    public CrearIncidenciaRequest(String titulo, String descripcion, String severidad, String prioridad) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.severidad = severidad;
        this.prioridad = prioridad;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getSeveridad() {
        return severidad;
    }

    public String getPrioridad() {
        return prioridad;
    }
}
