$ErrorActionPreference = 'Stop'

$pumlOutputClienteConductor = Join-Path $PSScriptRoot 'TFG_CargoHub_casos_uso_cliente_conductor.puml'
$pumlOutputAdministracion = Join-Path $PSScriptRoot 'TFG_CargoHub_casos_uso_administracion.puml'
$docxOutput = Join-Path $PSScriptRoot 'TFG_CargoHub_tablas_casos_uso_actualizadas.docx'
$mdOutput = Join-Path $PSScriptRoot 'TFG_CargoHub_casos_uso_actualizados.md'

if (Test-Path -LiteralPath $docxOutput) {
  try {
    $stream = [System.IO.File]::Open($docxOutput, 'Open', 'ReadWrite', 'None')
    $stream.Close()
  } catch {
    $docxOutput = Join-Path $PSScriptRoot ('TFG_CargoHub_tablas_casos_uso_actualizadas_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.docx')
  }
}

$plantUmlClienteConductor = @'
@startuml
top to bottom direction
scale max 650 width
skinparam monochrome true
skinparam shadowing false
skinparam packageStyle rectangle
skinparam actorStyle awesome
skinparam dpi 150
skinparam defaultFontSize 10
skinparam nodesep 15
skinparam ranksep 18
skinparam usecase {
  BackgroundColor White
  BorderColor Black
  FontColor Black
  FontSize 10
}

actor "Cliente" as Cliente
actor "Conductor" as Conductor

rectangle "CargoHub - Cliente y conductor" {
  usecase "Iniciar\nsesion" as CU01
  usecase "Gestionar\nperfil" as CU02
  usecase "Solicitar\nporte" as CU03
  usecase "Mis\nportes" as CU04
  usecase "Seguimiento\ndel porte" as CU05
  usecase "Facturas" as CU06
  usecase "Ofertas /\nasignaciones" as CU07
  usecase "Aceptar /\nrechazar" as CU08
  usecase "Estado\ndel porte" as CU09
  usecase "Ubicacion\nGPS" as CU10
  usecase "Entrega y\nalbaran" as CU11
  usecase "Reportar\nincidencia" as CU12
  usecase "Vehiculo\ny agenda" as CU13

  CU01 -[hidden]down- CU02
  CU02 -[hidden]down- CU03
  CU03 -[hidden]down- CU04
  CU04 -[hidden]down- CU05
  CU05 -[hidden]down- CU06
  CU06 -[hidden]down- CU07
  CU07 -[hidden]down- CU08
  CU08 -[hidden]down- CU09
  CU09 -[hidden]down- CU10
  CU10 -[hidden]down- CU11
  CU11 -[hidden]down- CU12
  CU12 -[hidden]down- CU13
}

CU01 -right- Cliente
CU02 -right- Cliente
CU03 -right- Cliente
CU04 -right- Cliente
CU05 -right- Cliente
CU06 -right- Cliente

CU01 -right- Conductor
CU02 -right- Conductor
CU07 -right- Conductor
CU08 -right- Conductor
CU09 -right- Conductor
CU10 -right- Conductor
CU11 -right- Conductor
CU12 -right- Conductor
CU13 -right- Conductor

CU03 ..> CU01 : <<include>>
CU04 ..> CU01 : <<include>>
CU07 ..> CU01 : <<include>>
CU11 ..> CU09 : <<extend>>
CU12 ..> CU09 : <<extend>>
@enduml
'@

$plantUmlAdministracion = @'
@startuml
top to bottom direction
scale max 650 width
skinparam monochrome true
skinparam shadowing false
skinparam packageStyle rectangle
skinparam actorStyle awesome
skinparam dpi 150
skinparam defaultFontSize 10
skinparam nodesep 15
skinparam ranksep 18
skinparam usecase {
  BackgroundColor White
  BorderColor Black
  FontColor Black
  FontSize 10
}

actor "Administrador" as Admin
actor "Superadministrador" as SuperAdmin

SuperAdmin --|> Admin

