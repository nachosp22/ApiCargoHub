package com.cargohub.backend.dto;

import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehiculoUpsertRequest {
    private Long id;

    @Size(min = 4, max = 16)
    private String matricula;

    private String marca;
    private String modelo;
    private TipoVehiculo tipo;
    private EstadoVehiculo estado;
    private Integer capacidadCargaKg;
    private Integer largoUtilMm;
    private Integer anchoUtilMm;
    private Integer altoUtilMm;

    @Valid
    private ConductorRef conductor;

    @Data
    public static class ConductorRef {
        private Long id;
    }
}
