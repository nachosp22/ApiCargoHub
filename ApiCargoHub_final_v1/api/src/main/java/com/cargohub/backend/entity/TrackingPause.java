package com.cargohub.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "tracking_pauses",
        indexes = {
                @Index(name = "idx_tracking_pauses_session", columnList = "session_id")
        }
)
public class TrackingPause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TrackingSession session;

    @Column(nullable = false, length = 50)
    private String motivo;

    @Column(length = 500)
    private String nota;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
