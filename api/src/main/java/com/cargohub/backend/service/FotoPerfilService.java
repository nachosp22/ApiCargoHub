package com.cargohub.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
public class FotoPerfilService {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public String subirFoto(Long usuarioId, String base64Image) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "folder", "cargohub/perfiles",
                    "public_id", "usuario_" + usuarioId,
                    "overwrite", true,
                    "resource_type", "image"
            ));

            String url = (String) result.get("secure_url");
            usuario.setFotoUrl(url);
            usuarioRepository.save(usuario);
            return url;

        } catch (IOException e) {
            throw new RuntimeException("Error al subir foto a Cloudinary", e);
        }
    }

    @Transactional
    public void eliminarFoto(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        try {
            String publicId = "cargohub/perfiles/usuario_" + usuarioId;
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
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
}
