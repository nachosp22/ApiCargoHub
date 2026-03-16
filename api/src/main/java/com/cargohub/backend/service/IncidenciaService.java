package com.cargohub.backend.service;

import com.cargohub.backend.entity.Incidencia;
import com.cargohub.backend.entity.IncidenciaEvento;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.enums.EstadoIncidencia;
import com.cargohub.backend.entity.enums.PrioridadIncidencia;
import com.cargohub.backend.entity.enums.SeveridadIncidencia;
import com.cargohub.backend.exception.IncidenciaTransitionException;
import com.cargohub.backend.repository.IncidenciaEventoRepository;
import com.cargohub.backend.repository.IncidenciaRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class IncidenciaService {

    private static final Set<EstadoIncidencia> ESTADOS_RESOLVER_PERMITIDOS =
            EnumSet.of(EstadoIncidencia.EN_REVISION, EstadoIncidencia.RESUELTA, EstadoIncidencia.DESESTIMADA);

    private static final Map<EstadoIncidencia, Set<EstadoIncidencia>> TRANSICIONES_VALIDAS =
            new EnumMap<>(EstadoIncidencia.class);

    static {
        TRANSICIONES_VALIDAS.put(EstadoIncidencia.ABIERTA,
                EnumSet.of(EstadoIncidencia.EN_REVISION, EstadoIncidencia.RESUELTA, EstadoIncidencia.DESESTIMADA));
        TRANSICIONES_VALIDAS.put(EstadoIncidencia.EN_REVISION,
                EnumSet.of(EstadoIncidencia.RESUELTA, EstadoIncidencia.DESESTIMADA));
        TRANSICIONES_VALIDAS.put(EstadoIncidencia.RESUELTA, EnumSet.noneOf(EstadoIncidencia.class));
        TRANSICIONES_VALIDAS.put(EstadoIncidencia.DESESTIMADA, EnumSet.noneOf(EstadoIncidencia.class));
    }

    @Autowired private IncidenciaRepository incidenciaRepository;
    @Autowired private IncidenciaEventoRepository incidenciaEventoRepository;
    @Autowired private PorteRepository porteRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // --- 1. REPORTAR PROBLEMA (Conductor/Cliente) ---
    @Transactional
    public Incidencia reportarIncidencia(Long porteId, String titulo, String descripcion) {
        return reportarIncidencia(porteId, titulo, descripcion, null, null, null);
    }

    @Transactional
    public Incidencia reportarIncidencia(Long porteId,
                                         String titulo,
                                         String descripcion,
                                         SeveridadIncidencia severidad,
                                         PrioridadIncidencia prioridad,
                                         Authentication authentication) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setPorte(porte);
        incidencia.setTitulo(titulo);
        incidencia.setDescripcion(descripcion);
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setFechaReporte(LocalDateTime.now());
        incidencia.setSeveridad(severidad != null ? severidad : SeveridadIncidencia.MEDIA);
        incidencia.setPrioridad(prioridad != null ? prioridad : PrioridadIncidencia.MEDIA);
        incidencia.setFechaLimiteSla(calcularFechaLimiteSla(
                incidencia.getSeveridad(),
                incidencia.getPrioridad(),
                incidencia.getFechaReporte()
        ));

        Incidencia guardada = incidenciaRepository.save(incidencia);
        registrarEvento(guardada,
                null,
                EstadoIncidencia.ABIERTA,
                resolverActor(authentication),
                "CREADA",
                "Incidencia reportada");
        return guardada;
    }

    // --- 2. RESOLVER (Admin) ---
    @Transactional
    public Incidencia resolverIncidencia(Long incidenciaId,
                                         Authentication authentication,
                                         String resolucion,
                                         EstadoIncidencia estadoFinal) {
        Incidencia incidencia = incidenciaRepository.findById(incidenciaId)
                .orElseThrow(() -> new RuntimeException("Incidencia no encontrada"));

        if (estadoFinal == null) {
            throw new IllegalArgumentException("El estado final es obligatorio");
        }
        if (!ESTADOS_RESOLVER_PERMITIDOS.contains(estadoFinal)) {
            throw new IllegalArgumentException("Estado final no permitido para resolver incidencias: " + estadoFinal);
        }
        if (esEstadoTerminal(estadoFinal) && (resolucion == null || resolucion.isBlank())) {
            throw new IllegalArgumentException("La resolución es obligatoria para estados finales RESUELTA/DESESTIMADA");
        }

        validarTransicion(incidencia.getEstado(), estadoFinal);

        String email = authentication != null ? authentication.getName() : null;
        if (email == null) {
            throw new RuntimeException("Admin no autenticado");
        }

        Usuario admin = usuarioRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        incidencia.setResolucion(resolucion);
        incidencia.setAdmin(admin);
        EstadoIncidencia estadoAnterior = incidencia.getEstado();
        incidencia.setEstado(estadoFinal);
        incidencia.setFechaResolucion(LocalDateTime.now());

        Incidencia guardada = incidenciaRepository.save(incidencia);
        registrarEvento(guardada,
                estadoAnterior,
                estadoFinal,
                admin,
                "TRANSICION_ESTADO",
                resolucion);
        return guardada;
    }

    // --- 3. CONSULTAS ---
    public List<Incidencia> listarPendientes() {
        // Devuelve las ABIERTA o EN_REVISION
        return incidenciaRepository.findByEstadoIn(EnumSet.of(EstadoIncidencia.ABIERTA, EstadoIncidencia.EN_REVISION));
    }

    public List<Incidencia> listarPorPorte(Long porteId) {
        return incidenciaRepository.findByPorteId(porteId);
    }

    public List<Incidencia> listarVencidasSla() {
        return incidenciaRepository.findByEstadoInAndFechaLimiteSlaBefore(
                EnumSet.of(EstadoIncidencia.ABIERTA, EstadoIncidencia.EN_REVISION),
                LocalDateTime.now()
        );
    }

    public List<IncidenciaEvento> listarHistorial(Long incidenciaId) {
        if (!incidenciaRepository.existsById(incidenciaId)) {
            throw new RuntimeException("Incidencia no encontrada");
        }
        return incidenciaEventoRepository.findByIncidenciaIdOrderByFechaAsc(incidenciaId);
    }

    // --- 4. MÉTODOS ADICIONALES ---
    public List<Incidencia> listarTodas() {
        return incidenciaRepository.findAll();
    }

    public Incidencia obtenerPorId(Long id) {
        return incidenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incidencia no encontrada"));
    }

    private void validarTransicion(EstadoIncidencia estadoActual, EstadoIncidencia estadoFinal) {
        Set<EstadoIncidencia> permitidos = TRANSICIONES_VALIDAS.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(estadoFinal)) {
            throw new IncidenciaTransitionException(
                    "Transición no permitida: " + estadoActual + " -> " + estadoFinal
            );
        }
    }

    private boolean esEstadoTerminal(EstadoIncidencia estado) {
        return estado == EstadoIncidencia.RESUELTA || estado == EstadoIncidencia.DESESTIMADA;
    }

    private LocalDateTime calcularFechaLimiteSla(SeveridadIncidencia severidad,
                                                 PrioridadIncidencia prioridad,
                                                 LocalDateTime fechaBase) {
        int horas;
        if (severidad == SeveridadIncidencia.ALTA || prioridad == PrioridadIncidencia.ALTA) {
            horas = 24;
        } else if (severidad == SeveridadIncidencia.MEDIA || prioridad == PrioridadIncidencia.MEDIA) {
            horas = 72;
        } else {
            horas = 120;
        }
        return fechaBase.plusHours(horas);
    }

    private Usuario resolverActor(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return usuarioRepository.findByEmail(authentication.getName().toLowerCase()).orElse(null);
    }

    private void registrarEvento(Incidencia incidencia,
                                 EstadoIncidencia estadoAnterior,
                                 EstadoIncidencia estadoNuevo,
                                 Usuario actor,
                                 String accion,
                                 String comentario) {
        IncidenciaEvento evento = new IncidenciaEvento();
        evento.setIncidencia(incidencia);
        evento.setEstadoAnterior(estadoAnterior);
        evento.setEstadoNuevo(estadoNuevo);
        evento.setActor(actor);
        evento.setAccion(accion);
        evento.setComentario(comentario);
        evento.setFecha(LocalDateTime.now());
        incidenciaEventoRepository.save(evento);
    }
}
