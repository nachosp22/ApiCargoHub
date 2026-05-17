$ErrorActionPreference = 'Stop'

$docx = Join-Path $PSScriptRoot 'TFG_CargoHub_requisitos_funcionales_sobre_original_actualizado.docx'
$work = Join-Path $env:TEMP ('cargohub_req_preserve_' + [guid]::NewGuid().ToString('N'))

Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($docx, $work)

function X([string]$s) {
  if ($null -eq $s) { return '' }
  return [System.Security.SecurityElement]::Escape($s)
}

function Cell([string]$text, [bool]$bold = $false, [string]$width = '7116') {
  $b = if ($bold) { '<w:b/>' } else { '' }
  return "<w:tc><w:tcPr><w:tcW w:w=`"$width`" w:type=`"dxa`"/></w:tcPr><w:p><w:pPr><w:spacing w:line=`"360`" w:lineRule=`"auto`"/><w:jc w:val=`"both`"/><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/>$b</w:rPr></w:pPr><w:r><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/>$b</w:rPr><w:t>$(X $text)</w:t></w:r></w:p></w:tc>"
}

function Row([string]$code, [string]$text, [bool]$bold = $false) {
  return '<w:tr>' + (Cell $code $bold '1900') + (Cell $text $bold '7116') + '</w:tr>'
}

function Req([string]$code, [string]$title, [string]$desc) {
  return (Row $code $title $false) + (Row '' $desc $false)
}

$rows = ''
$rows += Row 'CODIGO' 'REQUISITO FUNCIONAL' $true

# RF originales conservados salvo ajustes imprescindibles de alcance
$rows += Row 'RF 1.' 'Seguridad y control de acceso' $true
$rows += Req 'RF 1.1' 'Autenticacion de usuarios' 'El sistema debe validar las credenciales de acceso en conjunto con la BBDD y generar un token de sesion para permitir la navegacion entre las diferentes partes del sistema. Datos requeridos: email, contrasena y rol asignado en BBDD.'
$rows += Req 'RF 1.2.' 'Gestion de roles y permisos' 'El sistema debera restringir o habilitar funcionalidades segun el rol del usuario autentificado. SuperAdmin tendra acceso total, Admin acceso general de gestion, Conductor acceso a sus portes y Cliente acceso a sus solicitudes, seguimiento y facturas.'
$rows += Req 'RF 1.3.' 'Cifrado y Proteccion de Datos' 'Las credenciales de acceso deberan almacenarse de forma cifrada en la BBDD para asegurar su integridad y confidencialidad. Tambien se debera proteger informacion sensible como datos personales de clientes y conductores.'

$rows += Row 'RF 2.' 'Gestion de usuarios y conductores' $true
$rows += Req 'RF 2.1.' 'Registro de usuarios' 'La capacidad de generar usuarios estara establecida por roles. Un SuperAdmin puede crear usuarios administrativos y operativos, un Admin puede gestionar usuarios dentro de su ambito y los perfiles sin permisos administrativos no pueden crear otros usuarios.'
$rows += Req 'RF 2.2.' 'Modificacion de usuarios' 'Se debe permitir modificar datos de usuario desde la propia cuenta o desde cuentas con rol superior, respetando siempre las restricciones de permisos.'
$rows += Req 'RF 2.3.' 'Registro de conductores' 'Para registrar un conductor se le debera asignar una cuenta de usuario con rol Conductor. Este registro incluira nombre, apellidos, DNI, telefono, ciudad base, radio de accion, disponibilidad y datos operativos necesarios.'
$rows += Req 'RF 2.4.' 'Modificacion de conductores' 'SuperAdmin y Admin podran modificar perfiles de conductor. El conductor podra consultar su informacion principal desde la aplicacion movil.'
$rows += Req 'RF 2.5.' 'Eliminacion de cuentas de usuario y conductores.' 'Los SuperAdmin y Admin tendran la posibilidad de eliminar o desactivar conductores y sus cuentas asociadas cuando corresponda.'

$rows += Row 'RF 3.' 'Gestion de Clientes' $true
$rows += Req 'RF 3.1.' 'Alta de nuevo cliente' 'El sistema debe permitir registrar nuevas empresas o particulares en la base de datos para poder asociarles servicios de transporte y emitir facturas. Para este registro se requeriran datos como nombre de empresa, CIF, direccion fiscal, email y telefono.'
$rows += Req 'RF 3.2.' 'Edicion perfil empresa' 'Los administradores deben poder editar informacion de una empresa existente.'
$rows += Req 'RF 3.3.' 'Consultar Historial de Servicios' 'El sistema debe poder mostrar todos los servicios relacionados con un determinado cliente.'
$rows += Req 'RF 3.4.' 'Portal de cliente' 'El cliente debe poder acceder al portal web para solicitar portes, consultar sus portes, revisar el seguimiento cuando este disponible y consultar sus facturas.'