rectangle "CargoHub - Administracion" {
  usecase "Iniciar\nsesion" as CU01
  usecase "Usuarios\ny roles" as CU14
  usecase "Clientes" as CU15
  usecase "Conductores" as CU16
  usecase "Aprobar\nconductores" as CU17
  usecase "Vehiculos" as CU18
  usecase "Crear / revisar\nportes" as CU19
  usecase "Asignar\nconductor" as CU20
  usecase "Mapa de\nflota" as CU21
  usecase "Incidencias" as CU22
  usecase "Generar\nfactura" as CU23
  usecase "Estadisticas" as CU24

  CU01 -[hidden]down- CU14
  CU14 -[hidden]down- CU15
  CU15 -[hidden]down- CU16
  CU16 -[hidden]down- CU17
  CU17 -[hidden]down- CU18
  CU18 -[hidden]down- CU19
  CU19 -[hidden]down- CU20
  CU20 -[hidden]down- CU21
  CU21 -[hidden]down- CU22
  CU22 -[hidden]down- CU23
  CU23 -[hidden]down- CU24
}

CU01 -right- Admin
CU14 -right- Admin
CU15 -right- Admin
CU16 -right- Admin
CU17 -right- Admin
CU18 -right- Admin
CU19 -right- Admin
CU20 -right- Admin
CU21 -right- Admin
CU22 -right- Admin
CU23 -right- Admin
CU24 -right- Admin

CU14 ..> CU01 : <<include>>
CU19 ..> CU01 : <<include>>
CU20 ..> CU19 : <<extend>>
CU23 ..> CU19 : <<extend>>
CU21 ..> CU20 : <<include>>
@enduml
'@

Set-Content -LiteralPath $pumlOutputClienteConductor -Value $plantUmlClienteConductor -Encoding UTF8
Set-Content -LiteralPath $pumlOutputAdministracion -Value $plantUmlAdministracion -Encoding UTF8

$actors = @(
  @('CLIENTE','Entidad, empresa o particular que solicita servicios logisticos desde el portal web y consulta el estado de sus portes y facturas.'),
  @('CONDUCTOR','Profesional del transporte encargado de aceptar servicios, ejecutar portes, actualizar estados, emitir ubicacion y registrar incidencias.'),
  @('ADMINISTRADOR','Responsable de la operativa diaria: revisa solicitudes, gestiona recursos, asigna conductores, controla incidencias y genera facturas.'),
  @('SUPERADMINISTRADOR','Especializacion del administrador con permisos completos sobre usuarios, roles y configuracion general del sistema.')
)

