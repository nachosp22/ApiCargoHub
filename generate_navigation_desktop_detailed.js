const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

const box = 'rounded=1;whiteSpace=wrap;html=1;strokeColor=#000000;fillColor=#FFFFFF;fontColor=#000000;fontSize=12;align=center;verticalAlign=middle;spacing=6;';
const section = 'text;html=1;fontSize=15;fontStyle=1;align=center;strokeColor=none;fillColor=none;fontColor=#000000;';
const title = 'text;html=1;fontSize=24;fontStyle=1;align=center;strokeColor=none;fillColor=none;fontColor=#000000;';
const edge = 'endArrow=block;html=1;rounded=0;strokeColor=#000000;fontColor=#000000;fontSize=10;edgeStyle=orthogonalEdgeStyle;';
const dashed = edge + 'dashed=1;';

function cell(id, value, x, y, w = 165, h = 50, style = box) {
  return `<mxCell id="${id}" value="${esc(value)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`;
}

function link(id, source, target, label = '', style = edge) {
  return `<mxCell id="${id}" value="${esc(label)}" style="${style}" edge="1" parent="1" source="${source}" target="${target}"><mxGeometry relative="1" as="geometry"/></mxCell>`;
}

function model(items) {
  return `<mxGraphModel dx="1900" dy="1150" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1800" pageHeight="1200" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${items.join('')}</root></mxGraphModel>`;
}

function pack(graph) {
  const payload = zlib.deflateRawSync(Buffer.from(encodeURIComponent(graph), 'utf8')).toString('base64');
  return `<mxfile host="app.diagrams.net" agent="ApiCargoHub" version="24.0.0"><diagram id="nav-desktop-detallado" name="Navegacion escritorio detallada">${payload}</diagram></mxfile>`;
}

const b = [];
b.push(cell('title', 'Mapa de navegacion - Aplicacion de escritorio / Administracion', 430, 20, 950, 45, title));

// Section labels
b.push(cell('sec-acceso', 'Acceso', 70, 95, 170, 30, section));
b.push(cell('sec-base', 'Estructura base', 310, 95, 260, 30, section));
b.push(cell('sec-menu', 'Menu lateral', 610, 95, 300, 30, section));
b.push(cell('sec-modulos', 'Modulos principales', 930, 95, 390, 30, section));
b.push(cell('sec-acciones', 'Acciones / detalles', 1360, 95, 330, 30, section));

// Access + base layout
b.push(cell('login', 'Login', 80, 180));
b.push(cell('guard', 'Guard de autenticacion', 80, 300, 175));
b.push(cell('layout', 'MainLayout', 320, 220));
b.push(cell('sidebar', 'AppSidebar', 320, 350));
b.push(cell('topbar', 'AppTopBar', 320, 480));
b.push(cell('search', 'Busqueda global', 320, 610));
b.push(cell('profile', 'Perfil / foto usuario', 320, 730));

// Sidebar menu nodes
const menuX = 620;
const menu = [
  ['dashboard', 'Dashboard', 150],
  ['estadisticas', 'Estadisticas', 230],
  ['portes', 'Portes', 310],
  ['revision', 'Revision portes', 390],
  ['conductores', 'Conductores', 470],
  ['aprobaciones', 'Aprobaciones', 550],
  ['vehiculos', 'Vehiculos', 630],
  ['incidencias', 'Incidencias', 710],
  ['facturas', 'Facturas', 790],
  ['clientes', 'Clientes', 870],
  ['fleet', 'Mapa de flota', 950]
];
for (const [id, label, y] of menu) b.push(cell(id, label, menuX, y));

// Module detail / action nodes
b.push(cell('dash-kpis', 'KPIs y actividad reciente', 950, 145, 210));
b.push(cell('stats-detail', 'Graficas / metricas', 950, 225, 190));

