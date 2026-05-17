$ErrorActionPreference = 'Stop'

$template = Join-Path $PSScriptRoot 'Documentacion_TFG_copia.docx'
$output = Join-Path $PSScriptRoot 'TFG_CargoHub_requisitos_funcionales_actualizados.docx'
$work = Join-Path $env:TEMP ('cargohub_rf_docx_' + [guid]::NewGuid().ToString('N'))

Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path -LiteralPath $template) {
  [System.IO.Compression.ZipFile]::ExtractToDirectory($template, $work)
} else {
  New-Item -ItemType Directory -Path $work | Out-Null
  New-Item -ItemType Directory -Path (Join-Path $work '_rels') | Out-Null
  New-Item -ItemType Directory -Path (Join-Path $work 'word') | Out-Null
  New-Item -ItemType Directory -Path (Join-Path $work 'word\_rels') | Out-Null
}

function X([string]$s) {
  if ($null -eq $s) { return '' }
  return [System.Security.SecurityElement]::Escape($s)
}

function P([string]$text, [string]$style = '') {
  $st = if ($style) { "<w:pPr><w:pStyle w:val=`"$style`"/><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/></w:rPr></w:pPr>" } else { '<w:pPr><w:spacing w:line="360" w:lineRule="auto"/><w:jc w:val="both"/><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr></w:pPr>' }
  return "<w:p>$st<w:r><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>"
}

function Cell([string]$text, [bool]$bold = $false, [string]$width = '6186') {
  $b = if ($bold) { '<w:b/>' } else { '' }
  return "<w:tc><w:tcPr><w:tcW w:w=`"$width`" w:type=`"dxa`"/></w:tcPr><w:p><w:pPr><w:spacing w:line=`"360`" w:lineRule=`"auto`"/><w:jc w:val=`"both`"/><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/></w:rPr></w:pPr><w:r><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/>$b</w:rPr><w:t>$(X $text)</w:t></w:r></w:p></w:tc>"
}

function Row([string]$code, [string]$text, [bool]$bold = $false) {
  return '<w:tr>' + (Cell $code $bold '1900') + (Cell $text $bold '7116') + '</w:tr>'
}

function Req($code, $title, $desc) {
  return (Row $code $title $false) + (Row '' $desc $false)
}

$rows = ''
$rows += Row 'CODIGO' 'REQUISITO FUNCIONAL' $true
$rows += Row 'RF 1.' 'Seguridad y control de acceso' $true
$rows += Req 'RF 1.1.' 'Autenticacion de usuarios' 'El sistema debe permitir el inicio de sesion mediante email y contrasena, validando las credenciales contra la base de datos y generando un token de acceso para consumir la API desde las distintas aplicaciones.'
$rows += Req 'RF 1.2.' 'Gestion de roles y permisos' 'El sistema debe restringir las funcionalidades segun el rol del usuario autenticado. Los roles principales son SuperAdmin, Admin, Conductor y Cliente. Cada perfil solo debe acceder a las operaciones correspondientes a su responsabilidad.'
$rows += Req 'RF 1.3.' 'Proteccion de credenciales' 'El sistema debe almacenar las contrasenas de forma cifrada mediante un algoritmo seguro y nunca debe exponerlas en las respuestas de la API.'
$rows += Req 'RF 1.4.' 'Recuperacion y actualizacion de cuenta' 'El sistema debe permitir gestionar datos basicos de cuenta y procesos de recuperacion cuando corresponda, manteniendo siempre el control de permisos.'

$rows += Row 'RF 2.' 'Gestion de usuarios, clientes y conductores' $true
$rows += Req 'RF 2.1.' 'Gestion de usuarios' 'Los administradores autorizados deben poder crear, consultar, editar, activar o desactivar usuarios del sistema, respetando el rol asignado a cada cuenta.'
$rows += Req 'RF 2.2.' 'Gestion de clientes' 'El sistema debe permitir registrar y modificar clientes con sus datos principales: nombre de empresa, CIF, direccion fiscal, telefono, email de contacto y sector.'
$rows += Req 'RF 2.3.' 'Portal de cliente' 'El cliente debe poder acceder al portal web para consultar sus portes, solicitar nuevos servicios, revisar el estado de sus solicitudes y acceder a la documentacion asociada cuando proceda.'
$rows += Req 'RF 2.4.' 'Gestion de conductores' 'El sistema debe permitir registrar y modificar conductores, asociandolos a una cuenta de usuario y almacenando datos como nombre, apellidos, DNI, telefono, ciudad base, radio de accion y disponibilidad.'
$rows += Req 'RF 2.5.' 'Consulta del perfil propio' 'El conductor y el cliente deben poder consultar la informacion principal de su perfil desde sus respectivas interfaces, sin acceder a informacion que no les pertenece.'

$rows += Row 'RF 3.' 'Gestion de flota y disponibilidad' $true
$rows += Req 'RF 3.1.' 'Alta y edicion de vehiculos' 'El sistema debe permitir a administradores registrar y editar vehiculos con matricula, marca, modelo, tipo, estado, capacidad de carga, dimensiones utiles y volumen aproximado.'
$rows += Req 'RF 3.2.' 'Asignacion de vehiculo a conductor' 'El sistema debe permitir asociar uno o varios vehiculos a un conductor para reflejar los recursos disponibles de la flota.'
$rows += Req 'RF 3.3.' 'Estado del vehiculo' 'El sistema debe permitir modificar el estado de un vehiculo para indicar si esta disponible, en mantenimiento o dado de baja.'
$rows += Req 'RF 3.4.' 'Agenda y bloqueos del conductor' 'El sistema debe permitir registrar bloqueos puntuales o recurrentes en la agenda del conductor para tener en cuenta su disponibilidad real.'
$rows += Req 'RF 3.5.' 'Mapa de flota' 'El sistema debe permitir a administracion visualizar en un mapa la ubicacion y situacion operativa de los conductores durante servicios activos.'

$rows += Row 'RF 4.' 'Gestion de portes' $true
$rows += Req 'RF 4.1.' 'Solicitud de porte' 'El cliente debe poder solicitar un porte indicando origen, destino, descripcion de la carga, dimensiones, peso aproximado y datos necesarios para valorar el servicio.'
$rows += Req 'RF 4.2.' 'Analisis de la solicitud' 'El sistema debe analizar la solicitud de porte para calcular informacion basica como distancia, precio orientativo y posibles necesidades de revision manual.'
$rows += Req 'RF 4.3.' 'Revision manual de portes' 'Cuando una solicitud no pueda resolverse automaticamente o requiera comprobacion, debe quedar marcada para revision manual por parte de administracion.'
$rows += Req 'RF 4.4.' 'Asignacion de conductor' 'Administracion debe poder asignar un conductor a un porte pendiente, teniendo en cuenta disponibilidad, vehiculo y estado del servicio.'
$rows += Req 'RF 4.5.' 'Ofertas de porte al conductor' 'El conductor debe poder consultar ofertas o portes disponibles para el, y aceptar o rechazar la asignacion cuando el flujo lo requiera.'
$rows += Req 'RF 4.6.' 'Consulta de portes' 'Administracion debe poder consultar todos los portes. El cliente debe consultar sus propios portes y el conductor sus portes asignados, activos o historicos.'
$rows += Req 'RF 4.7.' 'Cambio de estado del porte' 'El sistema debe controlar la evolucion del porte mediante estados: pendiente, asignado, en recogida, en transito, entregado, facturado o cancelado.'
$rows += Req 'RF 4.8.' 'Cancelacion de porte' 'Administracion debe poder cancelar un porte cuando exista una causa operativa, error en la solicitud o peticion justificada del cliente.'
$rows += Req 'RF 4.9.' 'Evidencias de carga' 'El sistema debe permitir adjuntar fotografias relacionadas con la carga o el servicio para dejar constancia documental cuando sea necesario.'

$rows += Row 'RF 5.' 'Flujo movil del conductor' $true
$rows += Req 'RF 5.1.' 'Inicio del viaje' 'El conductor debe poder iniciar el viaje desde la aplicacion movil, pasando el porte al estado correspondiente de recogida.'
$rows += Req 'RF 5.2.' 'Confirmacion de recogida' 'El conductor debe poder confirmar la recogida de la mercancia, cambiando el porte al estado en transito.'
$rows += Req 'RF 5.3.' 'Finalizacion de entrega' 'El conductor debe poder finalizar el porte cuando la mercancia se haya entregado correctamente.'
$rows += Req 'RF 5.4.' 'Firma de entrega' 'El sistema debe permitir registrar una firma de entrega y la persona firmante como prueba documental de recepcion.'
$rows += Req 'RF 5.5.' 'Consulta de historial' 'El conductor debe poder consultar los portes realizados y la informacion basica asociada a cada servicio.'

$rows += Row 'RF 6.' 'Seguimiento GPS y ubicaciones' $true
$rows += Req 'RF 6.1.' 'Registro de sesiones de seguimiento' 'El sistema debe crear sesiones de seguimiento asociadas a un conductor y, cuando corresponda, a un porte concreto.'
$rows += Req 'RF 6.2.' 'Registro de ubicaciones' 'La aplicacion movil debe poder enviar muestras de ubicacion con latitud, longitud, fecha, velocidad y rumbo para permitir el seguimiento del servicio.'
$rows += Req 'RF 6.3.' 'Pausas de seguimiento' 'El sistema debe permitir registrar pausas dentro de una sesion de tracking cuando el conductor detenga temporalmente el seguimiento.'
$rows += Req 'RF 6.4.' 'Consulta de tracking por cliente' 'El cliente debe poder consultar el seguimiento del porte cuando el servicio este en una fase que permita mostrar informacion de ubicacion.'

$rows += Row 'RF 7.' 'Gestion de incidencias' $true
$rows += Req 'RF 7.1.' 'Alta de incidencia' 'El sistema debe permitir registrar incidencias asociadas a un porte, indicando titulo, descripcion, severidad, prioridad y estado.'
$rows += Req 'RF 7.2.' 'Gestion administrativa de incidencias' 'Administracion debe poder consultar, revisar, cambiar el estado y resolver incidencias.'
$rows += Req 'RF 7.3.' 'Historial de cambios de incidencia' 'El sistema debe conservar eventos de historial para conocer los cambios realizados sobre una incidencia y el usuario que los realizo.'
$rows += Req 'RF 7.4.' 'Consulta de incidencias por perfil' 'El conductor debe poder consultar las incidencias relacionadas con sus portes y administracion debe poder consultar todas las incidencias del sistema.'

$rows += Row 'RF 8.' 'Albaranes y facturacion' $true
$rows += Req 'RF 8.1.' 'Generacion de albaran' 'El sistema debe generar un albaran de entrega a partir de los datos del porte entregado, incluyendo datos de origen, destino, fecha y firma.'
$rows += Req 'RF 8.2.' 'Generacion de factura' 'Administracion debe poder generar una factura asociada a un porte entregado, calculando base imponible, IVA e importe total.'
$rows += Req 'RF 8.3.' 'Consulta de facturas' 'Administracion debe poder consultar todas las facturas y el cliente debe poder consultar las facturas asociadas a sus portes.'
$rows += Req 'RF 8.4.' 'Estado de pago de factura' 'El sistema debe permitir registrar si una factura esta pagada y, en su caso, la fecha y forma de pago.'
$rows += Req 'RF 8.5.' 'Estadisticas administrativas' 'El sistema debe mostrar a administracion estadisticas basicas de operativa y facturacion, como portes realizados, importes y actividad de la flota.'

$rows += Row 'RF 9.' 'Funciones fuera de alcance funcional actual' $true
$rows += Req 'RF 9.1.' 'Exclusion de mensajeria interna' 'El sistema no contempla como funcionalidad principal un chat entre conductores y administradores. Las incidencias y estados del porte cubren la comunicacion operativa necesaria.'
$rows += Req 'RF 9.2.' 'Exclusion de notificaciones push completas' 'El sistema no se defendera como un sistema completo de notificaciones push. Los cambios de estado se consultan desde las interfaces disponibles.'
$rows += Req 'RF 9.3.' 'Exclusion de valoraciones y requisitos especiales eliminados' 'No forman parte del alcance actual las valoraciones, requisitos de frio, material peligroso o trampilla elevadora, ya que se retiraron para simplificar el flujo funcional.'

$table = '<w:tbl><w:tblPr><w:tblStyle w:val="Tablaconcuadrcula4-nfasis1"/><w:tblW w:w="0" w:type="auto"/><w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="1" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr><w:tblGrid><w:gridCol w:w="1900"/><w:gridCol w:w="7116"/></w:tblGrid>' + $rows + '</w:tbl>'

$body = ''
$body += P '4. Especificacion de requisitos' 'Heading1'
$body += P '4.1. Requisitos funcionales' 'Heading2'
$body += P 'La siguiente tabla recoge los requisitos funcionales actualizados de CargoHub, adaptados al alcance real del sistema implementado y eliminando funcionalidades que no forman parte de la entrega actual.'
$body += $table

$document = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    $body
    <w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr>
  </w:body>
</w:document>
"@

Set-Content -LiteralPath (Join-Path $work 'word\document.xml') -Value $document -Encoding UTF8
if (Test-Path -LiteralPath $output) { Remove-Item -LiteralPath $output -Force }
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $output)
Remove-Item -LiteralPath $work -Recurse -Force
"DOCX generado: $output"
