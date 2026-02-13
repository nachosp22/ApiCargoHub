package com.cargohub.backend.repository;

import com.cargohub.backend.entity.N8nWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface N8nWebhookRepository extends JpaRepository<N8nWebhook, Long> {

    // Find all webhook calls for a specific porte
    List<N8nWebhook> findByPorteId(Long porteId);

    // Find all successful webhook calls
    List<N8nWebhook> findBySuccessTrue();

    // Find all failed webhook calls
    List<N8nWebhook> findBySuccessFalse();
}
