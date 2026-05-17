package com.cargohub.backend.repository;

import com.cargohub.backend.entity.TrackingPause;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackingPauseRepository extends JpaRepository<TrackingPause, Long> {

    List<TrackingPause> findBySessionIdOrderByStartedAtDesc(Long sessionId);

    Optional<TrackingPause> findTopBySessionIdAndEndedAtIsNullOrderByStartedAtDesc(Long sessionId);
}
