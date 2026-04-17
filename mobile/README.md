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

## QA manual E2E conductor (opcion 1)

### Preparacion

1. Configurar `mobile/local.properties` con `sdk.dir` y `api.base.url`.
2. Levantar backend local (ejemplo: `http://10.0.2.2:8080/` para emulador).
3. Ejecutar app en emulador/dispositivo Android con permisos de red y ubicacion.

### Casos E2E con pasos y resultado esperado

#### Caso 1 - Login conductor

- Pasos:
  1. Abrir app en frio.
  2. Validar errores de formulario (`email` vacio/invalido, `password` vacia).
  3. Ingresar credenciales validas y tocar `Iniciar sesion`.
- Resultado esperado: se ven validaciones en contexto, aparece loading y se navega a `MainActivity` con sesion activa.

#### Caso 2 - Ofertas

- Pasos:
  1. Abrir menu `Ofertas`.
  2. Validar estados `loading`, `empty` o `error` segun datos.
  3. Abrir una oferta desde tarjeta o CTA.
- Resultado esperado: lista usable, estados consistentes y navegacion correcta a detalle de oferta.

#### Caso 3 - Viajes

- Pasos:
  1. Abrir `Mis viajes`.
  2. Revisar modos activo/proximo/historico.
  3. Abrir un viaje y volver a lista.
- Resultado esperado: listas responden sin bloqueo, preservan navegacion y muestran datos del backend.

#### Caso 4 - Detalle de viaje

- Pasos:
  1. Desde `Mis viajes`, abrir detalle de viaje en estado operable.
  2. Ejecutar accion de estado (`ASIGNADO -> EN_TRANSITO -> ENTREGADO`) si aplica.
  3. Confirmar refresco del detalle luego de cada accion.
- Resultado esperado: la accion valida cambia estado remoto/local y refleja el nuevo estado en pantalla.

#### Caso 5 - Incidencias

- Pasos:
  1. Abrir opciones de incidencias (activas/historial/nueva).
  2. Crear incidencia vinculada a un porte activo.
  3. Verificar detalle e historial de la incidencia.
- Resultado esperado: alta exitosa, visibilidad en contexto del viaje y datos consistentes en historial.

#### Caso 6 - Perfil conductor

- Pasos:
  1. Abrir `Perfil`.
  2. Verificar carga de datos del conductor.
  3. Editar un dato permitido y guardar (si el entorno lo permite).
- Resultado esperado: se muestran datos principales y cambios persistidos sin romper sesion.

#### Caso 7 - Agenda

- Pasos:
  1. Desde `Perfil`, abrir `Agenda`.
  2. Crear bloqueo/disponibilidad.
  3. Eliminar el bloqueo creado.
- Resultado esperado: alta y baja impactan en la lista de agenda del conductor.

#### Caso 8 - Vehiculo

- Pasos:
  1. Desde `Perfil`, abrir `Vehiculos`.
  2. Alta de vehiculo de prueba.
  3. Activar/desactivar vehiculo.
- Resultado esperado: operaciones reflejan estado real del vehiculo para el conductor.

#### Caso 9 - Tracking

- Pasos:
  1. Abrir `Tracking` con viaje activo.
  2. Conceder permiso de ubicacion si se solicita.
  3. Iniciar/pausar tracking y observar estado.
- Resultado esperado: estado de tracking cambia correctamente y envia ubicacion o muestra error accionable.

#### Caso 10 - Logout

- Pasos:
  1. Ir a `Perfil`.
  2. Tocar `Cerrar sesion`.
  3. Intentar volver a pantalla autenticada sin relogin.
- Resultado esperado: sesion invalidada, retorno a `Login` y bloqueo de acceso autenticado.

### Matriz de ejecucion manual

| Caso | Estado (OK/FAIL/PENDIENTE) | Evidencia | Notas |
|---|---|---|---|
| Login conductor | PENDIENTE | - | - |
| Ofertas | PENDIENTE | - | - |
| Viajes | PENDIENTE | - | - |
| Detalle de viaje | PENDIENTE | - | - |
| Incidencias | PENDIENTE | - | - |
| Perfil conductor | PENDIENTE | - | - |
| Agenda | PENDIENTE | - | - |
| Vehiculo | PENDIENTE | - | - |
| Tracking | PENDIENTE | - | - |
| Logout | PENDIENTE | - | - |

Nota: esta matriz queda preparada para cierre manual en emulador/dispositivo real. No se marca OK/FAIL desde CLI.

## Limitaciones conocidas

- La verificacion E2E con emulador no se puede automatizar desde este entorno CLI y debe completarse antes de release.
