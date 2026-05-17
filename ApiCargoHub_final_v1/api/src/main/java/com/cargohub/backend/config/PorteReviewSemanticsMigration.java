package com.cargohub.backend.config;

import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.PorteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PorteReviewSemanticsMigration {

    private static final String OLD_NO_DRIVER_MESSAGE = "Vehículos compatibles encontrados pero sin conductores disponibles";
    private static final String OLD_NO_VEHICLE_MESSAGE = "No hay vehículo compatible (Peso/Volumen/Dimensiones)";
    private static final String NEW_NO_DRIVER_MESSAGE = "Porte validado correctamente, pero no hay conductores disponibles que se ajusten a los requisitos en este momento";
    private static final String NEW_NO_MATCH_MESSAGE = "Porte validado correctamente, pero no hay vehículos o conductores compatibles para estos requisitos en este momento";

    private final PorteRepository porteRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void normalizeLegacyReviewSemantics() {
        List<Porte> candidates = porteRepository.findByRevisionManualTrue();
        int updated = 0;

        for (Porte porte : candidates) {
            String motivo = porte.getMotivoRevision();
            if (!hasCompleteCargoData(porte) || motivo == null || motivo.isBlank()) {
                continue;
            }

            if (OLD_NO_DRIVER_MESSAGE.equals(motivo)) {
                porte.setRevisionManual(false);
                porte.setMotivoRevision(NEW_NO_DRIVER_MESSAGE);
                updated++;
                continue;
            }

            if (OLD_NO_VEHICLE_MESSAGE.equals(motivo)) {
                porte.setRevisionManual(false);
                porte.setMotivoRevision(NEW_NO_MATCH_MESSAGE);
                updated++;
            }
        }

        if (updated > 0) {
            porteRepository.saveAll(candidates);
            log.info("Normalized {} legacy porte review records to new semantics", updated);
        }
    }

    private boolean hasCompleteCargoData(Porte porte) {
        return porte.getPesoTotalKg() != null && porte.getPesoTotalKg() > 0
                && porte.getVolumenTotalM3() != null && porte.getVolumenTotalM3() > 0
                && porte.getLargoMaxPaquete() != null && porte.getLargoMaxPaquete() > 0
                && porte.getTipoVehiculoRequerido() != null;
    }
}