$cases = @(
  @{Actor='CLIENTE'; ID='CU-01'; Nombre='Iniciar sesion'; Descripcion='El usuario accede al sistema introduciendo sus credenciales para utilizar las funcionalidades segun su rol.'; Pre='El usuario debe estar registrado y activo.'; Post='Se crea una sesion valida y se permite el acceso a las pantallas correspondientes.'; Restr='Las credenciales deben ser correctas y el usuario no puede estar bloqueado o dado de baja.'; RF='RF 1.1 Autenticacion de usuarios / RF 1.2 Gestion de roles y permisos.'},
  @{Actor='CLIENTE'; ID='CU-02'; Nombre='Gestionar perfil'; Descripcion='El cliente consulta o actualiza sus datos basicos de contacto y acceso.'; Pre='El cliente debe haber iniciado sesion.'; Post='Los datos modificados quedan actualizados en el sistema.'; Restr='No se permite modificar identificadores sensibles sin validacion administrativa.'; RF='RF 2.2 Modificacion de usuarios / RF 3.2 Edicion perfil empresa.'},
  @{Actor='CLIENTE'; ID='CU-03'; Nombre='Solicitar porte'; Descripcion='El cliente crea una solicitud de transporte indicando origen, destino, fechas y datos de la carga.'; Pre='Cliente autenticado y con datos minimos completados.'; Post='Se registra un nuevo porte o solicitud con estado pendiente de revision.'; Restr='Origen, destino, fechas y descripcion de la carga son obligatorios.'; RF='RF 5.1 Solicitud nuevo porte.'},
  @{Actor='CLIENTE'; ID='CU-04'; Nombre='Consultar mis portes'; Descripcion='El cliente visualiza el listado de portes asociados a su cuenta.'; Pre='Cliente autenticado.'; Post='El sistema muestra solo los portes pertenecientes al cliente.'; Restr='Un cliente no puede acceder a portes de otros clientes.'; RF='RF 5.4 Consulta de portes / RF 3.3 Consultar Historial de Servicios.'},
  @{Actor='CLIENTE'; ID='CU-05'; Nombre='Ver seguimiento del porte'; Descripcion='El cliente consulta el estado actual y la trazabilidad basica de un porte.'; Pre='El porte debe pertenecer al cliente autenticado.'; Post='Se muestran estado, informacion de seguimiento y datos relevantes del transporte.'; Restr='La informacion se limita al porte solicitado y a datos autorizados.'; RF='RF 5.7 Reporte a cliente / RF 4.5 Reporte de ubicacion / RF 7.2 Muestras de ubicacion.'},
  @{Actor='CLIENTE'; ID='CU-06'; Nombre='Consultar facturas'; Descripcion='El cliente accede a sus facturas emitidas y puede consultar o descargar la informacion disponible.'; Pre='Cliente autenticado y factura generada previamente.'; Post='Se muestra el listado de facturas propias.'; Restr='No se permite consultar facturas de otros clientes.'; RF='RF 8.1 Factura de porte a cliente.'},

  @{Actor='CONDUCTOR'; ID='CU-07'; Nombre='Consultar ofertas o asignaciones'; Descripcion='El conductor visualiza los portes disponibles, ofertados o asignados a su perfil.'; Pre='Conductor autenticado y aprobado por administracion.'; Post='Se muestran los servicios vinculados al conductor.'; Restr='No se muestran servicios asignados a otros conductores.'; RF='RF 5.4 Consulta de portes / RF 5.9 Rechazo de ofertas.'},
  @{Actor='CONDUCTOR'; ID='CU-08'; Nombre='Aceptar o rechazar porte'; Descripcion='El conductor acepta o rechaza una oferta de porte desde la aplicacion movil.'; Pre='Debe existir una oferta o asignacion pendiente para el conductor.'; Post='El porte queda aceptado, rechazado o pendiente de nueva asignacion.'; Restr='Solo puede responder el conductor destinatario de la oferta.'; RF='RF 5.9 Rechazo de ofertas / RF 5.5 Modificacion de estado de porte.'},
  @{Actor='CONDUCTOR'; ID='CU-09'; Nombre='Actualizar estado del porte'; Descripcion='El conductor cambia el estado operativo del porte durante el servicio: inicio, recogida, transito y entrega.'; Pre='El porte debe estar asignado y en un estado que permita avanzar.'; Post='El sistema guarda el nuevo estado y su historial.'; Restr='No se pueden saltar estados obligatorios del flujo.'; RF='RF 5.5 Modificacion de estado de porte.'},
  @{Actor='CONDUCTOR'; ID='CU-10'; Nombre='Emitir ubicacion GPS'; Descripcion='La aplicacion movil envia muestras de ubicacion asociadas al conductor o porte activo.'; Pre='Conductor autenticado, permisos de ubicacion concedidos y servicio activo.'; Post='La ubicacion queda registrada para seguimiento y mapa de flota.'; Restr='La ubicacion debe tener coordenadas validas y timestamp coherente.'; RF='RF 4.5 Reporte de ubicacion / RF 7.1 Sesion de seguimiento / RF 7.2 Muestras de ubicacion.'},
  @{Actor='CONDUCTOR'; ID='CU-11'; Nombre='Registrar entrega y albaran'; Descripcion='El conductor finaliza la entrega registrando evidencias como firma, fotos o datos necesarios para el albaran.'; Pre='El porte debe encontrarse en transito y listo para entrega.'; Post='El porte queda entregado y se habilita la documentacion asociada.'; Restr='No se puede generar entrega si faltan datos obligatorios.'; RF='RF 5.8 Prueba de entrega / RF 5.10 Fotos de carga / RF 8.3 Albaran de entrega.'},
  @{Actor='CONDUCTOR'; ID='CU-12'; Nombre='Reportar incidencia'; Descripcion='El conductor registra una incidencia relacionada con un porte o situacion operativa.'; Pre='Conductor autenticado y, normalmente, porte activo.'; Post='Se crea una incidencia pendiente de gestion administrativa.'; Restr='Debe indicarse tipo y descripcion minima de la incidencia.'; RF='RF 6.1 Reportar incidencia / RF 6.3 Historial de incidencias.'},
  @{Actor='CONDUCTOR'; ID='CU-13'; Nombre='Gestionar vehiculo y agenda'; Descripcion='El conductor consulta sus datos, vehiculo vinculado, disponibilidad y agenda de trabajo.'; Pre='Conductor autenticado y perfil existente.'; Post='Se muestra o actualiza informacion operativa del conductor.'; Restr='Solo puede gestionar informacion asociada a su propio perfil.'; RF='RF 2.4 Modificacion de conductores / RF 4.2 Asignacion de vehiculo a conductor / RF 4.6 Agenda y disponibilidad del conductor.'},

  @{Actor='ADMINISTRADOR'; ID='CU-14'; Nombre='Gestionar usuarios y roles'; Descripcion='El administrador crea, consulta, modifica o desactiva usuarios y sus roles dentro del sistema.'; Pre='Usuario con rol administrador o superadministrador.'; Post='La informacion de acceso queda actualizada.'; Restr='Las acciones criticas pueden reservarse al superadministrador.'; RF='RF 1.2 Gestion de roles y permisos / RF 2.1 Registro de usuarios / RF 2.2 Modificacion de usuarios / RF 2.5 Eliminacion de cuentas de usuario y conductores.'},
  @{Actor='ADMINISTRADOR'; ID='CU-15'; Nombre='Gestionar clientes'; Descripcion='El administrador consulta, crea o actualiza clientes registrados en la plataforma.'; Pre='Administrador autenticado.'; Post='Los datos del cliente quedan registrados o actualizados.'; Restr='No se deben eliminar datos historicos necesarios para portes y facturas.'; RF='RF 3.1 Alta de nuevo cliente / RF 3.2 Edicion perfil empresa / RF 3.3 Consultar Historial de Servicios.'},
  @{Actor='ADMINISTRADOR'; ID='CU-16'; Nombre='Gestionar conductores'; Descripcion='El administrador da de alta, edita, baja o reactiva conductores.'; Pre='Administrador autenticado.'; Post='El estado o datos del conductor quedan actualizados.'; Restr='No se deben romper relaciones historicas con portes ya realizados.'; RF='RF 2.3 Registro de conductores / RF 2.4 Modificacion de conductores / RF 2.5 Eliminacion de cuentas de usuario y conductores.'},
  @{Actor='ADMINISTRADOR'; ID='CU-17'; Nombre='Aprobar o rechazar conductores'; Descripcion='El administrador revisa solicitudes o perfiles de conductor y decide su aprobacion operativa.'; Pre='Debe existir un conductor pendiente de revision.'; Post='El conductor queda aprobado, rechazado o pendiente de correccion.'; Restr='Solo conductores aprobados pueden recibir servicios.'; RF='RF 2.3 Registro de conductores / RF 2.4 Modificacion de conductores.'},
  @{Actor='ADMINISTRADOR'; ID='CU-18'; Nombre='Gestionar vehiculos'; Descripcion='El administrador registra, modifica, da de baja o reactiva vehiculos de la flota.'; Pre='Administrador autenticado.'; Post='La flota disponible queda actualizada.'; Restr='No se puede asignar un vehiculo no operativo a un porte.'; RF='RF 4.1 Alta de Vehiculo / RF 4.2 Asignacion de vehiculo a conductor / RF 4.3 Eliminacion de vehiculo a conductor / RF 4.4 Modificacion estado vehiculo.'},
  @{Actor='ADMINISTRADOR'; ID='CU-19'; Nombre='Crear o revisar portes'; Descripcion='El administrador crea portes internos o revisa solicitudes realizadas por clientes.'; Pre='Solicitud existente o datos completos para crear el porte.'; Post='El porte queda registrado, revisado o preparado para asignacion.'; Restr='Deben validarse carga, fechas, origen, destino y datos economicos.'; RF='RF 5.1 Solicitud nuevo porte / RF 5.2 Estudio de solicitud / RF 5.6 Modificacion de datos o cancelacion de porte.'},
  @{Actor='ADMINISTRADOR'; ID='CU-20'; Nombre='Asignar conductor'; Descripcion='El administrador selecciona un conductor disponible y lo vincula a un porte.'; Pre='Porte pendiente o revisado y conductor disponible.'; Post='El porte pasa a estado asignado y queda vinculado al conductor.'; Restr='Debe respetarse disponibilidad, capacidad y estado operativo.'; RF='RF 5.2 Estudio de solicitud / RF 5.5 Modificacion de estado de porte / RF 4.6 Agenda y disponibilidad del conductor.'},
  @{Actor='ADMINISTRADOR'; ID='CU-21'; Nombre='Monitorizar mapa de flota'; Descripcion='El administrador visualiza la posicion y estado de conductores o portes activos sobre un mapa.'; Pre='Existencia de ubicaciones GPS recientes.'; Post='Se muestra una vision operativa de la flota.'; Restr='Las posiciones obsoletas o invalidas deben tratarse de forma controlada.'; RF='RF 4.7 Mapa de flota / RF 4.5 Reporte de ubicacion / RF 7.2 Muestras de ubicacion.'},
  @{Actor='ADMINISTRADOR'; ID='CU-22'; Nombre='Gestionar incidencias'; Descripcion='El administrador consulta, clasifica, actualiza y resuelve incidencias registradas.'; Pre='Existencia de incidencia abierta o en proceso.'; Post='La incidencia cambia de estado y queda documentada.'; Restr='Debe mantenerse el historial de actuacion.'; RF='RF 6.2 Modificacion de estado / RF 6.3 Historial de incidencias.'},
  @{Actor='ADMINISTRADOR'; ID='CU-23'; Nombre='Generar factura'; Descripcion='El administrador genera la factura de un porte entregado y revisado.'; Pre='Porte entregado y datos economicos validados.'; Post='Se crea una factura asociada al porte y cliente.'; Restr='No se debe facturar un porte no entregado o ya facturado.'; RF='RF 8.1 Factura de porte a cliente / RF 8.3 Albaran de entrega.'},
  @{Actor='ADMINISTRADOR'; ID='CU-24'; Nombre='Consultar estadisticas'; Descripcion='El administrador consulta indicadores de actividad, rendimiento y estado de la operativa.'; Pre='Administrador autenticado y datos registrados.'; Post='Se muestran metricas y graficas de apoyo a la gestion.'; Restr='Las estadisticas deben respetar los permisos del usuario.'; RF='RF 8.2 Estadisticas de facturacion.'}
)

