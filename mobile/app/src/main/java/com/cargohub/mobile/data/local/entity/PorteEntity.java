package com.cargohub.mobile.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "portes")
public class PorteEntity {

    @PrimaryKey
    public long id;
    public String origen;
    public String destino;
    public String estado;
    public String fechaRecogida;
    public String fechaEntrega;
    public String descripcionMercancia;
    public String descripcionCliente;
    public Double precio;
    public Double distanciaKm;
    public Double origenLat;
    public Double origenLon;
    public Double destinoLat;
    public Double destinoLon;
    public Boolean mercanciaPeligrosa;
    public Boolean requiereFrio;
    public long conductorId;
    public boolean isOffer;
    public long cachedAt;
}