$rows += Row 'RF 4.' 'Gestion de Flota' $true
$rows += Req 'RF 4.1.' 'Alta de Vehiculo' 'El sistema debe permitir registrar vehiculos a los Administradores con matricula, marca, modelo, tipo de vehiculo, capacidad de carga, dimensiones utiles, volumen y estado del vehiculo.'
$rows += Req 'RF 4.2.' 'Asignacion de vehiculo a conductor' 'El sistema debe ser capaz de asociar vehiculos a un conductor para reflejar correctamente los recursos disponibles de la flota.'
$rows += Req 'RF 4.3.' 'Eliminacion de vehiculo a conductor' 'El sistema debe permitir desvincular un vehiculo de un conductor para dejarlo sin conductor o asignarselo a otro.'
$rows += Req 'RF 4.4.' 'Modificacion estado vehiculo' 'El sistema debera poder establecer el estado del vehiculo, por ejemplo disponible, en mantenimiento o dado de baja.'
$rows += Req 'RF 4.5.' 'Reporte de ubicacion' 'El conductor debera reportar ubicacion cuando se encuentre en un servicio activo. Esta informacion sera clave para el seguimiento del porte, el mapa de flota y la trazabilidad.'
$rows += Req 'RF 4.6.' 'Agenda y disponibilidad del conductor' 'El sistema debe permitir registrar bloqueos puntuales o recurrentes para indicar cuando un conductor no esta disponible.'
$rows += Req 'RF 4.7.' 'Mapa de flota' 'La aplicacion de escritorio debe permitir visualizar en mapa la situacion de conductores y portes activos.'

$rows += Row 'RF 5.' 'Gestion de portes' $true
$rows += Req 'RF 5.1.' 'Solicitud nuevo porte' 'Un cliente solicita un porte desde el portal web indicando cliente, origen, destino, datos de carga, dimensiones, peso aproximado y descripcion.'
$rows += Req 'RF 5.2.' 'Estudio de solicitud' 'El sistema debe analizar la solicitud para calcular informacion basica como distancia, precio orientativo y posibles motivos de revision manual. Cuando sea necesario, la solicitud quedara pendiente de revision administrativa.'
$rows += Req 'RF 5.3.' 'Confirmacion y revision de solicitud' 'Administracion debe poder revisar la solicitud, ajustar datos necesarios y decidir si el porte queda pendiente de conductor, asignado, cancelado o en revision manual.'
$rows += Req 'RF 5.4' 'Consulta de portes' 'Los conductores deberan poder consultar los portes que tienen asignados, asi como los ya realizados. Los Admin deberan tener acceso a todos los portes y los clientes a los suyos propios.'
$rows += Req 'RF 5.5.' 'Modificacion de estado de porte' 'El sistema debera ser capaz de modificar el estado del porte en funcion de la etapa en la que se encuentre: pendiente, asignado, en recogida, en transito, entregado, cancelado o facturado.'
$rows += Req 'RF 5.6.' 'Modificacion de datos o cancelacion de porte.' 'Ante posibles cambios, el sistema debera permitir a los administradores modificar datos del porte o cancelarlo cuando corresponda.'
$rows += Req 'RF 5.7.' 'Reporte a cliente' 'Una vez el porte se confirma, el sistema debera permitir al cliente consultar el estado del servicio desde el portal web.'
$rows += Req 'RF 5.8.' 'Prueba de entrega' 'El conductor debe tener forma de demostrar que el pedido ha sido entregado mediante firma de entrega y generacion de albaran.'
$rows += Req 'RF 5.9.' 'Aceptacion o rechazo de ofertas' 'El conductor debe poder aceptar o rechazar una oferta o asignacion de porte cuando el flujo operativo lo requiera.'
$rows += Req 'RF 5.10.' 'Fotos de carga' 'El sistema debe permitir adjuntar fotografias relacionadas con la carga o con el proceso de entrega para dejar evidencia del servicio.'

