const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

const boxStyle = 'rounded=0;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;fontColor=#000000;fontSize=12;align=center;verticalAlign=middle;spacing=8;overflow=fill;';
const titleStyle = 'text;html=1;strokeColor=none;fillColor=none;fontColor=#000000;fontSize=24;fontStyle=1;align=center;verticalAlign=middle;whiteSpace=wrap;';
const sectionStyle = 'text;html=1;strokeColor=none;fillColor=none;fontColor=#000000;fontSize=16;fontStyle=1;align=center;verticalAlign=middle;whiteSpace=wrap;';
const dashedStyle = boxStyle + 'dashed=1;';
const edgeStyle = 'endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;strokeColor=#000000;fontColor=#000000;fontSize=11;';
const dashedEdge = edgeStyle + 'dashed=1;';

function cell(id, value, x, y, w = 190, h = 70, style = boxStyle) {
  return `<mxCell id="${id}" value="${esc(value)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`;
}

function edge(id, from, to, label = '', dashed = false) {
  return `<mxCell id="${id}" value="${esc(label)}" style="${dashed ? dashedEdge : edgeStyle}" edge="1" parent="1" source="${from}" target="${to}"><mxGeometry relative="1" as="geometry"/></mxCell>`;
}

function model(w, h, items) {
  return `<mxGraphModel dx="${w}" dy="${h}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${w}" pageHeight="${h}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${items.join('')}</root></mxGraphModel>`;
}

function packDiagram(id, name, graph) {
  const payload = zlib.deflateRawSync(Buffer.from(encodeURIComponent(graph), 'utf8')).toString('base64');
  return `<diagram id="${id}" name="${esc(name)}">${payload}</diagram>`;
}

