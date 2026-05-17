$ErrorActionPreference = 'Stop'

$template = Join-Path $PSScriptRoot 'Documentacion_TFG_copia.docx'
$output = Join-Path $PSScriptRoot 'TFG_CargoHub_requisitos_colores.docx'
$work = Join-Path $env:TEMP ('cargohub_req_color_' + [guid]::NewGuid().ToString('N'))

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

function P([string]$text, [string]$style = '', [bool]$bold = $false) {
  $b = if ($bold) { '<w:b/>' } else { '' }
  $st = if ($style) { "<w:pStyle w:val=`"$style`"/>" } else { '' }
  return "<w:p><w:pPr>$st<w:spacing w:line=`"360`" w:lineRule=`"auto`"/><w:jc w:val=`"both`"/><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/>$b</w:rPr></w:pPr><w:r><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/>$b</w:rPr><w:t>$(X $text)</w:t></w:r></w:p>"
}

function Cell([string]$text, [string]$fill, [bool]$bold = $false, [string]$width = '6800') {
  $b = if ($bold) { '<w:b/>' } else { '' }
  return "<w:tc><w:tcPr><w:tcW w:w=`"$width`" w:type=`"dxa`"/><w:shd w:val=`"clear`" w:color=`"auto`" w:fill=`"$fill`"/></w:tcPr><w:p><w:pPr><w:spacing w:line=`"360`" w:lineRule=`"auto`"/><w:jc w:val=`"both`"/><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/>$b</w:rPr></w:pPr><w:r><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:color w:val=`"000000`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/>$b</w:rPr><w:t>$(X $text)</w:t></w:r></w:p></w:tc>"
}

function Row([string]$code, [string]$text, [string]$fill, [bool]$bold = $false) {
  return '<w:tr>' + (Cell $code $fill $bold '1700') + (Cell $text $fill $bold '7300') + '</w:tr>'
}

function SectionRow([string]$code, [string]$text, [string]$fill) { Row $code $text $fill $true }
function ReqTitle([string]$code, [string]$text, [string]$fill) { Row $code $text $fill $true }
function ReqDesc([string]$text) { Row '' $text 'FFFFFF' $false }

$blueHeader = '1F4E78'
$blueBlock = '9DC3E6'
$blueName = 'D9EAF7'
$orangeHeader = 'C65911'
$orangeBlock = 'F4B183'
$orangeName = 'FCE4D6'

$rf = ''
$rf += Row 'CODIGO' 'REQUISITO FUNCIONAL' $blueHeader $true
$rf += SectionRow 'RF 1.' 'Seguridad y control de acceso' $blueBlock
$rf += ReqTitle 'RF 1.1.' 'Autenticacion de usuarios' $blueName
$rf += ReqDesc 'El sistema debe permitir iniciar sesion mediante email y contrasena, validando las credenciales contra la base de datos y generando un token de acceso para consumir la API desde las distintas aplicaciones.'
$rf += ReqTitle 'RF 1.2.' 'Gestion de roles y permisos' $blueName
$rf += ReqDesc 'El sistema debe restringir las funcionalidades segun el rol del usuario autenticado. Los roles principales son SuperAdmin, Admin, Conductor y Cliente.'
$rf += ReqTitle 'RF 1.3.' 'Proteccion de credenciales' $blueName
$rf += ReqDesc 'El sistema debe almacenar las contrasenas de forma cifrada y no exponerlas en las respuestas de la API.'
$rf += SectionRow 'RF 2.' 'Gestion de usuarios, clientes y conductores' $blueBlock
$rf += ReqTitle 'RF 2.1.' 'Gestion de usuarios' $blueName
$rf += ReqDesc 'Los administradores autorizados deben poder crear, consultar, editar, activar o desactivar usuarios del sistema.'
$rf += ReqTitle 'RF 2.2.' 'Gestion de clientes' $blueName
$rf += ReqDesc 'El sistema debe permitir registrar y modificar clientes con nombre de empresa, CIF, direccion fiscal, telefono, email de contacto y sector.'
$rf += ReqTitle 'RF 2.3.' 'Portal de cliente' $blueName
$rf += ReqDesc 'El cliente debe poder acceder al portal web para solicitar portes, consultar sus servicios, revisar estados y acceder a facturas.'
$rf += ReqTitle 'RF 2.4.' 'Gestion de conductores' $blueName
$rf += ReqDesc 'El sistema debe permitir registrar y modificar conductores, asociandolos a una cuenta de usuario y almacenando sus datos operativos.'
$rf += SectionRow 'RF 3.' 'Gestion de flota y disponibilidad' $blueBlock
$rf += ReqTitle 'RF 3.1.' 'Alta y edicion de vehiculos' $blueName
$rf += ReqDesc 'El sistema debe permitir registrar y editar vehiculos con matricula, marca, modelo, tipo, estado, capacidad y dimensiones utiles.'
$rf += ReqTitle 'RF 3.2.' 'Asignacion de vehiculo a conductor' $blueName
$rf += ReqDesc 'El sistema debe permitir asociar vehiculos a conductores para reflejar los recursos disponibles de la flota.'
$rf += ReqTitle 'RF 3.3.' 'Agenda y bloqueos del conductor' $blueName
$rf += ReqDesc 'El sistema debe permitir registrar bloqueos puntuales o recurrentes para reflejar la disponibilidad real del conductor.'
$rf += ReqTitle 'RF 3.4.' 'Mapa de flota' $blueName
$rf += ReqDesc 'Administracion debe poder visualizar en un mapa la ubicacion y situacion operativa de los conductores durante servicios activos.'
$rf += SectionRow 'RF 4.' 'Gestion de portes' $blueBlock
$rf += ReqTitle 'RF 4.1.' 'Solicitud de porte' $blueName
$rf += ReqDesc 'El cliente debe poder solicitar un porte indicando origen, destino, descripcion de la carga, dimensiones, peso aproximado y datos necesarios para valorar el servicio.'
$rf += ReqTitle 'RF 4.2.' 'Revision y asignacion de porte' $blueName
$rf += ReqDesc 'Administracion debe poder revisar solicitudes, marcar revision manual cuando corresponda y asignar conductor a un porte pendiente.'
$rf += ReqTitle 'RF 4.3.' 'Ofertas de porte al conductor' $blueName
$rf += ReqDesc 'El conductor debe poder consultar ofertas o portes disponibles para el y aceptar o rechazar una asignacion cuando el flujo lo requiera.'
$rf += ReqTitle 'RF 4.4.' 'Estados del porte' $blueName
$rf += ReqDesc 'El sistema debe gestionar los estados pendiente, asignado, en recogida, en transito, entregado, cancelado y facturado.'
$rf += ReqTitle 'RF 4.5.' 'Evidencias de carga' $blueName
$rf += ReqDesc 'El sistema debe permitir adjuntar fotografias relacionadas con la carga o el servicio para dejar constancia documental.'
$rf += SectionRow 'RF 5.' 'Flujo movil del conductor' $blueBlock
$rf += ReqTitle 'RF 5.1.' 'Inicio del viaje' $blueName
$rf += ReqDesc 'El conductor debe poder iniciar el viaje desde la aplicacion movil, pasando el porte al estado de recogida.'
$rf += ReqTitle 'RF 5.2.' 'Confirmacion de recogida' $blueName
$rf += ReqDesc 'El conductor debe poder confirmar la recogida de la mercancia, cambiando el porte al estado en transito.'
$rf += ReqTitle 'RF 5.3.' 'Finalizacion y firma de entrega' $blueName
$rf += ReqDesc 'El conductor debe poder finalizar el porte y registrar la firma de entrega de la persona receptora.'
$rf += SectionRow 'RF 6.' 'Seguimiento GPS y ubicaciones' $blueBlock
$rf += ReqTitle 'RF 6.1.' 'Registro de sesiones de seguimiento' $blueName
$rf += ReqDesc 'El sistema debe crear sesiones de seguimiento asociadas a un conductor y, cuando corresponda, a un porte concreto.'
$rf += ReqTitle 'RF 6.2.' 'Registro de ubicaciones' $blueName
$rf += ReqDesc 'La aplicacion movil debe poder enviar muestras de ubicacion con latitud, longitud, fecha, velocidad y rumbo.'
$rf += ReqTitle 'RF 6.3.' 'Consulta de tracking' $blueName
$rf += ReqDesc 'El cliente debe poder consultar el seguimiento del porte cuando el servicio este en una fase que permita mostrar ubicacion.'
$rf += SectionRow 'RF 7.' 'Gestion de incidencias' $blueBlock
$rf += ReqTitle 'RF 7.1.' 'Alta de incidencia' $blueName
$rf += ReqDesc 'El sistema debe permitir registrar incidencias asociadas a un porte, indicando titulo, descripcion, severidad, prioridad y estado.'
$rf += ReqTitle 'RF 7.2.' 'Gestion e historial de incidencias' $blueName
$rf += ReqDesc 'Administracion debe poder revisar, resolver y auditar incidencias, manteniendo un historial de eventos.'
$rf += SectionRow 'RF 8.' 'Albaranes y facturacion' $blueBlock
$rf += ReqTitle 'RF 8.1.' 'Generacion de albaran' $blueName
$rf += ReqDesc 'El sistema debe generar un albaran de entrega a partir de los datos del porte entregado, incluyendo origen, destino, fecha y firma.'
$rf += ReqTitle 'RF 8.2.' 'Generacion de factura' $blueName
$rf += ReqDesc 'Administracion debe poder generar una factura asociada a un porte entregado, calculando base imponible, IVA e importe total.'
$rf += ReqTitle 'RF 8.3.' 'Consulta y estado de facturas' $blueName
$rf += ReqDesc 'Administracion y cliente deben poder consultar facturas segun permisos, y el sistema debe registrar si una factura esta pagada.'
$rf += SectionRow 'RF 9.' 'Funciones fuera de alcance actual' $blueBlock
$rf += ReqTitle 'RF 9.1.' 'Exclusiones funcionales' $blueName
$rf += ReqDesc 'No forman parte del alcance defendido el chat interno, las notificaciones push completas, la gestion de nominas, las valoraciones, frio, mercancia peligrosa o trampilla elevadora.'

$rnf = ''
$rnf += Row 'CODIGO' 'REQUISITO NO FUNCIONAL' $orangeHeader $true
$rnf += SectionRow 'RNF 1.' 'Usabilidad y experiencia de usuario' $orangeBlock
$rnf += ReqTitle 'RNF 1.1.' 'Interfaz movil adaptada al conductor' $orangeName
$rnf += ReqDesc 'La aplicacion movil debe priorizar claridad, botones visibles y uso sencillo durante la operativa del conductor.'
$rnf += ReqTitle 'RNF 1.2.' 'Aplicacion de escritorio eficiente' $orangeName
$rnf += ReqDesc 'La aplicacion de escritorio debe permitir a administracion localizar y gestionar informacion mediante tablas, filtros y acciones claras.'
$rnf += ReqTitle 'RNF 1.3.' 'Portal cliente sencillo' $orangeName
$rnf += ReqDesc 'El portal web debe permitir al cliente solicitar portes y consultar informacion sin complejidad innecesaria.'
$rnf += SectionRow 'RNF 2.' 'Rendimiento y sincronizacion' $orangeBlock
$rnf += ReqTitle 'RNF 2.1.' 'Respuesta de la API' $orangeName
$rnf += ReqDesc 'La API debe responder de forma adecuada a las operaciones habituales de consulta, alta, modificacion y eliminacion.'
$rnf += ReqTitle 'RNF 2.2.' 'Seguimiento GPS eficiente' $orangeName
$rnf += ReqDesc 'El sistema debe registrar muestras GPS de forma eficiente para no sobrecargar la aplicacion movil ni el backend.'
$rnf += ReqTitle 'RNF 2.3.' 'Actualizacion de estados' $orangeName
$rnf += ReqDesc 'Los cambios de estado del porte deben reflejarse de forma consistente en las distintas interfaces.'
$rnf += SectionRow 'RNF 3.' 'Seguridad e integridad' $orangeBlock
$rnf += ReqTitle 'RNF 3.1.' 'Proteccion de datos personales' $orangeName
$rnf += ReqDesc 'El sistema debe proteger datos personales de clientes, conductores y usuarios, aplicando control de acceso por rol y propiedad.'
$rnf += ReqTitle 'RNF 3.2.' 'Integridad de documentos' $orangeName
$rnf += ReqDesc 'Albaranes y facturas deben generarse a partir de datos coherentes del porte para evitar contradicciones documentales.'
$rnf += ReqTitle 'RNF 3.3.' 'Control de concurrencia' $orangeName
$rnf += ReqDesc 'El sistema debe evitar modificaciones incompatibles sobre un mismo porte mediante mecanismos de control de version cuando corresponda.'
$rnf += SectionRow 'RNF 4.' 'Disponibilidad y fiabilidad' $orangeBlock
$rnf += ReqTitle 'RNF 4.1.' 'Disponibilidad del servicio' $orangeName
$rnf += ReqDesc 'El backend y las aplicaciones deben estar disponibles durante la operativa normal del sistema.'
$rnf += ReqTitle 'RNF 4.2.' 'Tolerancia a fallos de red' $orangeName
$rnf += ReqDesc 'La aplicacion movil debe gestionar de forma razonable situaciones de conectividad limitada, especialmente durante el seguimiento.'
$rnf += SectionRow 'RNF 5.' 'Mantenibilidad y escalabilidad' $orangeBlock
$rnf += ReqTitle 'RNF 5.1.' 'Arquitectura separada por capas y aplicaciones' $orangeName
$rnf += ReqDesc 'El sistema debe mantener separadas la API, la base de datos, la aplicacion de escritorio, la aplicacion movil y el portal web.'
$rnf += ReqTitle 'RNF 5.2.' 'Codigo mantenible' $orangeName
$rnf += ReqDesc 'El desarrollo debe organizarse de forma que permita corregir errores y ampliar funcionalidades sin afectar innecesariamente a otros modulos.'
$rnf += SectionRow 'RNF 6.' 'Compatibilidad' $orangeBlock
$rnf += ReqTitle 'RNF 6.1.' 'Compatibilidad movil' $orangeName
$rnf += ReqDesc 'La aplicacion movil debe ser compatible con dispositivos Android dentro del rango previsto para el proyecto.'
$rnf += ReqTitle 'RNF 6.2.' 'Compatibilidad de escritorio y web' $orangeName
$rnf += ReqDesc 'La aplicacion de escritorio debe ejecutarse en equipos Windows actuales y el portal web debe funcionar en navegadores modernos.'

function TableXml([string]$rows) {
  return '<w:tbl><w:tblPr><w:tblW w:w="0" w:type="auto"/><w:tblBorders><w:top w:val="single" w:sz="8"/><w:left w:val="single" w:sz="8"/><w:bottom w:val="single" w:sz="8"/><w:right w:val="single" w:sz="8"/><w:insideH w:val="single" w:sz="8"/><w:insideV w:val="single" w:sz="8"/></w:tblBorders></w:tblPr><w:tblGrid><w:gridCol w:w="1700"/><w:gridCol w:w="7300"/></w:tblGrid>' + $rows + '</w:tbl>'
}

$body = ''
$body += P '4. Especificacion de requisitos' 'Heading1' $true
$body += P '4.1. Requisitos funcionales' 'Heading2' $true
$body += P 'La siguiente tabla recoge los requisitos funcionales actualizados de CargoHub, adaptados al alcance real del sistema implementado.'
$body += TableXml $rf
$body += P ''
$body += P '4.2. Requisitos no funcionales' 'Heading2' $true
$body += P 'La siguiente tabla recoge los requisitos no funcionales del sistema, centrados en calidad, seguridad, rendimiento, disponibilidad y mantenibilidad.'
$body += TableXml $rnf

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
