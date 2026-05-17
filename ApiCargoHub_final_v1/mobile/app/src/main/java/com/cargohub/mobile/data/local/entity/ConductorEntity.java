package com.cargohub.mobile.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conductores")
public class ConductorEntity {

    @PrimaryKey
    public long id;
    public String nombre;
    public String apellidos;
    public String telefono;
    public String dni;
    public String ciudadBase;
    public String email;
    public long cachedAt;
}
