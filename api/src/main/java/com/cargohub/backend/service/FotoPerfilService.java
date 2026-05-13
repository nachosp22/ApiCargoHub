package com.cargohub.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Map;

@Service
public class FotoPerfilService {

    private static final int MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Transactional
    public String subirFoto(Long usuarioId, String base64Image) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!isCloudinaryConfigured()) {
            throw new RuntimeException("Cloudinary no está configurado. Configurá CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET para subir fotos de perfil.");
        }

        String normalizedBase64 = normalizeBase64Payload(base64Image);

        try {
            byte[] imageBytes = Base64.getDecoder().decode(normalizedBase64);
            validateSize(imageBytes);
            validateSupportedFormat(imageBytes);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "folder", "cargohub/perfiles",
                    "public_id", "usuario_" + usuarioId,
                    "overwrite", true,
                    "resource_type", "image"
            ));

            String url = (String) result.get("secure_url");
            if (url == null || url.isBlank()) {
                throw new RuntimeException("Cloudinary no devolvió una URL válida para la foto subida");
            }

            usuario.setFotoUrl(url);
            usuarioRepository.save(usuario);
            return url;

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("La imagen base64 no es válida", e);
        } catch (IOException e) {
            throw new RuntimeException("Error al subir foto a Cloudinary. Verificá configuración y conectividad del servicio.", e);
        }
    }

    @Transactional
    public void eliminarFoto(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String currentUrl = usuario.getFotoUrl();
        if (currentUrl == null || currentUrl.isBlank()) {
            return;
        }

        if (currentUrl.startsWith("data:image/")) {
            // Legacy data URL: clean DB only (do not call Cloudinary)
            usuario.setFotoUrl(null);
            usuarioRepository.save(usuario);
            return;
        }

        try {
            String publicId = extractCloudinaryPublicId(currentUrl, usuarioId);
            if (publicId != null && !publicId.isBlank()) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar foto de Cloudinary", e);
        }

        usuario.setFotoUrl(null);
        usuarioRepository.save(usuario);
    }

    public String obtenerFotoUrl(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return usuario.getFotoUrl();
    }

    private String normalizeBase64Payload(String base64Image) {
        if (base64Image == null) {
            throw new RuntimeException("La imagen base64 es obligatoria");
        }

        String trimmed = base64Image.trim();
        if (trimmed.isBlank()) {
            throw new RuntimeException("La imagen base64 es obligatoria");
        }

        int commaIndex = trimmed.indexOf(',');
        if (trimmed.startsWith("data:image/") && commaIndex > -1 && commaIndex + 1 < trimmed.length()) {
            return trimmed.substring(commaIndex + 1);
        }

        return trimmed;
    }

    private boolean isCloudinaryConfigured() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    private void validateSize(byte[] imageBytes) {
        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw new RuntimeException("La imagen supera el tamaño máximo permitido de 2MB");
        }
    }

    private void validateSupportedFormat(byte[] imageBytes) {
        if (isJpeg(imageBytes) || isPng(imageBytes) || isGif(imageBytes) || isWebp(imageBytes)) {
            return;
        }
        throw new RuntimeException("Formato de imagen no soportado. Usá JPG, PNG, GIF o WEBP");
    }

    private boolean isJpeg(byte[] imageBytes) {
        return imageBytes.length >= 3
                && (imageBytes[0] & 0xFF) == 0xFF
                && (imageBytes[1] & 0xFF) == 0xD8
                && (imageBytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] imageBytes) {
        return imageBytes.length >= 8
                && (imageBytes[0] & 0xFF) == 0x89
                && imageBytes[1] == 0x50
                && imageBytes[2] == 0x4E
                && imageBytes[3] == 0x47
                && imageBytes[4] == 0x0D
                && imageBytes[5] == 0x0A
                && imageBytes[6] == 0x1A
                && imageBytes[7] == 0x0A;
    }

    private boolean isGif(byte[] imageBytes) {
        return imageBytes.length >= 6
                && imageBytes[0] == 0x47
                && imageBytes[1] == 0x49
                && imageBytes[2] == 0x46
                && imageBytes[3] == 0x38
                && (imageBytes[4] == 0x37 || imageBytes[4] == 0x39)
                && imageBytes[5] == 0x61;
    }

    private boolean isWebp(byte[] imageBytes) {
        return imageBytes.length >= 12
                && imageBytes[0] == 0x52
                && imageBytes[1] == 0x49
                && imageBytes[2] == 0x46
                && imageBytes[3] == 0x46
                && imageBytes[8] == 0x57
                && imageBytes[9] == 0x45
                && imageBytes[10] == 0x42
                && imageBytes[11] == 0x50;
    }

    private String extractCloudinaryPublicId(String url, Long usuarioId) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || !path.contains("/upload/")) {
                return "cargohub/perfiles/usuario_" + usuarioId;
            }

            String afterUpload = path.substring(path.indexOf("/upload/") + "/upload/".length());
            String[] segments = afterUpload.split("/");
            int startIndex = 0;

            if (segments.length > 0 && segments[0].matches("v\\d+")) {
                startIndex = 1;
            }

            if (segments.length - startIndex <= 0) {
                return "cargohub/perfiles/usuario_" + usuarioId;
            }

            StringBuilder publicIdBuilder = new StringBuilder();
            for (int i = startIndex; i < segments.length; i++) {
                String segment = segments[i];
                if (i == segments.length - 1) {
                    int dotIndex = segment.lastIndexOf('.');
                    if (dotIndex > 0) {
                        segment = segment.substring(0, dotIndex);
                    }
                }

                if (publicIdBuilder.length() > 0) {
                    publicIdBuilder.append('/');
                }
                publicIdBuilder.append(segment);
            }

            String parsedPublicId = publicIdBuilder.toString();
            return parsedPublicId.isBlank() ? "cargohub/perfiles/usuario_" + usuarioId : parsedPublicId;
        } catch (URISyntaxException e) {
            return "cargohub/perfiles/usuario_" + usuarioId;
        }
    }
}
