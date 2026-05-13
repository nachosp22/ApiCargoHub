package com.cargohub.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteCreateRequest {

    @NotBlank
    private String nombreEmpresa;

    @NotBlank
    private String cif;

    @NotBlank
    @Email
    private String emailContacto;

    private String telefono;

    @JsonAlias("direccion")
    private String direccionFiscal;

    private String ciudad;
    private String codigoPostal;
    private String pais;
}