function desktopMap() {
  const b = [];
  b.push(cell('title', 'Mapa de navegacion - Aplicacion de escritorio', 520, 20, 900, 40, titleStyle));
  b.push(cell('s-auth', 'Acceso', 80, 95, 300, 30, sectionStyle));
  b.push(cell('s-layout', 'MainLayout / Sidebar', 560, 95, 500, 30, sectionStyle));
  b.push(cell('s-pages', 'Vistas principales', 1180, 95, 500, 30, sectionStyle));
  b.push(cell('s-actions', 'Dialogos y acciones', 1180, 760, 500, 30, sectionStyle));

  b.push(cell('d-login', 'LoginView\n/login', 100, 150));
  b.push(cell('d-guard', 'Guard autenticacion\nrequiresAuth', 100, 270));
  b.push(cell('d-main', 'MainLayout\nSidebar + TopBar', 520, 210, 240, 85));
  b.push(cell('d-topbar', 'TopBar\nBusqueda global\nPerfil\nTema/idioma', 520, 360, 240, 100));
  b.push(cell('d-sidebar', 'Sidebar\nMenu principal', 800, 210, 210, 85));
  b.push(cell('d-profile', 'ProfileDialog\nfoto/perfil', 800, 390));
  b.push(cell('d-search', 'GlobalSearchBar\nbusqueda entidades', 800, 500));

  const pages = [
    ['d-dashboard','Dashboard\n/dashboard',1160,150],
    ['d-stats','Estadisticas\n/estadisticas',1400,150],
    ['d-portes','Portes\n/portes',1160,270],
    ['d-revision','Revision portes\n/revision-portes',1400,270],
    ['d-conductores','Conductores\n/conductores',1160,390],
    ['d-aprob','Aprobaciones\n/aprobacion-conductores',1400,390],
    ['d-vehiculos','Vehiculos\n/vehiculos',1160,510],
    ['d-incidencias','Incidencias\n/incidencias',1400,510],
    ['d-facturas','Facturas\n/facturas',1160,630],
    ['d-clientes','Clientes\n/clientes',1400,630],
    ['d-fleet','Mapa flota\n/fleet-map\n(feature flag)',1640,390, dashedStyle]
  ];
  for (const [id,label,x,y,style] of pages) b.push(cell(id,label,x,y,210,78,style || boxStyle));

  const actions = [
    ['d-porte-dialog','PorteDialog\ncrear/editar porte',1160,830],
    ['d-revision-dialog','RevisionPorteDialog\nvalidar carga',1400,830],
    ['d-fotos','PorteFotosSection\nfotos de carga',1640,830],
    ['d-cond-dialog','ConductorDialog\ncrear/editar',1160,950],
    ['d-veh-dialog','VehiculoDialog\ncrear/editar',1400,950],
    ['d-inc-dialog','IncidenciaDialog\ncrear/ver',1640,950],
    ['d-resolver','ResolverDialog\nresolver incidencia',1160,1070],
    ['d-fact-detail','FacturaDetail\nver/pagar/PDF',1400,1070],
    ['d-cli-dialog','ClienteDialog\ncrear/editar',1640,1070],
    ['d-cli-portes','ClientePortesTab\nportes del cliente',1400,1190]
  ];
  for (const [id,label,x,y] of actions) b.push(cell(id,label,x,y,220,78));

  let i = 1;
  b.push(edge('de'+i++,'d-login','d-guard','login OK'));
  b.push(edge('de'+i++,'d-guard','d-main','token valido'));
  b.push(edge('de'+i++,'d-main','d-topbar'));
  b.push(edge('de'+i++,'d-main','d-sidebar'));
  b.push(edge('de'+i++,'d-topbar','d-profile','avatar'));
  b.push(edge('de'+i++,'d-topbar','d-search','buscar'));
  for (const [id] of pages) b.push(edge('de'+i++,'d-sidebar',id));
  b.push(edge('de'+i++,'d-portes','d-porte-dialog'));
  b.push(edge('de'+i++,'d-portes','d-fotos'));
  b.push(edge('de'+i++,'d-revision','d-revision-dialog'));
  b.push(edge('de'+i++,'d-conductores','d-cond-dialog'));
  b.push(edge('de'+i++,'d-vehiculos','d-veh-dialog'));
  b.push(edge('de'+i++,'d-incidencias','d-inc-dialog'));
  b.push(edge('de'+i++,'d-incidencias','d-resolver'));
  b.push(edge('de'+i++,'d-facturas','d-fact-detail'));
  b.push(edge('de'+i++,'d-clientes','d-cli-dialog'));
  b.push(edge('de'+i++,'d-clientes','d-cli-portes'));
  return model(1900, 1320, b);
}

