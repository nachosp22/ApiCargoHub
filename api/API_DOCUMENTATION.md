# API CargoHub - Documentación de API REST

## Descripción General
Sistema de gestión de transporte de carga que conecta clientes, conductores y vehículos para realizar portes (transportes) de mercancías.

## Arquitectura
- **Framework**: Spring Boot 4.0.1
- **Base de Datos**: PostgreSQL
- **Patrón**: MVC con capas Service y Repository
- **Seguridad**: Spring Security con BCrypt para contraseñas

## Estructura del Proyecto

### Entidades Principales
1. **Usuario**: Gestión de autenticación y roles
2. **Cliente**: Empresas o particulares que solicitan transportes
3. **Conductor**: Conductores asignados a vehículos
4. **Vehiculo**: Flota de vehículos disponibles
5. **Porte**: Solicitudes de transporte
6. **Factura**: Facturación de portes completados
7. **Incidencia**: Reportes de problemas durante transportes
8. **BloqueoAgenda**: Gestión de disponibilidad de conductores

---

## Endpoints de API

### 1. Autenticación (`/api/auth`)

#### POST `/api/auth/register`
Registrar nuevo usuario
```
Parámetros:
- email: string
- password: string
- rol: ADMIN | CONDUCTOR | CLIENTE | SUPERADMIN

Respuesta: Usuario creado + perfil según rol
```

#### POST `/api/auth/login`
Iniciar sesión y obtener token JWT Bearer
```
Parámetros:
- email: string
- password: string

Respuesta (200): {
  accessToken: string,
  tokenType: "Bearer",
  expiresIn: number (segundos),
  expiresAt: ISO-8601 datetime,
  id: number,
  email: string,
  rol: ADMIN | CONDUCTOR | CLIENTE | SUPERADMIN,
  conductorId?: number,
  clienteId?: number,
  nombre?: string,
  nombreEmpresa?: string
}
```

#### Uso del Bearer Token
Para cualquier endpoint protegido bajo `/api/**` (excepto `/api/auth/register` y `/api/auth/login`), enviar:
```
Authorization: Bearer <accessToken>
```

Ejemplo:
```bash
curl -H "Authorization: Bearer eyJhbGciOiJI..." http://localhost:8080/api/facturas
```

---

### 2. Conductores (`/api/conductores`)

#### GET `/api/conductores/{id}`
Obtener perfil completo del conductor

#### PUT `/api/conductores/{id}`
Actualizar perfil del conductor
```
Body: {
  nombre, apellidos, telefono,
  ciudadBase, latitudBase, longitudBase,
  radioAccionKm, diasLaborables
}
```

#### POST `/api/conductores/{id}/ubicacion`
Reportar ubicación GPS
```
Parámetros:
- lat: double
- lon: double
```

#### GET `/api/conductores/{id}/agenda`
Ver agenda de bloqueos
```
Parámetros:
- desde: fecha (YYYY-MM-DD)
- hasta: fecha (YYYY-MM-DD)
```

#### POST `/api/conductores/{id}/agenda`
Crear bloqueo de agenda (vacaciones, baja médica, etc.)
```
Body: {
  fechaInicio: LocalDateTime,
  fechaFin: LocalDateTime,
  tipo: VACACIONES | BAJA_MEDICA | ASUNTOS_PROPIOS | DESCANSO_SEMANAL | OTROS,
  titulo: string
}
```

#### DELETE `/api/conductores/agenda/{bloqueoId}`
Eliminar bloqueo de agenda

#### DELETE `/api/conductores/{id}`
Dar de baja conductor (soft delete)

---

### 3. Clientes (`/api/clientes`)

#### GET `/api/clientes/{id}`
Obtener perfil del cliente

#### PUT `/api/clientes/{id}`
Actualizar perfil del cliente
```
Body: {
  nombreEmpresa, direccionFiscal,
  telefono, emailContacto
}
```

#### GET `/api/clientes/{id}/portes`
Listar historial de envíos del cliente

---

### 4. Vehículos (`/api/vehiculos`)

#### GET `/api/vehiculos`
Listar toda la flota

#### POST `/api/vehiculos`
Dar de alta un vehículo nuevo
```
Body: {
  matricula, marca, modelo,
  tipo: FURGONETA | RIGIDO | TRAILER,
  estado: DISPONIBLE | EN_SERVICIO | MANTENIMIENTO | BAJA,
  capacidadCargaKg, largoUtilMm, anchoUtilMm, altoUtilMm,
  trampillaElevadora: boolean,
  conductor: {id}
}
```

#### DELETE `/api/vehiculos/{id}`
Dar de baja vehículo (soft delete)

