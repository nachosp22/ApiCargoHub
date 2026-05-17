package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.TrackingSessionPhase;
import com.cargohub.backend.entity.enums.TrackingSessionStatus;
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
        name = "tracking_sessions",
        indexes = {
                @Index(name = "idx_tracking_sessions_conductor_status_started_at", columnList = "conductor_id, status, startedAt")
        }
)
public class TrackingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conductor_id", nullable = false)
    private Conductor conductor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "porte_id")
    private Porte porte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingSessionStatus status = TrackingSessionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingSessionPhase currentPhase = TrackingSessionPhase.IDLE;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime lastSampleAt;
}
