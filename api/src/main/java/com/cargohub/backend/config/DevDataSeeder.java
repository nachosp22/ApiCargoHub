package com.cargohub.backend.config;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.repository.PorteRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final String CLIENT_PW = "Cliente123!";
    private static final String DRIVER_PW = "Conductor123!";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ConductorRepository conductorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PorteRepository porteRepository;
    private final FacturaRepository facturaRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(UsuarioRepository ur, ClienteRepository cr, ConductorRepository cor,
                         VehiculoRepository vr, PorteRepository pr, FacturaRepository fr,
                         PasswordEncoder pe) {
        this.usuarioRepository = ur; this.clienteRepository = cr;
        this.conductorRepository = cor; this.vehiculoRepository = vr;
        this.porteRepository = pr; this.facturaRepository = fr;
        this.passwordEncoder = pe;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        log.info("[seed] Iniciando seed demo Asturias");

        seedSuperadmin();
        seedAdmin();

        List<Cliente> clientes = seedClientes();
        List<Conductor> conductores = seedConductores();
        seedVehiculos(conductores);
        seedPortes(clientes, conductores);

        log.info("[seed] OK. clientes={} conductores={} vehiculos={} portes={}",
                clientes.size(), conductores.size(),
                vehiculoRepository.count(), porteRepository.count());
    }

    // ═══ Admin / Superadmin ═══

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

    // ═══ Clientes (5) ═══

    private List<Cliente> seedClientes() {
        List<ClienteSeed> seeds = List.of(
            new ClienteSeed("logistics.express@cargohub.local", "Logistics Express S.L.", "B00000001",
                            "911000001", "Calle Atocha 100, Madrid", "Retail"),
            new ClienteSeed("trans.iberica@cargohub.local", "Trans Ibérica S.A.", "A00000002",
                            "932000002", "Av. Diagonal 220, Barcelona", "Distribución"),
            new ClienteSeed("asturias.industrial@cargohub.local", "Asturias Industrial S.L.",
                            "B33000001", "985100001", "C/ Uría 45, Oviedo", "Industrial"),
            new ClienteSeed("gijon.maritimo@cargohub.local", "Gijón Marítimo S.A.",
                            "A33000002", "985200002", "Av. de la Costa 12, Gijón", "Exportación"),
            new ClienteSeed("sidreria.tradicion@cargohub.local", "Sidrería Tradición S.L.",
                            "B33000003", "985300003", "C/ Gascona 8, Oviedo", "Alimentación")
        );
        List<Cliente> out = new ArrayList<>();
        for (ClienteSeed s : seeds) {
            Usuario u = upsertUsuario(s.email, CLIENT_PW, RolUsuario.CLIENTE, true);
            Cliente c = clienteRepository.findByUsuarioEmail(s.email).orElseGet(Cliente::new);
            c.setUsuario(u); c.setNombreEmpresa(s.nom); c.setCif(s.cif);
            c.setTelefono(s.tel); c.setDireccionFiscal(s.dir); c.setEmailContacto(s.email); c.setSector(s.sec);
            out.add(clienteRepository.save(c));
        }
        return out;
    }

    // ═══ Conductores (5) — foco Asturias ═══

    private List<Conductor> seedConductores() {
        List<ConductorSeed> seeds = List.of(
            new ConductorSeed("juan.perez@cargohub.local", "Juan", "Pérez", "11111111A",
                              "612000001", "Madrid", 40.4168, -3.7038),
            new ConductorSeed("pedro.oviedo@cargohub.local", "Pedro", "Fernández", "11111112B",
                              "612111001", "Oviedo", 43.3614, -5.8494),
            new ConductorSeed("luis.gijon@cargohub.local", "Luis", "González", "22222223C",
                              "612111002", "Gijón", 43.5322, -5.6611),
            new ConductorSeed("elena.oviedo@cargohub.local", "Elena", "García", "44444444D",
                              "612111003", "Oviedo", 43.3700, -5.8500),
            new ConductorSeed("carlos.aviles@cargohub.local", "Carlos", "Menéndez", "55555555E",
                              "612111004", "Avilés", 43.5560, -5.9240)
        );
        List<Conductor> out = new ArrayList<>();
        for (ConductorSeed s : seeds) {
            Usuario u = upsertUsuario(s.email, DRIVER_PW, RolUsuario.CONDUCTOR, true);
            Conductor c = conductorRepository.findByUsuarioEmail(s.email).orElseGet(Conductor::new);
            c.setUsuario(u); c.setNombre(s.nom); c.setApellidos(s.ape); c.setDni(s.dni);
            c.setTelefono(s.tel); c.setCiudadBase(s.ciu); c.setLatitudBase(s.lat); c.setLongitudBase(s.lon);
            c.setLatitudActual(s.lat); c.setLongitudActual(s.lon);
            c.setUltimaActualizacionUbicacion(LocalDateTime.now().minusHours(2));
            c.setRadioAccionKm(350); c.setDiasLaborables("1,2,3,4,5");
            c.setDisponible(true); c.setBuscarRetorno(true);
            out.add(conductorRepository.save(c));
        }
        return out;
    }

    // ═══ Vehículos (5) ═══

    private void seedVehiculos(List<Conductor> conductores) {
        List<VehiculoSeed> seeds = List.of(
            new VehiculoSeed("1234ABC", "Iveco", "Daily 35S14", TipoVehiculo.FURGONETA, 1500, 4200, 1800, 1900, "11111111A"),
            new VehiculoSeed("5678DEF", "Mercedes", "Atego 1224", TipoVehiculo.RIGIDO, 6000, 7200, 2400, 2500, "11111112B"),
            new VehiculoSeed("9012GHI", "Volvo", "FH 500", TipoVehiculo.TRAILER, 24000, 13600, 2480, 2700, "22222223C"),
            new VehiculoSeed("3456JKL", "Renault", "Master L3H2", TipoVehiculo.FURGONETA, 1200, 3700, 1765, 1880, "44444444D"),
            new VehiculoSeed("6789STU", "Iveco", "Eurocargo", TipoVehiculo.RIGIDO, 7500, 8000, 2400, 2500, "55555555E")
        );
        for (VehiculoSeed s : seeds) {
            Conductor co = conductores.stream().filter(c -> s.conductorDni.equalsIgnoreCase(c.getDni())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Conductor no encontrado: " + s.conductorDni));
            Optional<Vehiculo> existente = vehiculoRepository.findByMatricula(s.mat);
            Vehiculo v = existente.orElseGet(Vehiculo::new);
            v.setMatricula(s.mat); v.setMarca(s.mar); v.setModelo(s.mod); v.setTipo(s.tipo);
            v.setEstado(EstadoVehiculo.DISPONIBLE);
            v.setCapacidadCargaKg(s.cap); v.setLargoUtilMm(s.la); v.setAnchoUtilMm(s.an); v.setAltoUtilMm(s.al);
            v.setConductor(co);
            vehiculoRepository.save(v);
        }
    }

    // ═══ Portes demo (solo finalizados) ═══

    private void seedPortes(List<Cliente> clientes, List<Conductor> conductores) {
        List<PorteSeed> seeds = List.of(
            // Rutas Asturias
            new PorteSeed("Oviedo", "Gijón",      43.3614, -5.8494, 43.5322, -5.6611,  28.0,  85.0,  "Palés de repostería",          800.0,  4.0, 1.2, TipoVehiculo.FURGONETA, EstadoPorte.ENTREGADO, 2, 1),
            new PorteSeed("Gijón", "Avilés",       43.5322, -5.6611, 43.5560, -5.9240,  22.0,  75.0,  "Material de construcción",    2000.0,  8.0, 2.0, TipoVehiculo.RIGIDO,    EstadoPorte.ENTREGADO, 3, 4),
            new PorteSeed("Oviedo", "Madrid",       43.3614, -5.8494, 40.4168, -3.7038, 450.0, 650.0,  "Maquinaria industrial",       5000.0, 18.0, 3.5, TipoVehiculo.TRAILER,   EstadoPorte.FACTURADO, 2, 2),
            new PorteSeed("Avilés", "Santander",    43.5560, -5.9240, 43.4623, -3.8099, 170.0, 310.0,  "Productos siderúrgicos",      3500.0, 12.0, 2.8, TipoVehiculo.RIGIDO,    EstadoPorte.FACTURADO, 3, 4),
            new PorteSeed("Gijón", "Bilbao",        43.5322, -5.6611, 43.2630, -2.9350, 270.0, 420.0,  "Contenedor marítimo",         8000.0, 28.0, 6.0, TipoVehiculo.TRAILER,   EstadoPorte.ENTREGADO, 3, 1),
            // Nacional
            new PorteSeed("Madrid", "Barcelona",    40.4168, -3.7038, 41.3874,  2.1686, 620.0, 880.0,  "Electrónica de consumo",      2200.0, 10.5, 2.2, TipoVehiculo.RIGIDO,    EstadoPorte.ENTREGADO, 0, 0),
            new PorteSeed("Barcelona", "Valencia",   41.3874,  2.1686, 39.4699, -0.3763, 350.0, 520.0,  "Textil temporada",            1200.0,  6.0, 1.5, TipoVehiculo.FURGONETA, EstadoPorte.ENTREGADO, 1, null),
            new PorteSeed("Oviedo", "León",          43.3614, -5.8494, 42.5987, -5.5671, 120.0, 240.0,  "Lácteos refrigerados",        1500.0,  7.0, 1.8, TipoVehiculo.FURGONETA, EstadoPorte.FACTURADO, 4, 3),
            new PorteSeed("Gijón", "La Coruña",      43.5322, -5.6611, 43.3623, -8.4115, 290.0, 460.0,  "Pescado congelado",           3200.0, 14.0, 3.0, TipoVehiculo.RIGIDO,    EstadoPorte.FACTURADO, 3, 2),
            // 2 PENDIENTES para demo de matching
            new PorteSeed("Oviedo", "Mieres",        43.3614, -5.8494, 43.2500, -5.7750,  25.0,  80.0,  "Muebles de oficina",          900.0,  5.0,  2.0, TipoVehiculo.FURGONETA, EstadoPorte.PENDIENTE, 2, null),
            new PorteSeed("Gijón", "Oviedo",         43.5322, -5.6611, 43.3614, -5.8494,  30.0,  90.0,  "Cajas de bebidas",           1200.0,  6.0,  1.5, TipoVehiculo.FURGONETA, EstadoPorte.PENDIENTE, 3, null)
        );

        List<Porte> created = new ArrayList<>();
        for (PorteSeed s : seeds) {
            Porte p = findPorte(origen(s.origen), s.destino, s.desc).orElseGet(Porte::new);
            p.setOrigen(origen(s.origen)); p.setDestino(s.destino);
            p.setLatitudOrigen(s.latO); p.setLongitudOrigen(s.lonO);
            p.setLatitudDestino(s.latD); p.setLongitudDestino(s.lonD);
            p.setDistanciaKm(s.km); p.setDistanciaEstimada(true);
            p.setPrecio(s.precio); p.setAjustePrecio(0.0);
            p.setDescripcionCliente(s.desc); p.setPesoTotalKg(s.peso); p.setVolumenTotalM3(s.vol);
            p.setLargoMaxPaquete(s.largo); p.setTipoVehiculoRequerido(s.tipo);
            p.setRevisionManual(false); p.setMotivoRevision(null);
            p.setEstado(s.estado); p.setFechaCreacion(LocalDateTime.now().minusDays(7));
            p.setFechaRecogida(LocalDateTime.now().minusDays(3));
            p.setFechaEntrega(s.estado == EstadoPorte.ENTREGADO || s.estado == EstadoPorte.FACTURADO
                    ? LocalDateTime.now().minusDays(1) : LocalDateTime.now().plusDays(1));
            p.setCliente(clientes.get(s.cliIdx));
            p.setConductor(s.conIdx != null ? conductores.get(s.conIdx) : null);
            created.add(porteRepository.save(p));
        }

        // Facturas para los FACTURADOS
        int year = LocalDate.now().getYear();
        for (Porte p : created) {
            if (p.getEstado() != EstadoPorte.FACTURADO) continue;
            if (facturaRepository.findByPorteId(p.getId()).isPresent()) continue;
            Factura f = new Factura();
            f.setPorte(p); f.setBaseImponible(p.getPrecioFinal()); f.setPagada(true);
            f.setFechaEmision(LocalDate.now().minusDays(2));
            f.setNumeroSerie(String.format("SEED-%d-P%05d", year, p.getId()));
            facturaRepository.save(f);
        }
    }

    // ═══ Helpers ═══

    private Usuario upsertUsuario(String email, String plainPw, RolUsuario rol, boolean activo) {
        String n = email.toLowerCase();
        Usuario u = usuarioRepository.findByEmail(n).orElseGet(Usuario::new);
        u.setEmail(n);
        if (u.getPassword() == null || !passwordEncoder.matches(plainPw, u.getPassword()))
            u.setPassword(passwordEncoder.encode(plainPw));
        u.setRol(rol); u.setActivo(activo);
        if (u.getFechaRegistro() == null) u.setFechaRegistro(LocalDateTime.now());
        return usuarioRepository.save(u);
    }

    private Optional<Porte> findPorte(String origen, String destino, String desc) {
        return porteRepository.findAll().stream()
                .filter(p -> origen.equals(p.getOrigen()) && destino.equals(p.getDestino()) && desc.equals(p.getDescripcionCliente()))
                .findFirst();
    }

    private String origen(String ciudad) {
        return ciudad.startsWith("Oviedo") || ciudad.startsWith("Gijón") || ciudad.startsWith("Avilés")
                ? ciudad + ", Asturias" : ciudad;
    }

    // ═══ Records ═══

    private record ClienteSeed(String email, String nom, String cif, String tel, String dir, String sec) {}
    private record ConductorSeed(String email, String nom, String ape, String dni, String tel, String ciu, Double lat, Double lon) {}
    private record VehiculoSeed(String mat, String mar, String mod, TipoVehiculo tipo, Integer cap, Integer la, Integer an, Integer al, String conductorDni) {}
    private record PorteSeed(String origen, String destino, Double latO, Double lonO, Double latD, Double lonD,
                             double km, double precio, String desc, double peso, double vol, double largo,
                             TipoVehiculo tipo, EstadoPorte estado, int cliIdx, Integer conIdx) {}
}
