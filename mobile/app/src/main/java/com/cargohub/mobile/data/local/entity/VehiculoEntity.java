package com.cargohub.mobile.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehiculos")
public class VehiculoEntity {

    @PrimaryKey
    public long id;
    public String matricula;
    public String marca;
    public String modelo;
    public String tipo;
    public String estado;
    public Integer capacidadCargaKg;
    public Integer largoUtilMm;
    public Integer anchoUtilMm;
    public Integer altoUtilMm;
    public Double volumenM3;
    public boolean trampillaElevadora;
    public long conductorId;
    public long cachedAt;
}
