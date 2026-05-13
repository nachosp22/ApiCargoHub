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
     * Crea una nueva notificación para un usuario específico.
     *
     * @param usuarioId    identificador del usuario destinatario
     * @param titulo       título de la notificación
     * @param mensaje      contenido del mensaje
     * @param tipo         tipo de notificación a enviar
     * @param referenciaId identificador del recurso relacionado con la notificación
     * @return la notificación persistida con su identificador asignado
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
     * Obtiene todas las notificaciones de un usuario, ordenadas de la más
     * reciente a la más antigua.
     *
     * @param usuarioId identificador del usuario cuyas notificaciones se desean obtener
     * @return lista de notificaciones del usuario ordenadas por fecha de creación descendente
     */
    public List<Notificacion> getByUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    /**
     * Cuenta la cantidad de notificaciones no leídas de un usuario.
     *
     * @param usuarioId identificador del usuario
     * @return número de notificaciones pendientes de leer
     */
    public long countUnread(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    /**
     * Marca una notificación específica como leída.
     *
     * @param notificacionId identificador de la notificación a marcar
     * @return la notificación actualizada con el estado de lectura
     * @throws RuntimeException si la notificación no existe
     */
    @Transactional
    public Notificacion markAsRead(Long notificacionId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new RuntimeException("Notificacion no encontrada"));
        notificacion.setLeida(true);
        return notificacionRepository.save(notificacion);
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas.
     *
     * @param usuarioId identificador del usuario cuyas notificaciones se marcarán
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