function X([string]$s) { if ($null -eq $s) { return '' }; return [System.Security.SecurityElement]::Escape($s) }
function P([string]$text) { return "<w:p><w:pPr><w:spacing w:after='120'/></w:pPr><w:r><w:rPr><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='24'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function H1([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading1'/><w:spacing w:before='120' w:after='120'/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='32'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function H2([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading2'/><w:spacing w:before='180' w:after='80'/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='28'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function Cell([string]$text, [int]$width, [bool]$header=$false) {
  $shade = if ($header) { "<w:shd w:fill='D9EAF7'/>" } else { '' }
  $bold = if ($header) { '<w:b/>' } else { '' }
  return "<w:tc><w:tcPr><w:tcW w:w='$width' w:type='dxa'/>$shade</w:tcPr><w:p><w:pPr><w:spacing w:after='0'/></w:pPr><w:r><w:rPr>$bold<w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='20'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p></w:tc>"
}
function Table($headers, $widths, $rows) {
  $xml = "<w:tbl><w:tblPr><w:tblStyle w:val='TableGrid'/><w:tblW w:w='9500' w:type='dxa'/><w:tblLayout w:type='fixed'/><w:tblBorders><w:top w:val='single' w:sz='4'/><w:left w:val='single' w:sz='4'/><w:bottom w:val='single' w:sz='4'/><w:right w:val='single' w:sz='4'/><w:insideH w:val='single' w:sz='4'/><w:insideV w:val='single' w:sz='4'/></w:tblBorders></w:tblPr><w:tblGrid>"
  foreach ($w in $widths) { $xml += "<w:gridCol w:w='$w'/>" }
  $xml += '</w:tblGrid><w:tr>'
  for ($i=0; $i -lt $headers.Count; $i++) { $xml += Cell $headers[$i] $widths[$i] $true }
  $xml += '</w:tr>'
  foreach ($r in $rows) { $xml += '<w:tr>'; for ($i=0; $i -lt $headers.Count; $i++) { $xml += Cell $r[$i] $widths[$i] $false }; $xml += '</w:tr>' }
  $xml += '</w:tbl>'
  return $xml
}
function UseCaseTable($c) {
  $rows = @(
    @('ID', $c.ID),
    @('Nombre', $c.Nombre),
    @('Descripcion', $c.Descripcion),
    @('Precondicion', $c.Pre),
    @('Postcondicion', $c.Post),
    @('Restricciones', $c.Restr),
    @('RF Asociado', $c.RF)
  )
  return Table @('Campo','Detalle') @(1900,7600) $rows
}

