package com.cargohub.backend.config;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Incidencia;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoIncidencia;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.PrioridadIncidencia;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.entity.enums.SeveridadIncidencia;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.repository.IncidenciaRepository;
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

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ConductorRepository conductorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PorteRepository porteRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final FacturaRepository facturaRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(UsuarioRepository usuarioRepository,
                         ClienteRepository clienteRepository,
                         ConductorRepository conductorRepository,
                         VehiculoRepository vehiculoRepository,
                         PorteRepository porteRepository,
                         IncidenciaRepository incidenciaRepository,
                         FacturaRepository facturaRepository,
                         PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.conductorRepository = conductorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.porteRepository = porteRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.facturaRepository = facturaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String profile = System.getProperty("spring.profiles.active", "default");
        log.info("[seed] Iniciando seed de datos demo. profile={}", profile);

        Usuario admin = upsertUsuario(
                "admin@cargohub.local",
                "Admin123!",
                RolUsuario.ADMIN,
                true
        );

        List<Cliente> clientes = seedClientes();
        List<Conductor> conductores = seedConductores();
        List<Vehiculo> vehiculos = seedVehiculos(conductores);
        List<Porte> portes = seedPortes(clientes, conductores);
        List<Incidencia> incidencias = seedIncidencias(portes, admin);
        List<Factura> facturas = seedFacturas(portes);

        log.info("[seed] Seed completado. usuarios={}, clientes={}, conductores={}, vehiculos={}, portes={}, incidencias={}, facturas={}",
                usuarioRepository.count(),
                clientes.size(),
                conductores.size(),
                vehiculos.size(),
                portes.size(),
                incidencias.size(),
                facturas.size());
    }

    private List<Cliente> seedClientes() {
        List<ClienteSeed> seeds = List.of(
                new ClienteSeed("logistics.express@cargohub.local", "Logistics Express S.L.", "B00000001", "911000001", "Calle Atocha 100, Madrid", "Retail"),
                new ClienteSeed("trans.iberica@cargohub.local", "Trans Ibérica S.A.", "A00000002", "932000002", "Av. Diagonal 220, Barcelona", "Distribución"),
                new ClienteSeed("envios.rapidos@cargohub.local", "Envíos Rápidos S.L.", "B00000003", "961000003", "C/ Colón 45, Valencia", "Paquetería"),
                new ClienteSeed("frio.norte@cargohub.local", "Frío Norte Logística", "B00000004", "944000004", "Gran Vía 20, Bilbao", "Alimentación"),
                new ClienteSeed("mediterraneo.cargo@cargohub.local", "Mediterráneo Cargo S.L.", "B00000005", "965000005", "Av. Alfonso X, Alicante", "Exportación"),
                new ClienteSeed("andalucia.supply@cargohub.local", "Andalucía Supply", "B00000006", "954000006", "C/ Sierpes 80, Sevilla", "Industrial"),
                new ClienteSeed("ebro.freight@cargohub.local", "Ebro Freight Services", "B00000007", "976000007", "Paseo Independencia 12, Zaragoza", "Automoción"),
                new ClienteSeed("cantabrico.log@cargohub.local", "Cantábrico Log", "B00000008", "942000008", "C/ Burgos 13, Santander", "Mayorista")
        );

        List<Cliente> out = new ArrayList<>();
        for (ClienteSeed seed : seeds) {
            Usuario usuario = upsertUsuario(seed.email(), "Cliente123!", RolUsuario.CLIENTE, true);

            Cliente cliente = clienteRepository.findByUsuarioEmail(seed.email())
                    .orElseGet(Cliente::new);

            cliente.setUsuario(usuario);
            cliente.setNombreEmpresa(seed.nombreEmpresa());
            cliente.setCif(seed.cif());
            cliente.setTelefono(seed.telefono());
            cliente.setDireccionFiscal(seed.direccionFiscal());
            cliente.setEmailContacto(seed.email());
            cliente.setSector(seed.sector());

            out.add(clienteRepository.save(cliente));
        }
        return out;
    }

    private List<Conductor> seedConductores() {
        List<ConductorSeed> seeds = List.of(
                new ConductorSeed("juan.perez@cargohub.local", "Juan", "Pérez", "11111111A", "612000001", "Madrid", 40.4168, -3.7038, 300, "1,2,3,4,5", true, 4.5, 28, 126.0),
                new ConductorSeed("maria.lopez@cargohub.local", "María", "López", "22222222B", "612000002", "Barcelona", 41.3874, 2.1686, 260, "1,2,3,4,5", true, 4.8, 35, 168.0),
                new ConductorSeed("carlos.ruiz@cargohub.local", "Carlos", "Ruiz", "33333333C", "612000003", "Valencia", 39.4699, -0.3763, 220, "1,2,3,4,5,6", true, 4.2, 19, 84.0),
                new ConductorSeed("ana.garcia@cargohub.local", "Ana", "García", "44444444D", "612000004", "Sevilla", 37.3891, -5.9845, 180, "1,2,3,4,5", false, 3.9, 14, 55.0),
                new ConductorSeed("pedro.martin@cargohub.local", "Pedro", "Martín", "55555555E", "612000005", "Bilbao", 43.2630, -2.9350, 200, "1,2,3,4,5", false, 4.0, 10, 40.0),
                new ConductorSeed("laura.hernandez@cargohub.local", "Laura", "Hernández", "66666666F", "612000006", "Zaragoza", 41.6488, -0.8891, 320, "1,2,3,4,5", true, 4.6, 22, 101.0),
                new ConductorSeed("diego.navarro@cargohub.local", "Diego", "Navarro", "77777777G", "612000007", "Málaga", 36.7213, -4.4214, 170, "1,2,3,4,5,6", true, 4.3, 17, 73.0),
                new ConductorSeed("elena.morales@cargohub.local", "Elena", "Morales", "88888888H", "612000008", "Murcia", 37.9922, -1.1307, 210, "1,2,3,4,5", true, 4.4, 25, 110.0)
        );

        List<Conductor> out = new ArrayList<>();
        for (ConductorSeed seed : seeds) {
            Usuario usuario = upsertUsuario(seed.email(), "Conductor123!", RolUsuario.CONDUCTOR, true);

            Conductor conductor = conductorRepository.findByUsuarioEmail(seed.email())
                    .orElseGet(Conductor::new);

            conductor.setUsuario(usuario);
            conductor.setNombre(seed.nombre());
            conductor.setApellidos(seed.apellidos());
            conductor.setDni(seed.dni());
            conductor.setTelefono(seed.telefono());
            conductor.setCiudadBase(seed.ciudadBase());
            conductor.setLatitudBase(seed.latitudBase());
            conductor.setLongitudBase(seed.longitudBase());
            conductor.setLatitudActual(seed.latitudBase());
            conductor.setLongitudActual(seed.longitudBase());
            conductor.setUltimaActualizacionUbicacion(LocalDateTime.now().minusHours(2));
            conductor.setRadioAccionKm(seed.radioAccionKm());
            conductor.setDiasLaborables(seed.diasLaborables());
            conductor.setDisponible(seed.disponible());
            conductor.setBuscarRetorno(true);
            conductor.setRating(seed.rating());
            conductor.setNumeroValoraciones(seed.numeroValoraciones());
            conductor.setSumaPuntuaciones(seed.sumaPuntuaciones());

            out.add(conductorRepository.save(conductor));
        }
        return out;
    }

    private List<Vehiculo> seedVehiculos(List<Conductor> conductores) {
        List<VehiculoSeed> seeds = List.of(
                new VehiculoSeed("1234ABC", "Iveco", "Daily 35S14", TipoVehiculo.FURGONETA, EstadoVehiculo.DISPONIBLE, 1500, 4200, 1800, 1900, false, "11111111A"),
                new VehiculoSeed("5678DEF", "Mercedes-Benz", "Atego 1224", TipoVehiculo.RIGIDO, EstadoVehiculo.DISPONIBLE, 6000, 7200, 2400, 2500, true, "22222222B"),
                new VehiculoSeed("9012GHI", "Volvo", "FH 500", TipoVehiculo.TRAILER, EstadoVehiculo.DISPONIBLE, 24000, 13600, 2480, 2700, true, "33333333C"),
                new VehiculoSeed("3456JKL", "Renault", "Master L3H2", TipoVehiculo.FURGONETA, EstadoVehiculo.EN_MANTENIMIENTO, 1200, 3700, 1765, 1880, false, "66666666F"),
                new VehiculoSeed("7890MNO", "MAN", "TGX 18.510", TipoVehiculo.TRAILER, EstadoVehiculo.DISPONIBLE, 25000, 13600, 2480, 2700, true, "77777777G"),
                new VehiculoSeed("2345PQR", "DAF", "XF 480", TipoVehiculo.TRAILER, EstadoVehiculo.BAJA, 24000, 13600, 2480, 2700, false, "55555555E"),
                new VehiculoSeed("6789STU", "Iveco", "Eurocargo 120E25", TipoVehiculo.RIGIDO, EstadoVehiculo.DISPONIBLE, 7500, 8000, 2400, 2500, true, "88888888H"),
                new VehiculoSeed("0123VWX", "Peugeot", "Boxer L4H3", TipoVehiculo.FURGONETA, EstadoVehiculo.DISPONIBLE, 1400, 4070, 1870, 2172, false, "11111111A"),
                new VehiculoSeed("4567YZA", "Scania", "R 450", TipoVehiculo.ESPECIAL, EstadoVehiculo.EN_MANTENIMIENTO, 20000, 12000, 2500, 2800, true, "22222222B"),
                new VehiculoSeed("8901BCD", "Ford", "Transit L3H2", TipoVehiculo.FURGONETA, EstadoVehiculo.DISPONIBLE, 1100, 3494, 1784, 1886, false, "33333333C")
        );

        List<Vehiculo> out = new ArrayList<>();
        for (VehiculoSeed seed : seeds) {
            Conductor conductor = findConductorByDni(conductores, seed.conductorDni());
            Optional<Vehiculo> existente = vehiculoRepository.findByMatricula(seed.matricula());
            Vehiculo vehiculo = existente.orElseGet(Vehiculo::new);

            vehiculo.setMatricula(seed.matricula());
            vehiculo.setMarca(seed.marca());
            vehiculo.setModelo(seed.modelo());
            vehiculo.setTipo(seed.tipo());
            vehiculo.setEstado(seed.estado());
            vehiculo.setCapacidadCargaKg(seed.capacidadCargaKg());
            vehiculo.setLargoUtilMm(seed.largoUtilMm());
            vehiculo.setAnchoUtilMm(seed.anchoUtilMm());
            vehiculo.setAltoUtilMm(seed.altoUtilMm());
            vehiculo.setTrampillaElevadora(seed.trampillaElevadora());
            vehiculo.setConductor(conductor);

            out.add(vehiculoRepository.save(vehiculo));
        }
        return out;
    }

    private List<Porte> seedPortes(List<Cliente> clientes, List<Conductor> conductores) {
        List<PorteSeed> seeds = List.of(
                new PorteSeed("Madrid", "Barcelona", 40.4168, -3.7038, 41.3874, 2.1686, 621.0, 850.0, 0.0, "Palés de electrónica", 2200.0, 10.5, 2.2, TipoVehiculo.RIGIDO, false, EstadoPorte.EN_TRANSITO, 0, 0),
                new PorteSeed("Valencia", "Sevilla", 39.4699, -0.3763, 37.3891, -5.9845, 654.0, 720.0, 20.0, "Carga alimentaria", 1800.0, 9.2, 2.0, TipoVehiculo.RIGIDO, false, EstadoPorte.ENTREGADO, 1, 1),
                new PorteSeed("Bilbao", "Zaragoza", 43.2630, -2.9350, 41.6488, -0.8891, 305.0, 480.0, 0.0, "Materiales de construcción", 3500.0, 14.0, 3.0, TipoVehiculo.TRAILER, false, EstadoPorte.PENDIENTE, 2, null),
                new PorteSeed("Málaga", "Granada", 36.7213, -4.4214, 37.1773, -3.5986, 126.0, 280.0, 0.0, "Mobiliario de oficina", 1200.0, 6.0, 1.8, TipoVehiculo.FURGONETA, false, EstadoPorte.ASIGNADO, 3, 6),
                new PorteSeed("Alicante", "Murcia", 38.3452, -0.4810, 37.9922, -1.1307, 82.0, 180.0, -10.0, "Paquetería variada", 900.0, 4.0, 1.2, TipoVehiculo.FURGONETA, false, EstadoPorte.CANCELADO, 4, null),
                new PorteSeed("Valladolid", "Salamanca", 41.6523, -4.7245, 40.9701, -5.6635, 115.0, 220.0, 0.0, "Documentación urgente", 300.0, 1.2, 0.5, TipoVehiculo.FURGONETA, false, EstadoPorte.EN_TRANSITO, 5, 2),
                new PorteSeed("Córdoba", "Jaén", 37.8882, -4.7794, 37.7796, -3.7849, 107.0, 190.0, 5.0, "Aceite de oliva", 1700.0, 8.5, 2.1, TipoVehiculo.RIGIDO, false, EstadoPorte.FACTURADO, 6, 4),
                new PorteSeed("Santander", "Oviedo", 43.4623, -3.8099, 43.3614, -5.8494, 203.0, 350.0, 0.0, "Productos refrigerados", 2600.0, 11.0, 2.4, TipoVehiculo.RIGIDO, true, EstadoPorte.ENTREGADO, 7, 1),
                new PorteSeed("Pamplona", "San Sebastián", 42.8125, -1.6458, 43.3183, -1.9812, 79.0, 240.0, 0.0, "Vinos y licores", 1300.0, 5.8, 1.7, TipoVehiculo.FURGONETA, false, EstadoPorte.PENDIENTE, 0, null),
                new PorteSeed("Toledo", "Ciudad Real", 39.8628, -4.0273, 38.9848, -3.9274, 119.0, 300.0, 0.0, "Recambios industriales", 1450.0, 7.0, 1.9, TipoVehiculo.RIGIDO, false, EstadoPorte.ASIGNADO, 1, 0),
                new PorteSeed("Cáceres", "Badajoz", 39.4763, -6.3722, 38.8794, -6.9707, 90.0, 260.0, 0.0, "Maquinaria agrícola", 5000.0, 20.0, 3.4, TipoVehiculo.TRAILER, false, EstadoPorte.EN_TRANSITO, 2, 5),
                new PorteSeed("Tarragona", "Lleida", 41.1189, 1.2445, 41.6176, 0.6200, 98.0, 310.0, 0.0, "Textiles", 1600.0, 6.5, 1.6, TipoVehiculo.RIGIDO, false, EstadoPorte.FACTURADO, 3, 7)
        );

        List<Porte> out = new ArrayList<>();
        for (PorteSeed seed : seeds) {
            Porte porte = findPorteByRutaYDescripcion(seed.origen(), seed.destino(), seed.descripcionCliente())
                    .orElseGet(Porte::new);

            porte.setOrigen(seed.origen());
            porte.setDestino(seed.destino());
            porte.setLatitudOrigen(seed.latitudOrigen());
            porte.setLongitudOrigen(seed.longitudOrigen());
            porte.setLatitudDestino(seed.latitudDestino());
            porte.setLongitudDestino(seed.longitudDestino());
            porte.setDistanciaKm(seed.distanciaKm());
            porte.setDistanciaEstimada(true);
            porte.setPrecio(seed.precio());
            porte.setAjustePrecio(seed.ajustePrecio());
            porte.setMotivoAjuste(seed.ajustePrecio() != 0.0 ? "Ajuste seed" : null);
            porte.setDescripcionCliente(seed.descripcionCliente());
            porte.setPesoTotalKg(seed.pesoTotalKg());
            porte.setVolumenTotalM3(seed.volumenTotalM3());
            porte.setLargoMaxPaquete(seed.largoMaxPaquete());
            porte.setTipoVehiculoRequerido(seed.tipoVehiculoRequerido());
            porte.setRequiereFrio(seed.requiereFrio());
            porte.setRevisionManual(false);
            porte.setMotivoRevision(null);
            porte.setEstado(seed.estado());
            porte.setFechaCreacion(LocalDateTime.now().minusDays(10));
            porte.setFechaRecogida(LocalDateTime.now().plusDays(1));

            if (seed.estado() == EstadoPorte.ENTREGADO || seed.estado() == EstadoPorte.FACTURADO) {
                porte.setFechaEntrega(LocalDateTime.now().minusDays(1));
            } else {
                porte.setFechaEntrega(LocalDateTime.now().plusDays(2));
            }

            porte.setCliente(clientes.get(seed.clienteIdx()));

            if (seed.conductorIdx() == null) {
                porte.setConductor(null);
            } else {
                porte.setConductor(conductores.get(seed.conductorIdx()));
            }

            out.add(porteRepository.save(porte));
        }
        return out;
    }

    private List<Incidencia> seedIncidencias(List<Porte> portes, Usuario admin) {
        List<IncidenciaSeed> seeds = List.of(
                new IncidenciaSeed("Retraso por lluvia", "La ruta sufrió retraso de 3 horas.", EstadoIncidencia.ABIERTA, SeveridadIncidencia.ALTA, PrioridadIncidencia.ALTA, 0, false),
                new IncidenciaSeed("Daño parcial de mercancía", "Dos bultos dañados en descarga.", EstadoIncidencia.EN_REVISION, SeveridadIncidencia.MEDIA, PrioridadIncidencia.ALTA, 1, false),
                new IncidenciaSeed("Albarán incompleto", "Falta firma digital del albarán.", EstadoIncidencia.RESUELTA, SeveridadIncidencia.BAJA, PrioridadIncidencia.BAJA, 2, true),
                new IncidenciaSeed("Incidencia de dirección", "Dirección de entrega desactualizada.", EstadoIncidencia.DESESTIMADA, SeveridadIncidencia.BAJA, PrioridadIncidencia.MEDIA, 3, true),
                new IncidenciaSeed("Avería en ruta", "Fallo mecánico en autopista.", EstadoIncidencia.ABIERTA, SeveridadIncidencia.ALTA, PrioridadIncidencia.ALTA, 4, false),
                new IncidenciaSeed("Cambio horario cliente", "Solicitud de retraso en franja de entrega.", EstadoIncidencia.EN_REVISION, SeveridadIncidencia.BAJA, PrioridadIncidencia.MEDIA, 5, false),
                new IncidenciaSeed("Cadena de frío comprometida", "Temperatura por encima de umbral.", EstadoIncidencia.RESUELTA, SeveridadIncidencia.ALTA, PrioridadIncidencia.ALTA, 7, true),
                new IncidenciaSeed("Peaje no previsto", "Gasto adicional no contemplado en tarifa.", EstadoIncidencia.ABIERTA, SeveridadIncidencia.BAJA, PrioridadIncidencia.BAJA, 10, false)
        );

        List<Incidencia> out = new ArrayList<>();
        for (IncidenciaSeed seed : seeds) {
            Incidencia incidencia = findIncidenciaByTituloAndPorte(seed.titulo(), portes.get(seed.porteIdx()).getId())
                    .orElseGet(Incidencia::new);

            incidencia.setPorte(portes.get(seed.porteIdx()));
            incidencia.setTitulo(seed.titulo());
            incidencia.setDescripcion(seed.descripcion());
            incidencia.setEstado(seed.estado());
            incidencia.setSeveridad(seed.severidad());
            incidencia.setPrioridad(seed.prioridad());
            incidencia.setFechaReporte(LocalDateTime.now().minusDays(3));
            incidencia.setFechaLimiteSla(LocalDateTime.now().plusDays(2));

            if (seed.resuelta()) {
                incidencia.setAdmin(admin);
                incidencia.setResolucion("Resuelta por equipo de operaciones");
                incidencia.setFechaResolucion(LocalDateTime.now().minusDays(1));
            } else {
                incidencia.setAdmin(null);
                incidencia.setResolucion(null);
                incidencia.setFechaResolucion(null);
            }

            out.add(incidenciaRepository.save(incidencia));
        }
        return out;
    }

    private List<Factura> seedFacturas(List<Porte> portes) {
        List<Integer> portesFacturables = List.of(6, 7, 11);
        List<Factura> out = new ArrayList<>();
        int year = LocalDate.now().getYear();

        for (Integer porteIdx : portesFacturables) {
            Porte porte = portes.get(porteIdx);
            Optional<Factura> existente = facturaRepository.findByPorteId(porte.getId());

            Factura factura = existente.orElseGet(Factura::new);
            factura.setPorte(porte);
            factura.setBaseImponible(porte.getPrecioFinal());
            factura.setPagada(porteIdx % 2 == 0);
            factura.setFechaEmision(LocalDate.now().minusDays(1));

            if (factura.getNumeroSerie() == null || factura.getNumeroSerie().isBlank()) {
                factura.setNumeroSerie(String.format("SEED-%d-P%05d", year, porte.getId()));
            }

            out.add(facturaRepository.save(factura));
        }
        return out;
    }

    private Usuario upsertUsuario(String email, String plainPassword, RolUsuario rol, boolean activo) {
        String normalized = email.toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(normalized).orElseGet(Usuario::new);

        usuario.setEmail(normalized);
        if (usuario.getPassword() == null || !passwordEncoder.matches(plainPassword, usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(plainPassword));
        }
        usuario.setRol(rol);
        usuario.setActivo(activo);
        if (usuario.getFechaRegistro() == null) {
            usuario.setFechaRegistro(LocalDateTime.now());
        }
        usuario.setUltimoAcceso(LocalDateTime.now().minusDays(1));
        return usuarioRepository.save(usuario);
    }

    private Conductor findConductorByDni(List<Conductor> conductores, String dni) {
        return conductores.stream()
                .filter(c -> dni.equalsIgnoreCase(c.getDni()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Conductor no encontrado para DNI " + dni));
    }

    private Optional<Porte> findPorteByRutaYDescripcion(String origen, String destino, String descripcion) {
        return porteRepository.findAll().stream()
                .filter(p -> origen.equals(p.getOrigen())
                        && destino.equals(p.getDestino())
                        && descripcion.equals(p.getDescripcionCliente()))
                .findFirst();
    }

    private Optional<Incidencia> findIncidenciaByTituloAndPorte(String titulo, Long porteId) {
        return incidenciaRepository.findByPorteId(porteId).stream()
                .filter(i -> titulo.equals(i.getTitulo()))
                .findFirst();
    }

    private record ClienteSeed(
            String email,
            String nombreEmpresa,
            String cif,
            String telefono,
            String direccionFiscal,
            String sector
    ) {}

    private record ConductorSeed(
            String email,
            String nombre,
            String apellidos,
            String dni,
            String telefono,
            String ciudadBase,
            Double latitudBase,
            Double longitudBase,
            Integer radioAccionKm,
            String diasLaborables,
            boolean disponible,
            Double rating,
            Integer numeroValoraciones,
            Double sumaPuntuaciones
    ) {}

    private record VehiculoSeed(
            String matricula,
            String marca,
            String modelo,
            TipoVehiculo tipo,
            EstadoVehiculo estado,
            Integer capacidadCargaKg,
            Integer largoUtilMm,
            Integer anchoUtilMm,
            Integer altoUtilMm,
            boolean trampillaElevadora,
            String conductorDni
    ) {}

    private record PorteSeed(
            String origen,
            String destino,
            Double latitudOrigen,
            Double longitudOrigen,
            Double latitudDestino,
            Double longitudDestino,
            Double distanciaKm,
            Double precio,
            Double ajustePrecio,
            String descripcionCliente,
            Double pesoTotalKg,
            Double volumenTotalM3,
            Double largoMaxPaquete,
            TipoVehiculo tipoVehiculoRequerido,
            boolean requiereFrio,
            EstadoPorte estado,
            int clienteIdx,
            Integer conductorIdx
    ) {}

    private record IncidenciaSeed(
            String titulo,
            String descripcion,
            EstadoIncidencia estado,
            SeveridadIncidencia severidad,
            PrioridadIncidencia prioridad,
            int porteIdx,
            boolean resuelta
    ) {}
}
