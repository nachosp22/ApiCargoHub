package com.cargohub.backend.repository;

import com.cargohub.backend.entity.BloqueoRecurrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloqueoRecurrenteRepository extends JpaRepository<BloqueoRecurrente, Long> {

    List<BloqueoRecurrente> findByConductorId(Long conductorId);

    Optional<BloqueoRecurrente> findByConductorIdAndDiaSemana(Long conductorId, int diaSemana);

    void deleteByConductorId(Long conductorId);
}
