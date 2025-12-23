package com.cargohub.backend.service;

import com.cargohub.backend.entity.BloqueoAgenda;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.ConductorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConductorService {

    @Autowired
    private ConductorRepository conductorRepository;

    @Autowired
    private BloqueoAgendaRepository bloqueoRepository;

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
        return conductorRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new RuntimeException("No existe conductor asociado a este email"));
    }

    // --- 2. OPERATIVA DIARIA (GPS & ESTADO) ---

    @Transactional
    public void actualizarUbicacion(Long conductorId, Double lat, Double lon) {
        Conductor c = obtenerPorId(conductorId);
        c.setLatitudActual(lat);
        c.setLongitudActual(lon);
        c.setUltimaActualizacionUbicacion(LocalDateTime.now());
        c.setDisponible(true); // Si reporta GPS, está trabajando
        conductorRepository.save(c);
    }

    public void cambiarDisponibilidad(Long conductorId, boolean disponible) {
        Conductor c = obtenerPorId(conductorId);
        c.setDisponible(disponible);
        conductorRepository.save(c);
    }

    // --- 3. AGENDA (VACACIONES / BAJAS) ---

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