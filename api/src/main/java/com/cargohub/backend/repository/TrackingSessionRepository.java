package com.cargohub.backend.repository;

import com.cargohub.backend.entity.TrackingSession;
import com.cargohub.backend.entity.enums.TrackingSessionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackingSessionRepository extends JpaRepository<TrackingSession, Long> {
    Optional<TrackingSession> findFirstByConductorIdAndStatusOrderByStartedAtDesc(Long conductorId, TrackingSessionStatus status);
}
