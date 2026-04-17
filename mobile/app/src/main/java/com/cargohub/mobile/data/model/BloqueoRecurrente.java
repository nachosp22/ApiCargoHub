package com.cargohub.mobile.data.model;

public class BloqueoRecurrente {

    private int diaSemana;
    private String nombre;
    private boolean activo;

    public int getDiaSemana() {
        return diaSemana;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
