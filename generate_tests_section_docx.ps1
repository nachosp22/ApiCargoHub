$ErrorActionPreference = 'Stop'

$output = Join-Path $PSScriptRoot 'TFG_CargoHub_seccion_pruebas.docx'
$work = Join-Path $env:TEMP ('cargohub_tests_docx_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work '_rels') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word\_rels') | Out-Null

function X([string]$s) { if ($null -eq $s) { return '' }; return [System.Security.SecurityElement]::Escape($s) }
function P([string]$text) { return "<w:p><w:r><w:rPr><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='24'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function H1([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading1'/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='32'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function H2([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading2'/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='28'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p>" }
function Cell([string]$text, [bool]$header=$false) {
  $shade = if ($header) { "<w:shd w:fill='D9EAF7'/>" } else { '' }
  $bold = if ($header) { '<w:b/>' } else { '' }
  return "<w:tc><w:tcPr><w:tcW w:w='0' w:type='auto'/>$shade</w:tcPr><w:p><w:r><w:rPr>$bold<w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='18'/></w:rPr><w:t>$(X $text)</w:t></w:r></w:p></w:tc>"
}
function Table($headers, $rows) {
  $xml = "<w:tbl><w:tblPr><w:tblStyle w:val='TableGrid'/><w:tblW w:w='0' w:type='auto'/><w:tblBorders><w:top w:val='single' w:sz='4'/><w:left w:val='single' w:sz='4'/><w:bottom w:val='single' w:sz='4'/><w:right w:val='single' w:sz='4'/><w:insideH w:val='single' w:sz='4'/><w:insideV w:val='single' w:sz='4'/></w:tblBorders></w:tblPr>"
  $xml += '<w:tr>' + (($headers | ForEach-Object { Cell $_ $true }) -join '') + '</w:tr>'
  foreach ($r in $rows) { $xml += '<w:tr>' + (($r | ForEach-Object { Cell $_ $false }) -join '') + '</w:tr>' }
  $xml += '</w:tbl>'
  return $xml
}

$summaryRows = @(
  @('Backend API', 'JUnit 5, Mockito, Spring Boot Test, MockMvc', 'Servicios, controladores, seguridad, persistencia y reglas de negocio'),
  @('Aplicacion movil', 'JUnit, tests unitarios de repositorios/modelos/utilidades', 'Sesion, repositorios, interceptores, modelos y estados de sincronizacion'),
  @('Criterio de seleccion', 'Pruebas representativas', 'No se documentan todos los tests existentes; se incluyen los mas relevantes para defender funcionalidades principales')
)