function webMap() {
  const b = [];
  b.push(cell('title', 'Mapa de navegacion - Portal web cliente', 520, 20, 900, 40, titleStyle));
  b.push(cell('s-public', 'Zona publica', 100, 95, 420, 30, sectionStyle));
  b.push(cell('s-auth', 'Autenticacion', 610, 95, 380, 30, sectionStyle));
  b.push(cell('s-portal', 'Portal cliente autenticado', 1120, 95, 520, 30, sectionStyle));

  b.push(cell('w-landing','LandingPage\n/',120,150));
  b.push(cell('w-services','Seccion servicios\n#servicios',120,280));
  b.push(cell('w-pricing','Seccion precios\n#precios',120,400));
  b.push(cell('w-how','Como funciona\n#como-funciona',120,520));
  b.push(cell('w-login','LoginView\n/login',620,180));
  b.push(cell('w-register','RegisterView\n/register',620,330));
  b.push(cell('w-guard','Guard portal\ntoken + clienteId',620,500,220,90));
  b.push(cell('w-unavailable','Acceso no disponible\n/portal/acceso-no-disponible',620,650,250,90,dashedStyle));

  b.push(cell('w-layout','PortalLayout\nSidebar + Topbar',1110,180,240,90));
  b.push(cell('w-dashboard','Dashboard\n/portal/dashboard',1110,330));
  b.push(cell('w-request','Solicitar porte\n/portal/solicitar-porte',1370,330));
  b.push(cell('w-portes','Mis portes\n/portal/mis-portes',1110,470));
  b.push(cell('w-tracking','Tracking porte\n/portal/portes/:id/tracking',1370,470,230,80));
  b.push(cell('w-tracking-modal','TrackingModal\nseguimiento modal',1630,470,220,80));
  b.push(cell('w-facturas','Mis facturas\n/portal/mis-facturas',1110,610));
  b.push(cell('w-perfil','Perfil\n/portal/perfil',1370,610));
  b.push(cell('w-logout','Cerrar sesion\nlogout -> /login',1240,760));
  b.push(cell('w-theme','Tema / idioma',1630,610,190,65));

  let i = 1;
  b.push(edge('we'+i++,'w-landing','w-services'));
  b.push(edge('we'+i++,'w-landing','w-pricing'));
  b.push(edge('we'+i++,'w-landing','w-how'));
  b.push(edge('we'+i++,'w-landing','w-login'));
  b.push(edge('we'+i++,'w-landing','w-register'));
  b.push(edge('we'+i++,'w-login','w-guard','login OK'));
  b.push(edge('we'+i++,'w-register','w-login','registro'));
  b.push(edge('we'+i++,'w-guard','w-layout','cliente valido'));
  b.push(edge('we'+i++,'w-guard','w-unavailable','sin clienteId',true));
  for (const id of ['w-dashboard','w-request','w-portes','w-facturas','w-perfil']) b.push(edge('we'+i++,'w-layout',id));
  b.push(edge('we'+i++,'w-layout','w-theme'));
  b.push(edge('we'+i++,'w-layout','w-logout'));
  b.push(edge('we'+i++,'w-portes','w-tracking','ver tracking'));
  b.push(edge('we'+i++,'w-portes','w-tracking-modal','modal'));
  b.push(edge('we'+i++,'w-logout','w-login'));
  return model(1900, 940, b);
}

