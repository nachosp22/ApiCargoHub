$ErrorActionPreference = 'Stop'

$output = Join-Path $PSScriptRoot 'TFG_CargoHub_cambios_requisitos_RF_RNF.docx'
$work = Join-Path $env:TEMP ('cargohub_req_changes_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work '_rels') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word\_rels') | Out-Null

function X([string]$s) {
  if ($null -eq $s) { return '' }
  return [System.Security.SecurityElement]::Escape($s)
}

function P([string]$text, [string]$style = '') {
  $st = if ($style) { "<w:pStyle w:val=`"$style`"/>" } else { '' }
  return "<w:p><w:pPr>$st<w:spacing w:line=`"360`" w:lineRule=`"auto`"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>"
}

function H1([string]$text) { return "<w:p><w:pPr><w:pStyle w:val=`"Heading1`"/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`"/><w:sz w:val=`"32`"/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function H2([string]$text) { return "<w:p><w:pPr><w:pStyle w:val=`"Heading2`"/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`"/><w:sz w:val=`"28`"/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function Bullet([string]$text) { return P ("- " + $text) }

$body = ''
$body += H1 'Cambios propuestos en requisitos del TFG CargoHub'
$body += P 'Este documento NO sustituye al punto 4 original. Sirve como guia para editar manualmente los requisitos funcionales y no funcionales manteniendo tu redaccion y formato. La idea es quitar solo lo que no se defiende, conservar lo que esta bien y anadir lo que falta.'

$body += H1 '1. Requisitos funcionales'
$body += H2 '1.1. Requisitos funcionales que conviene quitar'
$body += Bullet 'RF 7 completo: Sistema de mensajeria. El chat interno entre conductores y administradores no forma parte del alcance funcional que se va a defender.'
$body += Bullet 'RF 7.1 Intercambio de mensajes. Quitar.'
$body += Bullet 'RF 7.2 Historial de chat. Quitar.'
$body += Bullet 'RF 8.3 Gestion de nominas. Quitar. La gestion de nominas o liquidaciones a conductores no forma parte del alcance actual.'
$body += Bullet 'RF 9 completo: Sistema de notificaciones. Quitar como requisito funcional principal.'
$body += Bullet 'RF 9.1 Notificacion a conductor. Quitar o mover a mejora futura, porque no se va a defender como funcionalidad completa.'
$body += Bullet 'Cualquier requisito que hable de valoraciones, ratings, frio/refrigerado, material peligroso o trampilla elevadora. Esas partes se retiraron del alcance.'

$body += H2 '1.2. Requisitos funcionales que conviene modificar sin borrar tu idea'
$body += Bullet 'RF 1.2 Gestion de roles y permisos: mantener, pero anadir CLIENTE como rol con acceso al portal web, sus portes, seguimiento y facturas.'
$body += Bullet 'RF 2.3 Registro de conductores: mantener, pero sustituir el campo estado por datos reales como ciudad base, radio de accion, disponibilidad y ubicacion actual si procede.'
$body += Bullet 'RF 4.2 Asignacion de vehiculo a conductor: mantener, pero no limitarlo a un conductor sin vehiculo; el sistema contempla vehiculos asociados al conductor.'
$body += Bullet 'RF 4.5 Reporte de ubicacion: mantener, pero redactarlo como ubicacion reportada por conductor/app movil durante portes activos, no como si el vehiculo reportara solo.'
$body += Bullet 'RF 5.2 Estudio de solicitud: mantener la idea, pero quitar promesas de negociacion formal cliente-admin. Redactarlo como analisis de solicitud, calculo aproximado y revision manual cuando corresponda.'
$body += Bullet 'RF 5.3 Confirmacion de solicitud: mantener solo si lo enfocas como revision/validacion administrativa. No prometer un flujo de negociacion completo.'
$body += Bullet 'RF 5.5 Modificacion de estado de porte: mantener y anadir estados reales: PENDIENTE, ASIGNADO, EN_RECOGIDA, EN_TRANSITO, ENTREGADO, CANCELADO y FACTURADO.'
$body += Bullet 'RF 5.8 Prueba de entrega: mantener, pero concretar que se realiza mediante firma de entrega y generacion de albaran.'
$body += Bullet 'RF 8.1 Factura de porte a cliente: cambiar automatico por generacion desde administracion una vez el porte esta entregado.'
$body += Bullet 'RF 8.2 Estadisticas de facturacion: mantener si se defiende como estadisticas basicas de administracion, no como BI avanzado.'

$body += H2 '1.3. Requisitos funcionales que faltan y conviene anadir'
$body += P 'Puedes anadirlos respetando tu numeracion. Si eliminas RF 7 y RF 9, puedes reutilizar esos bloques para seguimiento GPS y fuera de alcance.'
$body += Bullet 'RF 3.4 Portal de cliente: El cliente debe poder acceder al portal web para solicitar portes, consultar sus servicios, revisar el seguimiento cuando este disponible y consultar sus facturas.'
$body += Bullet 'RF 4.6 Agenda y disponibilidad del conductor: El sistema debe permitir registrar bloqueos puntuales o recurrentes para indicar cuando un conductor no esta disponible.'
$body += Bullet 'RF 4.7 Mapa de flota: La aplicacion de escritorio debe permitir visualizar en un mapa la situacion de conductores y portes activos.'
$body += Bullet 'RF 5.9 Aceptacion o rechazo de ofertas: El conductor debe poder aceptar o rechazar una oferta o asignacion de porte cuando el flujo operativo lo requiera.'
$body += Bullet 'RF 5.10 Fotos de carga: El sistema debe permitir adjuntar fotografias relacionadas con la carga o con el proceso de entrega para dejar evidencia del servicio.'
$body += Bullet 'RF 6.4 Historial de eventos de incidencia: El sistema debe conservar un historial de eventos para saber que cambios se han realizado sobre una incidencia y que usuario los ha realizado.'
$body += Bullet 'RF 7 Seguimiento GPS: nuevo bloque para sustituir mensajeria.'
$body += Bullet 'RF 7.1 Sesion de seguimiento: El sistema debe registrar sesiones de seguimiento asociadas a un conductor y, cuando corresponda, a un porte concreto.'
$body += Bullet 'RF 7.2 Muestras de ubicacion: La aplicacion movil debe enviar muestras de ubicacion con latitud, longitud, fecha, velocidad y rumbo durante servicios activos.'
$body += Bullet 'RF 7.3 Pausas de seguimiento: El sistema debe poder registrar pausas dentro de una sesion de seguimiento cuando el conductor interrumpa temporalmente el tracking.'
$body += Bullet 'RF 8.4 Albaran de entrega: El sistema debe generar un albaran con los datos del porte entregado, la fecha de entrega y la firma registrada.'
$body += Bullet 'RF 9 Elementos fuera de alcance funcional: nuevo bloque para dejar claro que no se defienden chat, notificaciones push completas, nominas, valoraciones, frio, material peligroso ni trampilla elevadora.'

$body += H1 '2. Requisitos no funcionales'
$body += H2 '2.1. Requisitos no funcionales que conviene quitar o suavizar'
$body += Bullet 'RNF 2.1 Tiempo de respuesta de la API: quitar la promesa concreta de menos de 200ms si no tienes mediciones. Mantenerlo como respuesta adecuada para operaciones habituales.'
$body += Bullet 'RNF 2.4 Alta demanda: suavizar. No prometer alta demanda si no hay pruebas de carga. Puedes decir que el sistema debe poder escalar en despliegues futuros.'
$body += Bullet 'RNF 4.1 Disponibilidad de servicio: quitar el 99,9% anual si no se puede demostrar en despliegue real. Mantener como objetivo de disponibilidad durante la operativa normal.'
$body += Bullet 'RNF 4.2 Tolerancia a fallos de red: quitar o suavizar si no existe sincronizacion offline real con base de datos secundaria movil. Puedes dejarlo como mejora futura o como gestion razonable de errores de conexion.'
$body += Bullet 'RNF 5.1 Escalabilidad automatica en Azure: suavizar si no esta desplegado con autoescalado real. Mantener Docker/despliegue como base de mantenibilidad y escalabilidad futura.'
$body += Bullet 'RNF 6.1 Compatibilidad movil 99,7%: quitar porcentaje exacto si no esta justificado. Mantener compatibilidad con versiones Android previstas.'

$body += H2 '2.2. Requisitos no funcionales que conviene mantener'
$body += Bullet 'RNF 1.1 Interfaz movil adaptada al conductor: mantener. Tiene sentido para la app Android.'
$body += Bullet 'RNF 1.2 Aplicacion de escritorio intuitiva y eficiente: mantener, corrigiendo la redaccion si hace falta.'
$body += Bullet 'RNF 2.2 App movil optimizada: mantener, pero sin prometer cifras concretas de bateria si no estan medidas.'
$body += Bullet 'RNF 2.3 Sincronizacion o registro eficiente de GPS: mantener, porque el tracking forma parte del sistema.'
$body += Bullet 'RNF 3.1 Proteccion de datos: mantener.'
$body += Bullet 'RNF 5.2 Modularidad del codigo: mantener. Encaja con API, desktop, mobile y web separados.'
$body += Bullet 'RNF 6.2 Compatibilidad de escritorio: mantener, redactado para Windows actuales.'

$body += H2 '2.3. Requisitos no funcionales que faltan y conviene anadir'
$body += Bullet 'RNF nuevo - Portal web sencillo: El portal del cliente debe ser claro y permitir solicitar portes, consultar seguimiento y revisar facturas sin pasos innecesarios.'
$body += Bullet 'RNF nuevo - Integridad documental: Albaranes y facturas deben generarse a partir de datos coherentes del porte para evitar contradicciones entre entrega y facturacion.'
$body += Bullet 'RNF nuevo - Control de concurrencia: El sistema debe evitar modificaciones incompatibles sobre un mismo porte mediante mecanismos de control de version cuando corresponda.'
$body += Bullet 'RNF nuevo - Separacion por aplicaciones: El sistema debe mantener separadas la API, la base de datos, la aplicacion de escritorio, la aplicacion movil y el portal web.'
$body += Bullet 'RNF nuevo - Trazabilidad operativa: Los cambios relevantes en portes, incidencias y tracking deben quedar reflejados de forma que se pueda reconstruir el estado del servicio.'

$body += H1 '3. Recomendacion de aplicacion'
$body += Bullet 'No rehacer todo el punto 4 desde cero.'
$body += Bullet 'Mantener tu tabla original como base.'
$body += Bullet 'Eliminar solo los bloques claramente fuera de alcance.'
$body += Bullet 'Anadir los requisitos faltantes al final de cada bloque relacionado.'
$body += Bullet 'Si una frase tuya ya expresa bien la idea, no tocarla.'

$document = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    $body
    <w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr>
  </w:body>
</w:document>
"@

$styles = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="24"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:rPr><w:b/><w:sz w:val="32"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:rPr><w:b/><w:sz w:val="28"/></w:rPr></w:style>
</w:styles>
'@

$contentTypes = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>
'@

$rels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
'@

$docRels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
'@

Set-Content -LiteralPath (Join-Path $work '[Content_Types].xml') -Value $contentTypes -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work '_rels\.rels') -Value $rels -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\document.xml') -Value $document -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\styles.xml') -Value $styles -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\_rels\document.xml.rels') -Value $docRels -Encoding UTF8

if (Test-Path -LiteralPath $output) { Remove-Item -LiteralPath $output -Force }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $output)
Remove-Item -LiteralPath $work -Recurse -Force

"DOCX generado: $output"
