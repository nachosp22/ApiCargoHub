$ErrorActionPreference = 'Stop'

$output = Join-Path $PSScriptRoot 'TFG_CargoHub_bibliografia_referencias_APA7.docx'
$mdOutput = Join-Path $PSScriptRoot 'TFG_CargoHub_bibliografia_referencias_APA7.md'
$work = Join-Path $env:TEMP ('cargohub_biblio_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work '_rels') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $work 'word\_rels') | Out-Null

function X([string]$s) { if ($null -eq $s) { return '' }; return [System.Security.SecurityElement]::Escape($s) }
function Run([string]$text, [bool]$bold=$false, [bool]$italic=$false, [int]$size=24) {
  $b = if ($bold) { '<w:b/>' } else { '' }
  $i = if ($italic) { '<w:i/>' } else { '' }
  return "<w:r><w:rPr>$b$i<w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:color w:val='000000'/><w:sz w:val='$size'/></w:rPr><w:t>$(X $text)</w:t></w:r>"
}
function P([string]$text) { return "<w:p><w:pPr><w:spacing w:after='120'/></w:pPr>$(Run $text)</w:p>" }
function H1([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading1'/><w:spacing w:before='180' w:after='120'/></w:pPr>$(Run $text $true $false 32)</w:p>" }
function H2([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading2'/><w:spacing w:before='150' w:after='90'/></w:pPr>$(Run $text $true $false 28)</w:p>" }
function H3([string]$text) { return "<w:p><w:pPr><w:pStyle w:val='Heading3'/><w:spacing w:before='120' w:after='70'/></w:pPr>$(Run $text $true $false 24)</w:p>" }
function L([string]$text) { return "<w:p><w:pPr><w:spacing w:after='80'/><w:ind w:left='720' w:hanging='360'/></w:pPr>$(Run ('- ' + $text))</w:p>" }
function Ref([string]$text) { return "<w:p><w:pPr><w:spacing w:after='120'/><w:ind w:left='720' w:hanging='360'/></w:pPr>$(Run $text)</w:p>" }

$retrieved = '15 de mayo de 2026'

$teoricas = @(
  'Ballou, R. H. (2004). Business logistics/supply chain management: Planning, organizing, and controlling the supply chain (5th ed.). Pearson Prentice Hall.',
  'Chopra, S. (2019). Supply chain management: Strategy, planning, and operation (7th ed.). Pearson.',
  'Christopher, M. (2016). Logistics & supply chain management (5th ed.). Pearson.',
  'Council of Supply Chain Management Professionals. (n.d.). CSCMP supply chain management definitions and glossary. Recuperado el ' + $retrieved + ', de https://cscmp.org/CSCMP/Educate/SCM_Definitions_and_Glossary_of_Terms.aspx',
  'European Commission. (2020). Sustainable and smart mobility strategy: Putting European transport on track for the future. https://transport.ec.europa.eu/transport-themes/mobility-strategy_en',
  'Eurostat. (n.d.). Freight transport statistics. Recuperado el ' + $retrieved + ', de https://ec.europa.eu/eurostat/statistics-explained/index.php?title=Freight_transport_statistics',
  'Ministerio de Transportes y Movilidad Sostenible. (n.d.). Observatorio del transporte y la logística en España. Recuperado el ' + $retrieved + ', de https://observatoriotransporte.mitma.gob.es/',
  'Rushton, A., Croucher, P., & Baker, P. (2017). The handbook of logistics and distribution management (6th ed.). Kogan Page.',
  'World Bank. (n.d.). Logistics Performance Index. Recuperado el ' + $retrieved + ', de https://lpi.worldbank.org/'
)

$ingenieria = @(
  'Booch, G., Rumbaugh, J., & Jacobson, I. (2005). The unified modeling language user guide (2nd ed.). Addison-Wesley.',
  'Fowler, M. (2003). UML distilled: A brief guide to the standard object modeling language (3rd ed.). Addison-Wesley.',
  'Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). Design patterns: Elements of reusable object-oriented software. Addison-Wesley.',
  'Object Management Group. (2017). OMG unified modeling language specification, version 2.5.1. https://www.omg.org/spec/UML/2.5.1/',
  'Pressman, R. S., & Maxim, B. R. (2020). Software engineering: A practitioner''s approach (9th ed.). McGraw-Hill.',
  'Sommerville, I. (2016). Software engineering (10th ed.). Pearson.'
)

$tecnicas = @(
  'Android Developers. (n.d.). Android developers documentation. Recuperado el ' + $retrieved + ', de https://developer.android.com/docs',
  'Android Developers. (n.d.). Android app bundles and APKs. Recuperado el ' + $retrieved + ', de https://developer.android.com/guide/app-bundle',
  'Android Developers. (n.d.). Room persistence library. Recuperado el ' + $retrieved + ', de https://developer.android.com/training/data-storage/room',
  'Apache Maven Project. (n.d.). Apache Maven documentation. Recuperado el ' + $retrieved + ', de https://maven.apache.org/guides/',
  'Axios. (n.d.). Axios documentation. Recuperado el ' + $retrieved + ', de https://axios-http.com/docs/intro',
  'Chart.js. (n.d.). Chart.js documentation. Recuperado el ' + $retrieved + ', de https://www.chartjs.org/docs/latest/',
  'Cloudinary. (n.d.). Cloudinary documentation. Recuperado el ' + $retrieved + ', de https://cloudinary.com/documentation',
  'Diagrams.net. (n.d.). Diagrams.net documentation. Recuperado el ' + $retrieved + ', de https://www.diagrams.net/doc/',
  'Electron. (n.d.). Electron documentation. Recuperado el ' + $retrieved + ', de https://www.electronjs.org/docs/latest/',
  'Electron Builder. (n.d.). Electron Builder documentation. Recuperado el ' + $retrieved + ', de https://www.electron.build/',
  'Glide. (n.d.). Glide documentation. Recuperado el ' + $retrieved + ', de https://bumptech.github.io/glide/',
  'Google. (n.d.). Material Design. Recuperado el ' + $retrieved + ', de https://m3.material.io/',
  'Hibernate. (n.d.). Hibernate ORM documentation. Recuperado el ' + $retrieved + ', de https://hibernate.org/orm/documentation/',
  'IETF. (2015). JSON Web Token (JWT) (RFC 7519). https://www.rfc-editor.org/rfc/rfc7519',
  'Java. (n.d.). Java documentation. Oracle. Recuperado el ' + $retrieved + ', de https://docs.oracle.com/en/java/',
  'JUnit. (n.d.). JUnit 4 documentation. Recuperado el ' + $retrieved + ', de https://junit.org/junit4/',
  'Leaflet. (n.d.). Leaflet documentation. Recuperado el ' + $retrieved + ', de https://leafletjs.com/reference.html',
  'LibrePDF. (n.d.). OpenPDF documentation. Recuperado el ' + $retrieved + ', de https://github.com/LibrePDF/OpenPDF',
  'Lombok Project. (n.d.). Project Lombok features. Recuperado el ' + $retrieved + ', de https://projectlombok.org/features/',
  'MapLibre. (n.d.). MapLibre GL JS documentation. Recuperado el ' + $retrieved + ', de https://maplibre.org/maplibre-gl-js/docs/',
  'Mockito. (n.d.). Mockito documentation. Recuperado el ' + $retrieved + ', de https://site.mockito.org/',
  'OkHttp. (n.d.). OkHttp documentation. Recuperado el ' + $retrieved + ', de https://square.github.io/okhttp/',
  'OpenStreetMap Foundation. (n.d.). OpenStreetMap. Recuperado el ' + $retrieved + ', de https://www.openstreetmap.org/',
  'osmdroid. (n.d.). osmdroid documentation. Recuperado el ' + $retrieved + ', de https://github.com/osmdroid/osmdroid',
  'OWASP Foundation. (2021). OWASP top 10: The ten most critical web application security risks. https://owasp.org/Top10/',
  'Pinia. (n.d.). Pinia documentation. Recuperado el ' + $retrieved + ', de https://pinia.vuejs.org/',
  'PlantUML. (n.d.). PlantUML documentation. Recuperado el ' + $retrieved + ', de https://plantuml.com/',
  'PostgreSQL Global Development Group. (n.d.). PostgreSQL documentation. Recuperado el ' + $retrieved + ', de https://www.postgresql.org/docs/',
  'PrimeTek. (n.d.). PrimeVue documentation. Recuperado el ' + $retrieved + ', de https://primevue.org/',
  'Retrofit. (n.d.). Retrofit documentation. Recuperado el ' + $retrieved + ', de https://square.github.io/retrofit/',
  'Spring. (n.d.). Spring Boot reference documentation. Recuperado el ' + $retrieved + ', de https://docs.spring.io/spring-boot/',
  'Spring. (n.d.). Spring Data JPA reference documentation. Recuperado el ' + $retrieved + ', de https://docs.spring.io/spring-data/jpa/reference/',
  'Spring. (n.d.). Spring Framework documentation. Recuperado el ' + $retrieved + ', de https://docs.spring.io/spring-framework/reference/',
  'Spring. (n.d.). Spring Security reference documentation. Recuperado el ' + $retrieved + ', de https://docs.spring.io/spring-security/reference/',
  'Tailwind Labs. (n.d.). Tailwind CSS documentation. Recuperado el ' + $retrieved + ', de https://tailwindcss.com/docs',
  'TypeScript. (n.d.). TypeScript documentation. Recuperado el ' + $retrieved + ', de https://www.typescriptlang.org/docs/',
  'Vite. (n.d.). Vite documentation. Recuperado el ' + $retrieved + ', de https://vite.dev/guide/',
  'Vitest. (n.d.). Vitest documentation. Recuperado el ' + $retrieved + ', de https://vitest.dev/guide/',
  'Vue.js. (n.d.). Vue.js documentation. Recuperado el ' + $retrieved + ', de https://vuejs.org/guide/',
  'Vue Router. (n.d.). Vue Router documentation. Recuperado el ' + $retrieved + ', de https://router.vuejs.org/',
  'Vue I18n. (n.d.). Vue I18n documentation. Recuperado el ' + $retrieved + ', de https://vue-i18n.intlify.dev/'
)

$citas = @(
  'Para justificar la importancia de la logística y la cadena de suministro: (Christopher, 2016), (Chopra, 2019) o (Rushton et al., 2017).',
  'Para hablar de transporte de mercancías y contexto europeo/español: (European Commission, 2020), (Eurostat, n.d.) y (Ministerio de Transportes y Movilidad Sostenible, n.d.).',
  'Para justificar trazabilidad, eficiencia y seguimiento: (World Bank, n.d.) y (Council of Supply Chain Management Professionals, n.d.).',
  'Para justificar UML y diagramas de casos de uso/clases: (Booch et al., 2005), (Fowler, 2003) y (Object Management Group, 2017).',
  'Para justificar arquitectura web/API y desarrollo software: (Sommerville, 2016) y (Pressman & Maxim, 2020).',
  'Para justificar patrones y separación de responsabilidades: (Gamma et al., 1994).',
  'Para justificar API REST con Spring Boot: (Spring, n.d.-a), (Spring, n.d.-b) y (Spring, n.d.-c).',
  'Para justificar seguridad, roles y autenticación: (Spring, n.d.-d), (IETF, 2015) y (OWASP Foundation, 2021).',
  'Para justificar persistencia relacional: (PostgreSQL Global Development Group, n.d.), (Hibernate, n.d.) y (Spring, n.d.-b).',
  'Para justificar frontend web y escritorio con Vue: (Vue.js, n.d.), (Vue Router, n.d.), (Pinia, n.d.) y (Vite, n.d.).',
  'Para justificar escritorio multiplataforma: (Electron, n.d.) y (Electron Builder, n.d.).',
  'Para justificar app móvil Android: (Android Developers, n.d.-a), (Retrofit, n.d.) y (OkHttp, n.d.).',
  'Para justificar mapas y localización: (Leaflet, n.d.), (MapLibre, n.d.), (OpenStreetMap Foundation, n.d.) y (osmdroid, n.d.).',
  'Para justificar pruebas: (JUnit, n.d.), (Mockito, n.d.) y (Vitest, n.d.).',
  'Para justificar diagramas usados en la documentación: (Diagrams.net, n.d.) y (PlantUML, n.d.).'
)

$body = ''
$body += H1 '11. Bibliografía y referencias'
$body += P 'Este documento reúne una propuesta amplia de bibliografía y referencias para la memoria del proyecto CargoHub. Se incluyen fuentes para el marco teórico, documentación técnica oficial, herramientas utilizadas en el desarrollo y referencias útiles para citar diagramas, seguridad, bases de datos, frontend, backend, móvil y aplicación de escritorio.'
$body += P 'La lista está preparada para ser revisada e integrada en el apartado de bibliografía, referencias web o anexo de referencias de la memoria. Las referencias se han redactado siguiendo el estilo APA 7 de forma práctica para un Trabajo de Fin de Grado.'

$body += H1 'Cómo usar estas referencias dentro de la memoria'
$body += P 'En APA 7, las referencias completas se colocan al final del documento. Dentro del texto se usan citas breves entre paréntesis o integradas en la frase.'
$body += P 'Ejemplo de cita parentética: La trazabilidad permite mejorar el control del proceso logístico y reducir incertidumbre en la cadena de suministro (Christopher, 2016).'
$body += P 'Ejemplo de cita narrativa: Según Sommerville (2016), el diseño del software debe facilitar el mantenimiento y la evolución del sistema.'
$body += P 'En fuentes web sin fecha concreta se utiliza n.d., que significa sin fecha. En el documento final se puede mantener así o adaptarlo a “s. f.” si el centro exige la traducción completa al español.'

$body += H1 'Citas recomendadas por apartado de la memoria'
$body += H2 'Marco teórico sobre logística y transporte'
$body += P 'Para explicar la importancia de la logística, la gestión de la cadena de suministro, el transporte de mercancías y la necesidad de digitalización, se pueden utilizar principalmente las fuentes de Christopher, Chopra, Rushton, la Comisión Europea, Eurostat, el Banco Mundial y el Observatorio del Transporte y la Logística en España.'
foreach ($c in $citas[0..2]) { $body += L $c }
$body += P 'Ejemplo de redacción: La logística moderna no se limita al movimiento físico de mercancías, sino que integra planificación, trazabilidad, coordinación de recursos y gestión de información entre los actores de la cadena de suministro (Christopher, 2016; Rushton et al., 2017). En este contexto, herramientas digitales como CargoHub permiten centralizar la gestión de portes, conductores, clientes y documentación asociada.'

$body += H2 'Metodología, análisis y diseño software'
$body += P 'Para justificar los diagramas UML, los casos de uso, el modelo de clases y las decisiones de diseño, conviene citar a Booch, Rumbaugh y Jacobson, Fowler, OMG, Pressman y Sommerville.'
foreach ($c in $citas[3..5]) { $body += L $c }
$body += P 'Ejemplo de redacción: El uso de UML permite representar el sistema desde diferentes perspectivas, facilitando la comprensión de requisitos, actores, entidades y relaciones antes de su implementación (Booch et al., 2005; Fowler, 2003).'

$body += H2 'Backend, API REST, seguridad y persistencia'
$body += P 'Para justificar la parte de servidor, API REST, seguridad JWT, roles, validación y persistencia relacional, se pueden citar las documentaciones oficiales de Spring Boot, Spring Security, Spring Data JPA, Hibernate, PostgreSQL, OWASP y el RFC 7519 de JWT.'
foreach ($c in $citas[6..9]) { $body += L $c }
$body += P 'Ejemplo de redacción: El backend se ha desarrollado con Spring Boot para estructurar la API REST y separar responsabilidades entre controladores, servicios, repositorios y entidades persistentes. La autenticación mediante tokens JWT permite transmitir información de sesión de forma compacta entre cliente y servidor (IETF, 2015; Spring, n.d.).'

$body += H2 'Frontend web, escritorio y móvil'
$body += P 'Para justificar las interfaces de usuario se pueden citar Vue.js, Vue Router, Pinia, Vite, PrimeVue, Electron, Electron Builder y la documentación oficial de Android. También pueden citarse Retrofit y OkHttp para la comunicación HTTP en la app móvil.'
foreach ($c in $citas[10..12]) { $body += L $c }
$body += P 'Ejemplo de redacción: La interfaz web y la aplicación de escritorio comparten una base tecnológica basada en Vue, permitiendo construir componentes reutilizables y organizar la navegación mediante rutas y estado centralizado (Pinia, n.d.; Vue.js, n.d.; Vue Router, n.d.).'

$body += H2 'Mapas, geolocalización, pruebas y documentación'
$body += P 'Para justificar mapas, tracking, pruebas y herramientas de documentación gráfica se pueden usar Leaflet, MapLibre, OpenStreetMap, osmdroid, JUnit, Mockito, Vitest, diagrams.net y PlantUML.'
foreach ($c in $citas[13..14]) { $body += L $c }
$body += P 'Ejemplo de redacción: Las herramientas de diagramación permiten representar visualmente los modelos del sistema, facilitando la revisión y comunicación de la arquitectura y del diseño funcional (Diagrams.net, n.d.; PlantUML, n.d.).'

$body += H1 '11.1. Bibliografía'
$body += P 'En este apartado se recogen las fuentes bibliográficas principales utilizadas para fundamentar el marco teórico, la logística, la cadena de suministro, el transporte de mercancías, UML y la ingeniería del software. Se trata principalmente de libros, especificaciones y fuentes institucionales.'
$body += H2 'Logística, transporte y cadena de suministro'
foreach ($r in $teoricas) { $body += Ref $r }

$body += H2 'Ingeniería del software, UML y diseño'
foreach ($r in $ingenieria) { $body += Ref $r }

$body += H1 '11.2. Referencias a texto y/o web de ayuda'
$body += P 'Estas referencias corresponden a la documentación oficial de tecnologías, librerías y herramientas relacionadas con CargoHub. Se pueden usar en el apartado de referencias web, bibliografía técnica o anexo de recursos utilizados.'
foreach ($r in $tecnicas) { $body += Ref $r }

$body += H1 'Referencias agrupadas por tecnología del proyecto'
$body += H2 'Backend y base de datos'
$body += L 'Spring Boot: estructura de la API REST, configuración y arranque de la aplicación.'
$body += L 'Spring Security: autenticación, autorización y control de acceso por roles.'
$body += L 'Spring Data JPA e Hibernate: persistencia de entidades, repositorios y relaciones con la base de datos.'
$body += L 'PostgreSQL: base de datos relacional utilizada para almacenar usuarios, portes, conductores, clientes, incidencias, facturas y tracking.'
$body += L 'JWT: mecanismo de token utilizado para representar la sesión autenticada.'

$body += H2 'Aplicación web y escritorio'
$body += L 'Vue.js: construcción de interfaces reactivas para portal web y escritorio.'
$body += L 'Vue Router: navegación entre pantallas.'
$body += L 'Pinia: gestión de estado de la aplicación.'
$body += L 'PrimeVue y PrimeIcons: componentes visuales e iconografía.'
$body += L 'Vite y TypeScript: tooling, desarrollo y compilación del frontend.'
$body += L 'Electron y Electron Builder: empaquetado de la aplicación de escritorio y generación del ejecutable.'
$body += L 'Chart.js y Vue Chart.js: representación de estadísticas y gráficos.'
$body += L 'MapLibre GL JS y Leaflet: visualización de mapas y elementos geográficos.'

$body += H2 'Aplicación móvil Android'
$body += L 'Android SDK y Android Developers: base de desarrollo de la aplicación móvil.'
$body += L 'Retrofit y OkHttp: comunicación HTTP con la API REST.'
$body += L 'Room: persistencia local en el dispositivo cuando sea necesaria.'
$body += L 'Glide: carga y presentación de imágenes.'
$body += L 'osmdroid y OpenStreetMap: mapas en la aplicación móvil.'
$body += L 'Material Design: criterios generales de diseño de interfaz móvil.'

$body += H2 'Pruebas y documentación'
$body += L 'JUnit y Mockito: pruebas unitarias del backend y componentes Java.'
$body += L 'Spring Security Test: pruebas relacionadas con seguridad y permisos.'
$body += L 'Vitest y Vue Test Utils: pruebas del frontend Vue.'
$body += L 'Diagrams.net: elaboración de diagramas visuales para la memoria.'
$body += L 'PlantUML: generación textual de diagramas UML.'

$body += H1 'Texto de apoyo para introducir la bibliografía en la memoria'
$body += P 'La bibliografía utilizada combina fuentes académicas y documentación técnica oficial. Las fuentes académicas permiten fundamentar la importancia de la logística, la cadena de suministro, la trazabilidad y la digitalización de procesos. Por otro lado, la documentación técnica oficial permite justificar las tecnologías empleadas en la implementación del sistema.'
$body += P 'En el caso de CargoHub, el marco teórico se apoya en bibliografía sobre logística y gestión de la cadena de suministro, mientras que el desarrollo técnico se respalda mediante documentación oficial de Spring Boot, PostgreSQL, Vue, Android, Electron y otras herramientas utilizadas durante el proyecto.'
$body += P 'Esta separación permite diferenciar claramente entre fuentes conceptuales, que explican el problema y el contexto, y fuentes técnicas, que justifican las decisiones de implementación.'

$body += H1 'Notas de revisión'
$body += L 'Si el centro exige “s. f.” en lugar de “n.d.”, se puede reemplazar en todas las referencias web.'
$body += L 'Si se insertan capturas o imágenes externas, conviene añadir una referencia específica a su fuente en el anexo de imágenes.'
$body += L 'Si las imágenes son capturas propias de la aplicación, se pueden indicar como elaboración propia y no necesitan una fuente externa.'
$body += L 'Si se exportan diagramas propios desde diagrams.net o PlantUML, pueden citarse como elaboración propia y referenciar la herramienta en bibliografía técnica.'

$document = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body>$body<w:sectPr><w:pgSz w:w='11906' w:h='16838'/><w:pgMar w:top='850' w:right='850' w:bottom='850' w:left='850'/></w:sectPr></w:body></w:document>"
$styles = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><w:styles xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:style w:type='paragraph' w:default='1' w:styleId='Normal'><w:name w:val='Normal'/><w:rPr><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:color w:val='000000'/><w:sz w:val='24'/></w:rPr></w:style><w:style w:type='paragraph' w:styleId='Heading1'><w:name w:val='heading 1'/><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='32'/></w:rPr></w:style><w:style w:type='paragraph' w:styleId='Heading2'><w:name w:val='heading 2'/><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='28'/></w:rPr></w:style><w:style w:type='paragraph' w:styleId='Heading3'><w:name w:val='heading 3'/><w:rPr><w:b/><w:rFonts w:ascii='Times New Roman' w:hAnsi='Times New Roman'/><w:sz w:val='24'/></w:rPr></w:style></w:styles>"
$ct = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Types xmlns='http://schemas.openxmlformats.org/package/2006/content-types'><Default Extension='rels' ContentType='application/vnd.openxmlformats-package.relationships+xml'/><Default Extension='xml' ContentType='application/xml'/><Override PartName='/word/document.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml'/><Override PartName='/word/styles.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml'/></Types>"
$rels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument' Target='word/document.xml'/></Relationships>"
$docRels = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles' Target='styles.xml'/></Relationships>"

Set-Content -LiteralPath (Join-Path $work '[Content_Types].xml') -Value $ct -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work '_rels\.rels') -Value $rels -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\document.xml') -Value $document -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\styles.xml') -Value $styles -Encoding UTF8
Set-Content -LiteralPath (Join-Path $work 'word\_rels\document.xml.rels') -Value $docRels -Encoding UTF8
if (Test-Path -LiteralPath $output) {
  try { Remove-Item -LiteralPath $output -Force } catch { $output = Join-Path $PSScriptRoot ('TFG_CargoHub_bibliografia_referencias_APA7_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.docx') }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($work, $output)
Remove-Item -LiteralPath $work -Recurse -Force

$md = @()
$md += '# Bibliografía y referencias APA 7'
$md += ''
$md += '## Bibliografía para marco teórico'
$md += ''
$teoricas | ForEach-Object { $md += '- ' + $_ }
$md += ''
$md += '## Ingeniería del software, UML y diseño'
$md += ''
$ingenieria | ForEach-Object { $md += '- ' + $_ }
$md += ''
$md += '## Referencias técnicas y documentación oficial'
$md += ''
$tecnicas | ForEach-Object { $md += '- ' + $_ }
$md += ''
$md += '## Citas sugeridas'
$md += ''
$citas | ForEach-Object { $md += '- ' + $_ }
Set-Content -LiteralPath $mdOutput -Value ($md -join "`n") -Encoding UTF8

"DOCX generado: $output"
"MD generado: $mdOutput"
