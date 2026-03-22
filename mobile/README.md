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

## Navegacion y pantallas MVP

- `MainActivity` con `BottomNavigationView` (5 secciones):
  - Inicio
  - Portes
  - Mapa
  - Incidencias
  - Perfil
- Fragments base con placeholders funcionales y look alineado a desktop:
  - paleta azul limpia
  - cards claras con bordes suaves
  - fondo neutro y jerarquia tipografica clara

## Capa API inicial (wiring)

Implementado con Retrofit + OkHttp:

- Login: `POST api/auth/login` (`application/x-www-form-urlencoded`: `email`, `password`)
- Perfil: `GET api/users/me`
- Portes: `GET api/portes`
- Snapshot mapa: `GET api/map/snapshot`

Clases principales:

- `network/ApiClient.java`
- `network/ApiService.java`
- `network/AuthInterceptor.java`
- `data/AuthRepository.java`
- `data/AppRepository.java`
- `data/model/*`

## Validacion en este entorno

Ejecutado:

- `./gradlew tasks` -> OK
- `./gradlew build` -> falla por SDK ausente en entorno CLI

Error observado:

`SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at 'mobile/local.properties'.`

### Pasos para compilar localmente sin error

1. Asegurar SDK instalado desde Android Studio.
2. Definir `sdk.dir` en `mobile/local.properties`.
3. Ejecutar `./gradlew build` o compilar desde Android Studio.

## Flujo de autenticacion implementado

- `LoginActivity` es launcher de la app.
- Login real contra `POST /api/auth/login` (form-url-encoded).
- Token persistente en `SharedPreferences` (`SessionManager`).
- `AuthInterceptor` toma token persistido para requests autenticados.
- Si existe sesion al abrir la app, se salta Login y abre `MainActivity`.
- Logout en Perfil limpia sesion y redirige a Login.

## Prueba manual (paso a paso)

1. Configurar `mobile/local.properties` con `sdk.dir` y `api.base.url`.
2. Levantar backend en local (ejemplo: `http://10.0.2.2:8080/` desde emulador).
3. Ejecutar app en emulador/dispositivo Android.
4. Verificar que abre en Login.
5. Probar validaciones:
   - email vacio -> error
   - email invalido -> error
   - password vacia -> error
6. Ingresar credenciales validas y tocar `Iniciar sesion`.
7. Verificar loading visible y redireccion a Main al exito.
8. Cerrar por completo la app y abrirla otra vez: debe saltar Login (sesion persistente).
9. Ir a pestaña Perfil y tocar `Cerrar sesion`.
10. Verificar regreso a Login y bloqueo de acceso a Main sin login.
