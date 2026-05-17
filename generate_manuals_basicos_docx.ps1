$ErrorActionPreference = 'Stop'

$output = Join-Path $PSScriptRoot 'TFG_CargoHub_manuales_basicos.docx'
$work = Join-Path $env:TEMP ('cargohub_manuals_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work '_rels') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word\_rels') | Out-Null

function X([string]$s) { if ($null -eq $s) { return '' }; return [System.Security.SecurityElement]::Escape($s) }
function RunText([string]$text, [bool]$bold=$false, [int]$size=24) {
  $b = if ($bold) { '<w:b/>' } else { '' }
  return "<w:r><w:rPr>$b<w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='$size'/></w:rPr><w:t>$(X $text)</w:t></w:r>"
}
function P([string]$text) { return "<w:p><w:pPr><w:spacing w:after='120'/></w:pPr>$(RunText $text)</w:p>" }
function H1([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading1'/><w:spacing w:before='160' w:after='120'/></w:pPr>$(RunText $text $true 32)</w:p>" }
function H2([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading2'/><w:spacing w:before='140' w:after='90'/></w:pPr>$(RunText $text $true 28)</w:p>" }
function H3([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading3'/><w:spacing w:before='100' w:after='70'/></w:pPr>$(RunText $text $true 24)</w:p>" }
function Bullet([string]$text) { return "<w:p><w:pPr><w:spacing w:after='80'/><w:ind w:left='720' w:hanging='360'/></w:pPr>$(RunText '• ')$(RunText $text)</w:p>" }
function Step([int]$n, [string]$text) { return "<w:p><w:pPr><w:spacing w:after='80'/><w:ind w:left='720' w:hanging='360'/></w:pPr>$(RunText ($n.ToString() + '. ') $true)$(RunText $text)</w:p>" }
function Capture([string]$text) {
  return "<w:p><w:pPr><w:spacing w:before='80' w:after='160'/><w:jc w:val='center'/></w:pPr><w:r><w:rPr><w:i/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='22'/><w:color w:val='666666'/></w:rPr><w:t>[Captura: $(X $text)]</w:t></w:r></w:p>"
}
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

$body = H1 'Manuales de CargoHub'
$body += P 'En este apartado se describen de forma basica los manuales de instalacion y uso de CargoHub. La explicacion se divide segun las tres interfaces principales del sistema: aplicacion movil para conductores, portal web para clientes y aplicacion de escritorio para administracion.'
$body += P 'Las indicaciones de captura sirven como guia para insertar imagenes reales de la aplicacion dentro del documento final.'

$body += H1 'Manual de instalacion y despliegue'
$body += H2 'Aplicacion movil del conductor'
$body += P 'La aplicacion movil esta pensada para ser instalada en dispositivos Android mediante un archivo APK generado durante el proceso de compilacion.'
$body += Step 1 'Copiar el archivo APK al dispositivo movil o descargarlo desde el enlace interno facilitado por administracion.'
$body += Step 2 'Permitir la instalacion de aplicaciones de origen desconocido si Android lo solicita.'
$body += Step 3 'Abrir el archivo APK e iniciar la instalacion.'
$body += Step 4 'Una vez finalizada la instalacion, abrir la aplicacion CargoHub desde el icono creado en el dispositivo.'
$body += Step 5 'Iniciar sesion con las credenciales del conductor.'
$body += Capture 'archivo APK antes de instalarlo o pantalla de instalacion de Android'
$body += Capture 'icono de CargoHub instalado en el movil'
$body += Capture 'pantalla de login de la aplicacion movil'

$body += H2 'Portal web del cliente'
$body += P 'El portal web no requiere instalacion en el equipo del usuario. Al tratarse de una aplicacion web, el cliente accede mediante navegador introduciendo la URL del sistema.'
$body += Step 1 'Abrir un navegador web actualizado.'
$body += Step 2 'Introducir la direccion del portal de CargoHub.'
$body += Step 3 'Acceder con el usuario y contrasena del cliente.'
$body += Step 4 'Utilizar las opciones disponibles: solicitar portes, consultar portes, revisar seguimiento y ver facturas.'
$body += Capture 'pagina inicial o landing del portal web'
$body += Capture 'pantalla de login o registro del portal cliente'
$body += Capture 'panel principal del cliente tras iniciar sesion'

$body += H2 'Aplicacion de escritorio de administracion'
$body += P 'La aplicacion de escritorio se distribuye mediante un instalador MSI. Este instalador comprueba que el equipo tenga instaladas las tecnologias necesarias para ejecutar la aplicacion y, tras la instalacion, genera el acceso al ejecutable .exe de CargoHub.'
$body += Step 1 'Ejecutar el archivo instalador MSI en el equipo de administracion.'
$body += Step 2 'El instalador comprueba las dependencias necesarias para el funcionamiento de la aplicacion.'
$body += Step 3 'Si falta alguna tecnologia requerida, el instalador informa al usuario o inicia el proceso de instalacion correspondiente.'
$body += Step 4 'Completar el asistente de instalacion indicando la ruta de destino.'
$body += Step 5 'Al finalizar, se genera el ejecutable .exe y el acceso directo de la aplicacion.'
$body += Step 6 'Abrir CargoHub desde el acceso directo o desde el ejecutable generado.'
$body += Capture 'archivo MSI antes de ejecutarlo'
$body += Capture 'asistente de instalacion comprobando requisitos'
$body += Capture 'pantalla final del instalador'
$body += Capture 'icono o ejecutable .exe generado'
$body += Capture 'login de la aplicacion de escritorio'

$body += H2 'Resumen de instalacion por aplicacion'
$body += Table @('Aplicacion','Tipo de acceso','Instalacion requerida','Capturas recomendadas') @(2100,2200,2500,2700) @(
  @('Movil conductor','APK Android','Si. Instalacion manual del APK en el dispositivo.','APK, instalacion Android, login movil, pantalla inicio.'),
  @('Portal cliente','Navegador web','No. Se accede desde una URL.','Landing, login/registro, panel cliente.'),
  @('Escritorio administracion','Instalador MSI y ejecutable .exe','Si. MSI que comprueba tecnologias y genera el .exe.','MSI, asistente, comprobacion, exe, login escritorio.')
)

$body += H1 'Manual de uso'
$body += H2 'Uso del portal web por parte del cliente'
$body += H3 'Inicio de sesion'
$body += Step 1 'El cliente accede al portal web desde el navegador.'
$body += Step 2 'Introduce email y contrasena.'
$body += Step 3 'El sistema valida los datos y muestra el panel principal.'
$body += Capture 'login del portal cliente'
$body += Capture 'panel principal del cliente'
$body += H3 'Solicitar un porte'
$body += Step 1 'Desde el panel principal, seleccionar la opcion Solicitar porte.'
$body += Step 2 'Rellenar origen, destino, fechas y datos de la carga.'
$body += Step 3 'Enviar la solicitud para que administracion la revise.'
$body += Capture 'boton u opcion Solicitar porte'
$body += Capture 'formulario de solicitud de porte completo'
$body += Capture 'mensaje de confirmacion de solicitud enviada'
$body += H3 'Consultar portes y seguimiento'
$body += Step 1 'Entrar en la seccion Mis portes.'
$body += Step 2 'Seleccionar un porte del listado.'
$body += Step 3 'Consultar el estado actual y la informacion de seguimiento disponible.'
$body += Capture 'listado Mis portes'
$body += Capture 'detalle de un porte'
$body += Capture 'seguimiento o estado del porte'
$body += H3 'Consultar facturas'
$body += Step 1 'Entrar en la seccion Mis facturas.'
$body += Step 2 'Revisar las facturas asociadas a los portes realizados.'
$body += Step 3 'Abrir o descargar la factura si la opcion esta disponible.'
$body += Capture 'listado de facturas del cliente'
$body += Capture 'detalle o descarga de factura'

$body += H2 'Uso de la aplicacion movil por parte del conductor'
$body += H3 'Inicio de sesion'
$body += Step 1 'Abrir la aplicacion CargoHub instalada en el movil.'
$body += Step 2 'Introducir email y contrasena del conductor.'
$body += Step 3 'Acceder a la pantalla principal de la aplicacion.'
$body += Capture 'login de la app movil'
$body += Capture 'pantalla de inicio del conductor'
$body += H3 'Consultar ofertas o portes asignados'
$body += Step 1 'Entrar en la seccion Ofertas o Mis portes.'
$body += Step 2 'Revisar los datos principales del servicio: origen, destino, fechas y carga.'
$body += Step 3 'Aceptar o rechazar la oferta cuando el sistema lo permita.'
$body += Capture 'listado de ofertas o portes asignados'
$body += Capture 'detalle de una oferta de porte'
$body += Capture 'botones aceptar o rechazar'
$body += H3 'Actualizar el estado del porte'
$body += Step 1 'Abrir el porte activo.'
$body += Step 2 'Marcar el avance del servicio segun corresponda: iniciar viaje, confirmar recogida, en transito o entrega.'
$body += Step 3 'El sistema guarda el nuevo estado y actualiza la informacion disponible para administracion y cliente.'
$body += Capture 'detalle del porte activo'
$body += Capture 'botones de cambio de estado'
$body += Capture 'estado actualizado del porte'
$body += H3 'Registrar entrega, fotos o incidencia'
$body += Step 1 'Al finalizar el servicio, registrar la entrega con la informacion requerida.'
$body += Step 2 'Adjuntar fotografias si forman parte de la evidencia de carga o entrega.'
$body += Step 3 'Si ocurre un problema, entrar en Incidencias y crear una nueva incidencia asociada al porte.'
$body += Capture 'pantalla de firma o confirmacion de entrega'
$body += Capture 'adjuntar foto de carga o entrega'
$body += Capture 'formulario de nueva incidencia'

$body += H2 'Uso de la aplicacion de escritorio por parte de administracion'
$body += H3 'Inicio de sesion y panel principal'
$body += Step 1 'Abrir la aplicacion desde el ejecutable .exe o acceso directo.'
$body += Step 2 'Introducir las credenciales de administrador.'
$body += Step 3 'Acceder al dashboard con la informacion general de la operativa.'
$body += Capture 'login de escritorio'
$body += Capture 'dashboard principal de administracion'
$body += H3 'Gestionar solicitudes y portes'
$body += Step 1 'Entrar en la seccion Portes o Revision de portes.'
$body += Step 2 'Seleccionar la solicitud o porte que se desea revisar.'
$body += Step 3 'Comprobar datos de carga, fechas, origen, destino y cliente.'
$body += Step 4 'Asignar conductor o modificar los datos si es necesario.'
$body += Capture 'menu lateral con opcion Portes'
$body += Capture 'listado de portes o solicitudes'
$body += Capture 'detalle de porte en administracion'
$body += Capture 'pantalla de asignacion de conductor'
$body += H3 'Gestionar conductores, vehiculos y clientes'
$body += Step 1 'Entrar en la seccion correspondiente: Conductores, Vehiculos o Clientes.'
$body += Step 2 'Consultar el listado de registros existentes.'
$body += Step 3 'Crear, editar, dar de baja o reactivar registros segun los permisos del usuario.'
$body += Capture 'listado de conductores'
$body += Capture 'formulario de alta o edicion de conductor'
$body += Capture 'listado de vehiculos'
$body += Capture 'formulario de alta o edicion de vehiculo'
$body += Capture 'listado de clientes'
$body += H3 'Controlar flota, incidencias y facturacion'
$body += Step 1 'Desde Mapa de flota, revisar la situacion de conductores y portes activos.'
$body += Step 2 'Desde Incidencias, consultar los problemas reportados y actualizar su estado.'
$body += Step 3 'Desde Facturas, generar o consultar facturas asociadas a portes entregados.'
$body += Capture 'mapa de flota con conductores o portes activos'
$body += Capture 'listado de incidencias'
$body += Capture 'detalle de incidencia y cambio de estado'
$body += Capture 'listado o detalle de facturas'

$body += H1 'Listado rapido de capturas recomendadas'
$body += Table @('Apartado','Captura que conviene insertar') @(2700,6800) @(
  @('Instalacion movil','APK, instalacion Android, icono instalado, login movil.'),
  @('Instalacion web','Landing, URL en navegador, login/registro, panel cliente.'),
  @('Instalacion escritorio','MSI, comprobacion de requisitos, ruta de instalacion, exe o acceso directo, login escritorio.'),
  @('Cliente','Panel cliente, solicitar porte, mis portes, seguimiento, facturas.'),
  @('Conductor','Inicio movil, ofertas, detalle de porte, cambio de estado, firma/albaran, incidencia.'),
  @('Administrador','Dashboard, portes, asignacion, conductores, vehiculos, clientes, mapa de flota, incidencias, facturas.')
)

$document = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body>$body<w:sectPr><w:pgSz w:w='11906' w:h='16838'/><w:pgMar w:top='850' w:right='850' w:bottom='850' w:left='850'/></w:sectPr></w:body></w:document>"
$styles = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:styles xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:style w:type='paragraph' w:default='1' w:styleId='Normal'><w:name w:val='Normal'/></w:style><w:style w:type='paragraph' w:styleId='Heading1'><w:name w:val='heading 1'/><w:rPr><w:b/><w:sz w:val='32'/></w:rPr></w:style><w:style w:type='paragraph' w:styleId='Heading2'><w:name w:val='heading 2'/><w:rPr><w:b/><w:sz w:val='28'/></w:rPr></w:style><w:style w:type='paragraph' w:styleId='Heading3'><w:name w:val='heading 3'/><w:rPr><w:b/><w:sz w:val='24'/></w:rPr></w:style><w:style w:type='table' w:styleId='TableGrid'><w:name w:val='Table Grid'/><w:tblPr><w:tblBorders><w:top w:val='single' w:sz='4'/><w:left w:val='single' w:sz='4'/><w:bottom w:val='single' w:sz='4'/><w:right w:val='single' w:sz='4'/><w:insideH w:val='single' w:sz='4'/><w:insideV w:val='single' w:sz='4'/></w:tblBorders></w:tblPr></w:style></w:styles>"
$ct = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Types xmlns='http://schemas.openxmlformats.org/package/2006/content-types'><Default Extension='rels' ContentType='application/vnd.openxmlformats-package.relationships+xml'/><Default Extension='xml' ContentType='application/xml'/><Override PartName='/word/document.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml'/><Override PartName='/word/styles.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml'/></Types>"
$rels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument' Target='word/document.xml'/></Relationships>"
$docRels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles' Target='styles.xml'/></Relationships>"

Set-Content -LiteralPath (Join-Path $work '[Content_Types].xml') -Value $ct -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work '_rels\.rels') -Value $rels -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\document.xml') -Value $document -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\styles.xml') -Value $styles -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\_rels\document.xml.rels') -Value $docRels -Encoding UTF8
if (Test-Path -LiteralPath $output) {
  try { Remove-Item -LiteralPath $output -Force } catch { $output = Join-Path $PSScriptRoot ('TFG_CargoHub_manuales_basicos_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.docx') }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $output)
Remove-Item -LiteralPath $work -Recurse -Force
"DOCX generado: $output"
