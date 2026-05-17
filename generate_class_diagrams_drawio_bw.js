const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function clsHtml(name, attrs, methods = []) {
  const a = attrs.map(x => `- ${x}`).join('<br>');
  const m = methods.length ? methods.map(x => `+ ${x}`).join('<br>') : '(sin metodos de dominio)';
  return `<div style="text-align:center"><b>${name}</b></div><hr>${a}<hr>${m}`;
}

function enumHtml(name, values) {
  return `<div style="text-align:center"><b>&lt;&lt;enumeration&gt;&gt;<br>${name}</b></div><hr>${values.join('<br>')}`;
}

function nameOnlyHtml(name) {
  return `<div style="text-align:center"><b>${name}</b></div>`;
}

const classStyle = 'rounded=0;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;fontColor=#000000;fontSize=12;align=left;verticalAlign=top;spacing=8;overflow=fill;';
const dashedStyle = classStyle + 'dashed=1;';
const enumStyle = classStyle + 'fontSize=11;';
const titleStyle = 'text;html=1;strokeColor=none;fillColor=none;fontColor=#000000;fontSize=22;fontStyle=1;align=center;verticalAlign=middle;whiteSpace=wrap;';
const sectionStyle = 'text;html=1;strokeColor=none;fillColor=none;fontColor=#000000;fontSize=15;fontStyle=1;align=center;verticalAlign=middle;whiteSpace=wrap;';
const noteStyle = 'rounded=0;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;fontColor=#000000;fontSize=11;align=left;verticalAlign=top;spacing=8;overflow=fill;';

const EDGE = {
  her: 'endArrow=block;endFill=0;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;strokeColor=#000000;fontColor=#000000;fontSize=11;',
  agg: 'startArrow=diamond;startFill=0;endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;strokeColor=#000000;fontColor=#000000;fontSize=11;',
  comp: 'startArrow=diamond;startFill=1;endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;strokeColor=#000000;fontColor=#000000;fontSize=11;',
  assoc: 'endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;strokeColor=#000000;fontColor=#000000;fontSize=11;',
  weak: 'endArrow=open;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;strokeColor=#000000;fontColor=#000000;fontSize=11;dashed=1;'
};

function cell(id, value, x, y, w, h, style = classStyle) {
  return `<mxCell id="${id}" value="${esc(value)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`;
}

function edge(id, label, style, source, target) {
  const parts = String(label).split('/').map(x => x.trim());
  const sourceCard = parts.length >= 2 ? parts[0] : '';
  const targetCard = parts.length >= 2 ? parts.slice(1).join(' / ') : label;
  const main = `<mxCell id="${id}" value="" style="${style}" edge="1" parent="1" source="${source}" target="${target}"><mxGeometry relative="1" as="geometry"/></mxCell>`;
  if (!sourceCard || !targetCard) return main;
  const labelStyle = 'edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontSize=11;fontColor=#000000;labelBackgroundColor=#ffffff;';
  const left = `<mxCell id="${id}-src-card" value="${esc(sourceCard)}" style="${labelStyle}" vertex="1" connectable="0" parent="${id}"><mxGeometry x="-0.82" y="-1" relative="1" as="geometry"><mxPoint x="0" y="0" as="offset"/></mxGeometry></mxCell>`;
  const right = `<mxCell id="${id}-tgt-card" value="${esc(targetCard)}" style="${labelStyle}" vertex="1" connectable="0" parent="${id}"><mxGeometry x="0.82" y="-1" relative="1" as="geometry"><mxPoint x="0" y="0" as="offset"/></mxGeometry></mxCell>`;
  return main + left + right;
}

function graph(w, h, body) {
  return `<mxGraphModel dx="${w}" dy="${h}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${w}" pageHeight="${h}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${body.join('')}</root></mxGraphModel>`;
}

function pack(id, name, g) {
  const payload = zlib.deflateRawSync(Buffer.from(encodeURIComponent(g), 'utf8')).toString('base64');
  return `<mxfile host="app.diagrams.net" agent="ApiCargoHub" version="24.0.0"><diagram id="${id}" name="${esc(name)}">${payload}</diagram></mxfile>`;
}

