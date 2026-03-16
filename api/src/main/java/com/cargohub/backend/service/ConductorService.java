package com.cargohub.backend.service;

import com.cargohub.backend.entity.BloqueoAgenda;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import com.cargohub.backend.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConductorService {

    @Autowired private ConductorRepository conductorRepository;
    @Autowired private BloqueoAgendaRepository bloqueoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VehiculoRepository vehiculoRepository; // Para dar de baja sus camiones

    // --- 1. GESTIÓN DE PERFIL ---
    @Transactional
    public Conductor guardarOActualizar(Conductor conductor) {
        return conductorRepository.save(conductor);
    }

    public Conductor obtenerPorId(Long id) {
        return conductorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));
    }

    public Conductor obtenerPorEmailUsuario(String email) {
        // Normalize email to lowercase for search
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        return conductorRepository.findByUsuarioEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No existe conductor asociado a este email"));
    }

    // --- NUEVO: DAR DE BAJA (SOFT DELETE) ---
    @Transactional
    public void darDeBajaConductor(Long conductorId) {
        Conductor conductor = obtenerPorId(conductorId);

        // 1. Desactivar disponibilidad del Conductor
        conductor.setDisponible(false);
        conductorRepository.save(conductor);

        // 2. Bloquear acceso al Usuario (Login)
        Usuario usuario = conductor.getUsuario();
        if (usuario != null) {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
        }

        // 3. (Opcional) Dar de baja sus vehículos asociados
        List<Vehiculo> flota = vehiculoRepository.findByConductorId(conductorId);
        for (Vehiculo v : flota) {
            v.setEstado(EstadoVehiculo.BAJA);
            vehiculoRepository.save(v);
        }

        System.out.println("Conductor ID " + conductorId + " dado de baja correctamente (Historial conservado).");
    }

    // --- 2. OPERATIVA DIARIA ---
    @Transactional
    public void actualizarUbicacion(Long conductorId, Double lat, Double lon) {
        Conductor c = obtenerPorId(conductorId);
        // Si está dado de baja, no debería poder reportar ubicación
        if (c.getUsuario() != null && !c.getUsuario().isActivo()) return;

        c.setLatitudActual(lat);
        c.setLongitudActual(lon);
        c.setUltimaActualizacionUbicacion(LocalDateTime.now());
        c.setDisponible(true);
        conductorRepository.save(c);
    }

    public void cambiarDisponibilidad(Long conductorId, boolean disponible) {
        Conductor c = obtenerPorId(conductorId);
        c.setDisponible(disponible);
        conductorRepository.save(c);
    }

    // --- 3. AGENDA ---
    public List<BloqueoAgenda> obtenerAgenda(Long conductorId, LocalDateTime desde, LocalDateTime hasta) {
        return bloqueoRepository.findByConductorIdAndFechaInicioBetween(conductorId, desde, hasta);
    }

    @Transactional
    public BloqueoAgenda agregarBloqueo(Long conductorId, BloqueoAgenda bloqueo) {
        Conductor c = obtenerPorId(conductorId);
        bloqueo.setConductor(c);
        return bloqueoRepository.save(bloqueo);
    }

    @Transactional
    public void eliminarBloqueo(Long bloqueoId) {
        bloqueoRepository.deleteById(bloqueoId);
    }
}