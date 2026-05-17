const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function esc(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

const styles = {
  box: 'rounded=1;whiteSpace=wrap;html=1;strokeColor=#000000;fillColor=#FFFFFF;fontColor=#000000;fontSize=12;align=center;verticalAlign=middle;spacing=6;',
  section: 'text;html=1;fontSize=15;fontStyle=1;align=center;strokeColor=none;fillColor=none;fontColor=#000000;',
  title: 'text;html=1;fontSize=24;fontStyle=1;align=center;strokeColor=none;fillColor=none;fontColor=#000000;',
  edge: 'endArrow=block;html=1;rounded=0;strokeColor=#000000;fontColor=#000000;fontSize=10;edgeStyle=orthogonalEdgeStyle;'
};

function cell(id, value, x, y, w = 165, h = 50, style = styles.box) {
  return `<mxCell id="${id}" value="${esc(value)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`;
}

function link(id, source, target, label = '', style = styles.edge) {
  return `<mxCell id="${id}" value="${esc(label)}" style="${style}" edge="1" parent="1" source="${source}" target="${target}"><mxGeometry relative="1" as="geometry"/></mxCell>`;
}

function model(edges, vertices, pageWidth = 1800, pageHeight = 1200) {
  // IMPORTANT: edges are placed before vertices so arrows stay behind nodes in draw.io.
  return `<mxGraphModel dx="1900" dy="1150" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${pageWidth}" pageHeight="${pageHeight}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${edges.join('')}${vertices.join('')}</root></mxGraphModel>`;
}

function pack(graph, id, name) {
  const payload = zlib.deflateRawSync(Buffer.from(encodeURIComponent(graph), 'utf8')).toString('base64');
  return `<mxfile host="app.diagrams.net" agent="ApiCargoHub" version="24.0.0"><diagram id="${id}" name="${esc(name)}">${payload}</diagram></mxfile>`;
}

function writeDrawio(fileName, id, name, edges, vertices) {
  fs.writeFileSync(path.join(__dirname, fileName), pack(model(edges, vertices), id, name), 'utf8');
  console.log(`Generated ${fileName}: nodes=${vertices.filter(v => v.includes('vertex="1"')).length} edges=${edges.length}`);
}

function desktop() {
  const v = [];
  const e = [];
  v.push(cell('title', 'Mapa de navegacion - Aplicacion de escritorio / Administracion', 430, 20, 950, 45, styles.title));
  v.push(cell('sec-acceso', 'Acceso', 70, 95, 170, 30, styles.section));
  v.push(cell('sec-base', 'Estructura base', 310, 95, 260, 30, styles.section));
  v.push(cell('sec-menu', 'Menu lateral', 610, 95, 300, 30, styles.section));
  v.push(cell('sec-modulos', 'Modulos principales', 930, 95, 390, 30, styles.section));
  v.push(cell('sec-acciones', 'Acciones / detalles', 1360, 95, 330, 30, styles.section));

  v.push(cell('login', 'Login', 80, 180));
  v.push(cell('guard', 'Guard de autenticacion', 80, 300, 175));
  v.push(cell('layout', 'MainLayout', 320, 220));
  v.push(cell('sidebar', 'AppSidebar', 320, 350));
  v.push(cell('topbar', 'AppTopBar', 320, 480));
  v.push(cell('search', 'Busqueda global', 320, 610));
  v.push(cell('profile', 'Perfil / foto usuario', 320, 730));

  const menu = [
    ['dashboard', 'Dashboard', 150], ['estadisticas', 'Estadisticas', 230], ['portes', 'Portes', 310],
    ['revision', 'Revision portes', 390], ['conductores', 'Conductores', 470], ['aprobaciones', 'Aprobaciones', 550],
    ['vehiculos', 'Vehiculos', 630], ['incidencias', 'Incidencias', 710], ['facturas', 'Facturas', 790],
    ['clientes', 'Clientes', 870], ['fleet', 'Mapa de flota', 950]
  ];
  for (const [id, label, y] of menu) v.push(cell(id, label, 620, y));

  v.push(cell('dash-kpis', 'KPIs y actividad reciente', 950, 145, 210));
  v.push(cell('stats-detail', 'Graficas / metricas', 950, 225, 190));
  v.push(cell('portes-list', 'Listado de portes', 950, 305, 190));
  v.push(cell('porte-dialog', 'Crear / editar porte', 1360, 275, 190));
  v.push(cell('porte-fotos', 'Fotos de carga', 1580, 275, 170));
  v.push(cell('revision-dialog', 'Revisar solicitud', 1360, 390, 190));
  v.push(cell('conductores-list', 'Listado conductores', 950, 465, 190));
  v.push(cell('conductor-dialog', 'Crear / editar conductor', 1360, 485, 200));
  v.push(cell('aprobacion-flow', 'Aprobar / rechazar', 1580, 545, 180));
  v.push(cell('vehiculos-list', 'Listado vehiculos', 950, 630, 190));
  v.push(cell('vehiculo-dialog', 'Crear / editar vehiculo', 1360, 625, 200));
  v.push(cell('inc-list', 'Listado incidencias', 950, 710, 190));
  v.push(cell('inc-detail', 'Detalle incidencia', 1360, 710, 190));
  v.push(cell('inc-resolver', 'Resolver incidencia', 1580, 710, 180));
  v.push(cell('fact-list', 'Listado facturas', 950, 790, 190));
  v.push(cell('fact-detail', 'Detalle factura', 1360, 790, 190));
  v.push(cell('fact-actions', 'Pagar / descargar PDF', 1580, 790, 190));
  v.push(cell('clientes-list', 'Listado clientes', 950, 870, 190));
  v.push(cell('cliente-dialog', 'Crear / editar cliente', 1360, 870, 200));
  v.push(cell('cliente-portes', 'Portes del cliente', 1580, 870, 180));
  v.push(cell('fleet-map', 'Mapa con conductores y portes', 950, 950, 230));

  let n = 1;
  e.push(link('e' + n++, 'login', 'guard', 'credenciales'));
  e.push(link('e' + n++, 'guard', 'layout', 'token valido'));
  e.push(link('e' + n++, 'layout', 'sidebar'));
  e.push(link('e' + n++, 'layout', 'topbar'));
  e.push(link('e' + n++, 'topbar', 'search'));
  e.push(link('e' + n++, 'topbar', 'profile'));
  for (const [id] of menu) e.push(link('e' + n++, 'sidebar', id));
  for (const pair of [
    ['dashboard', 'dash-kpis'], ['estadisticas', 'stats-detail'], ['portes', 'portes-list'], ['portes-list', 'porte-dialog'],
    ['portes-list', 'porte-fotos'], ['revision', 'revision-dialog'], ['conductores', 'conductores-list'],
    ['conductores-list', 'conductor-dialog'], ['aprobaciones', 'aprobacion-flow'], ['vehiculos', 'vehiculos-list'],
    ['vehiculos-list', 'vehiculo-dialog'], ['incidencias', 'inc-list'], ['inc-list', 'inc-detail'], ['inc-detail', 'inc-resolver'],
    ['facturas', 'fact-list'], ['fact-list', 'fact-detail'], ['fact-detail', 'fact-actions'], ['clientes', 'clientes-list'],
    ['clientes-list', 'cliente-dialog'], ['clientes-list', 'cliente-portes'], ['fleet', 'fleet-map']
  ]) e.push(link('e' + n++, pair[0], pair[1]));

  writeDrawio('TFG_CargoHub_mapa_navegacion_escritorio.drawio', 'nav-desktop-detallado', 'Navegacion escritorio detallada', e, v);
}

function mobile() {
  const v = [];
  const e = [];
  v.push(cell('title', 'Mapa de navegacion - Aplicacion movil / Conductor', 500, 20, 820, 45, styles.title));
  v.push(cell('sec-acceso', 'Acceso', 70, 95, 170, 30, styles.section));
  v.push(cell('sec-base', 'Inicio y navegacion', 315, 95, 260, 30, styles.section));
  v.push(cell('sec-menu', 'Secciones', 640, 95, 300, 30, styles.section));
  v.push(cell('sec-detalle', 'Detalle operativo', 990, 95, 330, 30, styles.section));
  v.push(cell('sec-accion', 'Acciones principales', 1390, 95, 320, 30, styles.section));

  v.push(cell('login', 'Login conductor', 80, 180));
  v.push(cell('session', 'Validar sesion / token', 80, 300, 180));
  v.push(cell('home', 'Pantalla inicio', 330, 220));
  v.push(cell('bottomnav', 'Barra inferior', 330, 350));
  v.push(cell('sync', 'Estado de sincronizacion', 330, 480, 190));
  v.push(cell('profile', 'Perfil conductor', 330, 610));

  const menu = [
    ['tracking', 'Tracking activo', 150], ['ofertas', 'Ofertas', 240], ['portes', 'Mis portes', 330],
    ['agenda', 'Agenda', 420], ['incidencias', 'Incidencias', 510], ['facturacion', 'Facturacion', 600],
    ['vehiculo', 'Vehiculo', 690], ['historial', 'Historial', 780]
  ];
  for (const [id, label, y] of menu) v.push(cell(id, label, 650, y));

  v.push(cell('tracking-map', 'Mapa / ubicacion actual', 1000, 145, 210));
  v.push(cell('gps-send', 'Envio GPS periodico', 1390, 145, 200));
  v.push(cell('offer-detail', 'Detalle oferta', 1000, 240, 190));
  v.push(cell('offer-actions', 'Aceptar / rechazar', 1390, 240, 190));
  v.push(cell('portes-tabs', 'Proximos / en curso', 1000, 330, 190));
  v.push(cell('porte-detail', 'Detalle del porte', 1390, 330, 190));
  v.push(cell('estado-actions', 'Iniciar / recoger / entregar', 1585, 330, 195));
  v.push(cell('albaran', 'Firma y albaran', 1390, 430, 190));
  v.push(cell('agenda-detail', 'Disponibilidad / bloqueos', 1000, 510, 210));
  v.push(cell('inc-list', 'Listado incidencias', 1000, 600, 190));
  v.push(cell('inc-create', 'Crear incidencia', 1390, 600, 190));
  v.push(cell('fact-list', 'Facturas / pagos', 1000, 690, 190));
  v.push(cell('vehicle-detail', 'Datos del vehiculo', 1000, 780, 190));
  v.push(cell('history-detail', 'Portes finalizados', 1390, 780, 190));

  let n = 1;
  e.push(link('m' + n++, 'login', 'session'));
  e.push(link('m' + n++, 'session', 'home'));
  e.push(link('m' + n++, 'home', 'bottomnav'));
  e.push(link('m' + n++, 'home', 'sync'));
  e.push(link('m' + n++, 'home', 'profile'));
  for (const [id] of menu) e.push(link('m' + n++, 'bottomnav', id));
  for (const pair of [
    ['tracking', 'tracking-map'], ['tracking-map', 'gps-send'], ['ofertas', 'offer-detail'], ['offer-detail', 'offer-actions'],
    ['portes', 'portes-tabs'], ['portes-tabs', 'porte-detail'], ['porte-detail', 'estado-actions'], ['porte-detail', 'albaran'],
    ['agenda', 'agenda-detail'], ['incidencias', 'inc-list'], ['inc-list', 'inc-create'], ['facturacion', 'fact-list'],
    ['vehiculo', 'vehicle-detail'], ['historial', 'history-detail']
  ]) e.push(link('m' + n++, pair[0], pair[1]));

  writeDrawio('TFG_CargoHub_mapa_navegacion_movil.drawio', 'nav-mobile-detallado', 'Navegacion movil detallada', e, v);
}

function web() {
  const v = [];
  const e = [];
  v.push(cell('title', 'Mapa de navegacion - Portal web / Cliente', 560, 20, 700, 45, styles.title));
  v.push(cell('sec-publica', 'Zona publica', 70, 95, 190, 30, styles.section));
  v.push(cell('sec-acceso', 'Acceso', 335, 95, 190, 30, styles.section));
  v.push(cell('sec-panel', 'Panel cliente', 640, 95, 260, 30, styles.section));
  v.push(cell('sec-modulos', 'Modulos', 980, 95, 300, 30, styles.section));
  v.push(cell('sec-detalle', 'Acciones / detalle', 1380, 95, 320, 30, styles.section));

  v.push(cell('landing', 'Landing / inicio', 80, 180));
  v.push(cell('info', 'Informacion servicio', 80, 300, 180));
  v.push(cell('login', 'Login', 340, 180));
  v.push(cell('registro', 'Registro cliente', 340, 300));
  v.push(cell('validacion', 'Validar cuenta', 340, 420));
  v.push(cell('panel', 'Panel cliente', 650, 250));
  v.push(cell('menu', 'Menu cliente', 650, 390));
  v.push(cell('perfil', 'Perfil / datos empresa', 650, 530, 190));

  const menu = [
    ['solicitar', 'Solicitar porte', 170], ['misportes', 'Mis portes', 280], ['tracking', 'Seguimiento', 390],
    ['facturas', 'Mis facturas', 500], ['soporte', 'Incidencias / contacto', 610]
  ];
  for (const [id, label, y] of menu) v.push(cell(id, label, 990, y));

  v.push(cell('form-porte', 'Formulario de solicitud', 1380, 160, 210));
  v.push(cell('carga', 'Datos de carga y fechas', 1600, 160, 180));
  v.push(cell('confirmacion', 'Confirmar solicitud', 1380, 260, 200));
  v.push(cell('portes-list', 'Listado de portes propios', 1380, 360, 210));
  v.push(cell('porte-detail', 'Detalle del porte', 1600, 360, 180));
  v.push(cell('tracking-detail', 'Estado y ubicacion', 1380, 470, 200));
  v.push(cell('fact-list', 'Listado facturas', 1380, 580, 190));
  v.push(cell('fact-download', 'Ver / descargar factura', 1600, 580, 190));
  v.push(cell('support-form', 'Crear consulta / incidencia', 1380, 690, 210));

  let n = 1;
  e.push(link('w' + n++, 'landing', 'info'));
  e.push(link('w' + n++, 'landing', 'login'));
  e.push(link('w' + n++, 'landing', 'registro'));
  e.push(link('w' + n++, 'registro', 'validacion'));
  e.push(link('w' + n++, 'login', 'panel'));
  e.push(link('w' + n++, 'validacion', 'panel'));
  e.push(link('w' + n++, 'panel', 'menu'));
  e.push(link('w' + n++, 'panel', 'perfil'));
  for (const [id] of menu) e.push(link('w' + n++, 'menu', id));
  for (const pair of [
    ['solicitar', 'form-porte'], ['form-porte', 'carga'], ['form-porte', 'confirmacion'],
    ['misportes', 'portes-list'], ['portes-list', 'porte-detail'], ['tracking', 'tracking-detail'],
    ['facturas', 'fact-list'], ['fact-list', 'fact-download'], ['soporte', 'support-form']
  ]) e.push(link('w' + n++, pair[0], pair[1]));

  writeDrawio('TFG_CargoHub_mapa_navegacion_web.drawio', 'nav-web-detallado', 'Navegacion web detallada', e, v);
}

desktop();
mobile();
web();
