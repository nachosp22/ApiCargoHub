package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    // Normalize email to lowercase to avoid case-sensitivity issues
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase() : null;
    }

    @Column(nullable = false)
    private String password; // Aquí guardaremos la contraseña encriptada

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol; // ADMIN, CONDUCTOR, CLIENTE, SUPERADMIN

    private boolean activo = true;

    @Column(name="fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    private LocalDateTime ultimoAcceso;

    private String tokenRecuperacion;
}