#### PUT `/api/vehiculos/{id}/reactivar`
Reactivar vehículo

---

### 5. Portes (`/api/portes`)

#### POST `/api/portes`
Crear nuevo porte
```
Body: {
  origen, destino,
  latitudOrigen, longitudOrigen,
  latitudDestino, longitudDestino,
  descripcionCliente,
  pesoTotalKg, volumenTotalM3, largoMaxPaquete,
  tipoVehiculoRequerido: FURGONETA | RIGIDO | TRAILER,
  fechaRecogida, fechaEntrega,
  cliente: {id}
}

Nota: La distancia y precio se calculan automáticamente
```

#### GET `/api/portes/{porteId}`
Obtener detalles de un porte específico

#### GET `/api/portes/ofertas/{conductorId}`
Ver ofertas disponibles para un conductor

#### GET `/api/portes/conductor/{conductorId}`
Listar portes asignados a un conductor

#### POST `/api/portes/{porteId}/aceptar`
Conductor acepta un porte disponible
```
Parámetros:
- conductorId: Long
```

#### PUT `/api/portes/{porteId}/estado`
Cambiar estado del porte
```
Parámetros:
- nuevo: PENDIENTE | ASIGNADO | EN_TRANSITO | ENTREGADO | CANCELADO | FACTURADO
```

#### POST `/api/portes/{porteId}/ajuste`
Agregar ajuste manual de precio (Admin)
```
Parámetros:
- cantidad: double (puede ser negativo para penalizaciones)
- concepto: string
```

#### POST `/api/portes/{porteId}/facturar`
Generar factura para porte entregado (Admin)

---

### 6. Facturas (`/api/facturas`)

#### GET `/api/facturas`
Listar todas las facturas

#### GET `/api/facturas/{id}`
Ver detalle de una factura

#### GET `/api/facturas/porte/{porteId}`
Buscar factura asociada a un porte

---

### 7. Incidencias (`/api/incidencias`)

#### POST `/api/incidencias`
Reportar incidencia
```
Parámetros:
- porteId: Long

Body: {
  titulo: string (obligatorio, no vacío, máx 150 chars),
  descripcion: string (obligatorio, no vacío, máx 4000 chars),
  severidad?: BAJA | MEDIA | ALTA (opcional, default MEDIA),
  prioridad?: BAJA | MEDIA | ALTA (opcional, default MEDIA)
}

Validación:
- Si faltan/son inválidos los campos del body => 400 Bad Request

Campos automáticos al crear:
- `estado=ABIERTA`
- `fechaReporte=now`
- `fechaLimiteSla` calculada por regla SLA (ver sección Reglas SLA)
```

#### GET `/api/incidencias`
Listar todas las incidencias (**ADMIN/SUPERADMIN**)

Respuesta: lista de `IncidenciaResponse`:
- `id`
- `porteId`
- `titulo`
- `descripcion`
- `estado`
- `severidad`
- `prioridad`
- `fechaReporte`
- `fechaLimiteSla`
- `resolucion`
- `fechaResolucion`
- `adminId`

> Nota: no se exponen entidades anidadas completas (`porte`, `admin`) para evitar fugas innecesarias de datos internos.

#### GET `/api/incidencias/{id}`
Obtener detalles de una incidencia (**owner del porte relacionado o ADMIN/SUPERADMIN**)

Respuesta: `IncidenciaResponse` (mismo contrato indicado arriba).

#### GET `/api/incidencias/pendientes`
Listar incidencias pendientes (ABIERTA y EN_REVISION) (**ADMIN/SUPERADMIN**)

Respuesta: lista de `IncidenciaResponse`.

#### GET `/api/incidencias/porte/{porteId}`
Listar incidencias de un porte específico (**owner del porte o ADMIN/SUPERADMIN**)

Respuesta: lista de `IncidenciaResponse`.

#### GET `/api/incidencias/{id}/historial`
Ver historial/auditoría de cambios de estado (**owner del porte relacionado o ADMIN/SUPERADMIN**)

Respuesta: lista de eventos `IncidenciaEventoResponse` con:
- `id`
- `incidenciaId`
- `actorId` (puede ser `null` si no había usuario autenticado al crear)
- `estadoAnterior`
- `estadoNuevo`
- `fecha`
- `accion` (ej. `CREADA`, `TRANSICION_ESTADO`)
- `comentario`

> Nota: no se exponen objetos anidados completos (`incidencia`, `actor`) en la respuesta.

#### GET `/api/incidencias/vencidas-sla`
Listar incidencias vencidas por SLA (**ADMIN/SUPERADMIN**)

