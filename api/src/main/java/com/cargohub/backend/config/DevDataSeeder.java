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
    private static final String CPW = "Cliente123!", DPW = "Conductor123!";

    private final UsuarioRepository ur; private final ClienteRepository cr;
    private final ConductorRepository cor; private final VehiculoRepository vr;
    private final PorteRepository pr; private final FacturaRepository fr;
    private final PasswordEncoder pe;

    public DevDataSeeder(UsuarioRepository ur, ClienteRepository cr, ConductorRepository cor,
                         VehiculoRepository vr, PorteRepository pr, FacturaRepository fr, PasswordEncoder pe) {
        this.ur=ur; this.cr=cr; this.cor=cor; this.vr=vr; this.pr=pr; this.fr=fr; this.pe=pe;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        log.info("[seed] Iniciando seed demo Asturias ×10");
        seedSuperadmin(); seedAdmin();
        List<Cliente> cl = seedClientes();
        List<Conductor> co = seedConductores();
        seedVehiculos(co);
        seedPortes(cl, co);
        log.info("[seed] OK. clientes={} conductores={} vehiculos={} portes={}", cl.size(), co.size(), vr.count(), pr.count());
    }

    // ═══ Admin ═══
    private void seedSuperadmin() { Usuario u=upsert("superadmin@cargohub.local","Super123!",RolUsuario.SUPERADMIN,true); u.setNombre("Super Admin"); ur.save(u); }
    private void seedAdmin()      { Usuario u=upsert("admin@cargohub.local",     "Admin123!", RolUsuario.ADMIN,true);      u.setNombre("Admin");       ur.save(u); }

    // ═══ Clientes (5) ═══
    private List<Cliente> seedClientes() {
        List<C> s = List.of(
            new C("logistics.express@cargohub.local","Logistics Express S.L.","B00000001","911000001","C/ Atocha 100, Madrid","Retail"),
            new C("trans.iberica@cargohub.local","Trans Ibérica S.A.","A00000002","932000002","Av. Diagonal 220, Barcelona","Distribución"),
            new C("asturias.industrial@cargohub.local","Asturias Industrial","B33000001","985100001","C/ Uría 45, Oviedo","Industrial"),
            new C("gijon.maritimo@cargohub.local","Gijón Marítimo S.A.","A33000002","985200002","Av. de la Costa 12, Gijón","Exportación"),
            new C("sidreria.tradicion@cargohub.local","Sidrería Tradición S.L.","B33000003","985300003","C/ Gascona 8, Oviedo","Alimentación")
        );
        List<Cliente> out=new ArrayList<>();
        for(C c:s){ Usuario u=upsert(c.e,CPW,RolUsuario.CLIENTE,true); Cliente x=cr.findByUsuarioEmail(c.e).orElseGet(Cliente::new); x.setUsuario(u);x.setNombreEmpresa(c.n);x.setCif(c.cif);x.setTelefono(c.t);x.setDireccionFiscal(c.d);x.setEmailContacto(c.e);x.setSector(c.se); out.add(cr.save(x)); }
        return out;
    }

    // ═══ Conductores (10) ═══
    private List<Conductor> seedConductores() {
        List<D> s = List.of(
            new D("juan.perez@cargohub.local","Juan","Pérez","11111111A","612000001","Madrid",40.4168,-3.7038),
            new D("maria.lopez@cargohub.local","María","López","22222222B","612000002","Barcelona",41.3874,2.1686),
            new D("pedro.oviedo@cargohub.local","Pedro","Fernández","33333333C","612111001","Oviedo",43.3614,-5.8494),
            new D("luis.gijon@cargohub.local","Luis","González","44444444D","612111002","Gijón",43.5322,-5.6611),
            new D("elena.oviedo@cargohub.local","Elena","García","55555555E","612111003","Oviedo",43.3700,-5.8500),
            new D("carlos.aviles@cargohub.local","Carlos","Menéndez","66666666F","612111004","Avilés",43.5560,-5.9240),
            new D("ana.mieres@cargohub.local","Ana","Suárez","77777777G","612111005","Mieres",43.2500,-5.7750),
            new D("diego.gijon@cargohub.local","Diego","Álvarez","88888888H","612111006","Gijón",43.5400,-5.6600),
            new D("laura.oviedo@cargohub.local","Laura","Díaz","99999999J","612111007","Oviedo",43.3500,-5.8400),
            new D("pablo.aviles@cargohub.local","Pablo","Rodríguez","10101010K","612111008","Avilés",43.5500,-5.9200)
        );
        List<Conductor> out=new ArrayList<>();
        for(D d:s){ Usuario u=upsert(d.e,DPW,RolUsuario.CONDUCTOR,true); Conductor c=cor.findByUsuarioEmail(d.e).orElseGet(Conductor::new); c.setUsuario(u);c.setNombre(d.n);c.setApellidos(d.a);c.setDni(d.dni);c.setTelefono(d.t);c.setCiudadBase(d.cb);c.setLatitudBase(d.la);c.setLongitudBase(d.lo);c.setLatitudActual(d.la);c.setLongitudActual(d.lo);c.setUltimaActualizacionUbicacion(LocalDateTime.now().minusHours(2));c.setRadioAccionKm(350);c.setDiasLaborables("1,2,3,4,5");c.setDisponible(true);c.setBuscarRetorno(true); out.add(cor.save(c)); }
        return out;
    }

    // ═══ Vehículos (10) ═══
    private void seedVehiculos(List<Conductor> co) {
        List<V> s = List.of(
            new V("1234ABC","Iveco","Daily 35S14",TipoVehiculo.FURGONETA,1500,4200,1800,1900,"11111111A"),
            new V("5678DEF","Mercedes","Atego 1224",TipoVehiculo.RIGIDO,6000,7200,2400,2500,"22222222B"),
            new V("9012GHI","Volvo","FH 500",TipoVehiculo.TRAILER,24000,13600,2480,2700,"33333333C"),
            new V("3456JKL","Renault","Master L3",TipoVehiculo.FURGONETA,1200,3700,1765,1880,"44444444D"),
            new V("7890MNO","MAN","TGX 18.510",TipoVehiculo.TRAILER,25000,13600,2480,2700,"55555555E"),
            new V("1122PQR","Iveco","Eurocargo",TipoVehiculo.RIGIDO,7500,8000,2400,2500,"66666666F"),
            new V("3345STU","Ford","Transit L3",TipoVehiculo.FURGONETA,1100,3494,1784,1886,"77777777G"),
            new V("5567VWX","DAF","XF 480",TipoVehiculo.TRAILER,24000,13600,2480,2700,"88888888H"),
            new V("7789YZA","Peugeot","Boxer L4",TipoVehiculo.FURGONETA,1400,4070,1870,2172,"99999999J"),
            new V("9901BCD","Scania","R 450",TipoVehiculo.RIGIDO,8000,7500,2450,2600,"10101010K")
        );
        for(V v:s){ Conductor c=co.stream().filter(x->v.dni.equalsIgnoreCase(x.getDni())).findFirst().orElseThrow(); Vehiculo ve=vr.findByMatricula(v.mat).orElseGet(Vehiculo::new); ve.setMatricula(v.mat);ve.setMarca(v.ma);ve.setModelo(v.mo);ve.setTipo(v.ti);ve.setEstado(EstadoVehiculo.DISPONIBLE);ve.setCapacidadCargaKg(v.ca);ve.setLargoUtilMm(v.la);ve.setAnchoUtilMm(v.an);ve.setAltoUtilMm(v.al);ve.setConductor(c); vr.save(ve); }
    }

    // ═══ Portes (20 finalizados + 4 pendientes) ═══
    private void seedPortes(List<Cliente> cl, List<Conductor> co) {
        List<P> s = new ArrayList<>();
        // ── Asturias (15) ──
        s.add(new P("Oviedo","Gijón",       43.3614,-5.8494,43.5322,-5.6611, 28, 85,  "Repostería industrial",    800, 4.0,1.2,TipoVehiculo.FURGONETA,EstadoPorte.ENTREGADO,2,3));
        s.add(new P("Gijón","Avilés",        43.5322,-5.6611,43.5560,-5.9240, 22, 75,  "Material construcción",   2000, 8.0,2.0,TipoVehiculo.RIGIDO,   EstadoPorte.ENTREGADO,3,4));
        s.add(new P("Oviedo","Madrid",        43.3614,-5.8494,40.4168,-3.7038,450,650,  "Maquinaria pesada",       5000,18.0,3.5,TipoVehiculo.TRAILER,  EstadoPorte.FACTURADO,2,2));
        s.add(new P("Avilés","Santander",     43.5560,-5.9240,43.4623,-3.8099,170,310,  "Acero laminado",          3500,12.0,2.8,TipoVehiculo.RIGIDO,   EstadoPorte.FACTURADO,3,5));
        s.add(new P("Gijón","Bilbao",         43.5322,-5.6611,43.2630,-2.9350,270,420,  "Contenedor exportación",  8000,28.0,6.0,TipoVehiculo.TRAILER,  EstadoPorte.ENTREGADO,3,7));
        s.add(new P("Oviedo","León",          43.3614,-5.8494,42.5987,-5.5671,120,240,  "Lácteos refrigerados",    1500, 7.0,1.8,TipoVehiculo.FURGONETA,EstadoPorte.FACTURADO,4,6));
        s.add(new P("Gijón","La Coruña",      43.5322,-5.6611,43.3623,-8.4115,290,460,  "Pescado congelado",       3200,14.0,3.0,TipoVehiculo.RIGIDO,   EstadoPorte.FACTURADO,3,4));
        s.add(new P("Mieres","Oviedo",        43.2500,-5.7750,43.3614,-5.8494, 18, 65,  "Mobiliario oficina",       900, 5.0,2.0,TipoVehiculo.FURGONETA,EstadoPorte.ENTREGADO,2,6));
        s.add(new P("Oviedo","Avilés",        43.3614,-5.8494,43.5560,-5.9240, 30, 90,  "Recambios automoción",    1100, 5.5,1.6,TipoVehiculo.FURGONETA,EstadoPorte.ENTREGADO,2,3));
        s.add(new P("Gijón","Oviedo",         43.5322,-5.6611,43.3614,-5.8494, 30, 90,  "Bebidas y licores",       1200, 6.0,1.5,TipoVehiculo.FURGONETA,EstadoPorte.ENTREGADO,3,7));
        s.add(new P("Luanco","Gijón",         43.6150,-5.8050,43.5322,-5.6611, 15, 55,  "Marisco fresco",           400, 2.5,1.0,TipoVehiculo.FURGONETA,EstadoPorte.FACTURADO,4,8));
        s.add(new P("Avilés","Lugo",          43.5560,-5.9240,43.0097,-7.5567,220,380,  "Madera tratada",          2800,15.0,3.2,TipoVehiculo.RIGIDO,   EstadoPorte.ENTREGADO,2,5));
        s.add(new P("Oviedo","Santander",     43.3614,-5.8494,43.4623,-3.8099,190,350,  "Productos lácteos",       2200,10.0,2.4,TipoVehiculo.RIGIDO,   EstadoPorte.FACTURADO,4,4));
        s.add(new P("Gijón","Ponferrada",     43.5322,-5.6611,42.5464,-6.5903,140,280,  "Vidrio laminado",         1800, 9.0,2.5,TipoVehiculo.RIGIDO,   EstadoPorte.ENTREGADO,3,8));
        s.add(new P("Cangas","Oviedo",        43.3100,-6.5400,43.3614,-5.8494, 55,110,  "Embutidos artesanales",    600, 3.0,1.2,TipoVehiculo.FURGONETA,EstadoPorte.FACTURADO,4,6));

        // ── Nacional (5) ──
        s.add(new P("Madrid","Barcelona",     40.4168,-3.7038,41.3874,2.1686, 620,880,  "Electrónica consumo",     2200,10.5,2.2,TipoVehiculo.RIGIDO,   EstadoPorte.ENTREGADO,0,0));
        s.add(new P("Barcelona","Valencia",    41.3874,2.1686,39.4699,-0.3763, 350,520,  "Textil temporada",        1200, 6.0,1.5,TipoVehiculo.FURGONETA,EstadoPorte.ENTREGADO,1,1));
        s.add(new P("Valencia","Sevilla",      39.4699,-0.3763,37.3891,-5.9845, 654,720,  "Cítricos exportación",    2500,12.0,2.8,TipoVehiculo.RIGIDO,   EstadoPorte.FACTURADO,0,1));
        s.add(new P("Bilbao","Zaragoza",       43.2630,-2.9350,41.6488,-0.8891, 305,480,  "Acero galvanizado",       3500,14.0,3.0,TipoVehiculo.TRAILER,  EstadoPorte.FACTURADO,1,2));
        s.add(new P("Oviedo","Valladolid",     43.3614,-5.8494,41.6523,-4.7245, 320,490,  "Sidra embotellada",       2800,16.0,2.5,TipoVehiculo.RIGIDO,   EstadoPorte.ENTREGADO,4,9));

        // ── Pendientes (4) ──
        s.add(new P("Oviedo","Mieres",         43.3614,-5.8494,43.2500,-5.7750, 25, 80,  "Muebles oficina urgentes", 900, 5.0,2.0,TipoVehiculo.FURGONETA,EstadoPorte.PENDIENTE,2,null));
        s.add(new P("Gijón","Oviedo",          43.5322,-5.6611,43.3614,-5.8494, 30, 90,  "Cajas de bebidas varios", 1200, 6.0,1.5,TipoVehiculo.FURGONETA,EstadoPorte.PENDIENTE,3,null));
        s.add(new P("Avilés","Gijón",          43.5560,-5.9240,43.5322,-5.6611, 22, 75,  "Hierro forjado",          1800, 7.0,2.2,TipoVehiculo.RIGIDO,   EstadoPorte.PENDIENTE,2,null));
        s.add(new P("Oviedo","Avilés",         43.3614,-5.8494,43.5560,-5.9240, 30, 90,  "Neumáticos recambios",    1000, 5.0,1.4,TipoVehiculo.FURGONETA,EstadoPorte.PENDIENTE,4,null));

        List<Porte> created = new ArrayList<>();
        for(P p:s){
            Porte x=findPorte(p.or+" Asturias",p.de,p.desc).orElseGet(Porte::new);
            x.setOrigen(p.or+" Asturias"); x.setDestino(p.de+" Asturias");
            x.setLatitudOrigen(p.lo);x.setLongitudOrigen(p.ln);
            x.setLatitudDestino(p.ld);x.setLongitudDestino(p.lg);
            x.setDistanciaKm(p.km);x.setDistanciaEstimada(true);
            x.setPrecio(p.pr);x.setAjustePrecio(0.0);
            x.setDescripcionCliente(p.desc);x.setPesoTotalKg(p.pe);x.setVolumenTotalM3(p.vo);x.setLargoMaxPaquete(p.la);
            x.setTipoVehiculoRequerido(p.ti);x.setRevisionManual(false);x.setMotivoRevision(null);
            x.setEstado(p.es);x.setFechaCreacion(LocalDateTime.now().minusDays(7));
            x.setFechaRecogida(LocalDateTime.now().minusDays(3));
            x.setFechaEntrega(p.es==EstadoPorte.ENTREGADO||p.es==EstadoPorte.FACTURADO?LocalDateTime.now().minusDays(1):LocalDateTime.now().plusDays(1));
            x.setCliente(cl.get(p.ci));
            x.setConductor(p.coi!=null?co.get(p.coi):null);
            created.add(pr.save(x));
        }
        int y=LocalDate.now().getYear();
        for(Porte p:created){
            if(p.getEstado()!=EstadoPorte.FACTURADO||fr.findByPorteId(p.getId()).isPresent()) continue;
            Factura f=new Factura();f.setPorte(p);f.setBaseImponible(p.getPrecioFinal());f.setPagada(true);
            f.setFechaEmision(LocalDate.now().minusDays(2));f.setNumeroSerie(String.format("F-%d-%05d",y,p.getId()));
            fr.save(f);
        }
    }

    // ═══ Helpers ═══
    private Usuario upsert(String e,String pw,RolUsuario r,boolean a){ String n=e.toLowerCase(); Usuario u=ur.findByEmail(n).orElseGet(Usuario::new); u.setEmail(n); if(u.getPassword()==null||!pe.matches(pw,u.getPassword()))u.setPassword(pe.encode(pw)); u.setRol(r);u.setActivo(a);if(u.getFechaRegistro()==null)u.setFechaRegistro(LocalDateTime.now()); return ur.save(u); }
    private Optional<Porte> findPorte(String o,String d,String de){ return pr.findAll().stream().filter(p->o.equals(p.getOrigen())&&d.equals(p.getDestino())&&de.equals(p.getDescripcionCliente())).findFirst(); }

    private record C(String e,String n,String cif,String t,String d,String se){}
    private record D(String e,String n,String a,String dni,String t,String cb,Double la,Double lo){}
    private record V(String mat,String ma,String mo,TipoVehiculo ti,Integer ca,Integer la,Integer an,Integer al,String dni){}
    private record P(String or,String de,Double lo,Double ln,Double ld,Double lg,double km,double pr,String desc,double pe,double vo,double la,TipoVehiculo ti,EstadoPorte es,int ci,Integer coi){}
}
