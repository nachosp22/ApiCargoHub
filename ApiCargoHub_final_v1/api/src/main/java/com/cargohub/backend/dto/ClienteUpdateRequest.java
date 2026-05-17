package com.cargohub.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class ClienteUpdateRequest {
    private String nombreEmpresa;
    private String cif;

    @Email
    private String emailContacto;

    private String telefono;

    @JsonAlias("direccion")
    private String direccionFiscal;

    private String ciudad;
    private String codigoPostal;
    private String pais;
}
