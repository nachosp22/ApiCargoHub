const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const rootDir = __dirname;

function xmlEscape(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function htmlClass(name, attrs, methods = []) {
  const attrText = attrs.map(a => `+ ${a}`).join('<br>');
  const methodText = methods.length ? methods.map(m => `+ ${m}`).join('<br>') : '(sin metodos de dominio)';
  return `<b>${name}</b><hr>${attrText}<hr>${methodText}`;
}

function enumBox(name, values) {
  return `<b>&lt;&lt;enumeration&gt;&gt;<br>${name}</b><hr>${values.join('<br>')}`;
}

function cell(id, value, style, x, y, w, h) {
  return `    <mxCell id="${id}" value="${xmlEscape(value)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`;
}

function edge(id, value, style, source, target) {
  return `    <mxCell id="${id}" value="${xmlEscape(value)}" style="${style}" edge="1" parent="1" source="${source}" target="${target}"><mxGeometry relative="1" as="geometry"/></mxCell>`;
}

const baseClassStyle = 'swimlane;html=1;whiteSpace=wrap;rounded=0;fontSize=11;startSize=26;';
const enumStyle = 'swimlane;html=1;whiteSpace=wrap;rounded=0;fontSize=10;startSize=28;fillColor=#f5f5f5;strokeColor=#666666;fontStyle=1;';
const titleStyle = 'text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=0;fontSize=20;fontStyle=1;';
const noteStyle = 'rounded=0;whiteSpace=wrap;html=1;fontSize=10;fillColor=#ffffff;strokeColor=#000000;';

function style(fill, stroke, extra = '') {
  return `${baseClassStyle}fillColor=${fill};strokeColor=${stroke};${extra}`;
}

const colors = {
  user: ['#dae8fc', '#6c8ebf'],
  core: ['#fff2cc', '#d6b656'],
  doc: ['#d5e8d4', '#82b366'],
  inc: ['#f8cecc', '#b85450'],
  track: ['#e1d5e7', '#9673a6'],
  out: ['#ffe6cc', '#d79b00'],
  log: ['#f5f5f5', '#666666']
};

const relStyles = {
  inheritance: 'endArrow=block;endFill=0;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;',
  aggregation: 'startArrow=diamond;startFill=0;endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;',
  composition: 'startArrow=diamond;startFill=1;endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;',
  association: 'endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;',
  dashed: 'endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=10;strokeColor=#000000;dashed=1;'
};

function model(cells, width, height) {
  return `<mxGraphModel dx="${width}" dy="${height}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${width}" pageHeight="${height}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>\n${cells.join('\n')}\n  </root></mxGraphModel>`;
}

function compressDiagram(mxGraphModel) {
  // diagrams.net native format: base64(deflateRaw(encodeURIComponent(xml)))
  return zlib.deflateRawSync(Buffer.from(encodeURIComponent(mxGraphModel), 'utf8')).toString('base64');
}

function mxfile(diagramName, mxGraphModel) {
  const payload = compressDiagram(mxGraphModel);
  return `<mxfile host="app.diagrams.net" agent="ApiCargoHub" version="24.0.0"><diagram id="${diagramName.toLowerCase().replace(/[^a-z0-9]+/g, '-')}" name="${xmlEscape(diagramName)}">${payload}</diagram></mxfile>`;
}

function reduced() {
  const c = [];
  c.push(cell('title', '<b>Diagrama de clases reducido - CargoHub backend</b>', titleStyle, 360, 20, 880, 40));
  c.push(cell('legend', '<b>Leyenda UML</b><hr>Triangulo blanco: herencia<br>Rombo blanco: agregacion<br>Rombo negro: composicion<br>Flecha: asociacion<br>Linea discontinua: fuera de alcance / sin FK real', noteStyle, 20, 40, 260, 140));

  c.push(cell('c-Usuario', htmlClass('Usuario', ['id: Long', 'email: String', 'nombre: String', 'rol: RolUsuario', 'activo: boolean'], ['setEmail(String)']), style(...colors.user), 600, 90, 230, 145));
  c.push(cell('c-Cliente', htmlClass('Cliente', ['id: Long', 'usuario: Usuario', 'nombreEmpresa: String', 'cif: String', 'emailContacto: String'], ['setCif(String)', 'setEmailContacto(String)']), style(...colors.user), 270, 280, 235, 150));
  c.push(cell('c-Conductor', htmlClass('Conductor', ['id: Long', 'usuario: Usuario', 'dni: String', 'ciudadBase: String', 'disponible: boolean', 'latitudActual: Double'], ['setDni(String)']), style(...colors.user), 920, 280, 250, 160));
  c.push(cell('c-Porte', htmlClass('Porte', ['id: Long', 'origen: String', 'destino: String', 'precio: Double', 'estado: EstadoPorte', 'cliente: Cliente', 'conductor: Conductor'], ['getPrecioFinal()']), style(...colors.core), 610, 520, 260, 185));
  c.push(cell('c-Vehiculo', htmlClass('Vehiculo', ['id: Long', 'matricula: String', 'tipo: TipoVehiculo', 'estado: EstadoVehiculo', 'conductor: Conductor'], ['setMatricula(String)', 'calcularVolumenAutomatico()']), style(...colors.user), 1230, 280, 250, 155));
  c.push(cell('c-BloqueoAgenda', htmlClass('BloqueoAgenda', ['id: Long', 'fechaInicio: LocalDateTime', 'fechaFin: LocalDateTime', 'tipo: TipoBloqueoAgenda'], []), style(...colors.user), 1230, 470, 250, 130));
  c.push(cell('c-BloqueoRecurrente', htmlClass('BloqueoRecurrente', ['id: Long', 'diaSemana: int', 'activo: boolean', 'conductor: Conductor'], ['onCreate()', 'onUpdate()']), style(...colors.user), 1230, 635, 250, 135));
  c.push(cell('c-Factura', htmlClass('Factura', ['id: Long', 'numeroSerie: String', 'importeTotal: Double', 'pagada: boolean', 'porte: Porte'], ['calcularTotales()']), style(...colors.doc), 210, 770, 240, 145));
  c.push(cell('c-FotoCarga', htmlClass('FotoCarga', ['id: Long', 'porte: Porte', 'tipo: TipoFotoCarga', 'fechaCaptura: LocalDateTime'], []), style(...colors.doc), 500, 790, 230, 130));
  c.push(cell('c-Incidencia', htmlClass('Incidencia', ['id: Long', 'titulo: String', 'estado: EstadoIncidencia', 'prioridad: PrioridadIncidencia', 'porte: Porte'], []), style(...colors.inc), 810, 790, 240, 135));
  c.push(cell('c-IncidenciaEvento', htmlClass('IncidenciaEvento', ['id: Long', 'incidencia: Incidencia', 'actor: Usuario', 'estadoNuevo: EstadoIncidencia'], []), style(...colors.inc), 1100, 810, 260, 130));
  c.push(cell('c-TrackingSession', htmlClass('TrackingSession', ['id: Long', 'conductor: Conductor', 'porte: Porte', 'status: TrackingSessionStatus', 'currentPhase: TrackingSessionPhase'], []), style(...colors.track), 910, 520, 270, 145));
  c.push(cell('c-TrackingPause', htmlClass('TrackingPause', ['id: Long', 'session: TrackingSession', 'motivo: String', 'startedAt: LocalDateTime'], []), style(...colors.track), 1230, 800, 250, 130));
  c.push(cell('c-LocationSample', htmlClass('LocationSample', ['id: Long', 'session: TrackingSession', 'lat: Double', 'lon: Double', 'recordedAt: LocalDateTime'], []), style(...colors.track), 1230, 960, 250, 135));
  c.push(cell('c-Notificacion', htmlClass('Notificacion', ['id: Long', 'usuarioId: Long', 'titulo: String', 'tipo: TipoNotificacion'], []), style(...colors.out, 'dashed=1;'), 20, 940, 230, 125));
  c.push(cell('c-CargoAnalysisLog', htmlClass('CargoAnalysisLog', ['id: Long', 'success: Boolean', 'porte: Porte', 'tipoVehiculoRequerido: String'], []), style(...colors.log, 'dashed=1;'), 290, 960, 230, 125));

  let i = 1;
  c.push(edge(`r${i++}`, '1 / 0..1', relStyles.inheritance, 'c-Usuario', 'c-Cliente'));
  c.push(edge(`r${i++}`, '1 / 0..1', relStyles.inheritance, 'c-Usuario', 'c-Conductor'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Cliente', 'c-Porte'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-Porte', 'c-Conductor'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Conductor', 'c-Vehiculo'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Conductor', 'c-BloqueoAgenda'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Conductor', 'c-BloqueoRecurrente'));
  c.push(edge(`r${i++}`, '1 / 0..1', relStyles.composition, 'c-Porte', 'c-Factura'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.composition, 'c-Porte', 'c-FotoCarga'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Porte', 'c-Incidencia'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.composition, 'c-Incidencia', 'c-IncidenciaEvento'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Conductor', 'c-TrackingSession'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-TrackingSession', 'c-Porte'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.composition, 'c-TrackingSession', 'c-TrackingPause'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-TrackingSession', 'c-LocationSample'));
  c.push(edge(`r${i++}`, '* / 1', relStyles.aggregation, 'c-LocationSample', 'c-Conductor'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-LocationSample', 'c-Porte'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-Incidencia', 'c-Usuario'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-IncidenciaEvento', 'c-Usuario'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.dashed, 'c-CargoAnalysisLog', 'c-Porte'));
  c.push(edge(`r${i++}`, '* / 1 sin FK', relStyles.dashed, 'c-Notificacion', 'c-Usuario'));

  return model(c, 1600, 1150);
}

function expanded() {
  const c = [];
  c.push(cell('title', '<b>Diagrama de clases completo - CargoHub backend (DIN A3)</b>', titleStyle, 880, 20, 1050, 45));
  c.push(cell('legend', '<b>Leyenda UML</b><hr>Triangulo blanco: herencia<br>Rombo blanco: agregacion<br>Rombo negro: composicion<br>Flecha: asociacion<br>Linea discontinua: fuera de alcance / sin FK real', noteStyle, 40, 40, 270, 145));
  c.push(cell('note', '<b>Nota metodologica</b><hr>Cliente y Conductor se muestran como especializaciones conceptuales de Usuario. En JPA se implementan con OneToOne cascade ALL, no con extends.', noteStyle, 320, 40, 390, 105));

  c.push(cell('c-Usuario', htmlClass('Usuario', ['id: Long', 'email: String', 'nombre: String', 'password: String', 'rol: RolUsuario', 'activo: boolean', 'fechaRegistro: LocalDateTime', 'ultimoAcceso: LocalDateTime', 'tokenRecuperacion: String', 'fotoUrl: String'], ['setEmail(String): void']), style(...colors.user), 980, 100, 270, 220));
  c.push(cell('c-Cliente', htmlClass('Cliente', ['id: Long', 'usuario: Usuario', 'nombreEmpresa: String', 'cif: String', 'direccionFiscal: String', 'telefono: String', 'emailContacto: String', 'sector: String'], ['setCif(String): void', 'setEmailContacto(String): void']), style(...colors.user), 430, 420, 270, 190));
  c.push(cell('c-Conductor', htmlClass('Conductor', ['id: Long', 'usuario: Usuario', 'nombre: String', 'apellidos: String', 'dni: String', 'telefono: String', 'ciudadBase: String', 'latitudBase: Double', 'longitudBase: Double', 'radioAccionKm: Integer', 'latitudActual: Double', 'longitudActual: Double', 'ultimaActualizacionUbicacion: LocalDateTime', 'velocidadKphActual: Double', 'rumboActualDeg: Integer', 'buscarRetorno: boolean', 'diasLaborables: String', 'disponible: boolean'], ['setDni(String): void']), style(...colors.user), 1480, 390, 310, 360));
  c.push(cell('c-Porte', htmlClass('Porte', ['id: Long', 'origen: String', 'destino: String', 'ciudadOrigen: String', 'ciudadDestino: String', 'latitudOrigen: Double', 'longitudOrigen: Double', 'latitudDestino: Double', 'longitudDestino: Double', 'distanciaKm: Double', 'distanciaEstimada: boolean', 'precio: Double', 'ajustePrecio: Double', 'motivoAjuste: String', 'descripcionCliente: String', 'pesoTotalKg: Double', 'volumenTotalM3: Double', 'largoMaxPaquete: Double', 'anchoMaxPaquete: Double', 'altoMaxPaquete: Double', 'tipoVehiculoRequerido: TipoVehiculo', 'revisionManual: boolean', 'motivoRevision: String', 'estado: EstadoPorte', 'version: Integer', 'fechaCreacion: LocalDateTime', 'fechaRecogida: LocalDateTime', 'fechaEntrega: LocalDateTime', 'firmaEntregaBase64: String', 'firmaEntregaFirmadoPor: String', 'firmaEntregaFecha: LocalDateTime', 'cliente: Cliente', 'conductor: Conductor', 'conductoresRechazados: Set<Long>'], ['getPrecioFinal(): Double']), style(...colors.core), 900, 820, 330, 620));
  c.push(cell('c-Vehiculo', htmlClass('Vehiculo', ['id: Long', 'matricula: String', 'marca: String', 'modelo: String', 'tipo: TipoVehiculo', 'estado: EstadoVehiculo', 'capacidadCargaKg: Integer', 'largoUtilMm: Integer', 'anchoUtilMm: Integer', 'altoUtilMm: Integer', 'volumenM3: Double', 'conductor: Conductor'], ['setMatricula(String): void', 'calcularVolumenAutomatico(): void']), style(...colors.user), 2020, 350, 300, 260));
  c.push(cell('c-BloqueoAgenda', htmlClass('BloqueoAgenda', ['id: Long', 'fechaInicio: LocalDateTime', 'fechaFin: LocalDateTime', 'tipo: TipoBloqueoAgenda', 'titulo: String', 'conductor: Conductor'], []), style(...colors.user), 2020, 670, 300, 150));
  c.push(cell('c-BloqueoRecurrente', htmlClass('BloqueoRecurrente', ['id: Long', 'conductor: Conductor', 'diaSemana: int', 'activo: boolean', 'createdAt: LocalDateTime', 'updatedAt: LocalDateTime'], ['onCreate(): void', 'onUpdate(): void']), style(...colors.user), 2020, 880, 300, 180));
  c.push(cell('c-Factura', htmlClass('Factura', ['id: Long', 'numeroSerie: String', 'baseImponible: Double', 'iva: Double', 'importeTotal: Double', 'fechaEmision: LocalDate', 'pagada: boolean', 'fechaPago: LocalDate', 'formaPago: String', 'condicionesPago: String', 'observaciones: String', 'porte: Porte'], ['calcularTotales(): void']), style(...colors.doc), 90, 900, 300, 270));
  c.push(cell('c-FotoCarga', htmlClass('FotoCarga', ['id: Long', 'porte: Porte', 'tipo: TipoFotoCarga', 'fotoBase64: String', 'descripcion: String', 'fechaCaptura: LocalDateTime'], []), style(...colors.doc), 470, 960, 300, 155));
  c.push(cell('c-Incidencia', htmlClass('Incidencia', ['id: Long', 'titulo: String', 'descripcion: String', 'fechaReporte: LocalDateTime', 'estado: EstadoIncidencia', 'severidad: SeveridadIncidencia', 'prioridad: PrioridadIncidencia', 'fechaLimiteSla: LocalDateTime', 'resolucion: String', 'fechaResolucion: LocalDateTime', 'admin: Usuario', 'porte: Porte'], []), style(...colors.inc), 1320, 1160, 310, 260));
  c.push(cell('c-IncidenciaEvento', htmlClass('IncidenciaEvento', ['id: Long', 'incidencia: Incidencia', 'actor: Usuario', 'estadoAnterior: EstadoIncidencia', 'estadoNuevo: EstadoIncidencia', 'fecha: LocalDateTime', 'accion: String', 'comentario: String'], []), style(...colors.inc), 1740, 1180, 320, 190));
  c.push(cell('c-TrackingSession', htmlClass('TrackingSession', ['id: Long', 'conductor: Conductor', 'porte: Porte', 'status: TrackingSessionStatus', 'currentPhase: TrackingSessionPhase', 'startedAt: LocalDateTime', 'endedAt: LocalDateTime', 'lastSampleAt: LocalDateTime'], []), style(...colors.track), 1500, 820, 320, 190));
  c.push(cell('c-TrackingPause', htmlClass('TrackingPause', ['id: Long', 'session: TrackingSession', 'motivo: String', 'nota: String', 'startedAt: LocalDateTime', 'endedAt: LocalDateTime'], []), style(...colors.track), 2050, 1180, 300, 155));
  c.push(cell('c-LocationSample', htmlClass('LocationSample', ['id: Long', 'session: TrackingSession', 'conductor: Conductor', 'porte: Porte', 'lat: Double', 'lon: Double', 'recordedAt: LocalDateTime', 'receivedAt: LocalDateTime', 'speedKph: Double', 'headingDeg: Integer'], []), style(...colors.track), 2050, 1410, 300, 220));
  c.push(cell('c-Notificacion', htmlClass('Notificacion', ['id: Long', 'usuarioId: Long', 'titulo: String', 'mensaje: String', 'tipo: TipoNotificacion', 'leida: boolean', 'fechaCreacion: LocalDateTime', 'referenciaId: Long'], []), style(...colors.out, 'dashed=1;'), 90, 1350, 290, 180));
  c.push(cell('c-CargoAnalysisLog', htmlClass('CargoAnalysisLog', ['id: Long', 'requestData: String', 'requestTimestamp: LocalDateTime', 'responseData: String', 'responseTimestamp: LocalDateTime', 'success: Boolean', 'errorMessage: String', 'pesoTotalKg: Double', 'volumenTotalM3: Double', 'largoMaxPaquete: Double', 'tipoVehiculoRequerido: String', 'revisionManual: Boolean', 'porte: Porte'], []), style(...colors.log, 'dashed=1;'), 470, 1300, 320, 280));

  const enums = [
    ['en-RolUsuario', 'RolUsuario', ['ADMIN', 'SUPERADMIN', 'CONDUCTOR', 'CLIENTE'], 1280, 110, 190, 120],
    ['en-EstadoPorte', 'EstadoPorte', ['PENDIENTE', 'ASIGNADO', 'EN_RECOGIDA', 'EN_TRANSITO', 'ENTREGADO', 'CANCELADO', 'FACTURADO'], 1245, 840, 190, 170],
    ['en-TipoVehiculo', 'TipoVehiculo', ['FURGONETA', 'CAMION_PEQUENO', 'CAMION_MEDIANO', 'CAMION_GRANDE', 'TRAILER'], 2350, 350, 210, 140],
    ['en-EstadoVehiculo', 'EstadoVehiculo', ['DISPONIBLE', 'EN_SERVICIO', 'MANTENIMIENTO', 'AVERIADO', 'RETIRADO'], 2350, 520, 210, 140],
    ['en-EstadoIncidencia', 'EstadoIncidencia', ['ABIERTA', 'EN_PROCESO', 'RESUELTA', 'CERRADA'], 2080, 1010, 210, 125],
    ['en-SeveridadIncidencia', 'SeveridadIncidencia', ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'], 2310, 1010, 215, 125],
    ['en-PrioridadIncidencia', 'PrioridadIncidencia', ['BAJA', 'MEDIA', 'ALTA', 'URGENTE'], 2545, 1010, 215, 125],
    ['en-TrackingSessionStatus', 'TrackingSessionStatus', ['ACTIVE', 'PAUSED', 'COMPLETED'], 1850, 820, 220, 110],
    ['en-TrackingSessionPhase', 'TrackingSessionPhase', ['PRE_TRIP', 'EN_ROUTE', 'POST_TRIP'], 1850, 950, 220, 110],
    ['en-TipoFotoCarga', 'TipoFotoCarga', ['RECOGIDA', 'ENTREGA', 'INCIDENCIA', 'OTRO'], 520, 1130, 190, 125],
    ['en-TipoBloqueoAgenda', 'TipoBloqueoAgenda', ['VACACIONES', 'DESCANSO', 'MANTENIMIENTO_VEHICULO', 'PERSONAL', 'OTRO'], 2350, 700, 245, 140],
    ['en-TipoNotificacion', 'TipoNotificacion', ['INFO', 'ALERTA', 'OFERTA', 'INCIDENCIA', 'SISTEMA'], 90, 1560, 200, 135]
  ];
  for (const [id, name, values, x, y, w, h] of enums) c.push(cell(id, enumBox(name, values), enumStyle, x, y, w, h));

  let i = 1;
  c.push(edge(`r${i++}`, '1 / 0..1', relStyles.inheritance, 'c-Usuario', 'c-Cliente'));
  c.push(edge(`r${i++}`, '1 / 0..1', relStyles.inheritance, 'c-Usuario', 'c-Conductor'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Cliente', 'c-Porte'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-Porte', 'c-Conductor'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Conductor', 'c-Vehiculo'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Conductor', 'c-BloqueoAgenda'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Conductor', 'c-BloqueoRecurrente'));
  c.push(edge(`r${i++}`, '1 / 0..1', relStyles.composition, 'c-Porte', 'c-Factura'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.composition, 'c-Porte', 'c-FotoCarga'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Porte', 'c-Incidencia'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.composition, 'c-Incidencia', 'c-IncidenciaEvento'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-Conductor', 'c-TrackingSession'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-TrackingSession', 'c-Porte'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.composition, 'c-TrackingSession', 'c-TrackingPause'));
  c.push(edge(`r${i++}`, '1 / 0..*', relStyles.aggregation, 'c-TrackingSession', 'c-LocationSample'));
  c.push(edge(`r${i++}`, '* / 1', relStyles.aggregation, 'c-LocationSample', 'c-Conductor'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-LocationSample', 'c-Porte'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-Incidencia', 'c-Usuario'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.association, 'c-IncidenciaEvento', 'c-Usuario'));
  c.push(edge(`r${i++}`, '* / 0..1', relStyles.dashed, 'c-CargoAnalysisLog', 'c-Porte'));
  c.push(edge(`r${i++}`, '* / 1 sin FK', relStyles.dashed, 'c-Notificacion', 'c-Usuario'));

  return model(c, 2800, 1900);
}

const outputs = [
  ['TFG_CargoHub_clases_reducido.drawio', 'Clases reducido', reduced()],
  ['TFG_CargoHub_clases_ampliado.drawio', 'Clases ampliado', expanded()]
];

for (const [filename, name, graph] of outputs) {
  const out = path.join(rootDir, filename);
  fs.writeFileSync(out, mxfile(name, graph), 'utf8');
  console.log(`Generated ${out}`);
}
