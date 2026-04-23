package com.cargohub.backend.repository;

import com.cargohub.backend.entity.CargoAnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CargoAnalysisLogRepository extends JpaRepository<CargoAnalysisLog, Long> {
}
