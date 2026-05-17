$ErrorActionPreference = 'Stop'

$template = Join-Path $PSScriptRoot 'Documentacion_TFG_copia.docx'
$output = Join-Path $PSScriptRoot 'TFG_CargoHub_atributos_modelo_ER_formato_original_sin_tildes.docx'
$work = Join-Path $env:TEMP ('cargohub_er_docx_' + [guid]::NewGuid().ToString('N'))
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
  $st = if ($style) { "<w:pPr><w:pStyle w:val=`"$style`"/></w:pPr>" } else { '' }
  return "<w:p>$st<w:r><w:t>$(X $text)</w:t></w:r></w:p>"
}

function Cell([string]$text, [bool]$bold = $false, [bool]$underline = $false, [string]$width = '6186', [bool]$gridSpan = $false) {
  $b = if ($bold) { '<w:b/>' } else { '' }
  $u = if ($underline) { '<w:u w:val="single"/>' } else { '' }
  $span = if ($gridSpan) { '<w:gridSpan w:val="2"/>' } else { '' }
  return "<w:tc><w:tcPr><w:tcW w:w=`"$width`" w:type=`"dxa`"/>$span</w:tcPr><w:p><w:pPr><w:spacing w:line=`"360`" w:lineRule=`"auto`"/><w:jc w:val=`"both`"/><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/></w:rPr></w:pPr><w:r><w:rPr><w:rFonts w:ascii=`"Times New Roman`" w:hAnsi=`"Times New Roman`" w:cs=`"Times New Roman`"/><w:sz w:val=`"24`"/><w:szCs w:val=`"24`"/>$b$u</w:rPr><w:t>$(X $text)</w:t></w:r></w:p></w:tc>"
}

function Table($rows) {
  $title = $rows[0].Entidad
  $xml = '<w:tbl><w:tblPr><w:tblStyle w:val="Tablaconcuadrcula4-nfasis1"/><w:tblW w:w="0" w:type="auto"/><w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="1" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr><w:tblGrid><w:gridCol w:w="2830"/><w:gridCol w:w="6186"/></w:tblGrid>'
  $xml += '<w:tr>' + (Cell $title $true $false '9016' $true) + '</w:tr>'
  foreach ($r in $rows) {
    $isPk = $r.Clave -like '*PK*'
    $xml += '<w:tr>' + (Cell $r.Atributo $false $isPk '2830' $false) + (Cell $r.Descripcion $false $false '6186' $false) + '</w:tr>'
  }
  $xml += '</w:tbl>'
  return $xml
}

function A($atributo, $tipo, $clave, $descripcion) {
  [pscustomobject]@{ Atributo = $atributo; Tipo = $tipo; Clave = $clave; Descripcion = $descripcion; Entidad = '' }
}

