$ErrorActionPreference = 'Stop'

$output = Join-Path $PSScriptRoot 'TFG_CargoHub_manuales_texto_plano.docx'
$work = Join-Path $env:TEMP ('cargohub_manuals_plain_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work '_rels') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word\_rels') | Out-Null

function X([string]$s) { if ($null -eq $s) { return '' }; return [System.Security.SecurityElement]::Escape($s) }
function P([string]$text) {
  return "<w:p><w:pPr><w:spacing w:after='120'/></w:pPr><w:r><w:rPr><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:color w:val='000000'/><w:sz w:val='24'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>"
}
function L([string]$text) {
  return "<w:p><w:pPr><w:spacing w:after='80'/><w:ind w:left='720' w:hanging='360'/></w:pPr><w:r><w:rPr><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:color w:val='000000'/><w:sz w:val='24'/></w:rPr><w:t>$(X ('- ' + $text))</w:t></w:r></w:p>"
}

$body = ''

$body += P 'MANUALES DE CARGOHUB'
$body += P 'Este apartado recoge de forma sencilla los manuales basicos de instalacion y uso de CargoHub. El sistema se divide en tres interfaces principales: aplicacion movil para conductores, portal web para clientes y aplicacion de escritorio para administracion.'
$body += P 'El objetivo de estos manuales es explicar el funcionamiento general de cada parte sin entrar en detalles tecnicos complejos. Las imagenes o capturas se pueden insertar posteriormente en los puntos que se consideren necesarios.'

$body += P 'MANUAL DE INSTALACION Y DESPLIEGUE'

$body += P 'Aplicacion movil del conductor'
$body += P 'La aplicacion movil esta pensada para dispositivos Android. Su instalacion se realiza mediante un archivo APK generado previamente. Este archivo contiene la aplicacion preparada para instalarse en el telefono del conductor.'
$body += L 'Copiar el archivo APK al dispositivo movil o descargarlo desde el enlace facilitado por la empresa.'
$body += L 'Abrir el archivo APK desde el dispositivo Android.'
$body += L 'Si Android lo solicita, permitir la instalacion de aplicaciones de origen desconocido.'
$body += L 'Completar el proceso de instalacion.'
$body += L 'Abrir la aplicacion CargoHub desde el icono creado en el dispositivo.'
$body += L 'Iniciar sesion con el usuario y contrasena del conductor.'
$body += P 'Una vez instalada, la aplicacion permite al conductor consultar sus servicios, aceptar o rechazar portes, actualizar estados, registrar incidencias y enviar informacion de ubicacion cuando corresponda.'

$body += P 'Portal web del cliente'
$body += P 'El portal web no necesita instalacion en el equipo del cliente. Al tratarse de una aplicacion web, el acceso se realiza desde un navegador mediante la direccion del sistema.'
$body += L 'Abrir un navegador web actualizado.'
$body += L 'Introducir la direccion del portal web de CargoHub.'
$body += L 'Acceder con el usuario y contrasena del cliente.'
$body += L 'Utilizar las opciones disponibles dentro del panel cliente.'
$body += P 'Desde el portal web, el cliente puede solicitar portes, consultar sus portes, revisar el seguimiento disponible y acceder a sus facturas. Al no requerir instalacion, cualquier actualizacion del portal se aplica directamente en el servidor y el usuario solo necesita volver a acceder desde el navegador.'

$body += P 'Aplicacion de escritorio de administracion'
$body += P 'La aplicacion de escritorio esta pensada para el personal de administracion. Su instalacion se realiza mediante un instalador MSI. Este instalador comprueba que el equipo tenga disponibles las tecnologias necesarias para ejecutar la aplicacion y, al finalizar, genera el ejecutable .exe correspondiente.'
$body += L 'Ejecutar el archivo MSI en el equipo de administracion.'
$body += L 'Seguir los pasos del asistente de instalacion.'
$body += L 'Permitir que el instalador compruebe las tecnologias necesarias.'
$body += L 'Si falta algun componente requerido, instalarlo o seguir las indicaciones del asistente.'
$body += L 'Seleccionar la ruta de instalacion si el instalador lo permite.'
$body += L 'Finalizar la instalacion.'
$body += L 'Abrir la aplicacion desde el acceso directo o desde el archivo .exe generado.'
$body += P 'Una vez instalada, la aplicacion de escritorio permite a administracion gestionar usuarios, clientes, conductores, vehiculos, portes, incidencias, facturas y el mapa de flota.'

