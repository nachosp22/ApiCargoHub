package com.cargohub.mobile.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "agenda_bloqueos")
public class AgendaBloqueoEntity {

    @PrimaryKey
    public long id;
    public String fechaInicio;
    public String fechaFin;
    public String tipo;
    public String titulo;
    public long conductorId;
    public long cachedAt;
}
