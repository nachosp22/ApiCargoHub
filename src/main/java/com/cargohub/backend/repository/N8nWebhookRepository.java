package com.cargohub.backend.repository;

import com.cargohub.backend.entity.N8nWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface N8nWebhookRepository extends JpaRepository<N8nWebhook, Long> {

    // Encuentra todas las llamadas al webhook para un porte específico
    List<N8nWebhook> findByPorteId(Long porteId);

    // Encuentra todas las llamadas al webhook exitosas
    List<N8nWebhook> findBySuccessTrue();

    // Encuentra todas las llamadas al webhook fallidas
    List<N8nWebhook> findBySuccessFalse();
}
