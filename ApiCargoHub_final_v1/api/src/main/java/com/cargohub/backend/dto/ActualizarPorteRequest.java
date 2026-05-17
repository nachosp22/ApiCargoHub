package com.cargohub.backend.dto;

import com.cargohub.backend.entity.enums.EstadoPorte;

import java.time.LocalDateTime;

public class ActualizarPorteRequest {
    private String origen;
    private String destino;
    private String descripcionCliente;
    private LocalDateTime fechaRecogida;
    private LocalDateTime fechaEntrega;
    private EstadoPorte estado;

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getDescripcionCliente() {
        return descripcionCliente;
    }

    public void setDescripcionCliente(String descripcionCliente) {
        this.descripcionCliente = descripcionCliente;
    }

    public LocalDateTime getFechaRecogida() {
        return fechaRecogida;
    }

    public void setFechaRecogida(LocalDateTime fechaRecogida) {
        this.fechaRecogida = fechaRecogida;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public EstadoPorte getEstado() {
        return estado;
    }

    public void setEstado(EstadoPorte estado) {
        this.estado = estado;
    }
}