$body += P 'MANUAL DE USO'

$body += P 'Uso del portal web por parte del cliente'
$body += P 'El cliente accede al portal web desde un navegador. Tras iniciar sesion, entra en su panel principal, desde donde puede gestionar las funciones relacionadas con sus servicios de transporte.'
$body += P 'Para iniciar sesion, el cliente debe introducir su email y contrasena. Si los datos son correctos, el sistema muestra el panel cliente. Desde este panel se accede a las opciones principales.'
$body += P 'Para solicitar un porte, el cliente debe entrar en la opcion correspondiente y completar los datos del servicio. Normalmente se indicara el origen, destino, fechas previstas y descripcion de la carga. Una vez enviada la solicitud, queda pendiente de revision por parte de administracion.'
$body += P 'Para consultar sus portes, el cliente accede a la seccion Mis portes. En ella puede ver los servicios asociados a su cuenta y seleccionar uno concreto para revisar su detalle. El sistema solo debe mostrar los portes propios del cliente autenticado.'
$body += P 'Para revisar el seguimiento de un porte, el cliente entra en el detalle del servicio. En esta vista puede consultar el estado actual del transporte y la informacion disponible sobre su evolucion.'
$body += P 'Para consultar facturas, el cliente accede a la seccion Mis facturas. Desde ahi puede revisar las facturas generadas para sus servicios y, si el sistema lo permite, abrirlas o descargarlas.'

$body += P 'Uso de la aplicacion movil por parte del conductor'
$body += P 'El conductor utiliza la aplicacion movil para gestionar su trabajo diario. La aplicacion esta centrada en la consulta de portes, actualizacion de estados, seguimiento GPS e incidencias.'
$body += P 'Para iniciar sesion, el conductor abre la aplicacion instalada en su movil e introduce sus credenciales. Una vez validado, accede a la pantalla principal.'
$body += P 'Para consultar ofertas o portes asignados, el conductor entra en la seccion correspondiente. En ella puede revisar la informacion principal del servicio, como origen, destino, fechas y datos de la carga.'
$body += P 'Si el flujo operativo lo permite, el conductor puede aceptar o rechazar una oferta de porte. Esta accion informa al sistema de si el conductor realizara el servicio o si debe buscarse otra asignacion.'
$body += P 'Durante el servicio, el conductor puede actualizar el estado del porte. Los estados representan el avance real del transporte, por ejemplo inicio del viaje, recogida, transito y entrega.'
$body += P 'La aplicacion movil tambien puede enviar muestras de ubicacion GPS durante servicios activos. Esta informacion ayuda a administracion a controlar la flota y permite ofrecer seguimiento al cliente.'
$body += P 'Al finalizar el porte, el conductor registra la entrega. Si el proceso lo requiere, puede asociar evidencias como firma, fotografias o informacion necesaria para generar el albaran.'
$body += P 'Si ocurre un problema durante el servicio, el conductor puede crear una incidencia. Para ello debe indicar el tipo de problema y una descripcion suficiente para que administracion pueda gestionarlo.'

