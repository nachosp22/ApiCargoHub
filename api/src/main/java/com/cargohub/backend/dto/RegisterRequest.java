package com.cargohub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    // --- Account fields (Usuario) ---

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    /**
     * Role selector: "CLIENTE" or "CONDUCTOR".
     * Defaults to CLIENTE if not provided.
     */
    private String rol;

    // --- Company fields (Cliente) — required when rol=CLIENTE ---

    private String nombreEmpresa;

    private String cif;

    private String direccionFiscal;

    private String telefono;

    private String emailContacto;

    private String sector;

    // --- Conductor fields — required when rol=CONDUCTOR ---

    private String nombre;

    private String apellidos;

    private String dni;

    private String ciudadBase;

    private String carnetConducir;

    private Integer experienciaAnios;
}