function reduced() {
  const b = [];
  const simpleClassStyle = classStyle + 'fontSize=14;fontStyle=1;align=center;verticalAlign=middle;spacing=0;';
  const simpleDashedStyle = simpleClassStyle + 'dashed=1;';
  b.push(cell('title', 'Diagrama de clases reducido - CargoHub backend', 760, 20, 900, 40, titleStyle));
  b.push(cell('legend', '<b>Leyenda UML</b><hr>Triangulo blanco: herencia<br>Rombo blanco: agregacion<br>Rombo negro: composicion<br>Flecha: asociacion<br>Linea discontinua: fuera de alcance / sin FK', 30, 40, 300, 155, noteStyle));
  b.push(cell('s1', 'Usuarios y perfiles', 760, 100, 850, 30, sectionStyle));
  b.push(cell('s2', 'Porte y documentacion', 390, 420, 800, 30, sectionStyle));
  b.push(cell('s3', 'Flota y agenda', 1450, 420, 680, 30, sectionStyle));
  b.push(cell('s4', 'Tracking e incidencias', 620, 780, 1220, 30, sectionStyle));
  b.push(cell('s5', 'Enumeraciones', 340, 1190, 1780, 30, sectionStyle));

  b.push(cell('Usuario', nameOnlyHtml('Usuario'), 1020, 150, 220, 70, simpleClassStyle));
  b.push(cell('Cliente', nameOnlyHtml('Cliente'), 620, 180, 220, 70, simpleClassStyle));
  b.push(cell('Conductor', nameOnlyHtml('Conductor'), 1420, 180, 240, 70, simpleClassStyle));

  b.push(cell('Porte', nameOnlyHtml('Porte'), 700, 500, 240, 70, simpleClassStyle));
  b.push(cell('Factura', nameOnlyHtml('Factura'), 360, 560, 220, 70, simpleClassStyle));
  b.push(cell('FotoCarga', nameOnlyHtml('FotoCarga'), 1050, 560, 220, 70, simpleClassStyle));

  b.push(cell('Vehiculo', nameOnlyHtml('Vehiculo'), 1450, 500, 230, 70, simpleClassStyle));
  b.push(cell('BloqueoAgenda', nameOnlyHtml('BloqueoAgenda'), 1770, 500, 240, 70, simpleClassStyle));
  b.push(cell('BloqueoRecurrente', nameOnlyHtml('BloqueoRecurrente'), 1770, 640, 260, 70, simpleClassStyle));

  b.push(cell('TrackingSession', nameOnlyHtml('TrackingSession'), 540, 850, 260, 70, simpleClassStyle));
  b.push(cell('TrackingPause', nameOnlyHtml('TrackingPause'), 880, 850, 240, 70, simpleClassStyle));
  b.push(cell('LocationSample', nameOnlyHtml('LocationSample'), 880, 1000, 250, 70, simpleClassStyle));
  b.push(cell('Incidencia', nameOnlyHtml('Incidencia'), 1250, 850, 230, 70, simpleClassStyle));
  b.push(cell('IncidenciaEvento', nameOnlyHtml('IncidenciaEvento'), 1580, 850, 260, 70, simpleClassStyle));

  b.push(cell('CargoAnalysisLog', nameOnlyHtml('CargoAnalysisLog'), 60, 980, 240, 70, simpleDashedStyle));

  const reducedEnumStyle = enumStyle + 'fontSize=10;';
  const enums = [
    ['ERolUsuario','RolUsuario',['ADMIN','SUPERADMIN','CONDUCTOR','CLIENTE'], 350,1240],
    ['EEstadoPorte','EstadoPorte',['PENDIENTE','ASIGNADO','EN_RECOGIDA','EN_TRANSITO','ENTREGADO','CANCELADO','FACTURADO'], 590,1240],
    ['ETipoVehiculo','TipoVehiculo',['FURGONETA','CAMION_PEQUENO','CAMION_MEDIANO','CAMION_GRANDE','TRAILER'], 840,1240],
    ['EEstadoVehiculo','EstadoVehiculo',['DISPONIBLE','EN_SERVICIO','MANTENIMIENTO','AVERIADO','RETIRADO'], 1090,1240],
    ['EEstadoIncidencia','EstadoIncidencia',['ABIERTA','EN_PROCESO','RESUELTA','CERRADA'], 1340,1240],
    ['EPrioridadIncidencia','PrioridadIncidencia',['BAJA','MEDIA','ALTA','URGENTE'], 1590,1240],
    ['ETrackingSessionStatus','TrackingSessionStatus',['ACTIVE','PAUSED','COMPLETED'], 1840,1240],
    ['ETrackingSessionPhase','TrackingSessionPhase',['PRE_TRIP','EN_ROUTE','POST_TRIP'], 350,1420],
    ['ETipoFotoCarga','TipoFotoCarga',['RECOGIDA','ENTREGA','INCIDENCIA','OTRO'], 600,1420],
    ['ETipoBloqueoAgenda','TipoBloqueoAgenda',['VACACIONES','DESCANSO','MANTENIMIENTO_VEHICULO','PERSONAL','OTRO'], 850,1420]
  ];
  for (const [id, name, values, x, y] of enums) b.push(cell(id, enumHtml(name, values), x, y, 220, 145, reducedEnumStyle));

  let i = 1;
  b.push(edge('e'+i++, '1 / 0..1', EDGE.her, 'Usuario', 'Cliente'));
  b.push(edge('e'+i++, '1 / 0..1', EDGE.her, 'Usuario', 'Conductor'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Cliente', 'Porte'));
  b.push(edge('e'+i++, '* / 0..1', EDGE.assoc, 'Porte', 'Conductor'));
  b.push(edge('e'+i++, '1 / 0..1', EDGE.comp, 'Porte', 'Factura'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.comp, 'Porte', 'FotoCarga'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Conductor', 'Vehiculo'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Conductor', 'BloqueoAgenda'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Conductor', 'BloqueoRecurrente'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Conductor', 'TrackingSession'));
  b.push(edge('e'+i++, '* / 0..1', EDGE.assoc, 'TrackingSession', 'Porte'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.comp, 'TrackingSession', 'TrackingPause'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'TrackingSession', 'LocationSample'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Porte', 'Incidencia'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.comp, 'Incidencia', 'IncidenciaEvento'));
  b.push(edge('e'+i++, '* / 0..1', EDGE.weak, 'CargoAnalysisLog', 'Porte'));
  return graph(2250, 1600, b);
}

function expanded() {
  const b = [];
  b.push(cell('title', 'Diagrama de clases ampliado - CargoHub backend', 1180, 20, 1050, 40, titleStyle));
  b.push(cell('legend', '<b>Leyenda UML</b><hr>Triangulo blanco: herencia<br>Rombo blanco: agregacion<br>Rombo negro: composicion<br>Flecha: asociacion<br>Linea discontinua: fuera de alcance / sin FK', 40, 40, 310, 155, noteStyle));
  b.push(cell('note', '<b>Nota metodologica</b><hr>Cliente y Conductor se muestran como especializaciones conceptuales de Usuario. En JPA son OneToOne cascade ALL, no extends.', 390, 40, 450, 120, noteStyle));
  b.push(cell('s1', 'Usuarios y perfiles', 980, 100, 950, 30, sectionStyle));
  b.push(cell('s2', 'Porte y documentacion', 520, 540, 960, 30, sectionStyle));
  b.push(cell('s3', 'Flota y agenda', 2120, 540, 790, 30, sectionStyle));
  b.push(cell('s4', 'Tracking', 1580, 1120, 620, 30, sectionStyle));
  b.push(cell('s5', 'Incidencias', 520, 1510, 960, 30, sectionStyle));

  b.push(cell('Usuario', clsHtml('Usuario', ['Long id','String email','String nombre','String password','RolUsuario rol','boolean activo','LocalDateTime fechaRegistro','LocalDateTime ultimoAcceso','String tokenRecuperacion','String fotoUrl'], ['registrarUsuario()','iniciarSesion()','actualizarPerfil()','crearAdmin()','toggleActivo()','eliminarUsuario()']), 1330, 150, 330, 360));
  b.push(cell('Cliente', clsHtml('Cliente', ['Long id','Usuario usuario','String nombreEmpresa','String cif','String direccionFiscal','String telefono','String emailContacto','String sector'], ['crearCliente()','verPerfil()','actualizarPerfil()','listarClientes()','deshabilitarCliente()','consultarEnvios()']), 870, 190, 330, 330));
  b.push(cell('Conductor', clsHtml('Conductor', ['Long id','Usuario usuario','String nombre','String apellidos','String dni','String telefono','String ciudadBase','Double latitudActual','Double longitudActual','boolean buscarRetorno','String diasLaborables','boolean disponible'], ['crearConductor()','aprobarConductor()','rechazarConductor()','darDeBajaConductor()','actualizarPerfil()','actualizarUbicacion()','consultarAgenda()','buscarDisponibles()']), 1790, 170, 350, 430));

  b.push(cell('Porte', clsHtml('Porte', ['Long id','String origen','String destino','String ciudadOrigen','String ciudadDestino','Double distanciaKm','Double precio','Double ajustePrecio','String descripcionCliente','Double pesoTotalKg','Double volumenTotalM3','TipoVehiculo tipoVehiculoRequerido','boolean revisionManual','EstadoPorte estado','Integer version','LocalDateTime fechaCreacion','LocalDateTime fechaRecogida','LocalDateTime fechaEntrega','String firmaEntregaBase64','Cliente cliente','Conductor conductor','Set<Long> conductoresRechazados'], ['crearPorte()','crearSolicitudPorte()','aceptarPorte()','rechazarPorte()','cambiarEstado()','actualizarPorte()','asignarConductor()','registrarFirmaEntrega()','obtenerTracking()']), 880, 610, 390, 720));
  b.push(cell('Factura', clsHtml('Factura', ['Long id','String numeroSerie','Double baseImponible','Double iva','Double importeTotal','LocalDate fechaEmision','boolean pagada','LocalDate fechaPago','String formaPago','Porte porte'], ['generarFactura()','pagarFactura()','consultarFactura()','listarFacturas()','descargarFacturaPdf()']), 460, 830, 330, 390));
  b.push(cell('FotoCarga', clsHtml('FotoCarga', ['Long id','Porte porte','TipoFotoCarga tipo','String fotoBase64','String descripcion','LocalDateTime fechaCaptura'], ['subirFotoCarga()','listarFotosCarga()','eliminarFotoCarga()','validarPropietarioFoto()']), 1340, 850, 330, 300));

  b.push(cell('Vehiculo', clsHtml('Vehiculo', ['Long id','String matricula','String marca','String modelo','TipoVehiculo tipo','EstadoVehiculo estado','Integer capacidadCargaKg','Double volumenM3','Conductor conductor'], ['crearVehiculo()','guardarVehiculo()','actualizarVehiculo()','activarVehiculo()','desactivarVehiculo()','darDeBajaVehiculo()','reactivarVehiculo()','listarFlota()']), 2150, 620, 350, 420));
  b.push(cell('BloqueoAgenda', clsHtml('BloqueoAgenda', ['Long id','LocalDateTime fechaInicio','LocalDateTime fechaFin','TipoBloqueoAgenda tipo','String titulo','Conductor conductor'], ['crearBloqueoAgenda()','eliminarBloqueoAgenda()','consultarAgendaConductor()']), 2550, 620, 340, 300));
  b.push(cell('BloqueoRecurrente', clsHtml('BloqueoRecurrente', ['Long id','Conductor conductor','int diaSemana','boolean activo','LocalDateTime createdAt','LocalDateTime updatedAt'], ['consultarBloqueosRecurrentes()','actualizarBloqueosRecurrentes()','toggleDiaBloqueado()']), 2550, 960, 340, 320));

  b.push(cell('TrackingSession', clsHtml('TrackingSession', ['Long id','Conductor conductor','Porte porte','TrackingSessionStatus status','TrackingSessionPhase currentPhase','LocalDateTime startedAt','LocalDateTime endedAt','LocalDateTime lastSampleAt'], ['iniciarSesionTracking()','actualizarSesionTracking()','obtenerSesionTracking()']), 1650, 1180, 350, 340));
  b.push(cell('TrackingPause', clsHtml('TrackingPause', ['Long id','TrackingSession session','String motivo','String nota','LocalDateTime startedAt','LocalDateTime endedAt'], ['registrarPausaTracking()']), 2100, 1180, 340, 260));
  b.push(cell('LocationSample', clsHtml('LocationSample', ['Long id','TrackingSession session','Conductor conductor','Porte porte','Double lat','Double lon','LocalDateTime recordedAt','LocalDateTime receivedAt','Double speedKph','Integer headingDeg'], ['registrarMuestraUbicacion()']), 2100, 1460, 340, 330));

  b.push(cell('Incidencia', clsHtml('Incidencia', ['Long id','String titulo','String descripcion','LocalDateTime fechaReporte','EstadoIncidencia estado','SeveridadIncidencia severidad','PrioridadIncidencia prioridad','LocalDateTime fechaLimiteSla','String resolucion','Usuario admin','Porte porte'], ['reportarIncidencia()','resolverIncidencia()','listarPendientes()','listarTodas()','obtenerIncidencia()','listarPorPorte()','contarPendientes()']), 610, 1580, 360, 455));
  b.push(cell('IncidenciaEvento', clsHtml('IncidenciaEvento', ['Long id','Incidencia incidencia','Usuario actor','EstadoIncidencia estadoAnterior','EstadoIncidencia estadoNuevo','LocalDateTime fecha','String accion','String comentario'], ['registrarEventoIncidencia()','listarHistorialIncidencia()']), 1120, 1600, 360, 330));

  b.push(cell('CargoAnalysisLog', clsHtml('CargoAnalysisLog', ['Long id','String requestData','LocalDateTime requestTimestamp','String responseData','Boolean success','String errorMessage','Double pesoTotalKg','Double volumenTotalM3','String tipoVehiculoRequerido','Boolean revisionManual','Porte porte'], ['registrarAnalisisCarga()','registrarErrorAnalisisCarga()']), 70, 1660, 340, 420, dashedStyle));

  const enums = [
    ['ERolUsuario','RolUsuario',['ADMIN','SUPERADMIN','CONDUCTOR','CLIENTE'], 1220,160],
    ['EEstadoPorte','EstadoPorte',['PENDIENTE','ASIGNADO','EN_RECOGIDA','EN_TRANSITO','ENTREGADO','CANCELADO','FACTURADO'], 1310,610],
    ['ETipoVehiculo','TipoVehiculo',['FURGONETA','CAMION_PEQUENO','CAMION_MEDIANO','CAMION_GRANDE','TRAILER'], 2150,950],
    ['EEstadoVehiculo','EstadoVehiculo',['DISPONIBLE','EN_SERVICIO','MANTENIMIENTO','AVERIADO','RETIRADO'], 2150,1120],
    ['ETipoBloqueoAgenda','TipoBloqueoAgenda',['VACACIONES','DESCANSO','MANTENIMIENTO_VEHICULO','PERSONAL','OTRO'], 2550,1210],
    ['ETrackingSessionStatus','TrackingSessionStatus',['ACTIVE','PAUSED','COMPLETED'], 1650,1490],
    ['ETrackingSessionPhase','TrackingSessionPhase',['PRE_TRIP','EN_ROUTE','POST_TRIP'], 1650,1640],
    ['ETipoFotoCarga','TipoFotoCarga',['RECOGIDA','ENTREGA','INCIDENCIA','OTRO'], 1340,1100],
    ['EEstadoIncidencia','EstadoIncidencia',['ABIERTA','EN_PROCESO','RESUELTA','CERRADA'], 610,1940],
    ['ESeveridadIncidencia','SeveridadIncidencia',['BAJA','MEDIA','ALTA','CRITICA'], 850,1940],
    ['EPrioridadIncidencia','PrioridadIncidencia',['BAJA','MEDIA','ALTA','URGENTE'], 1100,2100]
  ];
  for (const [id, name, values, x, y] of enums) b.push(cell(id, enumHtml(name, values), x, y, 230, 150, enumStyle));

  let i = 1;
  b.push(edge('e'+i++, '1 / 0..1', EDGE.her, 'Usuario', 'Cliente'));
  b.push(edge('e'+i++, '1 / 0..1', EDGE.her, 'Usuario', 'Conductor'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Cliente', 'Porte'));
  b.push(edge('e'+i++, '* / 0..1', EDGE.assoc, 'Porte', 'Conductor'));
  b.push(edge('e'+i++, '1 / 0..1', EDGE.comp, 'Porte', 'Factura'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.comp, 'Porte', 'FotoCarga'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Conductor', 'Vehiculo'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Conductor', 'BloqueoAgenda'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Conductor', 'BloqueoRecurrente'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Conductor', 'TrackingSession'));
  b.push(edge('e'+i++, '* / 0..1', EDGE.assoc, 'TrackingSession', 'Porte'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.comp, 'TrackingSession', 'TrackingPause'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'TrackingSession', 'LocationSample'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.agg, 'Porte', 'Incidencia'));
  b.push(edge('e'+i++, '1 / 0..*', EDGE.comp, 'Incidencia', 'IncidenciaEvento'));
  b.push(edge('e'+i++, '* / 0..1', EDGE.assoc, 'Incidencia', 'Usuario'));
  b.push(edge('e'+i++, '* / 0..1', EDGE.assoc, 'IncidenciaEvento', 'Usuario'));
  b.push(edge('e'+i++, '* / 1', EDGE.agg, 'LocationSample', 'Conductor'));
  b.push(edge('e'+i++, '* / 0..1', EDGE.assoc, 'LocationSample', 'Porte'));
  b.push(edge('e'+i++, '* / 0..1', EDGE.weak, 'CargoAnalysisLog', 'Porte'));
  return graph(3100, 2450, b);
}

const outputs = [
  ['TFG_CargoHub_clases_reducido.drawio', 'clases-reducido-bn', 'Clases reducido BN', reduced()],
  ['TFG_CargoHub_clases_ampliado.drawio', 'clases-ampliado-bn', 'Clases ampliado BN', expanded()]
];

for (const [file, id, name, g] of outputs) {
  fs.writeFileSync(path.join(__dirname, file), pack(id, name, g), 'utf8');
  console.log(`Generated ${file}`);
}
