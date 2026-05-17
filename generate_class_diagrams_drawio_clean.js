const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function htmlClass(name, attrs, methods = []) {
  return `<b>${name}</b><hr>${attrs.map(a => `+ ${a}`).join('<br>')}<hr>${methods.length ? methods.map(m => `+ ${m}`).join('<br>') : '(sin metodos de dominio)'}`;
}

function htmlEnum(name, values) {
  return `<b>&lt;&lt;enumeration&gt;&gt;<br>${name}</b><hr>${values.join('<br>')}`;
}

function cell(id, value, style, x, y, w, h) {
  return `<mxCell id="${id}" value="${esc(value)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`;
}

function edge(id, label, style, source, target, points = '') {
  return `<mxCell id="${id}" value="${esc(label)}" style="${style}" edge="1" parent="1" source="${source}" target="${target}"><mxGeometry relative="1" as="geometry">${points}</mxGeometry></mxCell>`;
}

function points(list) {
  if (!list || !list.length) return '';
  return `<Array as="points">${list.map(p => `<mxPoint x="${p[0]}" y="${p[1]}"/>`).join('')}</Array>`;
}

const classBase = 'swimlane;html=1;whiteSpace=wrap;rounded=0;fontSize=11;startSize=26;container=0;collapsible=0;';
const enumStyle = 'swimlane;html=1;whiteSpace=wrap;rounded=0;fontSize=10;startSize=28;container=0;collapsible=0;fillColor=#f5f5f5;strokeColor=#666666;fontStyle=1;';
const noteStyle = 'rounded=0;whiteSpace=wrap;html=1;fontSize=11;fillColor=#ffffff;strokeColor=#000000;';
const groupStyle = 'rounded=0;whiteSpace=wrap;html=1;fontSize=14;fontStyle=1;fillColor=none;strokeColor=#999999;dashed=1;verticalAlign=top;spacingTop=8;';
const titleStyle = 'text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=0;fontSize=22;fontStyle=1;';

function cls(fill, stroke, extra = '') { return `${classBase}fillColor=${fill};strokeColor=${stroke};${extra}`; }
const C = {
  user: ['#dae8fc', '#6c8ebf'], core: ['#fff2cc', '#d6b656'], doc: ['#d5e8d4', '#82b366'],
  inc: ['#f8cecc', '#b85450'], track: ['#e1d5e7', '#9673a6'], out: ['#ffe6cc', '#d79b00'], log: ['#f5f5f5', '#666666']
};

const EDGE = {
  her: 'endArrow=block;endFill=0;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;',
  agg: 'startArrow=diamond;startFill=0;endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;',
  comp: 'startArrow=diamond;startFill=1;endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;',
  assoc: 'endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;',
  weak: 'endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;dashed=1;'
};

function model(width, height, body) {
  return `<mxGraphModel dx="${width}" dy="${height}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${width}" pageHeight="${height}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${body.join('')}</root></mxGraphModel>`;
}

function pack(name, graph) {
  const payload = zlib.deflateRawSync(Buffer.from(encodeURIComponent(graph), 'utf8')).toString('base64');
  return `<mxfile host="app.diagrams.net" agent="ApiCargoHub" version="24.0.0"><diagram id="${name}" name="${name}">${payload}</diagram></mxfile>`;
}

