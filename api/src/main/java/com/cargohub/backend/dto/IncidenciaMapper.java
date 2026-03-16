package com.cargohub.backend.dto;

import com.cargohub.backend.entity.Incidencia;
import com.cargohub.backend.entity.IncidenciaEvento;

public final class IncidenciaMapper {

    private IncidenciaMapper() {
    }

    public static IncidenciaResponse toResponse(Incidencia incidencia) {
        if (incidencia == null) {
            return null;
        }

        IncidenciaResponse response = new IncidenciaResponse();
        response.setId(incidencia.getId());
        response.setPorteId(incidencia.getPorte() != null ? incidencia.getPorte().getId() : null);
        response.setTitulo(incidencia.getTitulo());
        response.setDescripcion(incidencia.getDescripcion());
        response.setEstado(incidencia.getEstado());
        response.setSeveridad(incidencia.getSeveridad());
        response.setPrioridad(incidencia.getPrioridad());
        response.setFechaReporte(incidencia.getFechaReporte());
        response.setFechaLimiteSla(incidencia.getFechaLimiteSla());
        response.setResolucion(incidencia.getResolucion());
        response.setFechaResolucion(incidencia.getFechaResolucion());
        response.setAdminId(incidencia.getAdmin() != null ? incidencia.getAdmin().getId() : null);
        return response;
    }

    public static IncidenciaEventoResponse toResponse(IncidenciaEvento evento) {
        if (evento == null) {
            return null;
        }

        IncidenciaEventoResponse response = new IncidenciaEventoResponse();
        response.setId(evento.getId());
        response.setIncidenciaId(evento.getIncidencia() != null ? evento.getIncidencia().getId() : null);
        response.setActorId(evento.getActor() != null ? evento.getActor().getId() : null);
        response.setEstadoAnterior(evento.getEstadoAnterior());
        response.setEstadoNuevo(evento.getEstadoNuevo());
        response.setFecha(evento.getFecha());
        response.setAccion(evento.getAccion());
        response.setComentario(evento.getComentario());
        return response;
    }
}
