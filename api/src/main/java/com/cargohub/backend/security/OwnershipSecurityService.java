package com.cargohub.backend.security;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.IncidenciaRepository;
import com.cargohub.backend.repository.PorteRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("ownership")
public class OwnershipSecurityService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ConductorRepository conductorRepository;
    private final PorteRepository porteRepository;
    private final BloqueoAgendaRepository bloqueoAgendaRepository;
    private final IncidenciaRepository incidenciaRepository;

    public OwnershipSecurityService(UsuarioRepository usuarioRepository,
                                    ClienteRepository clienteRepository,
                                    ConductorRepository conductorRepository,
                                    PorteRepository porteRepository,
                                    BloqueoAgendaRepository bloqueoAgendaRepository,
                                    IncidenciaRepository incidenciaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.conductorRepository = conductorRepository;
        this.porteRepository = porteRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.incidenciaRepository = incidenciaRepository;
    }

    public boolean canAccessCliente(Authentication authentication, Long clienteId) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (!hasRole(authentication, RolUsuario.CLIENTE)) {
            return false;
        }
        Long currentClienteId = resolveClienteId(authentication);
        return currentClienteId != null && currentClienteId.equals(clienteId);
    }

    public boolean canAccessConductor(Authentication authentication, Long conductorId) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (!hasRole(authentication, RolUsuario.CONDUCTOR)) {
            return false;
        }
        Long currentConductorId = resolveConductorId(authentication);
        return currentConductorId != null && currentConductorId.equals(conductorId);
    }

    public boolean canAccessPorte(Authentication authentication, Long porteId) {
        if (isAdmin(authentication)) {
            return true;
        }

        if (hasRole(authentication, RolUsuario.CLIENTE)) {
            Long currentClienteId = resolveClienteId(authentication);
            return currentClienteId != null && porteRepository.existsByIdAndClienteId(porteId, currentClienteId);
        }

        if (hasRole(authentication, RolUsuario.CONDUCTOR)) {
            Long currentConductorId = resolveConductorId(authentication);
            return currentConductorId != null && porteRepository.existsByIdAndConductorId(porteId, currentConductorId);
        }

        return false;
    }

    public boolean canDeleteBloqueo(Authentication authentication, Long bloqueoId) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (!hasRole(authentication, RolUsuario.CONDUCTOR)) {
            return false;
        }

        Long currentConductorId = resolveConductorId(authentication);
        return currentConductorId != null && bloqueoAgendaRepository.existsByIdAndConductorId(bloqueoId, currentConductorId);
    }

    public boolean canAccessIncidencia(Authentication authentication, Long incidenciaId) {
        if (isAdmin(authentication)) {
            return true;
        }

        if (hasRole(authentication, RolUsuario.CLIENTE)) {
            Long currentClienteId = resolveClienteId(authentication);
            return currentClienteId != null && incidenciaRepository.existsByIdAndPorteClienteId(incidenciaId, currentClienteId);
        }

        if (hasRole(authentication, RolUsuario.CONDUCTOR)) {
            Long currentConductorId = resolveConductorId(authentication);
            return currentConductorId != null && incidenciaRepository.existsByIdAndPorteConductorId(incidenciaId, currentConductorId);
        }

        return false;
    }

    private boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, RolUsuario.ADMIN) || hasRole(authentication, RolUsuario.SUPERADMIN);
    }

    private boolean hasRole(Authentication authentication, RolUsuario rol) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        String expected = "ROLE_" + rol.name();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (expected.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Long resolveClienteId(Authentication authentication) {
        String email = resolveEmail(authentication);
        if (email == null) {
            return null;
        }

        return usuarioRepository.findByEmail(email)
                .flatMap(usuario -> clienteRepository.findByUsuarioEmail(usuario.getEmail()))
                .map(Cliente::getId)
                .orElse(null);
    }

    private Long resolveConductorId(Authentication authentication) {
        String email = resolveEmail(authentication);
        if (email == null) {
            return null;
        }

        return usuarioRepository.findByEmail(email)
                .flatMap(usuario -> conductorRepository.findByUsuarioEmail(usuario.getEmail()))
                .map(Conductor::getId)
                .orElse(null);
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return authentication.getName().toLowerCase();
    }
}
