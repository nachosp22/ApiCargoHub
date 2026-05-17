$ErrorActionPreference = 'Stop'

$output = Join-Path $PSScriptRoot 'TFG_CargoHub_seccion_pruebas_vertical.docx'
$work = Join-Path $env:TEMP ('cargohub_tests_vertical_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work '_rels') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word\_rels') | Out-Null

function X([string]$s) { if ($null -eq $s) { return '' }; return [System.Security.SecurityElement]::Escape($s) }
function P([string]$text) { return "<w:p><w:pPr><w:spacing w:after='120'/></w:pPr><w:r><w:rPr><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='24'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function H1([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading1'/><w:spacing w:before='120' w:after='120'/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='32'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function H2([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading2'/><w:spacing w:before='180' w:after='80'/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='28'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function Cell([string]$text, [int]$width, [bool]$header=$false) {
  $shade = if ($header) { "<w:shd w:fill='D9EAF7'/>" } else { '' }
  $bold = if ($header) { '<w:b/>' } else { '' }
  return "<w:tc><w:tcPr><w:tcW w:w='$width' w:type='dxa'/>$shade</w:tcPr><w:p><w:pPr><w:spacing w:after='0'/></w:pPr><w:r><w:rPr>$bold<w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='19'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p></w:tc>"
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

$unitarias = @(
  @('PU-01','PorteServiceTest','Gestion de estado y ofertas','Cambiar estado, aceptar y rechazar portes en condiciones validas e invalidas.','El servicio aplica las reglas de negocio y evita estados o asignaciones incorrectas.'),
  @('PU-02','ConductorMatchingServiceTest','Asignacion de conductor','Buscar conductores disponibles segun radio, ubicacion y disponibilidad.','Solo se devuelven conductores candidatos que cumplen condiciones operativas.'),
  @('PU-03','CalculadoraPrecioServiceTest','Calculo de precio','Calcular precio del porte a partir de distancia, carga y vehiculo requerido.','El precio se obtiene de forma coherente con los parametros introducidos.'),
  @('PU-04','FacturaServiceNumeroSerieTest','Numeracion de facturas','Generar numero de serie de factura.','La factura mantiene una numeracion correcta y unica.'),
  @('PU-05','AlbaranEntregaPdfServiceTest','Albaran de entrega','Generar PDF de albaran con datos validos y casos incompletos.','Con datos validos se genera PDF; si faltan firma o estado valido se informa error claro.'),
  @('PU-06','IncidenciaServiceTest','Gestion de incidencias','Crear, resolver y consultar incidencias.','La incidencia cambia de estado y mantiene informacion de seguimiento.'),
  @('PU-07','FleetTrackingServiceTest','Mapa de flota','Clasificar conductores online, obsoletos u offline.','El snapshot clasifica correctamente y descarta coordenadas invalidas.'),
  @('PU-08','CargoAnalysisServiceTest / GeminiCargaServiceTest','Analisis de carga','Calcular dimensiones de carga y registrar resultado o error.','Se obtiene vehiculo recomendado o se registra fallo controlado.'),
  @('PU-09','VehiculoTest / EstadoVehiculoTest','Vehiculos','Normalizar matricula, calcular volumen y comprobar estados.','Los datos de vehiculo se normalizan y calculan correctamente.'),
  @('PU-10','Tests movil de repositorio/sesion','Comportamiento movil','Validar sesion, token, repositorios y estado de sincronizacion.','La app interpreta respuestas, sesion y errores de forma controlada.')
)

$integracion = @(
  @('PI-01','JwtSecurityIntegrationTest','Seguridad JWT','Acceder a endpoints protegidos con y sin token.','Sin token se rechaza; con token valido se permite segun rol.'),
  @('PI-02','OwnershipAuthorizationIntegrationTest','Propiedad de recursos','Cliente/conductor intenta acceder a recursos propios y ajenos.','Recursos propios permitidos; ajenos prohibidos; admin/superadmin tienen excepcion.'),
  @('PI-03','PorteControllerSolicitudTest','Solicitud de porte','Crear solicitud como cliente y probar roles incorrectos/datos incompletos.','Cliente autenticado puede solicitar; admin, anonimo o payload invalido se rechazan.'),
  @('PI-04','PorteTrackingTest','Consulta de tracking','Consultar tracking como propietario, no propietario, admin y anonimo.','Propietario/admin obtienen 200; no propietario 403; anonimo 401.'),
  @('PI-05','TrackingControllerTest','Registro GPS','Enviar muestra de ubicacion mediante API.','La API valida y registra la ubicacion o rechaza datos incorrectos.'),
  @('PI-06','FacturaControllerMisFacturasTest','Facturas propias','Consultar facturas desde usuario autorizado.','Solo se devuelven facturas permitidas para el usuario.'),
  @('PI-07','ConductorAprobacionTest','Aprobacion de conductor','Aprobar, rechazar o dar de baja conductor desde administracion.','El sistema actualiza el estado del conductor y aplica permisos.'),
  @('PI-08','FleetControllerTest','Flota','Consultar informacion de flota desde endpoint.','La API devuelve informacion consistente de vehiculos/conductores.'),
  @('PI-09','DatabaseUserDetailsServiceTest','Carga de usuario','Autenticacion recuperando usuario desde base de datos.','El servicio carga credenciales y roles esperados.'),
  @('PI-10','BackendApplicationTests','Contexto Spring','Arrancar el contexto de la aplicacion.','El contexto carga correctamente sin errores de configuracion.')
)

$body = H1 'Seccion de pruebas'
$body += P 'Las pruebas del proyecto se han organizado en dos grupos: pruebas unitarias, centradas en componentes concretos y reglas de negocio, y pruebas de integracion, centradas en el comportamiento de la API, seguridad y contexto Spring. No se listan todos los tests existentes; se seleccionan los mas representativos para la memoria.'
$body += H2 'Pruebas unitarias'
$body += Table @('ID','Test de referencia','Area','Datos / accion','Resultado esperado') @(900,2300,1900,2500,1900) $unitarias
$body += H2 'Pruebas de integracion'
$body += Table @('ID','Test de referencia','Area','Datos / accion','Resultado esperado') @(900,2300,1900,2500,1900) $integracion
$body += H2 'Conclusion'
$body += P 'La bateria de pruebas cubre los flujos principales del sistema: seguridad, permisos, portes, tracking GPS, flota, facturacion, albaranes, incidencias, analisis de carga y componentes de la aplicacion movil. Esta separacion facilita defender que se han validado tanto reglas internas como comportamiento integrado del sistema.'

$document = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body>$body<w:sectPr><w:pgSz w:w='11906' w:h='16838'/><w:pgMar w:top='850' w:right='850' w:bottom='850' w:left='850'/></w:sectPr></w:body></w:document>"
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
"DOCX generado: $output"
