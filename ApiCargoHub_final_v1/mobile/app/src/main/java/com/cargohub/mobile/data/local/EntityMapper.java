package com.cargohub.mobile.data.local;

import com.cargohub.mobile.data.local.entity.AgendaBloqueoEntity;
import com.cargohub.mobile.data.local.entity.ConductorEntity;
import com.cargohub.mobile.data.local.entity.PorteEntity;
import com.cargohub.mobile.data.local.entity.VehiculoEntity;
import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.data.model.Vehiculo;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapea entidades Room con modelos de API usando Gson.
 */
public final class EntityMapper {

    private EntityMapper() {
    }


    public static PorteEntity toEntity(Porte porte, long conductorId, boolean isOffer) {
        PorteEntity e = new PorteEntity();
        e.id = porte.getId() != null ? porte.getId() : 0;
        e.origen = porte.getOrigen();
        e.destino = porte.getDestino();
        e.estado = porte.getEstado();
        e.fechaRecogida = porte.getFechaRecogida();
        e.fechaEntrega = porte.getFechaEntrega();
        e.descripcionMercancia = porte.getDescripcionMercancia();
        e.descripcionCliente = porte.getDescripcionCliente();
        e.precio = porte.getPrecio();
        e.distanciaKm = porte.getDistanciaKm();
        e.origenLat = porte.getOrigenLat();
        e.origenLon = porte.getOrigenLon();
        e.destinoLat = porte.getDestinoLat();
        e.destinoLon = porte.getDestinoLon();
        e.mercanciaPeligrosa = porte.getMercanciaPeligrosa();
        e.conductorId = conductorId;
        e.isOffer = isOffer;
        e.cachedAt = System.currentTimeMillis();
        return e;
    }

    public static List<PorteEntity> toEntities(List<Porte> portes, long conductorId, boolean isOffer) {
        List<PorteEntity> entities = new ArrayList<>(portes.size());
        for (Porte p : portes) {
            entities.add(toEntity(p, conductorId, isOffer));
        }
        return entities;
    }

    public static Porte toPorte(PorteEntity e) {
        Porte p = new Porte();
        p.setId(e.id);
        p.setEstado(e.estado);
        return reconstructPorteViaGson(e);
    }

    public static List<Porte> toPortes(List<PorteEntity> entities) {
        List<Porte> portes = new ArrayList<>(entities.size());
        for (PorteEntity e : entities) {
            portes.add(toPorte(e));
        }
        return portes;
    }

    private static Porte reconstructPorteViaGson(PorteEntity e) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(e);
        com.google.gson.JsonObject obj = gson.fromJson(json, com.google.gson.JsonObject.class);
        obj.remove("conductorId");
        obj.remove("isOffer");
        obj.remove("cachedAt");
        return gson.fromJson(obj, Porte.class);
    }


    public static ConductorEntity toEntity(ConductorProfileResponse profile) {
        ConductorEntity e = new ConductorEntity();
        e.id = profile.getId() != null ? profile.getId() : 0;
        e.nombre = profile.getNombre();
        e.apellidos = profile.getApellidos();
        e.telefono = profile.getTelefono();
        e.dni = profile.getDni();
        e.ciudadBase = profile.getCiudadBase();
        e.email = profile.getEmail();
        e.cachedAt = System.currentTimeMillis();
        return e;
    }

    public static ConductorProfileResponse toConductorProfile(ConductorEntity e) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(e);
        com.google.gson.JsonObject obj = gson.fromJson(json, com.google.gson.JsonObject.class);
        obj.remove("cachedAt");
        if (e.email != null) {
            com.google.gson.JsonObject usuario = new com.google.gson.JsonObject();
            usuario.addProperty("email", e.email);
            obj.add("usuario", usuario);
            obj.remove("email");
        }
        return gson.fromJson(obj, ConductorProfileResponse.class);
    }


    public static VehiculoEntity toEntity(Vehiculo vehiculo, long conductorId) {
        VehiculoEntity e = new VehiculoEntity();
        e.id = vehiculo.getId() != null ? vehiculo.getId() : 0;
        e.matricula = vehiculo.getMatricula();
        e.marca = vehiculo.getMarca();
        e.modelo = vehiculo.getModelo();
        e.tipo = vehiculo.getTipo() != null ? vehiculo.getTipo().name() : null;
        e.estado = vehiculo.getEstado() != null ? vehiculo.getEstado().name() : null;
        e.capacidadCargaKg = vehiculo.getCapacidadCargaKg();
        e.largoUtilMm = vehiculo.getLargoUtilMm();
        e.anchoUtilMm = vehiculo.getAnchoUtilMm();
        e.altoUtilMm = vehiculo.getAltoUtilMm();
        e.volumenM3 = vehiculo.getVolumenM3();
        e.conductorId = conductorId;
        e.cachedAt = System.currentTimeMillis();
        return e;
    }

    public static List<VehiculoEntity> toVehiculoEntities(List<Vehiculo> vehiculos, long conductorId) {
        List<VehiculoEntity> entities = new ArrayList<>(vehiculos.size());
        for (Vehiculo v : vehiculos) {
            entities.add(toEntity(v, conductorId));
        }
        return entities;
    }

    public static List<Vehiculo> toVehiculos(List<VehiculoEntity> entities) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        List<Vehiculo> vehiculos = new ArrayList<>(entities.size());
        for (VehiculoEntity e : entities) {
            String json = gson.toJson(e);
            com.google.gson.JsonObject obj = gson.fromJson(json, com.google.gson.JsonObject.class);
            obj.remove("conductorId");
            obj.remove("cachedAt");
            vehiculos.add(gson.fromJson(obj, Vehiculo.class));
        }
        return vehiculos;
    }


    public static AgendaBloqueoEntity toEntity(AgendaBloqueo bloqueo, long conductorId) {
        AgendaBloqueoEntity e = new AgendaBloqueoEntity();
        e.id = bloqueo.getId() != null ? bloqueo.getId() : 0;
        e.fechaInicio = bloqueo.getFechaInicio();
        e.fechaFin = bloqueo.getFechaFin();
        e.tipo = bloqueo.getTipo() != null ? bloqueo.getTipo().name() : null;
        e.titulo = bloqueo.getTitulo();
        e.conductorId = conductorId;
        e.cachedAt = System.currentTimeMillis();
        return e;
    }

    public static List<AgendaBloqueoEntity> toAgendaEntities(List<AgendaBloqueo> bloqueos, long conductorId) {
        List<AgendaBloqueoEntity> entities = new ArrayList<>(bloqueos.size());
        for (AgendaBloqueo b : bloqueos) {
            entities.add(toEntity(b, conductorId));
        }
        return entities;
    }

    public static List<AgendaBloqueo> toAgendaBloqueos(List<AgendaBloqueoEntity> entities) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        List<AgendaBloqueo> bloqueos = new ArrayList<>(entities.size());
        for (AgendaBloqueoEntity e : entities) {
            String json = gson.toJson(e);
            com.google.gson.JsonObject obj = gson.fromJson(json, com.google.gson.JsonObject.class);
            obj.remove("conductorId");
            obj.remove("cachedAt");
            bloqueos.add(gson.fromJson(obj, AgendaBloqueo.class));
        }
        return bloqueos;
    }
}
