# API CargoHub - Documentación de API REST

## Descripción General
Sistema de gestión de transporte de carga que conecta clientes, conductores y vehículos para realizar portes (transportes) de mercancías.

## Arquitectura
- **Framework**: Spring Boot 4.0.1
- **Base de Datos**: MySQL
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
Iniciar sesión
```
Parámetros:
- email: string
- password: string

Respuesta: {id, email, rol, [conductorId/clienteId], [nombre/nombreEmpresa]}
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
  titulo: string,
  descripcion: string
}
```

#### GET `/api/incidencias`
Listar todas las incidencias

#### GET `/api/incidencias/{id}`
Obtener detalles de una incidencia

#### GET `/api/incidencias/pendientes`
Listar incidencias pendientes (ABIERTA)

#### GET `/api/incidencias/porte/{porteId}`
Listar incidencias de un porte específico

#### PUT `/api/incidencias/{id}/resolver`
Resolver incidencia (Admin)
```
Parámetros:
- adminId: Long
- estado: RESUELTA | DESESTIMADA

Body: string (resolución/comentario)
```

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

### Encriptación de Contraseñas
Todas las contraseñas se almacenan encriptadas usando BCrypt.

### Roles de Usuario
- **SUPERADMIN**: Acceso total
- **ADMIN**: Gestión de operaciones y facturación
- **CONDUCTOR**: Ver ofertas, aceptar portes, reportar ubicación
- **CLIENTE**: Crear portes, ver historial

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
- MySQL 8.0+
- XAMPP o servidor MySQL local

### Configuración Base de Datos
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/cargohub_db?createDatabaseIfNotExist=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```

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
