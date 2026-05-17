const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

const box = 'rounded=1;whiteSpace=wrap;html=1;strokeColor=#000000;fillColor=#FFFFFF;fontColor=#000000;fontSize=13;align=center;verticalAlign=middle;';
const title = 'text;html=1;fontSize=24;fontStyle=1;align=center;strokeColor=none;fillColor=none;fontColor=#000000;';
const section = 'text;html=1;fontSize=15;fontStyle=1;align=center;strokeColor=none;fillColor=none;fontColor=#000000;';
const edgeStyle = 'endArrow=block;html=1;rounded=0;strokeColor=#000000;fontColor=#000000;fontSize=10;edgeStyle=orthogonalEdgeStyle;';
const dashed = box + 'dashed=1;';

function cell(id, value, x, y, w = 170, h = 55, style = box) {
  return `<mxCell id="${id}" value="${esc(value)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`;
}

function edge(id, source, target, label = '') {
  return `<mxCell id="${id}" value="${esc(label)}" style="${edgeStyle}" edge="1" parent="1" source="${source}" target="${target}"><mxGeometry relative="1" as="geometry"/></mxCell>`;
}

function model(items) {
  return `<mxGraphModel dx="1800" dy="950" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1654" pageHeight="1169" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${items.join('')}</root></mxGraphModel>`;
}

function pack(id, name, graph) {
  const payload = zlib.deflateRawSync(Buffer.from(encodeURIComponent(graph), 'utf8')).toString('base64');
  return `<mxfile host="app.diagrams.net" agent="ApiCargoHub" version="24.0.0"><diagram id="${id}" name="${esc(name)}">${payload}</diagram></mxfile>`;
}

function write(file, id, name, items) {
  fs.writeFileSync(path.join(__dirname, file), pack(id, name, model(items)), 'utf8');
  console.log(`Generated ${file}`);
}

function desktop() {
  const b = [];
  b.push(cell('title', 'Mapa de navegacion - Aplicacion de escritorio', 430, 25, 780, 45, title));
  b.push(cell('auth', 'Acceso', 90, 100, 180, 30, section));
  b.push(cell('main', 'Panel principal', 420, 100, 250, 30, section));
  b.push(cell('gestion', 'Gestion operativa', 760, 100, 530, 30, section));
  b.push(cell('soporte', 'Control y consulta', 1260, 100, 270, 30, section));

  b.push(cell('login', 'Login', 100, 190));
  b.push(cell('dashboard', 'Dashboard', 400, 190));
  b.push(cell('estadisticas', 'Estadisticas', 400, 330));
  b.push(cell('perfil', 'Perfil / busqueda', 400, 470, 190));

  b.push(cell('portes', 'Portes', 730, 160));
  b.push(cell('revision', 'Revision portes', 980, 160));
  b.push(cell('conductores', 'Conductores', 730, 300));
  b.push(cell('aprobaciones', 'Aprobaciones', 980, 300));
  b.push(cell('vehiculos', 'Vehiculos', 730, 440));
  b.push(cell('clientes', 'Clientes', 980, 440));

  b.push(cell('incidencias', 'Incidencias', 1260, 190));
  b.push(cell('facturas', 'Facturas', 1260, 330));
  b.push(cell('mapa', 'Mapa de flota', 1260, 470));

  let e = 1;
  b.push(edge('e'+e++, 'login', 'dashboard'));
  for (const t of ['estadisticas','perfil','portes','conductores','vehiculos','clientes','incidencias','facturas','mapa']) b.push(edge('e'+e++, 'dashboard', t));
  b.push(edge('e'+e++, 'portes', 'revision'));
  b.push(edge('e'+e++, 'conductores', 'aprobaciones'));
  b.push(edge('e'+e++, 'conductores', 'vehiculos'));
  b.push(edge('e'+e++, 'clientes', 'portes'));
  b.push(edge('e'+e++, 'portes', 'facturas'));
  return b;
}

