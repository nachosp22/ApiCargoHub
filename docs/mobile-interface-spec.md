# Especificacion de interfaz movil MVP - ApiCargoHub

Fecha: 2026-03-18
Estado: Ready para implementacion
Alcance: App movil para operacion de flota y seguimiento (MVP)

## 1) Objetivo del MVP movil

Entregar una app movil enfocada en operacion diaria en campo, con prioridad en:

- Autenticacion segura por JWT.
- Vista operativa rapida (estado de portes, incidencias y alertas).
- Mapa de seguimiento de conductores con comportamiento consistente con desktop.
- Acciones minimas de negocio sin bloquear la continuidad del backend actual.

Resultado esperado del MVP: que un usuario operativo pueda iniciar sesion, ver su estado de trabajo, ubicar conductores/portes en mapa, consultar detalle y gestionar alertas basicas sin depender del desktop para tareas de seguimiento.

## 2) Contexto actual del repo (base para este documento)

- Backend Spring Boot expone `/api` (auth, portes, incidencias, conductores, clientes, etc.) y endpoints v1 de tracking en `/api/v1`.
- Desktop Vue/Electron ya implementa login, modulos CRUD, y mapa de flota con polling y estados de conexion.
- Realtime actual usa `GET /api/v1/fleet/snapshot` + `POST /api/v1/tracking/drivers/{driverId}/locations` + `GET /api/v1/eta/estimate`.
- Existe feature flag de mapa realtime en desktop (`VITE_FEATURE_FLEET_REALTIME`).
- Carpeta `mobile/` aun sin implementacion, por lo que este spec define contrato UX/UI inicial para iniciar desarrollo.

## 3) Perfiles de usuario y permisos (MVP)

Se mantiene RBAC existente del backend; en movil se habilita solo el subconjunto operativo:

| Perfil | Permisos MVP movil | Restricciones clave |
|---|---|---|
| CONDUCTOR | Ver sus portes, actualizar estado permitido, enviar ubicacion, ver incidencias de sus portes | Solo recursos propios (ownership) |
| ADMIN | Ver snapshot de flota, ETA, incidencias, detalle de porte, panel operativo | No incluye gestion administrativa completa en MVP1 |
| SUPERADMIN | Mismo alcance que ADMIN en MVP | Igual que backend actual |
| CLIENTE | Solo lectura de sus portes e incidencias (opcional en MVP2) | Fuera de MVP1 si complica navegacion |

## 4) Mapa de navegacion (tabs + stack)

### Estructura principal

- Auth Stack
  - Login
- App Tabs (post-login)
  - Inicio
  - Mapa
  - Alertas
  - Perfil

### Stacks por tab

- Inicio Stack
  - Dashboard/Listado (home operativo)
  - Detalle de porte
  - Detalle de incidencia
- Mapa Stack
  - Mapa de flota
  - Filtros avanzados de mapa (modal/sheet)
  - Detalle rapido de conductor/porte
- Alertas Stack
  - Lista de alertas
  - Detalle de alerta/incidencia
- Perfil Stack
  - Perfil
  - Configuracion (notificaciones, refresco, sesion)

## 5) Pantallas obligatorias (MVP)

## 5.1 Login

Objetivo: autenticacion con `/api/auth/login` y persistencia de sesion.

Elementos minimos:

- Campo email.
- Campo password.
- CTA "Iniciar sesion".
- Mensajes de error por credenciales o red.

## 5.2 Dashboard/Listado

Objetivo: resumen rapido del estado operativo.

Elementos minimos:

- KPIs compactos (portes activos, incidencias pendientes, conductores online/stale/offline si aplica por rol).
- Listado principal (portes o incidencias segun rol) con busqueda simple.
- Acceso directo a detalle.

## 5.3 Mapa

Objetivo: seguimiento de flota en tiempo casi real.

Elementos minimos:

- Mapa con marcadores por estado (`ONLINE`, `STALE`, `OFFLINE`).
- Filtros por estado y busqueda por identificador.
- Boton refrescar manual.
- Indicador de estado de conexion (`ONLINE`, `DEGRADED`, `OFFLINE`).
- Auto-fit configurable ON/OFF.

## 5.4 Detalle

Objetivo: profundizar en un porte/incidencia/conductor sin salir del flujo.

Elementos minimos:

- Datos de cabecera (ID, estado, timestamp).
- Timeline basico de estado.
- Acciones permitidas por rol (ej. cambiar estado de porte para conductor cuando corresponda).

