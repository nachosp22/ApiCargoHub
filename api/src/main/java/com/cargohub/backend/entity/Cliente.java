package com.cargohub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private Usuario usuario;

    @Column(nullable = false)
    private String nombreEmpresa; // O Nombre completo si es particular

    @Column(unique = true, nullable = false)
    private String cif; // DNI o CIF

    private String direccionFiscal;
    private String telefono;

    @Column(nullable = false)
    private String emailContacto; // Email de contacto para notificaciones

    private String sector;
    
    // Normalize CIF/DNI to uppercase
    public void setCif(String cif) {
        this.cif = cif != null ? cif.toUpperCase() : null;
    }
    
    // Normalize email to lowercase
    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto != null ? emailContacto.toLowerCase() : null;
    }
}