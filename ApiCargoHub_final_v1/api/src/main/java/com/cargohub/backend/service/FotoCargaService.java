package com.cargohub.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cargohub.backend.entity.FotoCarga;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.FotoCargaRepository;
import com.cargohub.backend.repository.PorteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class FotoCargaService {

    private static final int MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB para fotos de carga

    @Autowired
    private FotoCargaRepository fotoCargaRepository;

    @Autowired
    private PorteRepository porteRepository;

    @Autowired
    private Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    /**
     * Sube una nueva foto de carga asociada a un porte específico.
     * La imagen se envía en base64, se normaliza y se sube a Cloudinary.
     * Solo se almacena la URL resultante en la base de datos.
     *
     * @param porteId   identificador del porte al que pertenece la foto
     * @param foto      entidad {@link FotoCarga} con tipo y descripción
     * @param base64Image imagen codificada en base64, con o sin prefijo data:image/...
     * @return la entidad {@link FotoCarga} persistida con su identificador asignado y la URL de Cloudinary
     */
    @Transactional
    public FotoCarga subirFoto(Long porteId, FotoCarga foto, String base64Image) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        if (!isCloudinaryConfigured()) {
            throw new RuntimeException("Cloudinary no está configurado. Configurá CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET para subir fotos de carga.");
        }

        String normalizedBase64 = normalizeBase64Payload(base64Image);

        try {
            byte[] imageBytes = Base64.getDecoder().decode(normalizedBase64);

            if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                throw new RuntimeException("La imagen supera el tamaño máximo permitido de 5MB");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "folder", "cargohub/cargas",
                    "public_id", "porte_" + porteId + "_" + System.currentTimeMillis(),
                    "overwrite", false,
                    "resource_type", "image"
            ));

            String url = (String) result.get("secure_url");
            if (url == null || url.isBlank()) {
                throw new RuntimeException("Cloudinary no devolvió una URL válida para la foto de carga");
            }

            foto.setFotoUrl(url);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("La imagen base64 no es válida", e);
        } catch (IOException e) {
            throw new RuntimeException("Error al subir foto a Cloudinary. Verificá configuración y conectividad del servicio.", e);
        }

        foto.setPorte(porte);
        foto.setFechaCaptura(LocalDateTime.now());
        return fotoCargaRepository.save(foto);
    }

    /**
     * Normaliza la cadena base64 eliminando espacios y el prefijo data:image/... si estuviera presente.
     */
    private String normalizeBase64Payload(String base64Image) {
        if (base64Image == null || base64Image.trim().isBlank()) {
            throw new RuntimeException("La imagen base64 es obligatoria");
        }

        String trimmed = base64Image.trim();
        int commaIndex = trimmed.indexOf(',');
        if (trimmed.startsWith("data:image/") && commaIndex > -1 && commaIndex + 1 < trimmed.length()) {
            return trimmed.substring(commaIndex + 1);
        }

        return trimmed;
    }

    /**
     * Verifica que las credenciales de Cloudinary estén configuradas.
     */
    private boolean isCloudinaryConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
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
        FotoCarga foto = fotoCargaRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto no encontrada para este porte"));

        if (!foto.getPorte().getId().equals(porteId)) {
            throw new RuntimeException("La foto no pertenece al porte indicado");
        }

        // Eliminar de Cloudinary si la URL pertenece a Cloudinary
        String url = foto.getFotoUrl();
        if (url != null && isCloudinaryConfigured() && url.contains("cloudinary.com")) {
            try {
                String publicId = extractCloudinaryPublicId(url);
                if (publicId != null && !publicId.isBlank()) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                }
            } catch (IOException e) {
                // Si falla Cloudinary, seguimos adelante: la BBDD queda limpia igual
            }
        }

        fotoCargaRepository.delete(foto);
    }

    /**
     * Extrae el public_id de una URL de Cloudinary.
     */
    private String extractCloudinaryPublicId(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String path = uri.getPath();
            if (path == null || !path.contains("/upload/")) {
                return null;
            }
            String afterUpload = path.substring(path.indexOf("/upload/") + "/upload/".length());
            int dotIndex = afterUpload.lastIndexOf('.');
            if (dotIndex > -1) {
                return afterUpload.substring(0, dotIndex);
            }
            return afterUpload;
        } catch (java.net.URISyntaxException e) {
            return null;
        }
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