Definición de vencida: incidencia en estado `ABIERTA` o `EN_REVISION` con `fechaLimiteSla < now`.

Respuesta: lista de `IncidenciaResponse`.

#### PUT `/api/incidencias/{id}/resolver`
Resolver incidencia (Admin)
```
Body: {
  resolucion: string (obligatoria si estadoFinal es RESUELTA o DESESTIMADA; máx 4000 chars),
  estadoFinal: EN_REVISION | RESUELTA | DESESTIMADA
}

Nota: el admin resolvedor se toma del principal autenticado (JWT), no de un parámetro de request.

Errores:
- 400 Bad Request: payload inválido (campos faltantes/formato inválido)
- 409 Conflict: transición de estado no permitida por reglas de ciclo de vida

Respuesta (200): `IncidenciaResponse`.
```

Reglas de transición de estado de Incidencia:
```
ABIERTA -> EN_REVISION | RESUELTA | DESESTIMADA
EN_REVISION -> RESUELTA | DESESTIMADA
RESUELTA -> (terminal, sin transiciones permitidas)
DESESTIMADA -> (terminal, sin transiciones permitidas)
```

Reglas SLA de Incidencia:
```
ALTA  -> 24h
MEDIA -> 72h
BAJA  -> 120h
```

La regla se evalúa usando el mayor nivel entre `severidad` y `prioridad`:
- Si cualquiera es `ALTA` => 24h
- Si no hay ALTA pero cualquiera es `MEDIA` => 72h
- Solo si ambas son `BAJA` => 120h

---

## Reglas de Negocio

### Cálculo de Precio
El precio se calcula automáticamente basándose en:
- **Distancia**: Calculada usando Haversine (línea recta * 1.2 para aproximar carretera)
- **Tipo de vehículo**:
  - Furgoneta: 0.90€/km (mínimo 40€)
  - Rígido: 1.40€/km (mínimo 70€)
  - Tráiler: 1.65€/km (mínimo 90€)
- **Suplementos**:
  - Nocturno (22h-6h): +20%
  - Fin de semana: +25%
- **Fijo de arranque**: 20€

### Asignación Automática
Al crear un porte, el sistema busca automáticamente:
1. Vehículos DISPONIBLES
2. Que sean del tipo requerido
3. Con capacidad de peso suficiente
4. Con dimensiones adecuadas

Si encuentra coincidencia, asigna el conductor automáticamente.
Si no, queda PENDIENTE para asignación manual.

### Estados de Porte
```
PENDIENTE → ASIGNADO → EN_TRANSITO → ENTREGADO → FACTURADO
                                    ↘ CANCELADO
```

### Facturación
- Solo se pueden facturar portes con estado ENTREGADO
- La factura incluye el precio base + ajustes manuales
- IVA calculado automáticamente (21%)
- Número de serie secuencial: F-{AÑO}-{NÚMERO}

---

## Seguridad

### Modelo de autenticación/autorización (JWT + RBAC)

Implementación actual (según `SecurityConfig` + `@PreAuthorize`):

- Autenticación **JWT Bearer** stateless.
- Endpoints públicos:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- `OPTIONS /**` público (preflight CORS).
- Todo endpoint bajo `/api/**` (excepto auth) requiere usuario autenticado.
- La autorización por rol se resuelve con `@PreAuthorize` en controladores/métodos.
- En endpoints con `{id}`/`{porteId}` críticos se aplica además **ownership** (propiedad del recurso) con expresiones centralizadas (`@ownership...`).
- HTTP Basic está deshabilitado para operación normal.

### Ownership (control incremental implementado)

- **CLIENTE**
  - Puede acceder a `GET/PUT /api/clientes/{id}` y `GET /api/clientes/{id}/portes` **solo** cuando `{id}` coincide con su propio perfil.
  - Puede acceder a `GET /api/portes/{porteId}` **solo** si el porte pertenece a su cliente.
- **CONDUCTOR**
  - Puede operar `GET/PUT /api/conductores/{id}`, `POST /api/conductores/{id}/ubicacion`, `GET/POST /api/conductores/{id}/agenda`, `DELETE /api/conductores/agenda/{bloqueoId}` **solo** sobre su propio conductor.
  - Puede operar `GET /api/portes/ofertas/{conductorId}`, `GET /api/portes/conductor/{conductorId}`, `POST /api/portes/{porteId}/aceptar?conductorId=...` **solo** con su propio `conductorId`.
  - Puede operar `PUT /api/portes/{porteId}/estado` y `GET /api/portes/{porteId}` **solo** sobre portes asociados a su conductor.