function mobileMap() {
  const b = [];
  b.push(cell('title', 'Mapa de navegacion - Aplicacion movil conductor', 600, 20, 940, 40, titleStyle));
  b.push(cell('s-auth', 'Acceso y contenedor', 60, 95, 430, 30, sectionStyle));
  b.push(cell('s-home', 'Home / menu conductor', 580, 95, 520, 30, sectionStyle));
  b.push(cell('s-flows', 'Flujos principales', 1180, 95, 760, 30, sectionStyle));

  b.push(cell('m-login','LoginActivity\nemail/password',80,160,220,80));
  b.push(cell('m-session','SessionManager\nrol CONDUCTOR',80,300,220,80));
  b.push(cell('m-main','MainActivity\nTopbar + contenedor',80,450,240,85));
  b.push(cell('m-offline','Offline banner\nSyncManager',80,600,220,80,dashedStyle));
  b.push(cell('m-home','HomeFragment\nperfil + tarjetas',600,210,260,95));
  b.push(cell('m-profile','ProfileFragment\neditar perfil/foto',600,360,230,75));
  b.push(cell('m-vehicle','VehicleFragment\nmis vehiculos',900,360,230,75));
  b.push(cell('m-agenda','AgendaFragment\nbloqueos',900,490,230,75));
  b.push(cell('m-billing','FacturacionDashboard\nKPIs conductor',600,490,250,75));

  b.push(cell('m-offers','OfferInboxFragment\nofertas',1210,150,240,75));
  b.push(cell('m-offer-detail','OfferDetailFragment\naceptar/rechazar',1510,150,250,75));
  b.push(cell('m-portes','PortesFragment\nproximos/historial',1210,280,250,80));
  b.push(cell('m-trip-list','TripListFragment\nlista filtrada',1510,280,230,75));
  b.push(cell('m-trip-detail','TripDetailFragment\ndetalle porte',1780,280,230,75));
  b.push(cell('m-firma','FirmaEntregaFragment\nfirma entrega',1780,410,240,75));
  b.push(cell('m-fotos','FotoCargaFragment\nfotos carga',1510,410,230,75));

  b.push(cell('m-tracking','TrackingStatusFragment\nmapa/tracking',1210,540,250,85));
  b.push(cell('m-pause','PauseReasonBottomSheet\npausa tracking',1510,540,250,75));
  b.push(cell('m-driving','Modo conduccion\nFABs mapa',1780,540,230,75));
  b.push(cell('m-inc-options','IncidenciasOptions\nactivas/historial',1210,720,260,80));
  b.push(cell('m-inc-active','IncidenciasActivas\nlista',1510,700,230,70));
  b.push(cell('m-inc-history','HistorialIncidencias\nresueltas',1510,790,230,70));
  b.push(cell('m-inc-new','NuevaIncidencia\ncrear',1780,700,230,70));
  b.push(cell('m-inc-detail','IncidenciaDetail\ndetalle',1780,790,230,70));

  let i = 1;
  b.push(edge('me'+i++,'m-login','m-session','login OK'));
  b.push(edge('me'+i++,'m-session','m-main','sesion valida'));
  b.push(edge('me'+i++,'m-main','m-home'));
  b.push(edge('me'+i++,'m-main','m-offline','sin red',true));
  for (const id of ['m-tracking','m-offers','m-portes','m-inc-options','m-agenda','m-billing','m-profile','m-vehicle']) b.push(edge('me'+i++,'m-home',id));
  b.push(edge('me'+i++,'m-offers','m-offer-detail','seleccionar oferta'));
  b.push(edge('me'+i++,'m-portes','m-trip-list','tabs/filtros'));
  b.push(edge('me'+i++,'m-trip-list','m-trip-detail','seleccionar porte'));
  b.push(edge('me'+i++,'m-trip-detail','m-inc-new','reportar'));
  b.push(edge('me'+i++,'m-trip-detail','m-fotos','fotos'));
  b.push(edge('me'+i++,'m-trip-detail','m-inc-detail','ver incidencia'));
  b.push(edge('me'+i++,'m-trip-detail','m-firma','entrega'));
  b.push(edge('me'+i++,'m-tracking','m-pause','pausar'));
  b.push(edge('me'+i++,'m-tracking','m-driving','modo conduccion'));
  b.push(edge('me'+i++,'m-tracking','m-portes','ver portes'));
  b.push(edge('me'+i++,'m-tracking','m-inc-new','incidencia'));
  b.push(edge('me'+i++,'m-tracking','m-fotos','fotos'));
  b.push(edge('me'+i++,'m-tracking','m-firma','finalizar'));
  b.push(edge('me'+i++,'m-inc-options','m-inc-active','tab activas'));
  b.push(edge('me'+i++,'m-inc-options','m-inc-history','tab historial'));
  b.push(edge('me'+i++,'m-inc-options','m-inc-new','nueva'));
  b.push(edge('me'+i++,'m-inc-active','m-inc-detail','detalle'));
  b.push(edge('me'+i++,'m-inc-history','m-inc-detail','detalle'));
  return model(2100, 980, b);
}

const output = `<mxfile host="app.diagrams.net" agent="ApiCargoHub" version="24.0.0">${packDiagram('nav-desktop','1. Navegacion escritorio',desktopMap())}${packDiagram('nav-web','2. Navegacion portal web',webMap())}${packDiagram('nav-mobile','3. Navegacion movil conductor',mobileMap())}</mxfile>`;
fs.writeFileSync(path.join(__dirname, 'TFG_CargoHub_mapas_navegacion_3_apps.drawio'), output, 'utf8');
console.log('Generated TFG_CargoHub_mapas_navegacion_3_apps.drawio');
