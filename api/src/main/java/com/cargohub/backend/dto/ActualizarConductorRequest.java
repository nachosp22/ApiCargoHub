package com.cargohub.backend.dto;

public class ActualizarConductorRequest {
    private String nombre;
    private String apellidos;
    private String telefono;
    private String dni;
    private String ciudadBase;
    private Integer radioAccionKm;
    private Boolean disponible;

    public ActualizarConductorRequest() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getCiudadBase() { return ciudadBase; }
    public void setCiudadBase(String ciudadBase) { this.ciudadBase = ciudadBase; }
    public Integer getRadioAccionKm() { return radioAccionKm; }
    public void setRadioAccionKm(Integer radioAccionKm) { this.radioAccionKm = radioAccionKm; }
    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
}