$body += P 'Uso de la aplicacion de escritorio por parte de administracion'
$body += P 'La aplicacion de escritorio es la herramienta principal del administrador. Desde ella se controla la operativa diaria de CargoHub.'
$body += P 'Para iniciar sesion, el administrador abre la aplicacion desde el .exe o acceso directo generado durante la instalacion. Tras introducir sus credenciales, accede al panel principal.'
$body += P 'Desde el panel principal, administracion puede revisar informacion general del sistema y acceder a los modulos principales mediante el menu de la aplicacion.'
$body += P 'Para gestionar portes, el administrador entra en la seccion Portes o Revision de portes. Desde ahi puede consultar solicitudes, revisar datos del servicio, modificar informacion necesaria y preparar la asignacion.'
$body += P 'Para asignar un conductor, el administrador selecciona un porte pendiente o revisado y elige un conductor disponible. El sistema debe tener en cuenta la disponibilidad, el estado del conductor y la relacion con el vehiculo correspondiente.'
$body += P 'Para gestionar conductores, administracion puede consultar el listado, crear nuevos perfiles, modificar datos, aprobar conductores o cambiar su estado operativo.'
$body += P 'Para gestionar vehiculos, administracion puede registrar vehiculos, modificar sus datos, cambiar su estado y asociarlos o desasociarlos de conductores.'
$body += P 'Para gestionar clientes, administracion puede crear o editar clientes y consultar la informacion relacionada con sus servicios.'
$body += P 'Desde el mapa de flota, administracion puede revisar la situacion de conductores y portes activos cuando exista informacion de ubicacion disponible.'
$body += P 'Desde la seccion de incidencias, administracion puede consultar problemas reportados por conductores, revisar su detalle y actualizar su estado hasta resolverlos.'
$body += P 'Desde la seccion de facturas, administracion puede generar o consultar facturas asociadas a portes entregados. La factura se genera una vez que los datos del porte han sido revisados.'

$body += P 'RECOMENDACION PARA INSERTAR CAPTURAS'
$body += P 'Las capturas se pueden insertar despues de cada explicacion importante. Se recomienda usar imagenes simples y claras, evitando capturas repetidas o demasiado cargadas.'
$body += L 'En la aplicacion movil conviene mostrar la instalacion del APK, el login, la pantalla principal, el detalle de un porte, el cambio de estado y la creacion de una incidencia.'
$body += L 'En el portal web conviene mostrar la pagina inicial, el login o registro, el panel cliente, el formulario de solicitud de porte, el listado de portes, el seguimiento y las facturas.'
$body += L 'En la aplicacion de escritorio conviene mostrar el instalador MSI, la comprobacion de requisitos, el .exe generado, el login, el dashboard, el listado de portes, la asignacion de conductor, el mapa de flota, incidencias y facturas.'

$document = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body>$body<w:sectPr><w:pgSz w:w='11906' w:h='16838'/><w:pgMar w:top='850' w:right='850' w:bottom='850' w:left='850'/></w:sectPr></w:body></w:document>"
$styles = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:styles xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:style w:type='paragraph' w:default='1' w:styleId='Normal'><w:name w:val='Normal'/><w:rPr><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:color w:val='000000'/><w:sz w:val='24'/></w:rPr></w:style></w:styles>"
$ct = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Types xmlns='http://schemas.openxmlformats.org/package/2006/content-types'><Default Extension='rels' ContentType='application/vnd.openxmlformats-package.relationships+xml'/><Default Extension='xml' ContentType='application/xml'/><Override PartName='/word/document.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml'/><Override PartName='/word/styles.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml'/></Types>"
$rels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument' Target='word/document.xml'/></Relationships>"
$docRels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles' Target='styles.xml'/></Relationships>"

Set-Content -LiteralPath (Join-Path $work '[Content_Types].xml') -Value $ct -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work '_rels\.rels') -Value $rels -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\document.xml') -Value $document -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\styles.xml') -Value $styles -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\_rels\document.xml.rels') -Value $docRels -Encoding UTF8
if (Test-Path -LiteralPath $output) {
  try { Remove-Item -LiteralPath $output -Force } catch { $output = Join-Path $PSScriptRoot ('TFG_CargoHub_manuales_texto_plano_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.docx') }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $output)
Remove-Item -LiteralPath $work -Recurse -Force
"DOCX generado: $output"
