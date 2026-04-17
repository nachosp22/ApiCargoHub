package com.cargohub.mobile.data.model;

public class Vehiculo {

    private Long id;
    private String matricula;
    private String marca;
    private String modelo;
    private TipoVehiculo tipo;
    private EstadoVehiculo estado;
    private Integer capacidadCargaKg;
    private Integer largoUtilMm;
    private Integer anchoUtilMm;
    private Integer altoUtilMm;
    private Double volumenM3;
    private boolean trampillaElevadora;

    public Long getId() {
        return id;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public EstadoVehiculo getEstado() {
        return estado;
    }

    public Integer getCapacidadCargaKg() {
        return capacidadCargaKg;
    }

    public Integer getLargoUtilMm() {
        return largoUtilMm;
    }

    public Integer getAnchoUtilMm() {
        return anchoUtilMm;
    }

    public Integer getAltoUtilMm() {
        return altoUtilMm;
    }

    public Double getVolumenM3() {
        return volumenM3;
    }

    public boolean isTrampillaElevadora() {
        return trampillaElevadora;
    }
}
