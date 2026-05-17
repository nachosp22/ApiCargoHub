# Diagrama de clases reducido - CargoHub backend

```mermaid
classDiagram
direction TB

class Usuario {
  +Long id
  +String email
  +String nombre
  +RolUsuario rol
  +boolean activo
  +setEmail(String) void
}

class Cliente {
  +Long id
  +Usuario usuario
  +String nombreEmpresa
  +String cif
  +String emailContacto
  +setCif(String) void
  +setEmailContacto(String) void
}

class Conductor {
  +Long id
  +Usuario usuario
  +String dni
  +String ciudadBase
  +boolean disponible
  +Double latitudActual
  +setDni(String) void
}

class Vehiculo {
  +Long id
  +String matricula
  +TipoVehiculo tipo
  +EstadoVehiculo estado
  +Conductor conductor
  +setMatricula(String) void
  +calcularVolumenAutomatico() void
}

class Porte {
  +Long id
  +String origen
  +String destino
  +Double precio
  +EstadoPorte estado
  +Cliente cliente
  +Conductor conductor
  +getPrecioFinal() Double
}

class Factura {
  +Long id
  +String numeroSerie
  +Double importeTotal
  +boolean pagada
  +Porte porte
  +calcularTotales() void
}

class FotoCarga {
  +Long id
  +Porte porte
  +TipoFotoCarga tipo
  +LocalDateTime fechaCaptura
}

class Incidencia {
  +Long id
  +String titulo
  +EstadoIncidencia estado
  +PrioridadIncidencia prioridad
  +Porte porte
}

class IncidenciaEvento {
  +Long id
  +Incidencia incidencia
  +Usuario actor
  +EstadoIncidencia estadoNuevo
}

class TrackingSession {
  +Long id
  +Conductor conductor
  +Porte porte
  +TrackingSessionStatus status
  +TrackingSessionPhase currentPhase
}

class TrackingPause {
  +Long id
  +TrackingSession session
  +String motivo
  +LocalDateTime startedAt
}

class LocationSample {
  +Long id
  +TrackingSession session
  +Conductor conductor
  +Porte porte
  +Double lat
  +Double lon
}

class BloqueoAgenda {
  +Long id
  +LocalDateTime fechaInicio
  +LocalDateTime fechaFin
  +TipoBloqueoAgenda tipo
  +Conductor conductor
}

class BloqueoRecurrente {
  +Long id
  +Conductor conductor
  +int diaSemana
  +boolean activo
  +onCreate() void
  +onUpdate() void
}

class Notificacion {
  +Long id
  +Long usuarioId
  +String titulo
  +TipoNotificacion tipo
}

class CargoAnalysisLog {
  +Long id
  +Boolean success
  +Porte porte
  +String tipoVehiculoRequerido
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
```

## Leyenda

| Simbolo | Significado |
|---|---|
| `<|--` | Herencia / generalizacion conceptual |
| `o--` | Agregacion |
| `*--` | Composicion |
| `-->` | Asociacion simple |
| `..>` | Asociacion debil / sin FK real |