- **ADMIN / SUPERADMIN**
  - Mantienen bypass de ownership en estos checks y conservan acceso operativo amplio según RBAC.

Violaciones de ownership devuelven **403 Forbidden** (token válido sin permiso sobre ese recurso).

### Definición de roles

- **CLIENTE**: usuario autenticado sin privilegios administrativos; acceso a endpoints sin restricción de rol explícita.
- **CONDUCTOR**: como CLIENTE + acceso a endpoints operativos de portes marcados para conductor.
- **ADMIN**: permisos administrativos de operación/facturación y endpoints marcados como admin.
- **SUPERADMIN**: mismo alcance que ADMIN en la configuración actual (todos los `hasAnyRole` de admin incluyen SUPERADMIN).

### Matriz de autorización por endpoint (estado actual)

> Convención:
> - **Público** = sin token.
> - **Autenticado (cualquier rol)** = requiere JWT válido; válido para CLIENTE/CONDUCTOR/ADMIN/SUPERADMIN.

#### Módulo Auth (`/api/auth`)

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/auth/register` | Público |
| POST | `/api/auth/login` | Público |

#### Módulo Conductor (`/api/conductores`)

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/conductores/{id}/ubicacion` | CONDUCTOR (own `{id}`), ADMIN, SUPERADMIN |
| GET | `/api/conductores/{id}/agenda` | CONDUCTOR (own `{id}`), ADMIN, SUPERADMIN |
| POST | `/api/conductores/{id}/agenda` | CONDUCTOR (own `{id}`), ADMIN, SUPERADMIN |
| DELETE | `/api/conductores/agenda/{bloqueoId}` | CONDUCTOR (own bloqueo), ADMIN, SUPERADMIN |
| GET | `/api/conductores/{id}` | CONDUCTOR (own `{id}`), ADMIN, SUPERADMIN |
| PUT | `/api/conductores/{id}` | CONDUCTOR (own `{id}`), ADMIN, SUPERADMIN |
| DELETE | `/api/conductores/{id}` | CONDUCTOR (own `{id}`), ADMIN, SUPERADMIN |

#### Módulo Cliente (`/api/clientes`)

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/clientes/{id}` | CLIENTE (own `{id}`), ADMIN, SUPERADMIN |
| PUT | `/api/clientes/{id}` | CLIENTE (own `{id}`), ADMIN, SUPERADMIN |
| GET | `/api/clientes/{id}/portes` | CLIENTE (own `{id}`), ADMIN, SUPERADMIN |

#### Módulo Vehículo (`/api/vehiculos`)

> `@PreAuthorize` a nivel de clase: `hasAnyRole('ADMIN','SUPERADMIN')`

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/vehiculos` | ADMIN, SUPERADMIN |
| POST | `/api/vehiculos` | ADMIN, SUPERADMIN |
| DELETE | `/api/vehiculos/{id}` | ADMIN, SUPERADMIN |
| PUT | `/api/vehiculos/{id}/reactivar` | ADMIN, SUPERADMIN |

#### Módulo Porte (`/api/portes`)

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/portes` | ADMIN, SUPERADMIN |
| GET | `/api/portes/ofertas/{conductorId}` | CONDUCTOR (own `{conductorId}`), ADMIN, SUPERADMIN |
| POST | `/api/portes/{porteId}/aceptar` | CONDUCTOR (own `conductorId` param), ADMIN, SUPERADMIN |
| PUT | `/api/portes/{porteId}/estado` | CONDUCTOR (porte propio), ADMIN, SUPERADMIN |
| POST | `/api/portes/{porteId}/ajuste` | ADMIN, SUPERADMIN |
| POST | `/api/portes/{porteId}/facturar` | ADMIN, SUPERADMIN |
| GET | `/api/portes/{porteId}` | CLIENTE (porte propio), CONDUCTOR (porte propio), ADMIN, SUPERADMIN |
| GET | `/api/portes/conductor/{conductorId}` | CONDUCTOR (own `{conductorId}`), ADMIN, SUPERADMIN |

#### Módulo Factura (`/api/facturas`)

> `@PreAuthorize` a nivel de clase: `hasAnyRole('ADMIN','SUPERADMIN')`

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/facturas` | ADMIN, SUPERADMIN |
| GET | `/api/facturas/{id}` | ADMIN, SUPERADMIN |
| GET | `/api/facturas/porte/{porteId}` | ADMIN, SUPERADMIN |

