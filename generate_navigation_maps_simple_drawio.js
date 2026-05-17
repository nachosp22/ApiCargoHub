const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function cell(id, value, x, y, w = 150, h = 45, style = 'rounded=1;whiteSpace=wrap;html=1;strokeColor=#000000;fillColor=#FFFFFF') {
  return `<mxCell id="${id}" value="${esc(value)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`;
}

function edge(id, source, target, label = '') {
  return `<mxCell id="${id}" value="${esc(label)}" style="endArrow=block;html=1;rounded=0;strokeColor=#000000;fontSize=10" edge="1" parent="1" source="${source}" target="${target}"><mxGeometry relative="1" as="geometry"/></mxCell>`;
}

function model(items) {
  return `<mxGraphModel dx="1800" dy="1200" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1700" pageHeight="1150" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${items.join('')}</root></mxGraphModel>`;
}

function pack(graph) {
  return zlib.deflateRawSync(Buffer.from(encodeURIComponent(graph), 'utf8')).toString('base64');
}

const b = [];
const titleStyle = 'text;html=1;fontSize=24;fontStyle=1;align=center;strokeColor=none;fillColor=none';
const desktopLane = 'swimlane;html=1;whiteSpace=wrap;fillColor=#FFFFFF;strokeColor=#000000;fontStyle=1';
const mobileLane = 'swimlane;html=1;whiteSpace=wrap;fillColor=#FFFFFF;strokeColor=#000000;fontStyle=1';
const webLane = 'swimlane;html=1;whiteSpace=wrap;fillColor=#FFFFFF;strokeColor=#000000;fontStyle=1';

b.push(cell('title', 'Mapa de navegacion por aplicacion', 480, 20, 760, 40, titleStyle));
b.push(cell('nav-desktop', 'Aplicacion de escritorio - Administracion', 50, 100, 500, 900, desktopLane));
b.push(cell('nav-mobile', 'Aplicacion movil - Conductor', 600, 100, 500, 900, mobileLane));
b.push(cell('nav-web', 'Portal web - Cliente', 1150, 100, 500, 900, webLane));

// Desktop / admin
b.push(cell('d-login', 'Login', 225, 160));
b.push(cell('d-dashboard', 'Dashboard', 225, 240));
b.push(cell('d-est', 'Estadisticas', 95, 340));
b.push(cell('d-portes', 'Portes', 275, 340));
b.push(cell('d-revision', 'Revision portes', 365, 430));
b.push(cell('d-cond', 'Conductores', 95, 430));
b.push(cell('d-aprob', 'Aprobaciones', 275, 520));
b.push(cell('d-veh', 'Vehiculos', 365, 610));
b.push(cell('d-inc', 'Incidencias', 95, 610));
b.push(cell('d-fac', 'Facturas', 275, 700));
b.push(cell('d-cli', 'Clientes', 95, 700));
b.push(cell('d-map', 'Mapa de flota', 365, 790));
b.push(cell('d-profile', 'Perfil / Busqueda', 185, 880, 190, 45));

// Mobile / driver
b.push(cell('m-login', 'Login', 775, 160));
b.push(cell('m-home', 'Inicio', 775, 240));
b.push(cell('m-ofertas', 'Ofertas', 645, 340));
b.push(cell('m-portes', 'Portes', 825, 340));
b.push(cell('m-detalle', 'Detalle porte', 825, 430));
b.push(cell('m-track', 'Tracking', 645, 430));
b.push(cell('m-firma', 'Firma entrega', 825, 520));
b.push(cell('m-fotos', 'Fotos carga', 1005, 520));
b.push(cell('m-inc', 'Incidencias', 645, 610));
b.push(cell('m-inc-new', 'Nueva incidencia', 825, 700));
b.push(cell('m-agenda', 'Agenda', 1005, 610));
b.push(cell('m-billing', 'Facturacion', 645, 790));
b.push(cell('m-profile', 'Perfil', 825, 790));
b.push(cell('m-veh', 'Vehiculos', 1005, 790));

// Web / portal
b.push(cell('w-land', 'Landing', 1325, 160));
b.push(cell('w-auth', 'Login / Registro', 1325, 240));
b.push(cell('w-panel', 'Panel cliente', 1325, 330));
b.push(cell('w-sol', 'Solicitar porte', 1215, 430));
b.push(cell('w-mis', 'Mis portes', 1435, 430));
b.push(cell('w-track', 'Tracking porte', 1435, 520));
b.push(cell('w-fac', 'Mis facturas', 1215, 520));
b.push(cell('w-prof', 'Perfil', 1325, 610));
b.push(cell('w-no', 'Acceso no disponible', 1325, 700, 170, 45));

let e = 1;
// Desktop edges
b.push(edge('e'+e++, 'd-login', 'd-dashboard'));
for (const target of ['d-est','d-portes','d-cond','d-inc','d-cli','d-profile']) b.push(edge('e'+e++, 'd-dashboard', target));
b.push(edge('e'+e++, 'd-portes', 'd-revision'));
b.push(edge('e'+e++, 'd-cond', 'd-aprob'));
b.push(edge('e'+e++, 'd-cond', 'd-veh'));
b.push(edge('e'+e++, 'd-portes', 'd-fac'));
b.push(edge('e'+e++, 'd-dashboard', 'd-map'));

// Mobile edges
b.push(edge('e'+e++, 'm-login', 'm-home'));
for (const target of ['m-ofertas','m-portes','m-track','m-inc','m-agenda','m-billing','m-profile','m-veh']) b.push(edge('e'+e++, 'm-home', target));
b.push(edge('e'+e++, 'm-portes', 'm-detalle'));
b.push(edge('e'+e++, 'm-detalle', 'm-firma'));
b.push(edge('e'+e++, 'm-detalle', 'm-fotos'));
b.push(edge('e'+e++, 'm-detalle', 'm-inc-new'));
b.push(edge('e'+e++, 'm-track', 'm-firma'));
b.push(edge('e'+e++, 'm-track', 'm-fotos'));
b.push(edge('e'+e++, 'm-track', 'm-inc-new'));
b.push(edge('e'+e++, 'm-inc', 'm-inc-new'));

// Web edges
b.push(edge('e'+e++, 'w-land', 'w-auth'));
b.push(edge('e'+e++, 'w-auth', 'w-panel'));
for (const target of ['w-sol','w-mis','w-fac','w-prof']) b.push(edge('e'+e++, 'w-panel', target));
b.push(edge('e'+e++, 'w-mis', 'w-track'));
b.push(edge('e'+e++, 'w-auth', 'w-no', 'sin cliente'));

const graph = model(b);
const out = `<mxfile host="app.diagrams.net" agent="ApiCargoHub" version="24.0.0"><diagram id="nav-simple" name="Mapa navegacion simplificado">${pack(graph)}</diagram></mxfile>`;
fs.writeFileSync(path.join(__dirname, 'TFG_CargoHub_mapa_navegacion_simplificado.drawio'), out, 'utf8');
console.log('Generated TFG_CargoHub_mapa_navegacion_simplificado.drawio');