## 5.5 Alertas

Objetivo: listar eventos relevantes que requieren atencion.

Elementos minimos:

- Lista cronologica de alertas/incidentes.
- Filtros por severidad/prioridad/estado.
- Accion de marcar como vista localmente.

## 5.6 Perfil/Configuracion

Objetivo: preferencias de usuario y sesion.

Elementos minimos:

- Datos basicos de usuario.
- Config de notificaciones y frecuencia de refresco.
- Cerrar sesion.

## 6) Componentes UI reutilizables

- `AppShellMobile` (header, area segura, tab bar).
- `StatusBadge` (ONLINE/STALE/OFFLINE y estados de negocio).
- `KpiCardCompact`.
- `ListItemPorte`.
- `ListItemAlerta`.
- `SearchInput` con debounce.
- `FilterChips`.
- `ConnectionBanner` (degradado/offline).
- `EmptyStateCard`.
- `ErrorStateCard` con reintento.
- `LoadingSkeleton`.
- `MapLegend`.
- `ActionBottomSheet`.

## 7) Estados de UI obligatorios

- `loading`: skeletons visibles y CTA principal deshabilitado cuando corresponda.
- `empty`: mensaje claro + accion recomendada.
- `offline`: banner persistente + uso de ultimo snapshot valido si existe.
- `error`: mensaje accionable + boton "Reintentar".
- `stale`: datos mostrados con marca de antiguedad (ej. "actualizado hace 45s").

Regla general: nunca pantalla en blanco; siempre debe existir estado explicito renderizado.

## 8) Reglas UX de tiempo real

- Refresh base recomendado: 10s (alineado con backend `pollingSuggestedSec`).
- Permitir refresco manual inmediato sin esperar ciclo de polling.
- Backoff automatico ante fallo: 10s -> 20s -> 40s -> max 60s.
- Mantener ultimo snapshot valido hasta 60s en degradacion puntual.
- Auto-fit del mapa activo por defecto en primera carga; luego respetar preferencia del usuario.
- Filtros activos no se pierden al refrescar.
- Si hay conductor seleccionado, mantener foco mientras exista en snapshot.

## 9) Requisitos de accesibilidad y responsive

- Contraste minimo WCAG AA en texto y controles.
- Tamaño minimo de tap target: 44x44 px.
- Labels explicitos en inputs y botones icon-only con `accessibilityLabel`.
- Soporte de tamano de fuente dinamico del sistema (sin romper layout).
- Navegacion usable entre 360px y 480px de ancho como baseline.
- Soporte portrait obligatorio; landscape recomendado en mapa.
- Feedback haptico/visual para acciones criticas (cuando plataforma lo permita).

## 10) Reglas de copy/microcopy (espanol)

Tono:

- Claro, directo, operativo.
- Evitar tecnicismos innecesarios.
- Usar voseo neutro o imperativo estandar consistente (este doc usa imperativo estandar).

Patrones de copy:

- Login error credenciales: "Credenciales invalidas. Revisa email y contrasena."
- Login error red: "No se pudo conectar con el servidor. Intenta de nuevo."
- Estado degradado: "Conexion inestable. Mostrando ultima informacion disponible."
- Estado offline: "Sin conexion. Reintentando automaticamente."
- Empty alertas: "No hay alertas pendientes."
- CTA retry: "Reintentar"
- CTA refresh: "Actualizar"

Reglas:

- No mezclar ingles/espanol en etiquetas visibles.
- No usar mensajes genericos tipo "Error inesperado" sin siguiente accion.
- Toda validacion debe indicar que campo corregir.

## 11) Criterios de aceptacion por pantalla (checklist)

## 11.1 Login

- [ ] Permite iniciar sesion contra `/api/auth/login` con `application/x-www-form-urlencoded`.
- [ ] Guarda token y perfil de usuario en storage seguro local.
- [ ] Redirige al tab inicial al autenticar.
- [ ] Muestra error de credenciales y de red diferenciados.

## 11.2 Dashboard/Listado

- [ ] Renderiza KPIs y listado segun rol.
- [ ] Permite abrir detalle desde un item.
- [ ] Soporta loading, empty y error.
- [ ] Mantiene filtros/busqueda al volver desde detalle.

## 11.3 Mapa