#### Módulo Incidencia (`/api/incidencias`)

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/incidencias` | Autenticado (cualquier rol) |
| PUT | `/api/incidencias/{id}/resolver` | ADMIN, SUPERADMIN |
| GET | `/api/incidencias/pendientes` | ADMIN, SUPERADMIN |
| GET | `/api/incidencias` | ADMIN, SUPERADMIN |
| GET | `/api/incidencias/{id}` | CLIENTE (incidencia de porte propio), CONDUCTOR (incidencia de porte propio), ADMIN, SUPERADMIN |
| GET | `/api/incidencias/porte/{porteId}` | CLIENTE (porte propio), CONDUCTOR (porte propio), ADMIN, SUPERADMIN |
| GET | `/api/incidencias/{id}/historial` | CLIENTE (incidencia de porte propio), CONDUCTOR (incidencia de porte propio), ADMIN, SUPERADMIN |
| GET | `/api/incidencias/vencidas-sla` | ADMIN, SUPERADMIN |

### Respuestas de seguridad (401 vs 403)

- **401 Unauthorized**
  - Sin token JWT en un endpoint protegido `/api/**`
  - Token inválido, mal formado o expirado
  - Lo gestiona `authenticationEntryPoint` devolviendo 401
- **403 Forbidden**
  - Token válido, pero el rol no cumple `@PreAuthorize`
  - Ejemplo típico: CLIENTE en `/api/facturas`

### Ejemplos prácticos con `Authorization: Bearer`

```bash
# Login para obtener token
curl -X POST "http://localhost:8080/api/auth/login?email=admin@admin.com&password=admin"

# Usar token en endpoint protegido
curl -H "Authorization: Bearer <accessToken>" "http://localhost:8080/api/portes/1"

# Esperado 401: sin token en endpoint protegido
curl -i "http://localhost:8080/api/facturas"

# Esperado 403: token CLIENTE en endpoint ADMIN/SUPERADMIN
curl -i -H "Authorization: Bearer <tokenCliente>" "http://localhost:8080/api/facturas"
```

### Configuración JWT

```properties
# Recomendado en entorno: variable de entorno JWT_SECRET
security.jwt.secret=${JWT_SECRET:}

# Expiración configurable (milisegundos)
security.jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}
```

> Obligatorio: definir `JWT_SECRET` con valor aleatorio robusto (>=32 chars). Si no está definido, la aplicación falla al arrancar por seguridad.

### Smoke test (JWT/RBAC)

Script disponible: `scripts/smoke-jwt.ps1`

Uso rápido:

```powershell
# API ya levantada
pwsh -File .\scripts\smoke-jwt.ps1

# Levantar API automáticamente antes del smoke
pwsh -File .\scripts\smoke-jwt.ps1 -StartApi
```

El script verifica, entre otros checks:
- registro/login
- `GET /api/facturas` sin token => **401**
- `GET /api/facturas` con token CLIENTE => **403**
- `GET /api/facturas` con token ADMIN => **200**

### CORS

Configurado para permitir peticiones desde cualquier origen (`*`) para desarrollo.
En producción, restringir a dominios específicos.

---

## Testing

### Tests Unitarios Disponibles
- **PorteServiceTest**: 7 tests
- **ConductorServiceTest**: 6 tests
- **IncidenciaServiceTest**: 8 tests
- **CalculadoraPrecioServiceTest**: 8 tests

### Ejecutar Tests
```bash
./mvnw test
```

---

## Compilar y Ejecutar

### Requisitos
- Java 17+
- Maven 3.6+
- PostgreSQL 14+ (recomendado)

### Configuración Base de Datos
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cargohub
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### Estrategia `ddl-auto` en desarrollo (PostgreSQL)

- Valor por defecto (modo estable):

```properties
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
```

Esto evita que Hibernate borre el esquema al apagar/arrancar la API.

- Para forzar un reset puntual del schema en local:

```powershell
$env:JPA_DDL_AUTO="create-drop"
./mvnw spring-boot:run
```

Al cerrar esa shell o quitar la variable, vuelve el modo estable (`update`).

### Compilar
```bash
./mvnw clean compile
```

### Ejecutar
```bash
./mvnw spring-boot:run
```

La API estará disponible en `http://localhost:8080`

---

## Notas de Desarrollo

### Soft Delete
Tanto conductores como vehículos utilizan "soft delete":
- No se eliminan físicamente de la base de datos
- Se marca como inactivo/dado de baja
- Se conserva el historial

### Integración Externa
- **OSRM**: Se utiliza para cálculo de distancias reales por carretera
- Si falla la conexión, se usa cálculo Haversine como fallback

### Versionado
El proyecto usa `@Version` en la entidad Porte para control de concurrencia optimista.

---

## Autor
Proyecto desarrollado como parte del curso 2DAM