function mobile() {
  const b = [];
  b.push(cell('title', 'Mapa de navegacion - Aplicacion movil conductor', 420, 25, 820, 45, title));
  b.push(cell('auth', 'Acceso', 90, 100, 180, 30, section));
  b.push(cell('home-sec', 'Inicio', 350, 100, 180, 30, section));
  b.push(cell('work', 'Trabajo diario', 660, 100, 500, 30, section));
  b.push(cell('extras', 'Gestion personal', 1210, 100, 300, 30, section));

  b.push(cell('login', 'Login', 90, 220));
  b.push(cell('home', 'Inicio', 350, 220));
  b.push(cell('ofertas', 'Ofertas', 640, 160));
  b.push(cell('portes', 'Portes', 640, 280));
  b.push(cell('detalle', 'Detalle porte', 880, 280));
  b.push(cell('tracking', 'Tracking', 640, 420));
  b.push(cell('firma', 'Firma entrega', 880, 420));
  b.push(cell('fotos', 'Fotos carga', 1080, 420));
  b.push(cell('incidencias', 'Incidencias', 880, 560));
  b.push(cell('nueva-inc', 'Nueva incidencia', 1080, 560));
  b.push(cell('agenda', 'Agenda', 1260, 180));
  b.push(cell('facturacion', 'Facturacion', 1260, 300));
  b.push(cell('perfil', 'Perfil', 1260, 420));
  b.push(cell('vehiculos', 'Vehiculos', 1260, 540));

  let e = 1;
  b.push(edge('e'+e++, 'login', 'home'));
  for (const t of ['ofertas','portes','tracking','incidencias','agenda','facturacion','perfil','vehiculos']) b.push(edge('e'+e++, 'home', t));
  b.push(edge('e'+e++, 'portes', 'detalle'));
  b.push(edge('e'+e++, 'detalle', 'firma'));
  b.push(edge('e'+e++, 'detalle', 'fotos'));
  b.push(edge('e'+e++, 'detalle', 'nueva-inc'));
  b.push(edge('e'+e++, 'tracking', 'firma'));
  b.push(edge('e'+e++, 'tracking', 'fotos'));
  b.push(edge('e'+e++, 'tracking', 'nueva-inc'));
  b.push(edge('e'+e++, 'incidencias', 'nueva-inc'));
  return b;
}

function web() {
  const b = [];
  b.push(cell('title', 'Mapa de navegacion - Portal web cliente', 450, 25, 760, 45, title));
  b.push(cell('public', 'Zona publica', 110, 100, 260, 30, section));
  b.push(cell('auth', 'Acceso', 500, 100, 210, 30, section));
  b.push(cell('portal', 'Portal cliente', 850, 100, 550, 30, section));

  b.push(cell('landing', 'Landing', 120, 210));
  b.push(cell('servicios', 'Servicios', 120, 340));
  b.push(cell('precios', 'Precios', 120, 470));
  b.push(cell('como', 'Como funciona', 120, 600));
  b.push(cell('login', 'Login', 500, 240));
  b.push(cell('registro', 'Registro', 500, 390));
  b.push(cell('no', 'Acceso no disponible', 500, 540, 180, 55, dashed));
  b.push(cell('panel', 'Panel cliente', 850, 240));
  b.push(cell('solicitar', 'Solicitar porte', 1100, 170));
  b.push(cell('mis-portes', 'Mis portes', 1100, 300));
  b.push(cell('tracking', 'Tracking porte', 1330, 300));
  b.push(cell('facturas', 'Mis facturas', 1100, 430));
  b.push(cell('perfil', 'Perfil', 1100, 560));

  let e = 1;
  for (const t of ['servicios','precios','como','login','registro']) b.push(edge('e'+e++, 'landing', t));
  b.push(edge('e'+e++, 'login', 'panel'));
  b.push(edge('e'+e++, 'registro', 'login'));
  b.push(edge('e'+e++, 'login', 'no', 'sin cliente'));
  for (const t of ['solicitar','mis-portes','facturas','perfil']) b.push(edge('e'+e++, 'panel', t));
  b.push(edge('e'+e++, 'mis-portes', 'tracking'));
  return b;
}

write('TFG_CargoHub_mapa_navegacion_escritorio.drawio', 'nav-desktop-horizontal', 'Navegacion escritorio', desktop());
write('TFG_CargoHub_mapa_navegacion_movil.drawio', 'nav-mobile-horizontal', 'Navegacion movil', mobile());
write('TFG_CargoHub_mapa_navegacion_web.drawio', 'nav-web-horizontal', 'Navegacion web', web());
