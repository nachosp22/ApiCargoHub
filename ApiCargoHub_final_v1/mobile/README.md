# ApiCargoHub Mobile (Android Java MVP)

MVP nativo Android en Java para ApiCargoHub, preparado para abrir en Android Studio y evolucionar sobre el spec del proyecto.

## Requisitos

- Java 17 (alineado con la API backend)
- Android Studio Jellyfish o superior recomendado
- Android SDK Platform 35
- Android SDK Build-Tools recientes (instaladas desde Android Studio)

## Estructura

- `mobile/` proyecto Gradle Android
- `mobile/app/` modulo Android principal
- `mobile/app/src/main/java/com/cargohub/mobile/` codigo Java
- `mobile/app/src/main/res/` layouts, tema, recursos y navegacion

## Como abrir en Android Studio

1. Abrir Android Studio.
2. Seleccionar `Open`.
3. Elegir la carpeta `mobile/`.
4. Esperar a que sincronice Gradle.
5. Si lo pide, instalar SDK/Build-Tools sugeridos por el IDE.

## Como ejecutar

1. Crear o iniciar un emulador Android (API 26+).
2. Seleccionar configuracion `app`.
3. Ejecutar desde el boton Run.

Tambien por terminal:

```bash
cd mobile
./gradlew tasks
./gradlew installDebug
```

## Configurar API local

El base URL se inyecta en `BuildConfig.API_BASE_URL`.

### Opcion A: `local.properties` (recomendada para local)

Crear o editar `mobile/local.properties`:

```properties
sdk.dir=/ruta/a/Android/Sdk
api.base.url=http://10.0.2.2:8080/
```

Notas:
- En emulador Android, `10.0.2.2` apunta al host local.
- En dispositivo fisico, `10.0.2.2` NO funciona: usa la IP LAN del PC (ej. `http://192.168.1.50:8080/`).
- Mantener slash final `/` en la URL.

### Opcion B: propiedad Gradle por comando

```bash
./gradlew assembleDebug -PAPI_BASE_URL=http://10.0.2.2:8080/
```

## Navegacion y pantallas actuales

- `LoginActivity` restaura sesion valida y redirige a `MainActivity`.
- `MainActivity` usa drawer navigation para abrir las pantallas operativas del conductor.
- Flujos disponibles:
  - inicio/resumen
  - ofertas y detalle de oferta
  - mis viajes y detalle/ejecucion de viaje
  - incidencias activas, historial y alta vinculada a un porte
  - perfil
  - agenda
  - vehiculos
  - tracking

## Capa API movil

Implementado con Retrofit + OkHttp sobre los contratos actuales del backend:

- Auth: `POST /api/auth/login`
- Perfil conductor: `GET /api/conductores/{id}`
- Perfil conductor: `PUT /api/conductores/{id}`, `DELETE /api/conductores/{id}`
- Agenda: `GET/POST /api/conductores/{id}/agenda`, `DELETE /api/conductores/agenda/{bloqueoId}`
- Vehiculos: `GET/POST /api/conductores/{conductorId}/vehiculos`, activacion/desactivacion por conductor
- Ofertas y viajes: `GET /api/portes/ofertas/{conductorId}`, `GET /api/portes/conductor/{conductorId}`, `GET /api/portes/{porteId}`
- Acciones de viaje: `POST /api/portes/{porteId}/aceptar`, `POST /api/portes/{porteId}/rechazar`, `PUT /api/portes/{porteId}/estado`
- Incidencias: `GET /api/incidencias/{id}`, `GET /api/incidencias/{id}/historial`, `GET /api/incidencias/porte/{porteId}`, `POST /api/incidencias?porteId=` con payload JSON
- Tracking: `POST /api/v1/tracking/drivers/{driverId}/locations` con fallback a `POST /api/conductores/{id}/ubicacion`

Clases principales:

- `network/ApiClient.java`
- `network/ApiService.java`
- `network/AuthInterceptor.java`
- `data/AuthRepository.java`
- `data/PorteRepository.java`
- `data/AgendaRepository.java`
- `data/VehiculoRepository.java`
- `data/IncidenciaRepository.java`
- `data/TrackingRepository.java`
- `data/model/*`

## Validacion en este entorno

Ejecutado en CLI:

- `./gradlew testDebugUnitTest --tests "com.cargohub.mobile.data.RepositoryIntegrationTest"` -> OK
- `./gradlew testDebugUnitTest --tests "com.cargohub.mobile.network.AuthInterceptorTest" --tests "com.cargohub.mobile.data.RepositorySupportTest" --tests "com.cargohub.mobile.data.PorteRepositoryTest"` -> OK

Cobertura automatizada agregada para:

- login y parseo de sesion
- agenda
- vehiculos
- incidencias JSON
- aceptar oferta y refresco de detalle
- transicion de viaje y refresco de detalle
- payload de tracking
- edicion y baja de perfil conductor
- detalle + historial de incidencias
- fallback de tracking realtime a endpoint legado de ubicacion

Pendiente fuera de este entorno:

- verificacion manual en emulador/dispositivo del flujo completo `login -> ofertas -> viaje -> incidencia -> tracking`
- confirmacion operativa de permisos GPS y comportamiento en background/foreground

Nota conocida:

- Gradle muestra advertencia porque AGP `8.5.2` aun no declara soporte probado para `compileSdk 35`, aunque la compilacion local de este modulo pasa.

## Flujo de autenticacion implementado