- [ ] Consume `GET /api/v1/fleet/snapshot` y dibuja marcadores por estado.
- [ ] Muestra estado de conexion y motivo de degradacion si aplica.
- [ ] Permite refresco manual y polling automatico.
- [ ] Respeta auto-fit ON/OFF.
- [ ] Mantiene seleccion y filtros tras refresh.

## 11.4 Detalle

- [ ] Muestra informacion completa de porte/incidencia seleccionada.
- [ ] Respeta permisos/ownership devolviendo feedback claro ante 403.
- [ ] Incluye acciones permitidas por rol sin exponer acciones prohibidas.

## 11.5 Alertas

- [ ] Lista alertas/incidencias ordenadas por prioridad y fecha.
- [ ] Permite filtrar por estado/severidad/prioridad.
- [ ] Tiene estado vacio y accion de reintento.

## 11.6 Perfil/Configuracion

- [ ] Muestra datos basicos de usuario autenticado.
- [ ] Permite ajustar frecuencia de refresco (10s/20s/30s).
- [ ] Incluye cerrar sesion y limpieza de storage.

## 12) Fuera de alcance (evitar scope creep)

- Chat interno en tiempo real.
- WebSockets/SSE en movil para MVP1 (se usa polling).
- Edicion avanzada de entidades maestras (vehiculos/clientes) desde movil.
- Reporteria compleja y dashboards analiticos avanzados.
- Modo offline con escritura diferida y sincronizacion bidireccional.
- Multi-idioma (solo espanol en MVP).

## 13) Dependencias backend y contratos API esperados

## Contratos existentes reutilizables

- Auth
  - `POST /api/auth/login` (form-urlencoded)
- Tracking v1
  - `GET /api/v1/fleet/snapshot` (ADMIN/SUPERADMIN)
  - `POST /api/v1/tracking/drivers/{driverId}/locations` (ADMIN/SUPERADMIN o owner conductor)
  - `GET /api/v1/eta/estimate` (ADMIN/SUPERADMIN)
- Operacion
  - `GET /api/portes/{porteId}`
  - `GET /api/portes/conductor/{conductorId}`
  - `PUT /api/portes/{porteId}/estado`
  - `GET /api/incidencias/porte/{porteId}`
  - `GET /api/incidencias/{id}`

## Ajustes recomendados para mejorar UX movil (MVP2)

- Endpoint agregado de alertas consolidadas para mobile feed (evitar N+1 de consultas).
- Endpoint de perfil actual (`/api/me`) para no depender de IDs en cliente.
- Contrato de notificaciones push (si se habilita en fase posterior).

## 14) Plan de implementacion por fases

## MVP1 (objetivo inmediato)

- Infra base mobile (navegacion tabs/stack, auth store, API client con interceptor JWT).
- Pantallas: Login, Dashboard/Listado, Mapa, Detalle, Alertas, Perfil.
- Polling realtime y estados de conexion.
- Checklist de aceptacion de seccion 11 completado para flujo principal.

## MVP2 (iteracion siguiente)

- Mejoras de productividad (filtros avanzados, cache local de lectura, ajustes UX de mapa).
- Perfil/ajustes ampliados y soporte cliente si no entra en MVP1.
- Endpointes backend optimizados para feed de alertas y perfil unificado.
- Preparacion para push notifications y eventos cercanos a tiempo real.

## 15) Checklist de arranque manana

Orden recomendado de ejecucion:

1. Validar stack mobile del repo (confirmar Java Android nativo vs alternativa) y crear scaffolding base en `mobile/`.
2. Implementar cliente API y capa auth (login, storage token, interceptor Bearer, logout).
3. Levantar navegacion (Auth Stack + Tabs + Stacks internos).
4. Construir pantalla Login con manejo de errores exactos del backend.
5. Construir Dashboard/Listado minimo con datos reales por rol.
6. Implementar pantalla Mapa con polling, filtros, auto-fit y estados `ONLINE/DEGRADED/OFFLINE`.
7. Implementar Detalle y Alertas con estados de UI completos (loading/empty/error/offline/stale).
8. Implementar Perfil/Configuracion con frecuencia de refresh y cierre de sesion.
9. Ejecutar smoke manual E2E: login -> dashboard -> mapa -> detalle -> alertas -> logout.
10. Documentar decisiones tecnicas y gaps detectados en `docs/mobile-interface-spec.md` al finalizar la jornada.

## 16) Riesgos y mitigaciones rapidas