$rows += Row 'RF 6.' 'Gestion de incidencias' $true
$rows += Req 'RF 6.1.' 'Reportar incidencia' 'El conductor debe tener la posibilidad de generar una incidencia asociada a un porte. En esta incidencia se recogeran datos como titulo, descripcion, fecha, estado, prioridad, severidad y porte asociado.'
$rows += Req 'RF 6.2.' 'Modificacion de estado' 'Una vez los Admin reciban la incidencia deberan poder ir cambiando su estado segun evolucione la misma.'
$rows += Req 'RF 6.3.' 'Historial de incidencias' 'El conductor debera tener acceso a su historial de incidencias y los Admin a todas las incidencias del sistema.'
$rows += Req 'RF 6.4.' 'Historial de eventos de incidencia' 'El sistema debe conservar un historial de eventos para saber que cambios se han realizado sobre una incidencia y que usuario los ha realizado.'

# RF7 mensajeria eliminado por fuera de alcance

$rows += Row 'RF 7.' 'Seguimiento GPS' $true
$rows += Req 'RF 7.1.' 'Sesion de seguimiento' 'El sistema debe registrar sesiones de seguimiento asociadas a un conductor y, cuando corresponda, a un porte concreto.'
$rows += Req 'RF 7.2.' 'Muestras de ubicacion' 'La aplicacion movil debe enviar muestras de ubicacion con latitud, longitud, fecha, velocidad y rumbo durante servicios activos.'
$rows += Req 'RF 7.3.' 'Pausas de seguimiento' 'El sistema debe poder registrar pausas dentro de una sesion de seguimiento cuando el conductor interrumpa temporalmente el tracking.'

$rows += Row 'RF 8.' 'Facturacion y documentacion' $true
$rows += Req 'RF 8.1.' 'Albaran de entrega' 'El sistema debe generar un albaran con los datos del porte entregado, la fecha de entrega y la firma registrada.'
$rows += Req 'RF 8.2.' 'Factura de porte a cliente' 'El sistema debe permitir a administracion generar una factura de un porte entregado, usando los datos del servicio, base imponible, IVA e importe total.'
$rows += Req 'RF 8.3.' 'Consulta de facturas' 'El cliente debe poder consultar sus facturas desde el portal web y administracion debe poder consultar todas las facturas desde la aplicacion de escritorio.'
$rows += Req 'RF 8.4.' 'Estadisticas de facturacion' 'El sistema debe generar estadisticas basicas de facturacion, numero de portes realizados y datos relacionados con la actividad del sistema.'

$rows += Row 'RF 9.' 'Elementos fuera de alcance funcional' $true
$rows += Req 'RF 9.1.' 'Mensajeria interna' 'El sistema de chat entre conductores y Admin queda fuera del alcance funcional actual. La comunicacion operativa se cubre mediante estados e incidencias.'
$rows += Req 'RF 9.2.' 'Notificaciones completas' 'Las notificaciones push completas no se defenderan como requisito funcional principal del sistema.'
$rows += Req 'RF 9.3.' 'Nominas, valoraciones y requisitos especiales retirados' 'Quedan fuera del alcance actual la gestion de nominas, las valoraciones, y los requisitos especiales de frio, material peligroso o trampilla elevadora.'

$table = '<w:tbl xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:tblPr><w:tblStyle w:val="Tablaconcuadrcula4-nfasis1"/><w:tblW w:w="0" w:type="auto"/><w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="1" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr><w:tblGrid><w:gridCol w:w="1900"/><w:gridCol w:w="7116"/></w:tblGrid>' + $rows + '</w:tbl>'

$xmlPath = Join-Path $work 'word\document.xml'
[xml]$doc = Get-Content -LiteralPath $xmlPath -Encoding UTF8
$ns = New-Object System.Xml.XmlNamespaceManager($doc.NameTable)
$ns.AddNamespace('w', 'http://schemas.openxmlformats.org/wordprocessingml/2006/main')
$tables = $doc.SelectNodes('//w:tbl', $ns)
$old = $tables[3]
$frag = $doc.CreateDocumentFragment()
$frag.InnerXml = $table
$null = $old.ParentNode.ReplaceChild($frag.FirstChild, $old)
$doc.Save($xmlPath)

$tmpOut = Join-Path $env:TEMP ('req_out_' + [guid]::NewGuid().ToString('N') + '.docx')
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $tmpOut)
Remove-Item -LiteralPath $work -Recurse -Force
Move-Item -LiteralPath $tmpOut -Destination $docx -Force

"DOCX actualizado: $docx"

