package com.cargohub.mobile.data.model;

public class VehiculoUpsertRequest {

    private final String matricula;
    private final String marca;
    private final String modelo;
    private final TipoVehiculo tipo;
    private final Integer capacidadCargaKg;
    private final Integer largoUtilMm;
    private final Integer anchoUtilMm;
    private final Integer altoUtilMm;
    private final boolean trampillaElevadora;

    public VehiculoUpsertRequest(String matricula,
                                 String marca,
                                 String modelo,
                                 TipoVehiculo tipo,
                                 Integer capacidadCargaKg,
                                 Integer largoUtilMm,
                                 Integer anchoUtilMm,
                                 Integer altoUtilMm,
                                 boolean trampillaElevadora) {
        this.matricula = matricula;
        this.marca = marca;
        this.modelo = modelo;
        this.tipo = tipo;
        this.capacidadCargaKg = capacidadCargaKg;
        this.largoUtilMm = largoUtilMm;
        this.anchoUtilMm = anchoUtilMm;
        this.altoUtilMm = altoUtilMm;
        this.trampillaElevadora = trampillaElevadora;
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

    public boolean isTrampillaElevadora() {
        return trampillaElevadora;
    }
}
