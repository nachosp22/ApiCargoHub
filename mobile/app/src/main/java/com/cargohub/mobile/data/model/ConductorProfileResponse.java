package com.cargohub.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ConductorProfileResponse {

    private Long id;
    private String nombre;
    private String apellidos;
    private String telefono;
    private String dni;
    private String ciudadBase;

    @SerializedName("usuario")
    private UsuarioProfile usuarioProfile;

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        String firstName = nombre != null ? nombre.trim() : "";
        String lastName = apellidos != null ? apellidos.trim() : "";
        if (firstName.isEmpty() && lastName.isEmpty()) {
            return null;
        }
        if (lastName.isEmpty()) {
            return firstName;
        }
        if (firstName.isEmpty()) {
            return lastName;
        }
        return firstName + " " + lastName;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDni() {
        return dni;
    }

    public String getCiudadBase() {
        return ciudadBase;
    }

    public String getEmail() {
        if (usuarioProfile == null) {
            return null;
        }
        return usuarioProfile.email;
    }

    private static class UsuarioProfile {
        private String email;
    }
}