$body = H1 'Casos de uso actualizados'
$body += P 'Esta version actualiza los casos de uso para reflejar el alcance consolidado de CargoHub: portal cliente, aplicacion movil del conductor y aplicacion de escritorio de administracion. Se excluyen chat, notificaciones, valoraciones y funcionalidades no defendidas en la memoria.'
$body += H2 'TABLA DE ACTORES'
$body += Table @('ACTOR','ACTIVIDAD') @(2400,7100) $actors
foreach ($actor in @('CLIENTE','CONDUCTOR','ADMINISTRADOR')) {
  $body += H2 "ACTOR: $actor"
  foreach ($c in ($cases | Where-Object { $_.Actor -eq $actor })) {
    $body += UseCaseTable $c
    $body += P ''
  }
}
$body += H2 'Nota sobre superadministrador'
$body += P 'El superadministrador se considera una especializacion del administrador. Por tanto, hereda los casos de uso administrativos y se reserva para operaciones con permisos completos, especialmente usuarios, roles y configuracion general.'

$document = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body>$body<w:sectPr><w:pgSz w:w='11906' w:h='16838'/><w:pgMar w:top='850' w:right='850' w:bottom='850' w:left='850'/></w:sectPr></w:body></w:document>"
$styles = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:styles xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:style w:type='paragraph' w:default='1' w:styleId='Normal'><w:name w:val='Normal'/></w:style><w:style w:type='paragraph' w:styleId='Heading1'><w:name w:val='heading 1'/><w:rPr><w:b/><w:sz w:val='32'/></w:rPr></w:style><w:style w:type='paragraph' w:styleId='Heading2'><w:name w:val='heading 2'/><w:rPr><w:b/><w:sz w:val='28'/></w:rPr></w:style><w:style w:type='table' w:styleId='TableGrid'><w:name w:val='Table Grid'/><w:tblPr><w:tblBorders><w:top w:val='single' w:sz='4'/><w:left w:val='single' w:sz='4'/><w:bottom w:val='single' w:sz='4'/><w:right w:val='single' w:sz='4'/><w:insideH w:val='single' w:sz='4'/><w:insideV w:val='single' w:sz='4'/></w:tblBorders></w:tblPr></w:style></w:styles>"
$ct = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Types xmlns='http://schemas.openxmlformats.org/package/2006/content-types'><Default Extension='rels' ContentType='application/vnd.openxmlformats-package.relationships+xml'/><Default Extension='xml' ContentType='application/xml'/><Override PartName='/word/document.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml'/><Override PartName='/word/styles.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml'/></Types>"
$rels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument' Target='word/document.xml'/></Relationships>"
$docRels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles' Target='styles.xml'/></Relationships>"

