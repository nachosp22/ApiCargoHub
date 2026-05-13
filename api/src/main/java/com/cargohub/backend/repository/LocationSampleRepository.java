package com.cargohub.backend.repository;

import com.cargohub.backend.entity.LocationSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationSampleRepository extends JpaRepository<LocationSample, Long> {
}