- Riesgo: permisos RBAC/ownership generan 403 inesperados en mobile.
  - Mitigacion: mapear errores 401/403 con mensajes UX claros y ocultar acciones no permitidas por rol.
- Riesgo: polling agresivo impacta bateria/red.
  - Mitigacion: intervalos configurables y pausa de polling en background.
- Riesgo: falta de endpoint agregado para alertas.
  - Mitigacion: implementar feed inicial con endpoints existentes y planificar endpoint consolidado en MVP2.

## 17) Entregables solicitados para app movil de conductor

Esta seccion baja a definicion operativa 3 entregables concretos para implementacion.

## 17.1 Entregable 1: propuesta de estructura de navegacion

### Principios

- Mantener `Bottom Tabs` (consistente con la seccion 4), priorizando flujo diario del conductor.
- Evitar profundidad innecesaria: maximo 2 niveles desde tab raiz a accion principal.
- Mantener accesos directos a realtime, portes e incidencias sin salir del contexto del viaje.

### Navegacion propuesta (conductor)

| Capa | Nodo | Pantallas | Objetivo operativo | Notas realtime/RBAC |
|---|---|---|---|---|
| Auth Stack | Login | `Login` | Iniciar sesion JWT | Bloquea acceso a app si token invalido/expirado |
| Tab 1 | Inicio | `InicioDashboard` -> `PorteDetalle` | Ver trabajo del dia y abrir porte activo rapido | KPIs: portes activos, incidencias abiertas, estado conexion |
| Tab 2 | Portes | `MisPortesLista` -> `PorteDetalle` -> `EstadoPorteSheet` | Ejecutar ciclo del porte (asignado -> transito -> entregado) | Filtra por ownership (`/api/portes/conductor/{conductorId}`) |
| Tab 3 | Mapa | `MapaRutaActual` -> `PorteDetalleRapido` | Seguimiento visual de ruta y posicion | Polling sugerido 10s + estado `ONLINE/DEGRADED/OFFLINE` |
| Tab 4 | Incidencias | `IncidenciasLista` -> `IncidenciaDetalle` -> `CrearIncidencia` | Reportar y seguir incidencias de portes propios | Usa `/api/incidencias/porte/{porteId}` y `POST /api/incidencias` |
| Tab 5 | Perfil | `Perfil` -> `Configuracion` | Preferencias de refresh, notificaciones y logout | Ajusta frecuencia (10s/20s/30s), limpia sesion |

### Menus y accesos rapidos recomendados

| Ubicacion | Accion | Tipo | Resultado |
|---|---|---|---|
| Header de `InicioDashboard` | "Actualizar" | CTA primaria | Fuerza refresh de KPIs y lista |
| Card de porte activo | "Continuar porte" | Deep link interno | Abre `PorteDetalle` del porte en curso |
| `PorteDetalle` | "Cambiar estado" | Bottom sheet | Muestra solo transiciones validas para el estado actual |
| `PorteDetalle` | "Reportar incidencia" | CTA secundaria | Precompleta `porteId` en `CrearIncidencia` |
| Tab `Mapa` | "Centrar en mi ruta" | FAB | Reenfoca camara en origen/destino/conductor |

## 17.2 Entregable 2: modelo de datos relacional basico

Objetivo: cubrir 2 necesidades de negocio del movil conductor sin romper el backend actual:

1. Regla de vehiculos (elegibilidad/compatibilidad para asignacion).
2. Tracking auditable de cambios de estado del porte.

### Esquema relacional propuesto (minimo viable)

