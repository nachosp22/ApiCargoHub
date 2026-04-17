package com.cargohub.backend.service;

import com.cargohub.backend.entity.FotoCarga;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.FotoCargaRepository;
import com.cargohub.backend.repository.PorteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FotoCargaService {

    @Autowired
    private FotoCargaRepository fotoCargaRepository;

    @Autowired
    private PorteRepository porteRepository;

    @Transactional
    public FotoCarga subirFoto(Long porteId, FotoCarga foto) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        foto.setPorte(porte);
        foto.setFechaCaptura(LocalDateTime.now());
        return fotoCargaRepository.save(foto);
    }

    public List<FotoCarga> listarFotosPorPorte(Long porteId) {
        if (!porteRepository.existsById(porteId)) {
            throw new RuntimeException("Porte no encontrado");
        }
        return fotoCargaRepository.findByPorteIdOrderByFechaCapturaDesc(porteId);
    }

    @Transactional
    public void eliminarFoto(Long porteId, Long fotoId) {
        if (!fotoCargaRepository.existsByIdAndPorteId(fotoId, porteId)) {
            throw new RuntimeException("Foto no encontrada para este porte");
        }
        fotoCargaRepository.deleteById(fotoId);
    }

    public boolean isFotoOwnedByConductor(Long fotoId, Long conductorId) {
        return fotoCargaRepository.existsByIdAndPorteConductorId(fotoId, conductorId);
    }
}