$tests = @(
  @('PT-01','Seguridad y acceso','JwtSecurityIntegrationTest','Comprobar que el sistema exige autenticacion y aplica roles correctamente.','Acceder a endpoints protegidos con y sin token JWT.','Sin token se rechaza la peticion; con token valido se permite el acceso segun rol.'),
  @('PT-02','Propiedad de recursos','OwnershipAuthorizationIntegrationTest','Verificar que clientes y conductores solo acceden a recursos propios.','Cliente/conductor intenta consultar o modificar recursos propios y ajenos.','Los recursos propios se permiten; los ajenos devuelven prohibicion. Admin y superadmin pueden saltar la restriccion.'),
  @('PT-03','Solicitud de porte','PorteControllerSolicitudTest','Validar el alta de solicitud de porte desde el rol cliente.','Enviar solicitud valida, solicitud sin autenticacion, con rol incorrecto y con datos incompletos.','Cliente autenticado obtiene respuesta correcta; rol incorrecto o sin autenticacion se rechaza; datos incompletos devuelven error 400.'),
  @('PT-04','Gestion de estados del porte','PorteServiceTest','Comprobar cambios de estado, aceptacion y rechazo de ofertas.','Ejecutar aceptar, rechazar y cambiar estado sobre portes en distintas situaciones.','El porte cambia de estado cuando procede; se evitan asignaciones invalidas y rechazos repetidos o fuera de flujo.'),
  @('PT-05','Tracking de porte','PorteTrackingTest','Verificar la consulta del seguimiento de un porte segun permisos.','Cliente propietario, cliente no propietario, administrador y usuario sin autenticar consultan tracking.','Propietario y administrador obtienen 200; no propietario obtiene 403; sin autenticacion obtiene 401.'),
  @('PT-06','Ingestion de ubicacion GPS','TrackingControllerTest','Validar registro de ubicaciones del conductor.','Enviar muestras de ubicacion desde la API de tracking.','La muestra se valida y registra; se rechazan datos no validos segun reglas del endpoint.'),
  @('PT-07','Mapa de flota','FleetTrackingServiceTest','Comprobar clasificacion de conductores online, obsoletos u offline.','Construir snapshot de flota con ubicaciones recientes, antiguas, invalidas y fallo temporal.','El sistema clasifica correctamente estados de tracking, descarta coordenadas invalidas y usa cache cuando corresponde.'),
  @('PT-08','Asignacion de conductor','ConductorMatchingServiceTest','Verificar busqueda de conductores disponibles para un porte.','Evaluar disponibilidad por distancia, radio de accion y condiciones operativas.','Se devuelven conductores candidatos validos y se descartan los que no cumplen condiciones.'),
  @('PT-09','Vehiculos','VehiculoTest / EstadoVehiculoTest','Validar reglas de vehiculo y estados disponibles.','Crear vehiculo, normalizar matricula, calcular volumen y revisar estados del enum.','La matricula se normaliza, el volumen se calcula correctamente y existen los estados esperados.'),
  @('PT-10','Facturacion','FacturaServiceNumeroSerieTest / FacturaControllerMisFacturasTest','Comprobar generacion y consulta de facturas.','Generar factura, consultar facturas propias y validar numeracion.','La factura queda asociada al porte, se calcula correctamente y solo se listan facturas permitidas.'),
  @('PT-11','Albaran de entrega','AlbaranEntregaPdfServiceTest','Validar generacion de PDF de albaran tras entrega.','Generar PDF con porte valido y probar casos sin firma, sin firmante o estado invalido.','Con datos validos se generan bytes PDF; si faltan datos obligatorios se informa error claro.'),
  @('PT-12','Incidencias','IncidenciaServiceTest','Comprobar alta, resolucion, listado e historial de incidencias.','Crear incidencia, resolverla, listar pendientes, consultar por porte y revisar historial.','La incidencia se registra, cambia de estado correctamente y conserva eventos de historial.'),
  @('PT-13','Analisis de carga','CargoAnalysisServiceTest / GeminiCargaServiceTest','Verificar calculo de dimensiones y registro del analisis.','Enviar descripcion de carga y simular respuesta correcta o error del servicio externo.','Se calculan dimensiones cuando hay respuesta valida y se registra log de analisis o error.'),
  @('PT-14','Autorizacion de conductor','ConductorAprobacionTest','Validar flujo de aprobacion, rechazo y baja de conductor.','Crear conductor pendiente, aprobarlo, rechazarlo o darlo de baja.','El estado operativo del conductor cambia segun la accion y se aplican permisos de administracion.'),
  @('PT-15','Sesion movil','SessionSnapshotTest / AuthInterceptorTest','Comprobar persistencia de sesion y envio de token desde Android.','Crear snapshot de sesion y ejecutar interceptor de autenticacion.','La sesion se representa correctamente y las peticiones incluyen cabecera Authorization cuando corresponde.'),
  @('PT-16','Repositorios movil','PorteRepositoryTest / RepositorySupportTest','Validar comportamiento base de repositorios Android ante respuestas correctas y errores.','Simular respuestas de API correctas, errores y estados de conectividad.','El repositorio devuelve resultados controlados y comunica errores de forma consistente a la interfaz.'),
  @('PT-17','Sincronizacion de tracking movil','TrackingSyncStatusResolverTest','Comprobar interpretacion del estado de sincronizacion en la app movil.','Evaluar distintos estados de envio/pendiente/error de tracking.','La interfaz puede mostrar correctamente si el tracking esta sincronizado, pendiente o con error.')
)

$body = H1 'Seccion de pruebas'
$body += P 'En esta seccion se recogen pruebas representativas realizadas sobre el sistema CargoHub. No se incluyen todos los tests automatizados existentes, sino aquellos que mejor evidencian las funcionalidades principales del proyecto y los requisitos defendibles.'
$body += H2 'Resumen de estrategia de pruebas'
$body += Table @('Ambito','Herramientas','Cobertura') $summaryRows
$body += H2 'Casos de prueba representativos'
$body += Table @('ID','Area','Test de referencia','Objetivo','Datos / acciones','Resultado esperado') $tests
$body += H2 'Conclusion'
$body += P 'Las pruebas cubren los flujos principales del sistema: autenticacion y autorizacion, solicitud y gestion de portes, tracking GPS, flota, facturacion, albaran de entrega, incidencias, analisis de carga y componentes esenciales de la aplicacion movil. Esta seleccion permite justificar que el sistema ha sido validado tanto a nivel de reglas de negocio como a nivel de acceso y comportamiento de API.'

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
"DOCX generado: $output"