$work = Join-Path $env:TEMP ('cargohub_use_cases_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work '_rels') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word\_rels') | Out-Null
Set-Content -LiteralPath (Join-Path $work '[Content_Types].xml') -Value $ct -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work '_rels\.rels') -Value $rels -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\document.xml') -Value $document -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\styles.xml') -Value $styles -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\_rels\document.xml.rels') -Value $docRels -Encoding UTF8
if (Test-Path -LiteralPath $docxOutput) { Remove-Item -LiteralPath $docxOutput -Force }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $docxOutput)
Remove-Item -LiteralPath $work -Recurse -Force

$md = "# Casos de uso actualizados`n`n## Diagrama 1: Cliente y conductor`n`n``````plantuml`n$plantUmlClienteConductor`n```````n`n## Diagrama 2: Administracion`n`n``````plantuml`n$plantUmlAdministracion`n```````n`n## Archivos generados`n`n- Diagrama PlantUML cliente/conductor: ``TFG_CargoHub_casos_uso_cliente_conductor.puml```n- Diagrama PlantUML administracion: ``TFG_CargoHub_casos_uso_administracion.puml```n- Tablas Word: ``TFG_CargoHub_tablas_casos_uso_actualizadas.docx```n"
Set-Content -LiteralPath $mdOutput -Value $md -Encoding UTF8

"PUML generado: $pumlOutputClienteConductor"
"PUML generado: $pumlOutputAdministracion"
"DOCX generado: $docxOutput"
"MD generado: $mdOutput"