$entities = @(
  [pscustomobject]@{ Nombre='USUARIO'; Intro='Entidad que centraliza la autenticaciA³n, el rol de acceso y el estado de la cuenta dentro del sistema.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico del usuario.'
    A 'email' 'varchar' 'UK' 'Correo usado para iniciar sesiA³n. Debe ser Aºnico.'
    A 'password' 'varchar' '' 'ContraseA±a almacenada de forma codificada.'
    A 'rol' 'varchar' '' 'Rol del usuario: administrador, superadministrador, conductor o cliente.'
    A 'activo' 'boolean' '' 'Indica si la cuenta estA habilitada.'
    A 'fecha_registro' 'timestamp' '' 'Fecha de alta del usuario.'
    A 'ultimo_acceso' 'timestamp' '' 'Asltimo acceso registrado.'
    A 'foto_url' 'varchar' '' 'Ruta o URL de la imagen de perfil.'
    A 'token_recuperacion' 'varchar' '' 'Token temporal utilizado para recuperaciA³n de contraseA±a.'
  )}
  [pscustomobject]@{ Nombre='CLIENTE'; Intro='Representa a la empresa o persona que solicita portes mediante el portal web.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico del cliente.'
    A 'usuario_id' 'bigint' 'FK, UK' 'Usuario asociado al cliente. Mantiene la relaciA³n uno a uno con USUARIO.'
    A 'nombre_empresa' 'varchar' '' 'Nombre comercial o razA³n social del cliente.'
    A 'cif' 'varchar' 'UK' 'Identificador fiscal del cliente.'
    A 'direccion_fiscal' 'varchar' '' 'DirecciA³n fiscal de la empresa o cliente.'
    A 'telefono' 'varchar' '' 'TelA©fono de contacto.'
    A 'email_contacto' 'varchar' '' 'Correo de contacto operativo.'
    A 'sector' 'varchar' '' 'Sector de actividad del cliente.'
  )}
  [pscustomobject]@{ Nombre='CONDUCTOR'; Intro='Perfil operativo encargado de aceptar, transportar y entregar portes desde la aplicaciA³n mA³vil.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico del conductor.'
    A 'usuario_id' 'bigint' 'FK, UK' 'Usuario asociado al conductor. Mantiene la relaciA³n uno a uno con USUARIO.'
    A 'nombre' 'varchar' '' 'Nombre del conductor.'
    A 'apellidos' 'varchar' '' 'Apellidos del conductor.'
    A 'dni' 'varchar' 'UK' 'Documento identificativo Aºnico del conductor.'
    A 'telefono' 'varchar' '' 'TelA©fono de contacto.'
    A 'ciudad_base' 'varchar' '' 'Ciudad principal desde la que opera.'
    A 'latitud_base' 'double' '' 'Latitud de la ubicaciA³n base.'
    A 'longitud_base' 'double' '' 'Longitud de la ubicaciA³n base.'
    A 'radio_accion_km' 'int' '' 'Radio aproximado de trabajo del conductor.'
    A 'latitud_actual' 'double' '' 'Asltima latitud conocida.'
    A 'longitud_actual' 'double' '' 'Asltima longitud conocida.'
    A 'ultima_actualizacion_ubicacion' 'timestamp' '' 'Fecha de la Aºltima ubicaciA³n recibida.'
    A 'velocidad_actual_kph' 'double' '' 'Velocidad estimada en la Aºltima muestra.'
    A 'rumbo_actual_deg' 'int' '' 'DirecciA³n aproximada del desplazamiento.'
    A 'buscar_retorno' 'boolean' '' 'Indica si el conductor busca portes de retorno.'
    A 'dias_laborables' 'varchar' '' 'DA­as habituales de disponibilidad.'
    A 'disponible' 'boolean' '' 'Indica si puede recibir nuevas asignaciones.'
  )}
  [pscustomobject]@{ Nombre='VEHICULO'; Intro='VehA­culo asociado a un conductor y utilizado para realizar portes.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico del vehA­culo.'
    A 'conductor_id' 'bigint' 'FK' 'Conductor propietario o responsable del vehA­culo.'
    A 'matricula' 'varchar' 'UK' 'MatrA­cula Aºnica del vehA­culo.'
    A 'marca' 'varchar' '' 'Marca del vehA­culo.'
    A 'modelo' 'varchar' '' 'Modelo del vehA­culo.'
    A 'tipo' 'varchar' '' 'Tipo de vehA­culo: furgoneta, rA­gido, trAiler o especial.'
    A 'estado' 'varchar' '' 'Estado operativo del vehA­culo.'
    A 'capacidad_carga_kg' 'int' '' 'Carga mAxima soportada en kilogramos.'
    A 'largo_util_mm' 'int' '' 'Largo Aºtil de carga en milA­metros.'
    A 'ancho_util_mm' 'int' '' 'Ancho Aºtil de carga en milA­metros.'
    A 'alto_util_mm' 'int' '' 'Alto Aºtil de carga en milA­metros.'
    A 'volumen_m3' 'double' '' 'Volumen aproximado disponible en metros cAºbicos.'
  )}
  [pscustomobject]@{ Nombre='PORTE'; Intro='Entidad central del sistema. Representa el servicio logA­stico desde la solicitud hasta la entrega y facturaciA³n.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico del porte.'
    A 'cliente_id' 'bigint' 'FK' 'Cliente que solicita el porte.'
    A 'conductor_id' 'bigint' 'FK' 'Conductor asignado. Puede estar vacA­o hasta la asignaciA³n.'
    A 'origen' 'varchar' '' 'DirecciA³n de origen.'
    A 'destino' 'varchar' '' 'DirecciA³n de destino.'
    A 'ciudad_origen' 'varchar' '' 'Ciudad del punto de origen.'
    A 'ciudad_destino' 'varchar' '' 'Ciudad del punto de destino.'
    A 'latitud_origen' 'double' '' 'Latitud del origen.'
    A 'longitud_origen' 'double' '' 'Longitud del origen.'
    A 'latitud_destino' 'double' '' 'Latitud del destino.'
    A 'longitud_destino' 'double' '' 'Longitud del destino.'
    A 'distancia_km' 'double' '' 'Distancia calculada o estimada del trayecto.'
    A 'distancia_estimada_km' 'double' '' 'Distancia prevista antes de la ejecuciA³n.'
    A 'precio' 'double' '' 'Importe base del porte.'
    A 'ajuste_precio' 'double' '' 'ModificaciA³n manual del precio.'
    A 'motivo_ajuste' 'varchar' '' 'Motivo del ajuste aplicado.'
    A 'descripcion_cliente' 'text' '' 'DescripciA³n de la carga indicada por el cliente.'
    A 'peso_total_kg' 'double' '' 'Peso total aproximado.'
    A 'volumen_total_m3' 'double' '' 'Volumen total aproximado.'
    A 'largo_max_paquete_mm' 'int' '' 'Largo mAximo de un paquete.'
    A 'ancho_max_paquete_mm' 'int' '' 'Ancho mAximo de un paquete.'
    A 'alto_max_paquete_mm' 'int' '' 'Alto mAximo de un paquete.'
    A 'tipo_vehiculo_requerido' 'varchar' '' 'Tipo de vehA­culo necesario para el porte.'
    A 'revision_manual' 'boolean' '' 'Indica si el porte requiere revisiA³n administrativa.'
    A 'motivo_revision' 'text' '' 'ExplicaciA³n de la revisiA³n manual.'
    A 'estado' 'varchar' '' 'Estado del ciclo de vida del porte.'
    A 'fecha_creacion' 'timestamp' '' 'Fecha de creaciA³n de la solicitud.'
    A 'fecha_recogida' 'timestamp' '' 'Fecha de recogida de la mercancA­a.'
    A 'fecha_entrega' 'timestamp' '' 'Fecha de entrega final.'
    A 'firma_entrega_base64' 'text' '' 'Firma digital de entrega.'
    A 'firma_entrega_firmado_por' 'varchar' '' 'Nombre de la persona que firma la entrega.'
    A 'firma_entrega_fecha' 'timestamp' '' 'Fecha en la que se registrA³ la firma.'
    A 'version' 'bigint' '' 'Campo de control de concurrencia.'
  )}
  [pscustomobject]@{ Nombre='FACTURA'; Intro='Documento econA³mico generado a partir de un porte entregado.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico de la factura.'
    A 'porte_id' 'bigint' 'FK, UK' 'Porte facturado. RelaciA³n uno a uno.'
    A 'numero_serie' 'varchar' 'UK' 'NAºmero de factura Aºnico.'
    A 'base_imponible' 'double' '' 'Importe antes de impuestos.'
    A 'iva' 'double' '' 'Importe correspondiente al IVA.'
    A 'importe_total' 'double' '' 'Importe total de la factura.'
    A 'fecha_emision' 'timestamp' '' 'Fecha de emisiA³n.'
    A 'pagada' 'boolean' '' 'Indica si la factura estA pagada.'
    A 'fecha_pago' 'timestamp' '' 'Fecha de pago, si existe.'
    A 'forma_pago' 'varchar' '' 'Forma de pago utilizada.'
    A 'condiciones_pago' 'varchar' '' 'Condiciones pactadas de pago.'
    A 'observaciones' 'text' '' 'Notas adicionales de facturaciA³n.'
  )}
  [pscustomobject]@{ Nombre='ALBARAN'; Intro='Documento derivado del porte entregado. Se representa en el E/R para explicar la entrega, aunque se genera como documento.'; Rows=@(
    A 'porte_id' 'bigint' 'FK' 'Porte del que se deriva el albarAn.'
    A 'origen' 'varchar' '' 'Origen del servicio entregado.'
    A 'destino' 'varchar' '' 'Destino del servicio entregado.'
    A 'fecha_entrega' 'timestamp' '' 'Fecha de entrega reflejada en el documento.'
    A 'firmado_por' 'varchar' '' 'Persona que firmA³ la recepciA³n.'
    A 'firma_entrega' 'text' '' 'Firma digital utilizada como comprobante.'
  )}
  [pscustomobject]@{ Nombre='FOTO_CARGA'; Intro='Evidencia grAfica asociada a un porte.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico de la fotografA­a.'
    A 'porte_id' 'bigint' 'FK' 'Porte al que pertenece la fotografA­a.'
    A 'tipo' 'varchar' '' 'Tipo de imagen o momento de captura.'
    A 'foto_base64' 'text' '' 'Imagen codificada.'
    A 'descripcion' 'varchar' '' 'DescripciA³n opcional.'
    A 'fecha_captura' 'timestamp' '' 'Fecha en la que se adjuntA³ la imagen.'
  )}
  [pscustomobject]@{ Nombre='INCIDENCIA'; Intro='Problema o situaciA³n anA³mala registrada durante la gestiA³n de un porte.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico de la incidencia.'
    A 'porte_id' 'bigint' 'FK' 'Porte afectado.'
    A 'admin_id' 'bigint' 'FK' 'Usuario administrador responsable, si se asigna.'
    A 'titulo' 'varchar' '' 'Resumen breve de la incidencia.'
    A 'descripcion' 'text' '' 'Detalle del problema.'
    A 'estado' 'varchar' '' 'Estado de gestiA³n de la incidencia.'
    A 'severidad' 'varchar' '' 'Gravedad del problema.'
    A 'prioridad' 'varchar' '' 'Prioridad de atenciA³n.'
    A 'fecha_reporte' 'timestamp' '' 'Fecha de creaciA³n.'
    A 'fecha_limite_sla' 'timestamp' '' 'Fecha lA­mite de resoluciA³n.'
    A 'resolucion' 'text' '' 'SoluciA³n aplicada.'
    A 'fecha_resolucion' 'timestamp' '' 'Fecha de cierre o resoluciA³n.'
  )}
  [pscustomobject]@{ Nombre='INCIDENCIA_EVENTO'; Intro='Historial de cambios y acciones realizadas sobre una incidencia.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico del evento.'
    A 'incidencia_id' 'bigint' 'FK' 'Incidencia a la que pertenece el evento.'
    A 'actor_id' 'bigint' 'FK' 'Usuario que realiza la acciA³n.'
    A 'estado_anterior' 'varchar' '' 'Estado antes del cambio.'
    A 'estado_nuevo' 'varchar' '' 'Estado despuA©s del cambio.'
    A 'fecha' 'timestamp' '' 'Fecha del evento.'
    A 'accion' 'varchar' '' 'AcciA³n realizada.'
    A 'comentario' 'text' '' 'Comentario asociado al evento.'
  )}
  [pscustomobject]@{ Nombre='AGENDA_BLOQUEO'; Intro='Bloqueo puntual de disponibilidad de un conductor.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico del bloqueo.'
    A 'conductor_id' 'bigint' 'FK' 'Conductor al que afecta.'
    A 'fecha_inicio' 'timestamp' '' 'Inicio del bloqueo.'
    A 'fecha_fin' 'timestamp' '' 'Fin del bloqueo.'
    A 'tipo' 'varchar' '' 'Tipo de bloqueo.'
    A 'titulo' 'varchar' '' 'TA­tulo o motivo resumido.'
  )}
  [pscustomobject]@{ Nombre='BLOQUEO_RECURRENTE'; Intro='Regla de disponibilidad repetida para un conductor.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico.'
    A 'conductor_id' 'bigint' 'FK' 'Conductor al que pertenece la regla.'
    A 'dia_semana' 'int' '' 'DA­a de la semana afectado.'
    A 'activo' 'boolean' '' 'Indica si la regla estA activa.'
    A 'created_at' 'timestamp' '' 'Fecha de creaciA³n.'
    A 'updated_at' 'timestamp' '' 'Fecha de Aºltima actualizaciA³n.'
  )}
  [pscustomobject]@{ Nombre='TRACKING_SESSION'; Intro='SesiA³n de seguimiento GPS asociada a un porte y conductor.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico de la sesiA³n.'
    A 'conductor_id' 'bigint' 'FK' 'Conductor que emite el seguimiento.'
    A 'porte_id' 'bigint' 'FK' 'Porte asociado a la sesiA³n.'
    A 'status' 'varchar' '' 'Estado de la sesiA³n de tracking.'
    A 'current_phase' 'varchar' '' 'Fase actual: recogida o entrega.'
    A 'started_at' 'timestamp' '' 'Inicio de la sesiA³n.'
    A 'ended_at' 'timestamp' '' 'Fin de la sesiA³n.'
    A 'last_sample_at' 'timestamp' '' 'Fecha de la Aºltima muestra recibida.'
  )}
  [pscustomobject]@{ Nombre='TRACKING_PAUSE'; Intro='Pausa registrada dentro de una sesiA³n de seguimiento.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico de la pausa.'
    A 'session_id' 'bigint' 'FK' 'SesiA³n a la que pertenece.'
    A 'motivo' 'varchar' '' 'Motivo de la pausa.'
    A 'nota' 'text' '' 'ObservaciA³n adicional.'
    A 'started_at' 'timestamp' '' 'Inicio de la pausa.'
    A 'ended_at' 'timestamp' '' 'Fin de la pausa.'
  )}
  [pscustomobject]@{ Nombre='UBICACION'; Intro='Muestra de localizaciA³n GPS recibida durante el seguimiento.'; Rows=@(
    A 'id' 'bigint' 'PK' 'Identificador Aºnico de la muestra.'
    A 'session_id' 'bigint' 'FK' 'SesiA³n de tracking asociada.'
    A 'conductor_id' 'bigint' 'FK' 'Conductor que emite la posiciA³n.'
    A 'porte_id' 'bigint' 'FK' 'Porte asociado, si procede.'
    A 'lat' 'double' '' 'Latitud registrada.'
    A 'lon' 'double' '' 'Longitud registrada.'
    A 'recorded_at' 'timestamp' '' 'Momento en que se registrA³ la posiciA³n.'
    A 'received_at' 'timestamp' '' 'Momento en que el backend recibiA³ la muestra.'
    A 'speed_kph' 'double' '' 'Velocidad estimada.'
    A 'heading_deg' 'int' '' 'Rumbo del desplazamiento.'
  )}
  [pscustomobject]@{ Nombre='PORTE_RECHAZO'; Intro='Tabla auxiliar que registra conductores que rechazaron una oferta de porte.'; Rows=@(
    A 'porte_id' 'bigint' 'FK' 'Porte rechazado.'
    A 'conductor_id' 'bigint' '' 'Conductor que rechazA³ la oferta.'
  )}
)

