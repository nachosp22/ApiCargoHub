package com.cargohub.backend.service;

import com.cargohub.backend.entity.Notificacion;
import com.cargohub.backend.entity.enums.TipoNotificacion;
import com.cargohub.backend.repository.NotificacionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    /**
     * Create a notification for a specific user.
     */
    public Notificacion crear(Long usuarioId, String titulo, String mensaje,
                              TipoNotificacion tipo, Long referenciaId) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(tipo);
        notificacion.setReferenciaId(referenciaId);
        notificacion.setLeida(false);
        notificacion.setFechaCreacion(LocalDateTime.now());

        Notificacion saved = notificacionRepository.save(notificacion);
        log.info("Notificacion creada para usuario {}: {}", usuarioId, titulo);
        return saved;
    }

    /**
     * Get all notifications for a user, ordered by most recent first.
     */
    public List<Notificacion> getByUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    /**
     * Count unread notifications for a user.
     */
    public long countUnread(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public Notificacion markAsRead(Long notificacionId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new RuntimeException("Notificacion no encontrada"));
        notificacion.setLeida(true);
        return notificacionRepository.save(notificacion);
    }

    /**
     * Mark all notifications for a user as read.
     */
    @Transactional
    public void markAllAsRead(Long usuarioId) {
        List<Notificacion> unread = notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
        for (Notificacion n : unread) {
            if (!n.isLeida()) {
                n.setLeida(true);
                notificacionRepository.save(n);
            }
        }
        log.info("Todas las notificaciones marcadas como leidas para usuario {}", usuarioId);
    }
}
