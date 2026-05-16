package com.cargohub.backend.config;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import com.cargohub.backend.repository.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder mínimo para demo/defensa.
 * Solo crea cuentas de usuario y vehículos. NO crea portes, incidencias ni facturas.
 * Activado con: APP_SEED_ENABLED=true
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final String DEFAULT_CLIENT_PASSWORD = "Cliente123!";
    private static final String DEFAULT_DRIVER_PASSWORD = "Conductor123!";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ConductorRepository conductorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(UsuarioRepository usuarioRepository,
                         ClienteRepository clienteRepository,
                         ConductorRepository conductorRepository,
                         VehiculoRepository vehiculoRepository,
                         PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.conductorRepository = conductorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[seed] Iniciando seed mínimo (solo cuentas y vehículos)");

        // ── Admin y Superadmin ──
        seedSuperadmin();
        seedAdmin();

        // ── Clientes (5) ──
        List<Cliente> clientes = seedClientes();

        // ── Conductores (5) + vehículos ──
        List<Conductor> conductores = seedConductores();
        seedVehiculos(conductores);

        log.info("[seed] Completado. clientes={}, conductores={}, vehículos={}",
                clientes.size(), conductores.size(), vehiculoRepository.count());
    }

    // ═══════════════════════════════════════════════════════════════
    // Admin / Superadmin
    // ═══════════════════════════════════════════════════════════════

    private void seedSuperadmin() {
        Usuario u = upsertUsuario("superadmin@cargohub.local", "Super123!", RolUsuario.SUPERADMIN, true);
        u.setNombre("Super Admin");
        usuarioRepository.save(u);
    }

    private void seedAdmin() {
        Usuario u = upsertUsuario("admin@cargohub.local", "Admin123!", RolUsuario.ADMIN, true);
        u.setNombre("Admin");
        usuarioRepository.save(u);
    }

    // ═══════════════════════════════════════════════════════════════
    // Clientes (5)
    // ═══════════════════════════════════════════════════════════════

    private List<Cliente> seedClientes() {
        List<ClienteSeed> seeds = List.of(
                new ClienteSeed("logistics.express@cargohub.local", "Logistics Express S.L.", "B00000001", "911000001", "Calle Atocha 100, Madrid", "Retail"),
                new ClienteSeed("trans.iberica@cargohub.local", "Trans Ibérica S.A.", "A00000002", "932000002", "Av. Diagonal 220, Barcelona", "Distribución"),
                new ClienteSeed("envios.rapidos@cargohub.local", "Envíos Rápidos S.L.", "B00000003", "961000003", "C/ Colón 45, Valencia", "Paquetería"),
                new ClienteSeed("frio.norte@cargohub.local", "Frío Norte Logística", "B00000004", "944000004", "Gran Vía 20, Bilbao", "Alimentación"),
                new ClienteSeed("andalucia.supply@cargohub.local", "Andalucía Supply", "B00000006", "954000006", "C/ Sierpes 80, Sevilla", "Industrial")
        );

        List<Cliente> out = new ArrayList<>();
        for (ClienteSeed s : seeds) {
            Usuario usuario = upsertUsuario(s.email, DEFAULT_CLIENT_PASSWORD, RolUsuario.CLIENTE, true);
            Cliente c = clienteRepository.findByUsuarioEmail(s.email).orElseGet(Cliente::new);
            c.setUsuario(usuario);
            c.setNombreEmpresa(s.nombreEmpresa);
            c.setCif(s.cif);
            c.setTelefono(s.telefono);
            c.setDireccionFiscal(s.direccionFiscal);
            c.setEmailContacto(s.email);
            c.setSector(s.sector);
            out.add(clienteRepository.save(c));
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════
    // Conductores (5)
    // ═══════════════════════════════════════════════════════════════

    private List<Conductor> seedConductores() {
        List<ConductorSeed> seeds = List.of(
                new ConductorSeed("juan.perez@cargohub.local", "Juan", "Pérez", "11111111A", "612000001", "Madrid", 40.4168, -3.7038),
                new ConductorSeed("maria.lopez@cargohub.local", "María", "López", "22222222B", "612000002", "Barcelona", 41.3874, 2.1686),
                new ConductorSeed("carlos.ruiz@cargohub.local", "Carlos", "Ruiz", "33333333C", "612000003", "Valencia", 39.4699, -0.3763),
                new ConductorSeed("laura.hernandez@cargohub.local", "Laura", "Hernández", "66666666F", "612000006", "Zaragoza", 41.6488, -0.8891),
                new ConductorSeed("elena.morales@cargohub.local", "Elena", "Morales", "88888888H", "612000008", "Murcia", 37.9922, -1.1307)
        );

        List<Conductor> out = new ArrayList<>();
        for (ConductorSeed s : seeds) {
            Usuario usuario = upsertUsuario(s.email, DEFAULT_DRIVER_PASSWORD, RolUsuario.CONDUCTOR, true);
            Conductor c = conductorRepository.findByUsuarioEmail(s.email).orElseGet(Conductor::new);
            c.setUsuario(usuario);
            c.setNombre(s.nombre);
            c.setApellidos(s.apellidos);
            c.setDni(s.dni);
            c.setTelefono(s.telefono);
            c.setCiudadBase(s.ciudadBase);
            c.setLatitudBase(s.latitudBase);
            c.setLongitudBase(s.longitudBase);
            c.setLatitudActual(s.latitudBase);
            c.setLongitudActual(s.longitudBase);
            c.setUltimaActualizacionUbicacion(LocalDateTime.now().minusHours(2));
            c.setRadioAccionKm(300);
            c.setDiasLaborables("1,2,3,4,5");
            c.setDisponible(true);
            c.setBuscarRetorno(true);
            out.add(conductorRepository.save(c));
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════
    // Vehículos (1 por conductor, tipos variados)
    // ═══════════════════════════════════════════════════════════════

    private void seedVehiculos(List<Conductor> conductores) {
        List<VehiculoSeed> seeds = List.of(
                new VehiculoSeed("1234ABC", "Iveco", "Daily 35S14", TipoVehiculo.FURGONETA, 1500, 4200, 1800, 1900, "11111111A"),
                new VehiculoSeed("5678DEF", "Mercedes-Benz", "Atego 1224", TipoVehiculo.RIGIDO, 6000, 7200, 2400, 2500, "22222222B"),
                new VehiculoSeed("9012GHI", "Volvo", "FH 500", TipoVehiculo.TRAILER, 24000, 13600, 2480, 2700, "33333333C"),
                new VehiculoSeed("3456JKL", "Renault", "Master L3H2", TipoVehiculo.FURGONETA, 1200, 3700, 1765, 1880, "66666666F"),
                new VehiculoSeed("6789STU", "Iveco", "Eurocargo 120E25", TipoVehiculo.RIGIDO, 7500, 8000, 2400, 2500, "88888888H")
        );

        for (VehiculoSeed s : seeds) {
            Conductor conductor = findConductorByDni(conductores, s.conductorDni);
            Optional<Vehiculo> existente = vehiculoRepository.findByMatricula(s.matricula);
            Vehiculo v = existente.orElseGet(Vehiculo::new);
            v.setMatricula(s.matricula);
            v.setMarca(s.marca);
            v.setModelo(s.modelo);
            v.setTipo(s.tipo);
            v.setEstado(EstadoVehiculo.DISPONIBLE);
            v.setCapacidadCargaKg(s.capacidadCargaKg);
            v.setLargoUtilMm(s.largoUtilMm);
            v.setAnchoUtilMm(s.anchoUtilMm);
            v.setAltoUtilMm(s.altoUtilMm);
            v.setConductor(conductor);
            vehiculoRepository.save(v);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    private Usuario upsertUsuario(String email, String plainPassword, RolUsuario rol, boolean activo) {
        String normalized = email.toLowerCase();
        Usuario u = usuarioRepository.findByEmail(normalized).orElseGet(Usuario::new);
        u.setEmail(normalized);
        if (u.getPassword() == null || !passwordEncoder.matches(plainPassword, u.getPassword())) {
            u.setPassword(passwordEncoder.encode(plainPassword));
        }
        u.setRol(rol);
        u.setActivo(activo);
        if (u.getFechaRegistro() == null) {
            u.setFechaRegistro(LocalDateTime.now());
        }
        return usuarioRepository.save(u);
    }

    private Conductor findConductorByDni(List<Conductor> conductores, String dni) {
        return conductores.stream()
                .filter(c -> dni.equalsIgnoreCase(c.getDni()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Conductor no encontrado: " + dni));
    }

    // ═══════════════════════════════════════════════════════════════
    // Records
    // ═══════════════════════════════════════════════════════════════

    private record ClienteSeed(String email, String nombreEmpresa, String cif,
                               String telefono, String direccionFiscal, String sector) {}

    private record ConductorSeed(String email, String nombre, String apellidos, String dni,
                                 String telefono, String ciudadBase, Double latitudBase, Double longitudBase) {}

    private record VehiculoSeed(String matricula, String marca, String modelo, TipoVehiculo tipo,
                                Integer capacidadCargaKg, Integer largoUtilMm, Integer anchoUtilMm,
                                Integer altoUtilMm, String conductorDni) {}
}
