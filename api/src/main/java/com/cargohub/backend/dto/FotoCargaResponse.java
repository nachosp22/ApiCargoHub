package com.cargohub.backend.dto;

import com.cargohub.backend.entity.FotoCarga;
import com.cargohub.backend.entity.enums.TipoFotoCarga;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoCargaResponse {

    private Long id;
    private Long porteId;
    private TipoFotoCarga tipo;
    private String fotoBase64;
    private String descripcion;
    private LocalDateTime fechaCaptura;

    public static FotoCargaResponse fromEntity(FotoCarga foto) {
        FotoCargaResponse response = new FotoCargaResponse();
        response.setId(foto.getId());
        response.setPorteId(foto.getPorte() != null ? foto.getPorte().getId() : null);
        response.setTipo(foto.getTipo());
        response.setFotoBase64(foto.getFotoBase64());
        response.setDescripcion(foto.getDescripcion());
        response.setFechaCaptura(foto.getFechaCaptura());
        return response;
    }
}