| Tabla | PK | Campos clave | FK / relaciones | Uso en app movil |
|---|---|---|---|---|
| `conductores` | `id` | `usuario_id`, `nombre`, `disponible` | 1:N con `vehiculos`, 1:N con `portes` | Ownership de recursos del conductor |
| `vehiculos` | `id` | `matricula`, `tipo`, `estado`, `capacidad_carga_kg`, `largo_util_mm`, `volumen_m3`, `trampilla_elevadora`, `conductor_id` | N:1 a `conductores` | Datos base para matching de porte-vehiculo |
| `portes` | `id` | `estado`, `tipo_vehiculo_requerido`, `peso_total_kg`, `largo_max_paquete`, `requiere_frio`, `conductor_id`, fechas | N:1 a `conductores`, N:1 a `clientes` | Entidad principal del flujo del conductor |
| `reglas_vehiculo` (nueva) | `id` | `nombre`, `tipo_vehiculo`, `peso_max_kg`, `largo_max_mm`, `requiere_frio`, `prioridad`, `activa` | Opcional N:1 a `clientes` (si reglas por cliente) | Parametriza la elegibilidad sin hardcode en app |
| `porte_vehiculo_match` (nueva) | `id` | `porte_id`, `vehiculo_id`, `regla_id`, `resultado` (`MATCH`/`NO_MATCH`), `motivo`, `evaluado_en` | N:1 a `portes`, N:1 a `vehiculos`, N:1 a `reglas_vehiculo` | Evidencia de por que se asigno/no asigno |
| `porte_estado_historial` (nueva) | `id` | `porte_id`, `estado_anterior`, `estado_nuevo`, `actor_tipo`, `actor_id`, `canal`, `motivo`, `fecha_evento` | N:1 a `portes` | Timeline auditable para detalle movil |

### Restricciones e indices recomendados

| Elemento | Recomendacion |
|---|---|
| Integridad de estados | `CHECK estado_nuevo IN ('PENDIENTE','ASIGNADO','EN_TRANSITO','ENTREGADO','CANCELADO','FACTURADO')` |
| Historial cronologico | Indice `idx_porte_estado_historial_porte_fecha (porte_id, fecha_evento DESC)` |
| Matching reusable | Indice `idx_porte_vehiculo_match_porte (porte_id, evaluado_en DESC)` |
| Regla activa por prioridad | Indice `idx_reglas_vehiculo_activa_prioridad (activa, prioridad DESC)` |

### Notas de compatibilidad con lo existente

- `portes.estado` se mantiene como fuente de verdad actual (sin romper endpoints actuales).
- `porte_estado_historial` agrega trazabilidad sin exigir cambios de contrato en `PUT /api/portes/{porteId}/estado`.
- `porte_vehiculo_match` se puede poblar en la logica de asignacion existente (`findCandidatos`) de forma incremental.

## 17.3 Entregable 3: logica de maquina de estados del porte + UI habilitada

### Estados oficiales (alineados con backend)

`PENDIENTE -> ASIGNADO -> EN_TRANSITO -> ENTREGADO -> FACTURADO`

Camino alterno de cierre:

`PENDIENTE/ASIGNADO -> CANCELADO`

### Tabla de transiciones de negocio y accion UI

| Estado actual | Accion UI visible | Estado destino | Rol permitido | Endpoint |
|---|---|---|---|---|
| `PENDIENTE` | "Aceptar porte" | `ASIGNADO` | CONDUCTOR owner / ADMIN | `POST /api/portes/{porteId}/aceptar?conductorId=` |
| `PENDIENTE` | "Cancelar porte" | `CANCELADO` | ADMIN/SUPERADMIN | `PUT /api/portes/{porteId}/estado?nuevo=CANCELADO` |
| `ASIGNADO` | "Iniciar viaje" | `EN_TRANSITO` | CONDUCTOR owner / ADMIN | `PUT /api/portes/{porteId}/estado?nuevo=EN_TRANSITO` |
| `ASIGNADO` | "Cancelar porte" | `CANCELADO` | ADMIN/SUPERADMIN | `PUT /api/portes/{porteId}/estado?nuevo=CANCELADO` |
| `EN_TRANSITO` | "Marcar entregado" | `ENTREGADO` | CONDUCTOR owner / ADMIN | `PUT /api/portes/{porteId}/estado?nuevo=ENTREGADO` |
| `ENTREGADO` | "Facturar" | `FACTURADO` | ADMIN/SUPERADMIN | `POST /api/portes/{porteId}/facturar` |

### Matriz de botones habilitados/deshabilitados (vista conductor)

| Estado porte | Boton "Aceptar" | Boton "Iniciar viaje" | Boton "Marcar entregado" | Boton "Reportar incidencia" | Boton "Facturar" |
|---|---|---|---|---|---|
| `PENDIENTE` | Habilitado (solo en pantalla de ofertas) | Deshabilitado | Deshabilitado | Habilitado | Deshabilitado |
| `ASIGNADO` | Deshabilitado | Habilitado | Deshabilitado | Habilitado | Deshabilitado |
| `EN_TRANSITO` | Deshabilitado | Deshabilitado | Habilitado | Habilitado | Deshabilitado |
| `ENTREGADO` | Deshabilitado | Deshabilitado | Deshabilitado | Habilitado (solo seguimiento) | Deshabilitado (conductor) |
| `CANCELADO` | Deshabilitado | Deshabilitado | Deshabilitado | Habilitado (solo consulta historica) | Deshabilitado |
| `FACTURADO` | Deshabilitado | Deshabilitado | Deshabilitado | Habilitado (solo consulta historica) | Deshabilitado |

