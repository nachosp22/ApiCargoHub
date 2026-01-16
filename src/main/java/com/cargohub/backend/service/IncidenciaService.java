package com.cargohub.backend.service;

import com.cargohub.backend.entity.Incidencia;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.EstadoIncidencia;
import com.cargohub.backend.repository.IncidenciaRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidenciaService {

    @Autowired private IncidenciaRepository incidenciaRepository;
    @Autowired private PorteRepository porteRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // --- 1. REPORTAR PROBLEMA (Conductor/Cliente) ---
    @Transactional
    public Incidencia reportarIncidencia(Long porteId, String titulo, String descripcion) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        Incidencia incidencia = new Incidencia();
        incidencia.setPorte(porte);
        incidencia.setTitulo(titulo);
        incidencia.setDescripcion(descripcion);
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setFechaReporte(LocalDateTime.now());

        // Opcional: Podrías mandar un email al Admin aquí
        return incidenciaRepository.save(incidencia);
    }

    // --- 2. RESOLVER (Admin) ---
    @Transactional
    public Incidencia resolverIncidencia(Long incidenciaId, Long adminId, String resolucion, EstadoIncidencia estadoFinal) {
        Incidencia incidencia = incidenciaRepository.findById(incidenciaId)
                .orElseThrow(() -> new RuntimeException("Incidencia no encontrada"));

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        incidencia.setResolucion(resolucion);
        incidencia.setAdmin(admin);
        incidencia.setEstado(estadoFinal); // RESUELTA o DESESTIMADA
        incidencia.setFechaResolucion(LocalDateTime.now());

        return incidenciaRepository.save(incidencia);
    }

    // --- 3. CONSULTAS ---
    public List<Incidencia> listarPendientes() {
        // Devuelve las ABIERTA o EN_REVISION
        return incidenciaRepository.findByEstado(EstadoIncidencia.ABIERTA);
    }

    public List<Incidencia> listarPorPorte(Long porteId) {
        return incidenciaRepository.findByPorteId(porteId);
    }

    // --- 4. MÉTODOS ADICIONALES ---
    public List<Incidencia> listarTodas() {
        return incidenciaRepository.findAll();
    }

    public Incidencia obtenerPorId(Long id) {
        return incidenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incidencia no encontrada"));
    }
}