$body = ''
$body += P 'Tablas de atributos del modelo Entidad-RelaciA³n completo' 'Title'
$body += P 'Este documento recoge las entidades del modelo E/R completo de CargoHub y explica sus atributos principales. Las tablas sirven como apoyo al diagrama completo incluido en el anexo.'
$body += P 'Leyenda: PK = clave primaria; FK = clave forAnea; UK = valor Aºnico.'
foreach ($e in $entities) {
  $body += P $e.Nombre 'Heading1'
  foreach ($row in $e.Rows) { $row.Entidad = $e.Nombre }
  $body += Table $e.Rows
  $body += P ''
}

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
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:sz w:val="22"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:rPr><w:b/><w:sz w:val="32"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:rPr><w:b/><w:sz w:val="28"/></w:rPr></w:style>
  <w:style w:type="table" w:styleId="TableGrid"><w:name w:val="Table Grid"/><w:tblPr><w:tblBorders><w:top w:val="single" w:sz="8"/><w:left w:val="single" w:sz="8"/><w:bottom w:val="single" w:sz="8"/><w:right w:val="single" w:sz="8"/><w:insideH w:val="single" w:sz="8"/><w:insideV w:val="single" w:sz="8"/></w:tblBorders></w:tblPr></w:style>
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

Set-Content -LiteralPath (Join-Path $work 'word\document.xml') -Value $document -Encoding UTF8
if (-not (Test-Path -LiteralPath $template)) {
  Set-Content -LiteralPath (Join-Path $work '[Content_Types].xml') -Value $contentTypes -Encoding UTF8
  Set-Content -LiteralPath (Join-Path $work '_rels\.rels') -Value $rels -Encoding UTF8
  Set-Content -LiteralPath (Join-Path $work 'word\styles.xml') -Value $styles -Encoding UTF8
  Set-Content -LiteralPath (Join-Path $work 'word\_rels\document.xml.rels') -Value $docRels -Encoding UTF8
}

if (Test-Path -LiteralPath $output) { Remove-Item -LiteralPath $output -Force }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $output)
Remove-Item -LiteralPath $work -Recurse -Force

"DOCX generado: $output"