### Reglas UI para evitar errores de operacion

| Regla | Comportamiento requerido |
|---|---|
| Boton invalido por estado | No renderizar o mostrar deshabilitado con tooltip "Accion no disponible en estado actual" |
| Error `403` ownership | Mostrar mensaje: "No tenes permisos para operar este porte" y volver a listado propio |
| Estado cambiado por otro actor | Refrescar detalle y mostrar toast: "El estado fue actualizado. Revisamos la vista." |
| Operacion exitosa | Refrescar `PorteDetalle` + timeline (`porte_estado_historial`) + badge en listas |

### Recomendacion tecnica (backend)

Para coherencia fuerte entre UI y negocio, agregar validacion de transiciones permitidas en backend (hoy `cambiarEstado` asigna estado directo), de forma que la app movil no dependa solo de validaciones cliente.

## 18) Contratos API moviles

Esta seccion define el contrato operativo para que desarrollo movil pueda empezar sin bloqueo.

### 18.1 Convenciones generales

| Item | Contrato |
|---|---|
| Base URL | `https://<host>/api` |
| Auth | `Authorization: Bearer <accessToken>` en todos los endpoints privados |
| Content-Type request | `application/json` salvo login (`application/x-www-form-urlencoded`) |
| Timezone | Fechas en ISO-8601 (`2026-03-18T10:15:30Z`) |
| Idempotencia | `GET` sin efectos; cambios de estado por `PUT`/`POST` |
| Ownership | Rol `CONDUCTOR` solo accede a recursos propios |

### 18.2 Error estandar (400/401/403/404/409/422)

Formato de error unificado esperado:

```json
{
  "timestamp": "2026-03-18T10:16:11Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Regla de negocio no cumplida",
  "details": [
    "solo puede existir un vehiculo activo por conductor"
  ],
  "traceId": "6dc7f7ed-6f5e-4f17-a89f-3d4187ca24c5"
}
```

Reglas de uso de codigos:

| Codigo | Cuándo aplica en mobile |
|---|---|
| `400` | JSON invalido, parametros faltantes o formato incorrecto |
| `401` | Token ausente/expirado/invalido |
| `403` | Token valido pero sin rol/ownership para el recurso |
| `404` | Recurso no encontrado o no visible para ese usuario |
| `409` | Conflicto de estado (ej. transicion de porte no permitida por concurrencia) |
| `422` | Regla de negocio valida sintacticamente pero rechazada (ej. doble vehiculo activo) |

### 18.3 Auth conductor

#### `POST /api/auth/login`

| Campo | Valor |
|---|---|
| Auth | Publico |
| Request | Form URL Encoded: `email`, `password` |
| 200 response | `accessToken`, `tokenType`, `expiresIn`, `expiresAt`, `id`, `email`, `rol`, `conductorId`, `nombre` |
| Errores | `400`, `401` |

Ejemplo minimo:

```json
{
  "accessToken": "eyJhbGciOiJI...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "expiresAt": "2026-03-18T11:00:00Z",
  "id": 44,
  "email": "conductor@acme.com",
  "rol": "CONDUCTOR",
  "conductorId": 12,
  "nombre": "Juan"
}
```

#### `GET /api/auth/me`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR`, `ADMIN`, `SUPERADMIN`, `CLIENTE` |
| Request | Sin body |
| 200 response | Perfil resumido + identificadores de contexto (`conductorId` o `clienteId`) |
| Errores | `401`, `403` |

Ejemplo minimo:

```json
{
  "id": 44,
  "email": "conductor@acme.com",
  "rol": "CONDUCTOR",
  "conductorId": 12,
  "nombre": "Juan",
  "telefono": "+34600000000"
}
```

#### `POST /api/auth/refresh`

| Campo | Valor |
|---|---|
| Auth | Publico con `refreshToken` |
| Request | JSON: `{ "refreshToken": "..." }` |
| 200 response | Nuevo `accessToken` y expiracion |
| Errores | `400`, `401`, `403` |