function reduced() {
  const b = [];
  b.push(cell('title', 'Diagrama de clases reducido - CargoHub backend', titleStyle, 520, 20, 760, 40));
  b.push(cell('legend', '<b>Leyenda UML</b><hr>Triangulo blanco: herencia<br>Rombo blanco: agregacion<br>Rombo negro: composicion<br>Flecha: asociacion<br>Discontinua: fuera de alcance / sin FK', noteStyle, 40, 40, 250, 145));

  b.push(cell('g1', 'Usuarios y perfiles', groupStyle, 330, 70, 1140, 230));
  b.push(cell('g2', 'Operacion principal del porte', groupStyle, 270, 340, 650, 520));
  b.push(cell('g3', 'Flota y agenda', groupStyle, 980, 340, 560, 520));
  b.push(cell('g4', 'Tracking e incidencias', groupStyle, 270, 900, 1270, 360));
  b.push(cell('g5', 'Fuera de alcance defendido / soporte tecnico', groupStyle, 40, 900, 200, 320));

  b.push(cell('Usuario', htmlClass('Usuario', ['Long id', 'String email', 'String nombre', 'RolUsuario rol', 'boolean activo'], ['setEmail(String)']), cls(...C.user), 770, 120, 230, 140));
  b.push(cell('Cliente', htmlClass('Cliente', ['Long id', 'Usuario usuario', 'String nombreEmpresa', 'String cif'], ['setCif(String)']), cls(...C.user), 420, 135, 230, 130));
  b.push(cell('Conductor', htmlClass('Conductor', ['Long id', 'Usuario usuario', 'String dni', 'boolean disponible', 'Double latitudActual'], ['setDni(String)']), cls(...C.user), 1120, 130, 250, 145));

  b.push(cell('Porte', htmlClass('Porte', ['Long id', 'String origen', 'String destino', 'Double precio', 'EstadoPorte estado', 'Cliente cliente', 'Conductor conductor'], ['getPrecioFinal()']), cls(...C.core), 480, 410, 270, 180));
  b.push(cell('Factura', htmlClass('Factura', ['Long id', 'String numeroSerie', 'Double importeTotal', 'boolean pagada'], ['calcularTotales()']), cls(...C.doc), 330, 650, 230, 135));
  b.push(cell('FotoCarga', htmlClass('FotoCarga', ['Long id', 'Porte porte', 'TipoFotoCarga tipo', 'LocalDateTime fechaCaptura'], []), cls(...C.doc), 640, 650, 230, 130));

  b.push(cell('Vehiculo', htmlClass('Vehiculo', ['Long id', 'String matricula', 'TipoVehiculo tipo', 'EstadoVehiculo estado'], ['calcularVolumenAutomatico()']), cls(...C.user), 1060, 400, 250, 140));
  b.push(cell('BloqueoAgenda', htmlClass('BloqueoAgenda', ['Long id', 'LocalDateTime fechaInicio', 'LocalDateTime fechaFin', 'TipoBloqueoAgenda tipo'], []), cls(...C.user), 1000, 610, 250, 130));
  b.push(cell('BloqueoRecurrente', htmlClass('BloqueoRecurrente', ['Long id', 'int diaSemana', 'boolean activo'], ['onCreate()', 'onUpdate()']), cls(...C.user), 1280, 610, 230, 135));

  b.push(cell('TrackingSession', htmlClass('TrackingSession', ['Long id', 'Conductor conductor', 'Porte porte', 'TrackingSessionStatus status'], []), cls(...C.track), 340, 970, 270, 130));
  b.push(cell('TrackingPause', htmlClass('TrackingPause', ['Long id', 'TrackingSession session', 'String motivo'], []), cls(...C.track), 650, 1080, 240, 115));
  b.push(cell('LocationSample', htmlClass('LocationSample', ['Long id', 'Double lat', 'Double lon', 'LocalDateTime recordedAt'], []), cls(...C.track), 650, 940, 240, 115));
  b.push(cell('Incidencia', htmlClass('Incidencia', ['Long id', 'String titulo', 'EstadoIncidencia estado', 'Porte porte'], []), cls(...C.inc), 990, 960, 240, 120));
  b.push(cell('IncidenciaEvento', htmlClass('IncidenciaEvento', ['Long id', 'Incidencia incidencia', 'Usuario actor', 'EstadoIncidencia estadoNuevo'], []), cls(...C.inc), 1260, 1050, 250, 130));

  b.push(cell('Notificacion', htmlClass('Notificacion', ['Long id', 'Long usuarioId', 'String titulo'], []), cls(...C.out, 'dashed=1;'), 55, 970, 170, 110));
  b.push(cell('CargoAnalysisLog', htmlClass('CargoAnalysisLog', ['Long id', 'Boolean success', 'Porte porte'], []), cls(...C.log, 'dashed=1;'), 55, 1110, 170, 110));

  let n = 1;
  b.push(edge('e'+n++, '1 / 0..1', EDGE.her, 'Usuario', 'Cliente'));
  b.push(edge('e'+n++, '1 / 0..1', EDGE.her, 'Usuario', 'Conductor'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Cliente', 'Porte'));
  b.push(edge('e'+n++, '* / 0..1', EDGE.assoc, 'Porte', 'Conductor'));
  b.push(edge('e'+n++, '1 / 0..1', EDGE.comp, 'Porte', 'Factura'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.comp, 'Porte', 'FotoCarga'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Conductor', 'Vehiculo'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Conductor', 'BloqueoAgenda'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Conductor', 'BloqueoRecurrente'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Conductor', 'TrackingSession'));
  b.push(edge('e'+n++, '* / 0..1', EDGE.assoc, 'TrackingSession', 'Porte'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.comp, 'TrackingSession', 'TrackingPause'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'TrackingSession', 'LocationSample'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Porte', 'Incidencia'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.comp, 'Incidencia', 'IncidenciaEvento'));
  b.push(edge('e'+n++, '* / 0..1', EDGE.weak, 'CargoAnalysisLog', 'Porte'));
  b.push(edge('e'+n++, '* / 1 sin FK', EDGE.weak, 'Notificacion', 'Usuario'));
  return model(1600, 1300, b);
}

function expanded() {
  const b = [];
  b.push(cell('title', 'Diagrama de clases ampliado - CargoHub backend', titleStyle, 1260, 20, 980, 40));
  b.push(cell('legend', '<b>Leyenda UML</b><hr>Triangulo blanco: herencia<br>Rombo blanco: agregacion<br>Rombo negro: composicion<br>Flecha: asociacion<br>Discontinua: fuera de alcance / sin FK', noteStyle, 40, 40, 260, 145));
  b.push(cell('note', '<b>Nota</b><hr>Cliente y Conductor se modelan como especializaciones conceptuales de Usuario. En JPA son OneToOne cascade ALL, no extends.', noteStyle, 330, 40, 430, 95));
  b.push(cell('g-auth', 'Usuarios', groupStyle, 760, 90, 1420, 360));
  b.push(cell('g-core', 'Porte y documentos', groupStyle, 500, 520, 1040, 760));
  b.push(cell('g-oper', 'Flota y agenda', groupStyle, 2210, 520, 670, 760));
  b.push(cell('g-track', 'Tracking', groupStyle, 1570, 860, 600, 660));
  b.push(cell('g-inc', 'Incidencias', groupStyle, 500, 1370, 1040, 430));
  b.push(cell('g-out', 'Fuera de alcance / soporte', groupStyle, 40, 1370, 390, 430));

  b.push(cell('Usuario', htmlClass('Usuario', ['Long id', 'String email', 'String nombre', 'String password', 'RolUsuario rol', 'boolean activo', 'LocalDateTime fechaRegistro', 'LocalDateTime ultimoAcceso', 'String tokenRecuperacion', 'String fotoUrl'], ['setEmail(String)']), cls(...C.user), 1360, 150, 290, 220));
  b.push(cell('Cliente', htmlClass('Cliente', ['Long id', 'Usuario usuario', 'String nombreEmpresa', 'String cif', 'String direccionFiscal', 'String telefono', 'String emailContacto', 'String sector'], ['setCif(String)', 'setEmailContacto(String)']), cls(...C.user), 860, 190, 300, 190));
  b.push(cell('Conductor', htmlClass('Conductor', ['Long id', 'Usuario usuario', 'String nombre', 'String apellidos', 'String dni', 'String telefono', 'String ciudadBase', 'Double latitudActual', 'Double longitudActual', 'boolean buscarRetorno', 'String diasLaborables', 'boolean disponible'], ['setDni(String)']), cls(...C.user), 1860, 150, 310, 260));

  b.push(cell('Porte', htmlClass('Porte', ['Long id', 'String origen', 'String destino', 'String ciudadOrigen', 'String ciudadDestino', 'Double distanciaKm', 'Double precio', 'Double ajustePrecio', 'String descripcionCliente', 'Double pesoTotalKg', 'Double volumenTotalM3', 'TipoVehiculo tipoVehiculoRequerido', 'boolean revisionManual', 'EstadoPorte estado', 'Integer version', 'LocalDateTime fechaCreacion', 'LocalDateTime fechaRecogida', 'LocalDateTime fechaEntrega', 'String firmaEntregaBase64', 'Cliente cliente', 'Conductor conductor', 'Set<Long> conductoresRechazados'], ['getPrecioFinal()']), cls(...C.core), 860, 610, 340, 430));
  b.push(cell('Factura', htmlClass('Factura', ['Long id', 'String numeroSerie', 'Double baseImponible', 'Double iva', 'Double importeTotal', 'LocalDate fechaEmision', 'boolean pagada', 'LocalDate fechaPago', 'String formaPago', 'Porte porte'], ['calcularTotales()']), cls(...C.doc), 560, 1080, 290, 240));
  b.push(cell('FotoCarga', htmlClass('FotoCarga', ['Long id', 'Porte porte', 'TipoFotoCarga tipo', 'String fotoBase64', 'String descripcion', 'LocalDateTime fechaCaptura'], []), cls(...C.doc), 1220, 1100, 290, 170));

  b.push(cell('Vehiculo', htmlClass('Vehiculo', ['Long id', 'String matricula', 'String marca', 'String modelo', 'TipoVehiculo tipo', 'EstadoVehiculo estado', 'Integer capacidadCargaKg', 'Double volumenM3', 'Conductor conductor'], ['setMatricula(String)', 'calcularVolumenAutomatico()']), cls(...C.user), 2260, 610, 300, 230));
  b.push(cell('BloqueoAgenda', htmlClass('BloqueoAgenda', ['Long id', 'LocalDateTime fechaInicio', 'LocalDateTime fechaFin', 'TipoBloqueoAgenda tipo', 'String titulo', 'Conductor conductor'], []), cls(...C.user), 2260, 920, 300, 170));
  b.push(cell('BloqueoRecurrente', htmlClass('BloqueoRecurrente', ['Long id', 'Conductor conductor', 'int diaSemana', 'boolean activo', 'LocalDateTime createdAt', 'LocalDateTime updatedAt'], ['onCreate()', 'onUpdate()']), cls(...C.user), 2260, 1140, 300, 190));

  b.push(cell('TrackingSession', htmlClass('TrackingSession', ['Long id', 'Conductor conductor', 'Porte porte', 'TrackingSessionStatus status', 'TrackingSessionPhase currentPhase', 'LocalDateTime startedAt', 'LocalDateTime endedAt', 'LocalDateTime lastSampleAt'], []), cls(...C.track), 1710, 940, 300, 200));
  b.push(cell('TrackingPause', htmlClass('TrackingPause', ['Long id', 'TrackingSession session', 'String motivo', 'String nota', 'LocalDateTime startedAt', 'LocalDateTime endedAt'], []), cls(...C.track), 1710, 1190, 300, 160));
  b.push(cell('LocationSample', htmlClass('LocationSample', ['Long id', 'TrackingSession session', 'Conductor conductor', 'Porte porte', 'Double lat', 'Double lon', 'LocalDateTime recordedAt', 'LocalDateTime receivedAt', 'Double speedKph', 'Integer headingDeg'], []), cls(...C.track), 1710, 1390, 300, 210));

  b.push(cell('Incidencia', htmlClass('Incidencia', ['Long id', 'String titulo', 'String descripcion', 'LocalDateTime fechaReporte', 'EstadoIncidencia estado', 'SeveridadIncidencia severidad', 'PrioridadIncidencia prioridad', 'LocalDateTime fechaLimiteSla', 'String resolucion', 'Usuario admin', 'Porte porte'], []), cls(...C.inc), 640, 1450, 320, 250));
  b.push(cell('IncidenciaEvento', htmlClass('IncidenciaEvento', ['Long id', 'Incidencia incidencia', 'Usuario actor', 'EstadoIncidencia estadoAnterior', 'EstadoIncidencia estadoNuevo', 'LocalDateTime fecha', 'String accion', 'String comentario'], []), cls(...C.inc), 1110, 1470, 320, 200));

  b.push(cell('Notificacion', htmlClass('Notificacion', ['Long id', 'Long usuarioId', 'String titulo', 'String mensaje', 'TipoNotificacion tipo', 'boolean leida', 'LocalDateTime fechaCreacion', 'Long referenciaId'], []), cls(...C.out, 'dashed=1;'), 80, 1450, 280, 190));
  b.push(cell('CargoAnalysisLog', htmlClass('CargoAnalysisLog', ['Long id', 'String requestData', 'LocalDateTime requestTimestamp', 'String responseData', 'Boolean success', 'String errorMessage', 'Double pesoTotalKg', 'Double volumenTotalM3', 'String tipoVehiculoRequerido', 'Boolean revisionManual', 'Porte porte'], []), cls(...C.log, 'dashed=1;'), 80, 1660, 300, 250));

  const enums = [
    ['RolUsuario', ['ADMIN','SUPERADMIN','CONDUCTOR','CLIENTE'], 1220, 160],
    ['EstadoPorte', ['PENDIENTE','ASIGNADO','EN_RECOGIDA','EN_TRANSITO','ENTREGADO','CANCELADO','FACTURADO'], 1220, 610],
    ['TipoVehiculo', ['FURGONETA','CAMION_PEQUENO','CAMION_MEDIANO','CAMION_GRANDE','TRAILER'], 2580, 610],
    ['EstadoVehiculo', ['DISPONIBLE','EN_SERVICIO','MANTENIMIENTO','AVERIADO','RETIRADO'], 2580, 760],
    ['TipoBloqueoAgenda', ['VACACIONES','DESCANSO','MANTENIMIENTO_VEHICULO','PERSONAL','OTRO'], 2580, 930],
    ['TrackingSessionStatus', ['ACTIVE','PAUSED','COMPLETED'], 2020, 940],
    ['TrackingSessionPhase', ['PRE_TRIP','EN_ROUTE','POST_TRIP'], 2020, 1080],
    ['TipoFotoCarga', ['RECOGIDA','ENTREGA','INCIDENCIA','OTRO'], 1220, 1290],
    ['EstadoIncidencia', ['ABIERTA','EN_PROCESO','RESUELTA','CERRADA'], 640, 1720],
    ['SeveridadIncidencia', ['BAJA','MEDIA','ALTA','CRITICA'], 860, 1720],
    ['PrioridadIncidencia', ['BAJA','MEDIA','ALTA','URGENTE'], 1090, 1720],
    ['TipoNotificacion', ['INFO','ALERTA','OFERTA','INCIDENCIA','SISTEMA'], 80, 1910]
  ];
  for (const [name, vals, x, y] of enums) b.push(cell('E'+name, htmlEnum(name, vals), enumStyle, x, y, 210, name.length > 16 ? 140 : 125));

  let n = 1;
  b.push(edge('e'+n++, '1 / 0..1', EDGE.her, 'Usuario', 'Cliente'));
  b.push(edge('e'+n++, '1 / 0..1', EDGE.her, 'Usuario', 'Conductor'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Cliente', 'Porte'));
  b.push(edge('e'+n++, '* / 0..1', EDGE.assoc, 'Porte', 'Conductor'));
  b.push(edge('e'+n++, '1 / 0..1', EDGE.comp, 'Porte', 'Factura'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.comp, 'Porte', 'FotoCarga'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Conductor', 'Vehiculo'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Conductor', 'BloqueoAgenda'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Conductor', 'BloqueoRecurrente'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Conductor', 'TrackingSession'));
  b.push(edge('e'+n++, '* / 0..1', EDGE.assoc, 'TrackingSession', 'Porte'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.comp, 'TrackingSession', 'TrackingPause'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'TrackingSession', 'LocationSample'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.agg, 'Porte', 'Incidencia'));
  b.push(edge('e'+n++, '1 / 0..*', EDGE.comp, 'Incidencia', 'IncidenciaEvento'));
  b.push(edge('e'+n++, '* / 0..1', EDGE.assoc, 'Incidencia', 'Usuario'));
  b.push(edge('e'+n++, '* / 0..1', EDGE.assoc, 'IncidenciaEvento', 'Usuario'));
  b.push(edge('e'+n++, '* / 1', EDGE.agg, 'LocationSample', 'Conductor'));
  b.push(edge('e'+n++, '* / 0..1', EDGE.assoc, 'LocationSample', 'Porte'));
  b.push(edge('e'+n++, '* / 0..1', EDGE.weak, 'CargoAnalysisLog', 'Porte'));
  b.push(edge('e'+n++, '* / 1 sin FK', EDGE.weak, 'Notificacion', 'Usuario'));
  return model(3000, 2050, b);
}

const files = [
  ['TFG_CargoHub_clases_reducido.drawio', 'Clases reducido limpio', reduced()],
  ['TFG_CargoHub_clases_ampliado.drawio', 'Clases ampliado limpio', expanded()]
];

for (const [file, name, graph] of files) {
  fs.writeFileSync(path.join(__dirname, file), pack(name, graph), 'utf8');
  console.log(`Generated clean ${file}`);
}