b.push(cell('portes-list', 'Listado de portes', 950, 305, 190));
b.push(cell('porte-dialog', 'Crear / editar porte', 1360, 275, 190));
b.push(cell('porte-fotos', 'Fotos de carga', 1580, 275, 170));
b.push(cell('revision-dialog', 'Revisar solicitud', 1360, 390, 190));

b.push(cell('conductores-list', 'Listado conductores', 950, 465, 190));
b.push(cell('conductor-dialog', 'Crear / editar conductor', 1360, 485, 200));
b.push(cell('aprobacion-flow', 'Aprobar / rechazar', 1580, 545, 180));

b.push(cell('vehiculos-list', 'Listado vehiculos', 950, 630, 190));
b.push(cell('vehiculo-dialog', 'Crear / editar vehiculo', 1360, 625, 200));

b.push(cell('inc-list', 'Listado incidencias', 950, 710, 190));
b.push(cell('inc-detail', 'Detalle incidencia', 1360, 710, 190));
b.push(cell('inc-resolver', 'Resolver incidencia', 1580, 710, 180));

b.push(cell('fact-list', 'Listado facturas', 950, 790, 190));
b.push(cell('fact-detail', 'Detalle factura', 1360, 790, 190));
b.push(cell('fact-actions', 'Pagar / descargar PDF', 1580, 790, 190));

b.push(cell('clientes-list', 'Listado clientes', 950, 870, 190));
b.push(cell('cliente-dialog', 'Crear / editar cliente', 1360, 870, 200));
b.push(cell('cliente-portes', 'Portes del cliente', 1580, 870, 180));

b.push(cell('fleet-map', 'Mapa con conductores y portes', 950, 950, 230));

let e = 1;
b.push(link('e'+e++, 'login', 'guard', 'credenciales'));
b.push(link('e'+e++, 'guard', 'layout', 'token valido'));
b.push(link('e'+e++, 'layout', 'sidebar'));
b.push(link('e'+e++, 'layout', 'topbar'));
b.push(link('e'+e++, 'topbar', 'search'));
b.push(link('e'+e++, 'topbar', 'profile'));
for (const [id] of menu) b.push(link('e'+e++, 'sidebar', id));

b.push(link('e'+e++, 'dashboard', 'dash-kpis'));
b.push(link('e'+e++, 'estadisticas', 'stats-detail'));
b.push(link('e'+e++, 'portes', 'portes-list'));
b.push(link('e'+e++, 'portes-list', 'porte-dialog'));
b.push(link('e'+e++, 'portes-list', 'porte-fotos'));
b.push(link('e'+e++, 'revision', 'revision-dialog'));
b.push(link('e'+e++, 'conductores', 'conductores-list'));
b.push(link('e'+e++, 'conductores-list', 'conductor-dialog'));
b.push(link('e'+e++, 'aprobaciones', 'aprobacion-flow'));
b.push(link('e'+e++, 'vehiculos', 'vehiculos-list'));
b.push(link('e'+e++, 'vehiculos-list', 'vehiculo-dialog'));
b.push(link('e'+e++, 'incidencias', 'inc-list'));
b.push(link('e'+e++, 'inc-list', 'inc-detail'));
b.push(link('e'+e++, 'inc-detail', 'inc-resolver'));
b.push(link('e'+e++, 'facturas', 'fact-list'));
b.push(link('e'+e++, 'fact-list', 'fact-detail'));
b.push(link('e'+e++, 'fact-detail', 'fact-actions'));
b.push(link('e'+e++, 'clientes', 'clientes-list'));
b.push(link('e'+e++, 'clientes-list', 'cliente-dialog'));
b.push(link('e'+e++, 'clientes-list', 'cliente-portes'));
b.push(link('e'+e++, 'fleet', 'fleet-map'));

fs.writeFileSync(path.join(__dirname, 'TFG_CargoHub_mapa_navegacion_escritorio.drawio'), pack(model(b)), 'utf8');
console.log('Generated detailed desktop navigation map');