```json
{
  "accessToken": "eyJhbGciOiJI...nuevo...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "expiresAt": "2026-03-18T12:00:00Z"
}
```

#### `POST /api/auth/logout`

| Campo | Valor |
|---|---|
| Auth | Requerido |
| Request | JSON opcional: `{ "refreshToken": "..." }` |
| 204 response | Sin body |
| Errores | `401`, `403` |

### 18.4 Perfil conductor

#### `GET /api/conductores/{conductorId}`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | `conductorId` path param |
| 200 response | Perfil completo del conductor |
| Errores | `401`, `403`, `404` |

#### `PUT /api/conductores/{conductorId}`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | JSON parcial: `nombre`, `apellidos`, `telefono`, `ciudadBase`, `latitudBase`, `longitudBase`, `radioAccionKm`, `diasLaborables` |
| 200 response | Perfil actualizado |
| Errores | `400`, `401`, `403`, `404`, `422` |

### 18.5 Vehiculos del conductor

#### `GET /api/conductores/{conductorId}/vehiculos`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | Query opcional: `historico=true|false` (default `false`) |
| 200 response | Lista de vehiculos del conductor |
| Errores | `401`, `403`, `404` |

#### `GET /api/conductores/{conductorId}/vehiculos/activo`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | Sin body |
| 200 response | Vehiculo activo |
| Errores | `401`, `403`, `404` |

#### `PUT /api/conductores/{conductorId}/vehiculos/activo`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | JSON: `{ "vehiculoId": 233 }` |
| 200 response | Vehiculo activo actualizado |
| Errores | `400`, `401`, `403`, `404`, `409`, `422` |

Ejemplo minimo:

```json
{
  "conductorId": 12,
  "vehiculoActivo": {
    "id": 233,
    "matricula": "1234ABC",
    "tipo": "CAMION_RIGIDO",
    "estado": "DISPONIBLE"
  },
  "changedAt": "2026-03-18T10:20:00Z"
}
```

Reglas de validacion de vehiculos:

| Regla | Error |
|---|---|
| Un solo vehiculo activo por conductor | `422` |
| Vehiculo debe pertenecer al conductor | `403` |
| Vehiculo en `BAJA` o `EN_MANTENIMIENTO` no puede activarse | `409` |

### 18.6 Portes del conductor

#### `GET /api/portes/conductor/{conductorId}`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | Query opcional: `scope=actuales|historicos|todos` (default `actuales`) |
| 200 response | Lista de portes del conductor |
| Errores | `401`, `403`, `404` |

Definicion de scope:

| Scope | Estados incluidos |
|---|---|
| `actuales` | `PENDIENTE`, `ASIGNADO`, `EN_TRANSITO` |
| `historicos` | `ENTREGADO`, `CANCELADO`, `FACTURADO` |
| `todos` | Todos los anteriores |

#### `GET /api/portes/{porteId}`

| Campo | Valor |
|---|---|
| Auth | Ownership por porte |
| Request | `porteId` path param |
| 200 response | Detalle de porte |
| Errores | `401`, `403`, `404` |

Ejemplo minimo (detalle):

```json
{
  "id": 9001,
  "estado": "EN_TRANSITO",
  "origen": "Madrid",
  "destino": "Valencia",
  "fechaRecogida": "2026-03-18T09:00:00Z",
  "conductor": {
    "id": 12,
    "nombre": "Juan"
  }
}
```

### 18.7 Maquina de estados del porte

#### `PUT /api/portes/{porteId}/estado?nuevo=<ESTADO>`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | Query `nuevo` con enum `EstadoPorte` |
| 200 response | Porte actualizado |
| Errores | `400`, `401`, `403`, `404`, `409`, `422` |

Transiciones permitidas para mobile conductor:

| Estado actual | Estado destino permitido | Error si no cumple |
|---|---|---|
| `PENDIENTE` | `ASIGNADO` | `422` |
| `ASIGNADO` | `EN_TRANSITO`, `CANCELADO` | `422` |
| `EN_TRANSITO` | `ENTREGADO` | `422` |
| `ENTREGADO` | Ninguna para conductor | `403` |
| `CANCELADO` | Ninguna | `409` |
| `FACTURADO` | Ninguna | `409` |

Reglas adicionales:

| Regla | Error |
|---|---|
| Cambio concurrente detectado contra ultimo estado conocido | `409` |
| Porte no pertenece al conductor autenticado | `403` |

