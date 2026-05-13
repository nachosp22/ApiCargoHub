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

    /**
     * Sube una nueva foto de carga asociada a un porte específico.
     * Busca el porte por su identificador y, si no existe, lanza una excepción.
     * Asocia la foto al porte y registra la fecha de captura con el momento actual.
     *
     * @param porteId identificador del porte al que pertenece la foto
     * @param foto    entidad {@link FotoCarga} con los datos de la foto a guardar
     * @return la entidad {@link FotoCarga} persistida con su identificador asignado
     */
    @Transactional
    public FotoCarga subirFoto(Long porteId, FotoCarga foto) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        foto.setPorte(porte);
        foto.setFechaCaptura(LocalDateTime.now());
        return fotoCargaRepository.save(foto);
    }

    /**
     * Retorna la lista de fotos asociadas a un porte, ordenadas de más reciente a más antigua.
     * Valida que el porte exista antes de realizar la consulta.
     *
     * @param porteId identificador del porte cuyas fotos se desean listar
     * @return lista de {@link FotoCarga} ordenadas por fecha de captura descendente
     */
    public List<FotoCarga> listarFotosPorPorte(Long porteId) {
        if (!porteRepository.existsById(porteId)) {
            throw new RuntimeException("Porte no encontrado");
        }
        return fotoCargaRepository.findByPorteIdOrderByFechaCapturaDesc(porteId);
    }

    /**
     * Elimina una foto de carga asociada a un porte específico.
     * Verifica que la foto exista y pertenezca al porte indicado antes de proceder con la eliminación.
     *
     * @param porteId identificador del porte al que pertenece la foto
     * @param fotoId  identificador de la foto a eliminar
     */
    @Transactional
    public void eliminarFoto(Long porteId, Long fotoId) {
        if (!fotoCargaRepository.existsByIdAndPorteId(fotoId, porteId)) {
            throw new RuntimeException("Foto no encontrada para este porte");
        }
        fotoCargaRepository.deleteById(fotoId);
    }

    /**
     * Verifica si una foto de carga pertenece a un porte cuyo conductor es el indicado.
     *
     * @param fotoId     identificador de la foto a verificar
     * @param conductorId identificador del conductor cuyo porte se valida
     * @return {@code true} si la foto pertenece a un porte del conductor, {@code false} en caso contrario
     */
    public boolean isFotoOwnedByConductor(Long fotoId, Long conductorId) {
        return fotoCargaRepository.existsByIdAndPorteConductorId(fotoId, conductorId);
    }
}