- `LoginActivity` es launcher de la app.
- Login real contra `POST /api/auth/login` (form-url-encoded).
- `SessionManager` persiste token, rol, `conductorId` y expiracion.
- `AuthInterceptor` toma token persistido para requests autenticados.
- Si existe sesion valida al abrir la app, se salta Login y abre `MainActivity`.
- Logout en Perfil limpia sesion y redirige a Login.
- Solo `401` invalida sesion automaticamente desde `AuthInterceptor` cuando hay token invalido/expirado.
- `403` mantiene la sesion y se muestra como falta de permisos para evitar logout incorrecto.

## QA manual E2E conductor y alcance de demo recomendado

### Alcance recomendado de demo mobile

Flujo objetivo para demo funcional de conductor (punta a punta):

`login -> ofertas -> detalle -> aceptar/rechazar -> viaje -> incidencias -> tracking -> logout`

Incluye validacion de estados de carga/error vacio, persistencia de sesion, transiciones de viaje y operacion minima de incidencias/tracking en entorno real (emulador o dispositivo).

### Precondiciones

1. Configurar `mobile/local.properties` con `sdk.dir` y `api.base.url`.
2. Levantar backend accesible desde Android (`10.0.2.2` en emulador o IP LAN en dispositivo).
3. Contar con usuario conductor valido con ofertas y al menos un porte operable.
4. Tener un viaje en estado que permita accion de avance (`ASIGNADO` o equivalente habilitado por backend).
5. Ejecutar app en emulador/dispositivo con red estable y permiso de ubicacion habilitable.
6. Preparar evidencia de ejecucion (capturas/video + timestamp + ID de porte/incidencia cuando aplique).

### Checklist QA E2E conductor (criterio OK/FAIL)

| Paso | Verificacion | OK | FAIL |
|---|---|---|---|
| 1. Login | Ingreso con credenciales validas desde app en frio | Navega a `MainActivity`, sesion queda activa y no hay crash | No autentica con credenciales validas, queda bloqueado o hay cierre inesperado |
| 2. Ofertas | Bandeja de ofertas desde menu `Ofertas` | Lista responde, permite abrir detalle y maneja `loading/empty/error` sin romper flujo | Lista congelada, navegacion rota o estados inconsistentes |
| 3. Detalle de oferta | Visualizacion de detalle y CTAs operativos | Se ve informacion clave y botones de accion en estado coherente | No carga detalle, CTAs mal habilitados o datos clave ausentes |
| 4. Aceptar/rechazar | Ejecutar una accion sobre oferta operable | Backend confirma accion y UI refleja resultado (estado o mensaje) | Accion no impacta en backend/UI o deja pantalla en estado inconsistente |
| 5. Viaje | Abrir viaje asociado y avanzar estado si aplica | Detalle refleja estado actualizado y permite continuar operacion | Estado no cambia o se pierde consistencia entre lista/detalle |
| 6. Incidencias | Crear incidencia y consultar activas/historial | Alta exitosa, incidencia visible y recuperable por contexto de porte | No crea incidencia, no aparece en listas o falla lectura de historial |
| 7. Tracking | Iniciar/pausar tracking con permiso de ubicacion | Estado cambia correctamente y se envia ubicacion o error accionable | No solicita permiso cuando corresponde, no cambia estado o falla sin feedback |
| 8. Logout | Cerrar sesion desde `Perfil` | Limpia sesion, vuelve a login y bloquea acceso autenticado sin relogin | Permite volver a pantallas autenticadas o mantiene token invalido |

### Matriz de ejecucion para QA

| Paso | Estado (OK/FAIL/PENDIENTE) | Evidencia | Notas |
|---|---|---|---|
| 1. Login | PENDIENTE | - | - |
| 2. Ofertas | PENDIENTE | - | - |
| 3. Detalle de oferta | PENDIENTE | - | - |
| 4. Aceptar/rechazar oferta | PENDIENTE | - | - |
| 5. Viaje | PENDIENTE | - | - |
| 6. Incidencias | PENDIENTE | - | - |
| 7. Tracking | PENDIENTE | - | - |
| 8. Logout | PENDIENTE | - | - |

Nota: esta matriz queda preparada para cierre manual en emulador/dispositivo real. No se marca OK/FAIL desde CLI.

### Riesgos y puntos fragiles del flujo

- Dependencia alta de datos semilla: sin ofertas/portes operables no se puede validar el flujo completo.
- Diferencias de entorno (`10.0.2.2` vs IP LAN) pueden simular fallas de API que no son bugs de app.
- Permisos y politicas de ubicacion (foreground/background) afectan tracking y pueden variar por version Android.
- Latencia del backend puede exponer estados intermedios (carga duplicada, feedback tardio en aceptar/rechazar).
- Reglas de negocio del backend (estados permitidos) pueden bloquear avance de viaje aun con UI correcta.

### Siguiente tanda de fixes sugeridos

1. Normalizar mensajes de error por etapa del flujo E2E (login, acciones de oferta, viaje, tracking) para diagnostico mas rapido.
2. Agregar IDs funcionales visibles en UI de QA (porte/incidencia) para trazabilidad entre app y backend.
3. Endurecer manejo de reintento y estados de carga en ofertas/detalles para evitar doble accion por taps repetidos.
4. Registrar eventos minimos de auditoria en mobile (accion + timestamp + resultado) para soporte de demo y triage.
5. Definir dataset de demo estable (usuario, oferta, viaje, incidencia) versionado para reproducibilidad entre equipos.

## Limitaciones conocidas

- La verificacion E2E con emulador no se puede automatizar desde este entorno CLI y debe completarse antes de release.