### 18.8 Incidencias

#### `GET /api/incidencias/conductor/{conductorId}`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | Query opcional: `estado`, `prioridad`, `page`, `size` |
| 200 response | Lista de incidencias asociadas a portes del conductor |
| Errores | `401`, `403`, `404` |

#### `POST /api/incidencias?porteId={porteId}`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner del porte o `ADMIN/SUPERADMIN` |
| Request | JSON: `titulo`, `descripcion`, `severidad`, `prioridad` |
| 200 response | Incidencia creada |
| Errores | `400`, `401`, `403`, `404`, `422` |

Ejemplo minimo (crear):

```json
{
  "titulo": "Retraso por trafico",
  "descripcion": "Accidente en A3, ETA +30min",
  "severidad": "MEDIA",
  "prioridad": "MEDIA"
}
```

### 18.9 Mapa / realtime

#### `GET /api/v1/fleet/snapshot`

| Campo | Valor |
|---|---|
| Auth | `ADMIN/SUPERADMIN` o `CONDUCTOR` (scope propio) |
| Request | Query opcional: `scope=self|fleet` (default segun rol) |
| 200 response | `snapshotAt`, `drivers[]`, `meta.pollingSuggestedSec`, `meta.degraded`, `meta.degradedReason` |
| Errores | `401`, `403` |

Ejemplo minimo:

```json
{
  "snapshotAt": "2026-03-18T10:30:00Z",
  "drivers": [
    {
      "driverId": "12",
      "lat": 40.4168,
      "lon": -3.7038,
      "recordedAt": "2026-03-18T10:29:52Z",
      "speedKph": 52.4,
      "headingDeg": 178,
      "state": "ONLINE"
    }
  ],
  "meta": {
    "pollingSuggestedSec": 10,
    "degraded": false,
    "degradedReason": null
  }
}
```

#### `POST /api/v1/tracking/drivers/{driverId}/locations`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | JSON: `lat`, `lon`, `recordedAt`, `speedKph`, `headingDeg` |
| 200 response | Sin body |
| Errores | `400`, `401`, `403`, `404`, `422` |

Reglas de polling mobile:

| Caso | Regla |
|---|---|
| Foreground normal | Poll cada `meta.pollingSuggestedSec` (default 10s) |
| Error temporal | Backoff 10s -> 20s -> 40s -> max 60s |
| Modo degradado | Mostrar ultimo snapshot valido hasta 60s |

### 18.10 Rendimiento / ingresos del mes actual

#### `GET /api/conductores/{conductorId}/rendimiento/mes-actual`

| Campo | Valor |
|---|---|
| Auth | `CONDUCTOR` owner o `ADMIN/SUPERADMIN` |
| Request | Query opcional: `tz=Europe/Madrid` |
| 200 response | KPIs del mes corriente |
| Errores | `401`, `403`, `404` |

Ejemplo minimo:

```json
{
  "conductorId": 12,
  "periodo": "2026-03",
  "moneda": "EUR",
  "portesEntregados": 18,
  "ingresoBruto": 4260.5,
  "ajustes": -120.0,
  "ingresoNeto": 4140.5,
  "kmRecorridos": 2810.4,
  "incidenciasAbiertas": 2
}
```

## 19) Versionado y compatibilidad

Objetivo: permitir evolucion del backend sin romper apps moviles publicadas.

| Regla | Politica |
|---|---|
| Version mayor | Cambios breaking solo en nueva ruta (`/api/mobile/v2/...` o equivalente versionada) |
| Version menor | Cambios backward-compatible en misma version (agregar campos/parametros opcionales) |
| Campos existentes | No renombrar ni cambiar tipo en `v1` |
| Nuevos campos | Siempre opcionales para cliente; mobile ignora desconocidos |
| Enum values | Agregar valores nuevos sin eliminar existentes; mobile debe tener fallback visual |
| Deprecaciones | Marcar en docs y mantener al menos 2 releases moviles activas |
| Headers recomendados | `X-API-Version`, `X-Request-Id`, `Deprecation` (si aplica) |

Checklist de compatibilidad previa a release backend:

1. Verificar que endpoints criticos (`login`, `me`, `portes`, `estado`, `incidencias`, `snapshot`) mantienen contrato de `v1`.
2. Ejecutar pruebas de contrato (request/response) contra app movil estable.
3. Publicar changelog de API indicando: agregado, deprecado, fecha de retiro.
