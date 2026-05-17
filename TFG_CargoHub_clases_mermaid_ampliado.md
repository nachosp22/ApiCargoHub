# Diagrama de clases ampliado - CargoHub backend

```mermaid
classDiagram
direction TB

class Usuario {
  +Long id
  +String email
  +String nombre
  +String password
  +RolUsuario rol
  +boolean activo
  +LocalDateTime fechaRegistro
  +LocalDateTime ultimoAcceso
  +String tokenRecuperacion
  +String fotoUrl
  +setEmail(String) void
}

class Cliente {
  +Long id
  +Usuario usuario
  +String nombreEmpresa
  +String cif
  +String direccionFiscal
  +String telefono
  +String emailContacto
  +String sector
  +setCif(String) void
  +setEmailContacto(String) void
}

class Conductor {
  +Long id
  +Usuario usuario
  +String nombre
  +String apellidos
  +String dni
  +String telefono
  +String ciudadBase
  +Double latitudBase
  +Double longitudBase
  +Integer radioAccionKm
  +Double latitudActual
  +Double longitudActual
  +LocalDateTime ultimaActualizacionUbicacion
  +Double velocidadKphActual
  +Integer rumboActualDeg
  +boolean buscarRetorno
  +String diasLaborables
  +boolean disponible
  +setDni(String) void
}

class Porte {
  +Long id
  +String origen
  +String destino
  +String ciudadOrigen
  +String ciudadDestino
  +Double latitudOrigen
  +Double longitudOrigen
  +Double latitudDestino
  +Double longitudDestino
  +Double distanciaKm
  +boolean distanciaEstimada
  +Double precio
  +Double ajustePrecio
  +String motivoAjuste
  +String descripcionCliente
  +Double pesoTotalKg
  +Double volumenTotalM3
  +Double largoMaxPaquete
  +Double anchoMaxPaquete
  +Double altoMaxPaquete
  +TipoVehiculo tipoVehiculoRequerido
  +boolean revisionManual
  +String motivoRevision
  +EstadoPorte estado
  +Integer version
  +LocalDateTime fechaCreacion
  +LocalDateTime fechaRecogida
  +LocalDateTime fechaEntrega
  +String firmaEntregaBase64
  +String firmaEntregaFirmadoPor
  +LocalDateTime firmaEntregaFecha
  +Cliente cliente
  +Conductor conductor
  +Set~Long~ conductoresRechazados
  +getPrecioFinal() Double
}

class Vehiculo {
  +Long id
  +String matricula
  +String marca
  +String modelo
  +TipoVehiculo tipo
  +EstadoVehiculo estado
  +Integer capacidadCargaKg
  +Integer largoUtilMm
  +Integer anchoUtilMm
  +Integer altoUtilMm
  +Double volumenM3
  +Conductor conductor
  +setMatricula(String) void
  +calcularVolumenAutomatico() void
}

class Factura {
  +Long id
  +String numeroSerie
  +Double baseImponible
  +Double iva
  +Double importeTotal
  +LocalDate fechaEmision
  +boolean pagada
  +LocalDate fechaPago
  +String formaPago
  +String condicionesPago
  +String observaciones
  +Porte porte
  +calcularTotales() void
}

class Incidencia {
  +Long id
  +String titulo
  +String descripcion
  +LocalDateTime fechaReporte
  +EstadoIncidencia estado
  +SeveridadIncidencia severidad
  +PrioridadIncidencia prioridad
  +LocalDateTime fechaLimiteSla
  +String resolucion
  +LocalDateTime fechaResolucion
  +Usuario admin
  +Porte porte
}

class IncidenciaEvento {
  +Long id
  +Incidencia incidencia
  +Usuario actor
  +EstadoIncidencia estadoAnterior
  +EstadoIncidencia estadoNuevo
  +LocalDateTime fecha
  +String accion
  +String comentario
}

class TrackingSession {
  +Long id
  +Conductor conductor
  +Porte porte
  +TrackingSessionStatus status
  +TrackingSessionPhase currentPhase
  +LocalDateTime startedAt
  +LocalDateTime endedAt
  +LocalDateTime lastSampleAt
}

class TrackingPause {
  +Long id
  +TrackingSession session
  +String motivo
  +String nota
  +LocalDateTime startedAt
  +LocalDateTime endedAt
}

class LocationSample {
  +Long id
  +TrackingSession session
  +Conductor conductor
  +Porte porte
  +Double lat
  +Double lon
  +LocalDateTime recordedAt
  +LocalDateTime receivedAt
  +Double speedKph
  +Integer headingDeg
}

class FotoCarga {
  +Long id
  +Porte porte
  +TipoFotoCarga tipo
  +String fotoBase64
  +String descripcion
  +LocalDateTime fechaCaptura
}

class BloqueoAgenda {
  +Long id
  +LocalDateTime fechaInicio
  +LocalDateTime fechaFin
  +TipoBloqueoAgenda tipo
  +String titulo
  +Conductor conductor
}

class BloqueoRecurrente {
  +Long id
  +Conductor conductor
  +int diaSemana
  +boolean activo
  +LocalDateTime createdAt
  +LocalDateTime updatedAt
  +onCreate() void
  +onUpdate() void
}

class Notificacion {
  +Long id
  +Long usuarioId
  +String titulo
  +String mensaje
  +TipoNotificacion tipo
  +boolean leida
  +LocalDateTime fechaCreacion
  +Long referenciaId
}

class CargoAnalysisLog {
  +Long id
  +String requestData
  +LocalDateTime requestTimestamp
  +String responseData
  +LocalDateTime responseTimestamp
  +Boolean success
  +String errorMessage
  +Double pesoTotalKg
  +Double volumenTotalM3
  +Double largoMaxPaquete
  +String tipoVehiculoRequerido
  +Boolean revisionManual
  +Porte porte
}

class RolUsuario {
  <<enumeration>>
  ADMIN
  SUPERADMIN
  CONDUCTOR
  CLIENTE
}

class EstadoPorte {
  <<enumeration>>
  PENDIENTE
  ASIGNADO
  EN_RECOGIDA
  EN_TRANSITO
  ENTREGADO
  CANCELADO
  FACTURADO
}

class TipoVehiculo {
  <<enumeration>>
  FURGONETA
  CAMION_PEQUENO
  CAMION_MEDIANO
  CAMION_GRANDE
  TRAILER
}

class EstadoVehiculo {
  <<enumeration>>
  DISPONIBLE
  EN_SERVICIO
  MANTENIMIENTO
  AVERIADO
  RETIRADO
}

class EstadoIncidencia {
  <<enumeration>>
  ABIERTA
  EN_PROCESO
  RESUELTA
  CERRADA
}

class SeveridadIncidencia {
  <<enumeration>>
  BAJA
  MEDIA
  ALTA
  CRITICA
}

class PrioridadIncidencia {
  <<enumeration>>
  BAJA
  MEDIA
  ALTA
  URGENTE
}

class TrackingSessionStatus {
  <<enumeration>>
  ACTIVE
  PAUSED
  COMPLETED
}

class TrackingSessionPhase {
  <<enumeration>>
  PRE_TRIP
  EN_ROUTE
  POST_TRIP
}

class TipoFotoCarga {
  <<enumeration>>
  RECOGIDA
  ENTREGA
  INCIDENCIA
  OTRO
}

class TipoBloqueoAgenda {
  <<enumeration>>
  VACACIONES
  DESCANSO
  MANTENIMIENTO_VEHICULO
  PERSONAL
  OTRO
}

class TipoNotificacion {
  <<enumeration>>
  INFO
  ALERTA
  OFERTA
  INCIDENCIA
  SISTEMA
}

%% Herencia conceptual / perfil de usuario
Usuario "1" <|-- "0..1" Cliente : herencia conceptual
Usuario "1" <|-- "0..1" Conductor : herencia conceptual

%% Agregaciones
Cliente "1" o-- "0..*" Porte : agregacion
Conductor "1" o-- "0..*" Vehiculo : agregacion
Conductor "1" o-- "0..*" BloqueoAgenda : agregacion
Conductor "1" o-- "0..*" BloqueoRecurrente : agregacion
Conductor "1" o-- "0..*" TrackingSession : agregacion
TrackingSession "1" o-- "0..*" LocationSample : agregacion
Porte "1" o-- "0..*" Incidencia : agregacion

%% Composiciones
Porte "1" *-- "0..1" Factura : composicion
Porte "1" *-- "0..*" FotoCarga : composicion
Incidencia "1" *-- "0..*" IncidenciaEvento : composicion
TrackingSession "1" *-- "0..*" TrackingPause : composicion

%% Asociaciones simples
Porte "*" --> "0..1" Conductor : asignado a
Incidencia "*" --> "0..1" Usuario : admin
IncidenciaEvento "*" --> "0..1" Usuario : actor
TrackingSession "*" --> "0..1" Porte : seguimiento de
LocationSample "*" --> "0..1" Porte : muestra de
LocationSample "*" --> "1" Conductor : tomada por
CargoAnalysisLog "*" --> "0..1" Porte : log de analisis
Notificacion "*" ..> "1" Usuario : usuarioId sin FK real

%% Uso de enumeraciones
Usuario --> RolUsuario
Porte --> EstadoPorte
Porte --> TipoVehiculo
Vehiculo --> TipoVehiculo
Vehiculo --> EstadoVehiculo
Incidencia --> EstadoIncidencia
Incidencia --> SeveridadIncidencia
Incidencia --> PrioridadIncidencia
IncidenciaEvento --> EstadoIncidencia
TrackingSession --> TrackingSessionStatus
TrackingSession --> TrackingSessionPhase
FotoCarga --> TipoFotoCarga
BloqueoAgenda --> TipoBloqueoAgenda
Notificacion --> TipoNotificacion
```

## Leyenda

| Simbolo | Significado |
|---|---|
| `<|--` | Herencia / generalizacion conceptual |
| `o--` | Agregacion |
| `*--` | Composicion |
| `-->` | Asociacion simple |
| `..>` | Asociacion debil / sin FK real |

## Nota metodologica

`Cliente` y `Conductor` se representan como especializaciones conceptuales de `Usuario` porque en el dominio son perfiles de usuario. En el codigo JPA no usan `extends`, sino una relacion `@OneToOne(cascade = ALL)` hacia `Usuario`.
