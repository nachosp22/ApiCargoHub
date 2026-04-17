package com.cargohub.backend.service;

import com.cargohub.backend.dto.tracking.EtaConfidence;
import com.cargohub.backend.dto.tracking.EtaEstimateResponse;
import com.cargohub.backend.dto.tracking.PorteTrackingResponse;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.repository.PorteRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class PorteTrackingService {

    private final PorteRepository porteRepository;
    private final EtaService etaService;

    public PorteTrackingService(PorteRepository porteRepository, EtaService etaService) {
        this.porteRepository = porteRepository;
        this.etaService = etaService;
    }

    public PorteTrackingResponse getTracking(Long porteId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new IllegalArgumentException("Porte no encontrado"));

        PorteTrackingResponse response = new PorteTrackingResponse();
        response.setStatus(porte.getEstado());

        // Origin / Destination
        response.setOriginLat(porte.getLatitudOrigen());
        response.setOriginLng(porte.getLongitudOrigen());
        response.setOriginName(porte.getOrigen());
        response.setDestinationLat(porte.getLatitudDestino());
        response.setDestinationLng(porte.getLongitudDestino());
        response.setDestinationName(porte.getDestino());

        Conductor conductor = porte.getConductor();
        if (conductor != null) {
            String name = conductor.getNombre();
            if (conductor.getApellidos() != null) {
                name += " " + conductor.getApellidos();
            }
            response.setDriverName(name.trim());

            // Live position from conductor
            if (conductor.getLatitudActual() != null && conductor.getLongitudActual() != null) {
                response.setDriverLat(conductor.getLatitudActual());
                response.setDriverLng(conductor.getLongitudActual());
                response.setSpeedKph(conductor.getVelocidadKphActual());
                response.setHeadingDeg(conductor.getRumboActualDeg());

                if (conductor.getUltimaActualizacionUbicacion() != null) {
                    response.setLastUpdate(
                            conductor.getUltimaActualizacionUbicacion().atOffset(ZoneOffset.UTC)
                    );
                }
            }

            // ETA — only for active portes with coordinates
            if (porte.getEstado() == EstadoPorte.EN_TRANSITO) {
                try {
                    EtaEstimateResponse eta = etaService.estimate(conductor.getId(), porteId);
                    response.setEtaMinutes(eta.getEtaMinutes());
                    response.setEtaConfidence(eta.getConfidence());
                } catch (Exception ignored) {
                    // ETA is best-effort — fleet feature may be disabled
                }
            }
        }

        // If no live driver position, fall back to origin coords for non-started portes
        if (response.getDriverLat() == null && porte.getEstado() != EstadoPorte.EN_TRANSITO) {
            response.setDriverLat(porte.getLatitudOrigen());
            response.setDriverLng(porte.getLongitudOrigen());
        }
        // For EN_TRANSITO without live data, fall back to destination (near arrival assumption)
        if (response.getDriverLat() == null) {
            response.setDriverLat(porte.getLatitudDestino());
            response.setDriverLng(porte.getLongitudDestino());
        }

        return response;
    }
}
