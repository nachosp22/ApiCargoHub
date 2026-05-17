$ErrorActionPreference = 'Stop'

$output = Join-Path $PSScriptRoot 'TFG_CargoHub_tablas_clases_limpio_v2.docx'
$work = Join-Path $env:TEMP ('cargohub_class_tables_clean_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work '_rels') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word\_rels') | Out-Null

function X([string]$s) { if ($null -eq $s) { return '' }; return [System.Security.SecurityElement]::Escape($s) }
function P([string]$text) { return "<w:p><w:pPr><w:spacing w:after='120'/></w:pPr><w:r><w:rPr><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='24'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function H1([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading1'/><w:spacing w:before='120' w:after='120'/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='32'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function H2([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading2'/><w:spacing w:before='180' w:after='80'/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='28'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }

function Cell([string]$text, [int]$width, [bool]$header=$false, [int]$span=1, [string]$fill='') {
  $shade = if ($fill) { "<w:shd w:fill='$fill'/>" } elseif ($header) { "<w:shd w:fill='D9EAF7'/>" } else { '' }
  $bold = if ($header) { '<w:b/>' } else { '' }
  $gridSpan = if ($span -gt 1) { "<w:gridSpan w:val='$span'/>" } else { '' }
  return "<w:tc><w:tcPr><w:tcW w:w='$width' w:type='dxa'/>$gridSpan$shade</w:tcPr><w:p><w:pPr><w:spacing w:after='0'/></w:pPr><w:r><w:rPr>$bold<w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='20'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p></w:tc>"
}

function FixedTable($title, $headers, $widths, $rows) {
  $xml = "<w:tbl><w:tblPr><w:tblStyle w:val='TableGrid'/><w:tblW w:w='15000' w:type='dxa'/><w:tblLayout w:type='fixed'/><w:tblBorders><w:top w:val='single' w:sz='4'/><w:left w:val='single' w:sz='4'/><w:bottom w:val='single' w:sz='4'/><w:right w:val='single' w:sz='4'/><w:insideH w:val='single' w:sz='4'/><w:insideV w:val='single' w:sz='4'/></w:tblBorders></w:tblPr><w:tblGrid>"
  foreach ($w in $widths) { $xml += "<w:gridCol w:w='$w'/>" }
  $totalWidth = ($widths | Measure-Object -Sum).Sum
  $xml += '</w:tblGrid>'
  if ($title) { $xml += '<w:tr>' + (Cell $title $totalWidth $true $headers.Count 'B4C7E7') + '</w:tr>' }
  $xml += '<w:tr>'
  for ($i=0; $i -lt $headers.Count; $i++) { $xml += Cell $headers[$i] $widths[$i] $true }
  $xml += '</w:tr>'
  foreach ($r in $rows) {
    $xml += '<w:tr>'
    for ($i=0; $i -lt $headers.Count; $i++) { $xml += Cell $r[$i] $widths[$i] $false }
    $xml += '</w:tr>'
  }
  $xml += '</w:tbl>'
  return $xml
}

function Rows($kind, $visibility, $items) {
  $rows = @()
  foreach ($i in $items) {
    $parts = $i -split ': ', 2
    $name = $parts[0]
    $detail = if ($parts.Count -gt 1) { $parts[1] } else { '' }
    $rows += ,@($kind, $visibility, $name, $detail)
  }
  return $rows
}

$classes = @(
  @{Name='Usuario'; Table='usuarios'; Resp='Cuenta base de acceso al sistema.'; Attr=@('id: Long','email: String','nombre: String','password: String','rol: RolUsuario','activo: boolean','fechaRegistro: LocalDateTime','ultimoAcceso: LocalDateTime','tokenRecuperacion: String','fotoUrl: String'); Meth=@('registrarUsuario(): Registra una nueva cuenta','iniciarSesion(): Autentica al usuario','actualizarPerfil(): Actualiza datos personales','crearAdmin(): Crea un usuario administrador','listarAdmins(): Lista administradores','toggleActivo(): Activa o desactiva la cuenta','eliminarUsuario(): Elimina usuario') ; Rel=@('Usuario - Cliente: Herencia conceptual 1 a 0..1','Usuario - Conductor: Herencia conceptual 1 a 0..1')},
  @{Name='Cliente'; Table='clientes'; Resp='Perfil empresarial que solicita portes.'; Attr=@('id: Long','usuario: Usuario','nombreEmpresa: String','cif: String','direccionFiscal: String','telefono: String','emailContacto: String','sector: String'); Meth=@('crearCliente(): Crea el perfil cliente','verPerfil(): Consulta el perfil','actualizarPerfil(): Modifica datos del cliente','listarClientes(): Lista clientes','deshabilitarCliente(): Desactiva cliente','consultarEnvios(): Consulta portes del cliente'); Rel=@('Cliente - Porte: Agregacion 1 a 0..*')},
  @{Name='Conductor'; Table='conductores'; Resp='Perfil operativo que realiza portes y reporta ubicacion.'; Attr=@('id: Long','usuario: Usuario','nombre: String','apellidos: String','dni: String','telefono: String','ciudadBase: String','latitudBase: Double','longitudBase: Double','radioAccionKm: Integer','latitudActual: Double','longitudActual: Double','ultimaActualizacionUbicacion: LocalDateTime','velocidadKphActual: Double','rumboActualDeg: Integer','buscarRetorno: boolean','diasLaborables: String','disponible: boolean'); Meth=@('crearConductor(): Crea conductor','aprobarConductor(): Aprueba conductor','rechazarConductor(): Rechaza conductor','darDeBajaConductor(): Da de baja conductor','actualizarPerfil(): Actualiza perfil','actualizarUbicacion(): Reporta ubicacion','consultarAgenda(): Consulta agenda','buscarDisponibles(): Busca candidatos disponibles'); Rel=@('Conductor - Vehiculo: Agregacion 1 a 0..*','Conductor - TrackingSession: Agregacion 1 a 0..*','Porte - Conductor: Asociacion * a 0..1')},
  @{Name='Porte'; Table='portes'; Resp='Servicio principal de transporte solicitado por un cliente.'; Attr=@('id: Long','origen: String','destino: String','ciudadOrigen: String','ciudadDestino: String','distanciaKm: Double','precio: Double','ajustePrecio: Double','descripcionCliente: String','pesoTotalKg: Double','volumenTotalM3: Double','tipoVehiculoRequerido: TipoVehiculo','revisionManual: boolean','estado: EstadoPorte','version: Integer','fechaCreacion: LocalDateTime','fechaRecogida: LocalDateTime','fechaEntrega: LocalDateTime','firmaEntregaBase64: String','cliente: Cliente','conductor: Conductor','conductoresRechazados: Set<Long>'); Meth=@('crearPorte(): Crea porte','crearSolicitudPorte(): Registra solicitud cliente','aceptarPorte(): Acepta oferta','rechazarPorte(): Rechaza oferta','cambiarEstado(): Cambia estado operativo','actualizarPorte(): Modifica datos','actualizarDimensiones(): Actualiza carga','asignarConductor(): Asigna conductor','registrarFirmaEntrega(): Registra firma','obtenerTracking(): Consulta seguimiento'); Rel=@('Cliente - Porte: Agregacion 1 a 0..*','Porte - Factura: Composicion 1 a 0..1','Porte - FotoCarga: Composicion 1 a 0..*','Porte - Incidencia: Agregacion 1 a 0..*')},
  @{Name='Vehiculo'; Table='vehiculos'; Resp='Vehiculo asociado a conductores para realizar portes.'; Attr=@('id: Long','matricula: String','marca: String','modelo: String','tipo: TipoVehiculo','estado: EstadoVehiculo','capacidadCargaKg: Integer','largoUtilMm: Integer','anchoUtilMm: Integer','altoUtilMm: Integer','volumenM3: Double','conductor: Conductor'); Meth=@('crearVehiculo(): Crea vehiculo','guardarVehiculo(): Guarda vehiculo','actualizarVehiculo(): Actualiza vehiculo','activarVehiculo(): Activa vehiculo','desactivarVehiculo(): Desactiva vehiculo','darDeBajaVehiculo(): Baja vehiculo','reactivarVehiculo(): Reactiva vehiculo','listarFlota(): Lista flota'); Rel=@('Conductor - Vehiculo: Agregacion 1 a 0..*')},
  @{Name='Factura'; Table='facturas'; Resp='Documento economico generado para un porte.'; Attr=@('id: Long','numeroSerie: String','baseImponible: Double','iva: Double','importeTotal: Double','fechaEmision: LocalDate','pagada: boolean','fechaPago: LocalDate','formaPago: String','porte: Porte'); Meth=@('generarFactura(): Genera factura','pagarFactura(): Marca como pagada','consultarFactura(): Consulta detalle','listarFacturas(): Lista facturas','descargarFacturaPdf(): Descarga PDF'); Rel=@('Porte - Factura: Composicion 1 a 0..1')},
  @{Name='Incidencia'; Table='incidencias'; Resp='Problema operativo asociado a un porte.'; Attr=@('id: Long','titulo: String','descripcion: String','fechaReporte: LocalDateTime','estado: EstadoIncidencia','severidad: SeveridadIncidencia','prioridad: PrioridadIncidencia','fechaLimiteSla: LocalDateTime','resolucion: String','admin: Usuario','porte: Porte'); Meth=@('reportarIncidencia(): Crea incidencia','resolverIncidencia(): Resuelve incidencia','listarPendientes(): Lista pendientes','listarTodas(): Lista incidencias','obtenerIncidencia(): Consulta incidencia','listarPorPorte(): Lista por porte','contarPendientes(): Cuenta pendientes'); Rel=@('Porte - Incidencia: Agregacion 1 a 0..*','Incidencia - IncidenciaEvento: Composicion 1 a 0..*')},
  @{Name='IncidenciaEvento'; Table='incidencia_eventos'; Resp='Registro historico de cambios sobre una incidencia.'; Attr=@('id: Long','incidencia: Incidencia','actor: Usuario','estadoAnterior: EstadoIncidencia','estadoNuevo: EstadoIncidencia','fecha: LocalDateTime','accion: String','comentario: String'); Meth=@('registrarEventoIncidencia(): Registra evento','listarHistorialIncidencia(): Lista historial'); Rel=@('Incidencia - IncidenciaEvento: Composicion 1 a 0..*')},
  @{Name='TrackingSession'; Table='tracking_sessions'; Resp='Sesion de seguimiento GPS de un conductor.'; Attr=@('id: Long','conductor: Conductor','porte: Porte','status: TrackingSessionStatus','currentPhase: TrackingSessionPhase','startedAt: LocalDateTime','endedAt: LocalDateTime','lastSampleAt: LocalDateTime'); Meth=@('iniciarSesionTracking(): Inicia sesion','actualizarSesionTracking(): Actualiza sesion','obtenerSesionTracking(): Consulta sesion'); Rel=@('Conductor - TrackingSession: Agregacion 1 a 0..*','TrackingSession - TrackingPause: Composicion 1 a 0..*','TrackingSession - LocationSample: Agregacion 1 a 0..*')},
  @{Name='TrackingPause'; Table='tracking_pauses'; Resp='Pausa temporal dentro de una sesion de tracking.'; Attr=@('id: Long','session: TrackingSession','motivo: String','nota: String','startedAt: LocalDateTime','endedAt: LocalDateTime'); Meth=@('registrarPausaTracking(): Registra pausa'); Rel=@('TrackingSession - TrackingPause: Composicion 1 a 0..*')},
  @{Name='LocationSample'; Table='location_samples'; Resp='Muestra puntual de ubicacion GPS.'; Attr=@('id: Long','session: TrackingSession','conductor: Conductor','porte: Porte','lat: Double','lon: Double','recordedAt: LocalDateTime','receivedAt: LocalDateTime','speedKph: Double','headingDeg: Integer'); Meth=@('registrarMuestraUbicacion(): Registra muestra GPS'); Rel=@('TrackingSession - LocationSample: Agregacion 1 a 0..*','LocationSample - Conductor: Agregacion * a 1')},
  @{Name='FotoCarga'; Table='fotos_carga'; Resp='Evidencia fotografica asociada a un porte.'; Attr=@('id: Long','porte: Porte','tipo: TipoFotoCarga','fotoBase64: String','descripcion: String','fechaCaptura: LocalDateTime'); Meth=@('subirFotoCarga(): Sube foto','listarFotosCarga(): Lista fotos','eliminarFotoCarga(): Elimina foto','validarPropietarioFoto(): Valida propietario'); Rel=@('Porte - FotoCarga: Composicion 1 a 0..*')},
  @{Name='BloqueoAgenda'; Table='agenda_bloqueos'; Resp='Bloqueo puntual de disponibilidad de conductor.'; Attr=@('id: Long','fechaInicio: LocalDateTime','fechaFin: LocalDateTime','tipo: TipoBloqueoAgenda','titulo: String','conductor: Conductor'); Meth=@('crearBloqueoAgenda(): Crea bloqueo','eliminarBloqueoAgenda(): Elimina bloqueo','consultarAgendaConductor(): Consulta agenda'); Rel=@('Conductor - BloqueoAgenda: Agregacion 1 a 0..*')},
  @{Name='BloqueoRecurrente'; Table='bloqueos_recurrentes'; Resp='Bloqueo semanal recurrente de disponibilidad.'; Attr=@('id: Long','conductor: Conductor','diaSemana: int','activo: boolean','createdAt: LocalDateTime','updatedAt: LocalDateTime'); Meth=@('consultarBloqueosRecurrentes(): Consulta bloqueos','actualizarBloqueosRecurrentes(): Actualiza bloqueos','toggleDiaBloqueado(): Alterna dia'); Rel=@('Conductor - BloqueoRecurrente: Agregacion 1 a 0..*')},
  @{Name='CargoAnalysisLog'; Table='cargo_analysis_logs'; Resp='Log tecnico de analisis de carga.'; Attr=@('id: Long','requestData: String','requestTimestamp: LocalDateTime','responseData: String','responseTimestamp: LocalDateTime','success: Boolean','errorMessage: String','pesoTotalKg: Double','volumenTotalM3: Double','tipoVehiculoRequerido: String','revisionManual: Boolean','porte: Porte'); Meth=@('registrarAnalisisCarga(): Registra analisis correcto','registrarErrorAnalisisCarga(): Registra error'); Rel=@('CargoAnalysisLog - Porte: Asociacion debil * a 0..1')}
)

$body = H1 'Tablas del diagrama de clases - CargoHub backend'
$body += P 'Este documento complementa el diagrama de clases ampliado. Para evitar problemas de formato, cada clase se resume en una unica tabla con ancho fijo. Los atributos aparecen como privados (-) y las operaciones como publicas (+).'
foreach ($c in $classes) {
  $rows = @()
  foreach ($a in $c.Attr) { $p = $a -split ': ',2; $rows += ,@('Atributo', "- $($p[0])", $(if($p.Count -gt 1){$p[1]}else{''})) }
  foreach ($m in $c.Meth) { $p = $m -split ': ',2; $rows += ,@('Operacion', "+ $($p[0])", $(if($p.Count -gt 1){$p[1]}else{'Entidad + controller/service'})) }
  foreach ($r in $c.Rel) { $p = $r -split ': ',2; $rows += ,@('Relacion', $p[0], $(if($p.Count -gt 1){$p[1]}else{''})) }
  $body += FixedTable $c.Name @('Tipo','Elemento','Detalle') @(2600,5200,7200) $rows
  $body += P ''
}

$body += FixedTable 'Enumeraciones' @('Enumeracion','Valores') @(4200,10800) @(
  @('RolUsuario','ADMIN, SUPERADMIN, CONDUCTOR, CLIENTE'),
  @('EstadoPorte','PENDIENTE, ASIGNADO, EN_RECOGIDA, EN_TRANSITO, ENTREGADO, CANCELADO, FACTURADO'),
  @('TipoVehiculo','FURGONETA, CAMION_PEQUENO, CAMION_MEDIANO, CAMION_GRANDE, TRAILER'),
  @('EstadoVehiculo','DISPONIBLE, EN_SERVICIO, MANTENIMIENTO, AVERIADO, RETIRADO'),
  @('EstadoIncidencia','ABIERTA, EN_PROCESO, RESUELTA, CERRADA'),
  @('SeveridadIncidencia','BAJA, MEDIA, ALTA, CRITICA'),
  @('PrioridadIncidencia','BAJA, MEDIA, ALTA, URGENTE'),
  @('TrackingSessionStatus','ACTIVE, PAUSED, COMPLETED'),
  @('TrackingSessionPhase','PRE_TRIP, EN_ROUTE, POST_TRIP'),
  @('TipoFotoCarga','RECOGIDA, ENTREGA, INCIDENCIA, OTRO'),
  @('TipoBloqueoAgenda','VACACIONES, DESCANSO, MANTENIMIENTO_VEHICULO, PERSONAL, OTRO')
)

$document = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body>$body<w:sectPr><w:pgSz w:w='16838' w:h='11906' w:orient='landscape'/><w:pgMar w:top='850' w:right='850' w:bottom='850' w:left='850'/></w:sectPr></w:body></w:document>"
$styles = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:styles xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:style w:type='paragraph' w:default='1' w:styleId='Normal'><w:name w:val='Normal'/></w:style><w:style w:type='paragraph' w:styleId='Heading1'><w:name w:val='heading 1'/><w:rPr><w:b/><w:sz w:val='32'/></w:rPr></w:style><w:style w:type='paragraph' w:styleId='Heading2'><w:name w:val='heading 2'/><w:rPr><w:b/><w:sz w:val='28'/></w:rPr></w:style><w:style w:type='table' w:styleId='TableGrid'><w:name w:val='Table Grid'/><w:tblPr><w:tblBorders><w:top w:val='single' w:sz='4'/><w:left w:val='single' w:sz='4'/><w:bottom w:val='single' w:sz='4'/><w:right w:val='single' w:sz='4'/><w:insideH w:val='single' w:sz='4'/><w:insideV w:val='single' w:sz='4'/></w:tblBorders></w:tblPr></w:style></w:styles>"
$ct = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Types xmlns='http://schemas.openxmlformats.org/package/2006/content-types'><Default Extension='rels' ContentType='application/vnd.openxmlformats-package.relationships+xml'/><Default Extension='xml' ContentType='application/xml'/><Override PartName='/word/document.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml'/><Override PartName='/word/styles.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml'/></Types>"
$rels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument' Target='word/document.xml'/></Relationships>"
$docRels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles' Target='styles.xml'/></Relationships>"

Set-Content -LiteralPath (Join-Path $work '[Content_Types].xml') -Value $ct -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work '_rels\.rels') -Value $rels -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\document.xml') -Value $document -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\styles.xml') -Value $styles -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\_rels\document.xml.rels') -Value $docRels -Encoding UTF8
if (Test-Path -LiteralPath $output) { Remove-Item -LiteralPath $output -Force }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $output)
Remove-Item -LiteralPath $work -Recurse -Force
"DOCX regenerado limpio: $output"

