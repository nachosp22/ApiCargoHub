# Tablas del diagrama de clases - CargoHub backend

Este documento complementa los diagramas UML. El diagrama reducido queda limpio, sin atributos ni metodos; el detalle se recoge aqui clase por clase. En UML, los atributos de las entidades se representan como privados (`-`) y los metodos de dominio como publicos (`+`).

## 1. Usuario

| Elemento | Detalle |
|---|---|
| Tabla JPA | `usuarios` |
| Responsabilidad | Cuenta base de acceso al sistema. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | email | String |
| - privado | nombre | String |
| - privado | password | String |
| - privado | rol | RolUsuario |
| - privado | activo | boolean |
| - privado | fechaRegistro | LocalDateTime |
| - privado | ultimoAcceso | LocalDateTime |
| - privado | tokenRecuperacion | String |
| - privado | fotoUrl | String |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| + publico | setEmail(String) | Normaliza el email a minusculas. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Usuario - Cliente | Herencia conceptual | 1 a 0..1 |
| Usuario - Conductor | Herencia conceptual | 1 a 0..1 |

## 2. Cliente

| Elemento | Detalle |
|---|---|
| Tabla JPA | `clientes` |
| Responsabilidad | Perfil empresarial que solicita portes. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | usuario | Usuario |
| - privado | nombreEmpresa | String |
| - privado | cif | String |
| - privado | direccionFiscal | String |
| - privado | telefono | String |
| - privado | emailContacto | String |
| - privado | sector | String |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| + publico | setCif(String) | Normaliza el CIF a mayusculas. |
| + publico | setEmailContacto(String) | Normaliza el email de contacto a minusculas. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Cliente - Usuario | Herencia conceptual / perfil | 0..1 a 1 |
| Cliente - Porte | Agregacion | 1 a 0..* |

## 3. Conductor

| Elemento | Detalle |
|---|---|
| Tabla JPA | `conductores` |
| Responsabilidad | Perfil operativo que realiza portes y reporta ubicacion. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | usuario | Usuario |
| - privado | nombre | String |
| - privado | apellidos | String |
| - privado | dni | String |
| - privado | telefono | String |
| - privado | ciudadBase | String |
| - privado | latitudBase | Double |
| - privado | longitudBase | Double |
| - privado | radioAccionKm | Integer |
| - privado | latitudActual | Double |
| - privado | longitudActual | Double |
| - privado | ultimaActualizacionUbicacion | LocalDateTime |
| - privado | velocidadKphActual | Double |
| - privado | rumboActualDeg | Integer |
| - privado | buscarRetorno | boolean |
| - privado | diasLaborables | String |
| - privado | disponible | boolean |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| + publico | setDni(String) | Normaliza el DNI a mayusculas. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Conductor - Usuario | Herencia conceptual / perfil | 0..1 a 1 |
| Conductor - Vehiculo | Agregacion | 1 a 0..* |
| Conductor - BloqueoAgenda | Agregacion | 1 a 0..* |
| Conductor - BloqueoRecurrente | Agregacion | 1 a 0..* |
| Conductor - TrackingSession | Agregacion | 1 a 0..* |
| Porte - Conductor | Asociacion | * a 0..1 |

## 4. Porte

| Elemento | Detalle |
|---|---|
| Tabla JPA | `portes` |
| Responsabilidad | Servicio principal de transporte solicitado por un cliente y opcionalmente asignado a un conductor. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | origen | String |
| - privado | destino | String |
| - privado | ciudadOrigen | String |
| - privado | ciudadDestino | String |
| - privado | latitudOrigen | Double |
| - privado | longitudOrigen | Double |
| - privado | latitudDestino | Double |
| - privado | longitudDestino | Double |
| - privado | distanciaKm | Double |
| - privado | distanciaEstimada | boolean |
| - privado | precio | Double |
| - privado | ajustePrecio | Double |
| - privado | motivoAjuste | String |
| - privado | descripcionCliente | String |
| - privado | pesoTotalKg | Double |
| - privado | volumenTotalM3 | Double |
| - privado | largoMaxPaquete | Double |
| - privado | anchoMaxPaquete | Double |
| - privado | altoMaxPaquete | Double |
| - privado | tipoVehiculoRequerido | TipoVehiculo |
| - privado | revisionManual | boolean |
| - privado | motivoRevision | String |
| - privado | estado | EstadoPorte |
| - privado | version | Integer |
| - privado | fechaCreacion | LocalDateTime |
| - privado | fechaRecogida | LocalDateTime |
| - privado | fechaEntrega | LocalDateTime |
| - privado | firmaEntregaBase64 | String |
| - privado | firmaEntregaFirmadoPor | String |
| - privado | firmaEntregaFecha | LocalDateTime |
| - privado | cliente | Cliente |
| - privado | conductor | Conductor |
| - privado | conductoresRechazados | Set<Long> |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| + publico | getPrecioFinal() | Calcula el precio base mas el ajuste aplicado. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Cliente - Porte | Agregacion | 1 a 0..* |
| Porte - Conductor | Asociacion | * a 0..1 |
| Porte - Factura | Composicion | 1 a 0..1 |
| Porte - FotoCarga | Composicion | 1 a 0..* |
| Porte - Incidencia | Agregacion | 1 a 0..* |
| TrackingSession - Porte | Asociacion | * a 0..1 |
| LocationSample - Porte | Asociacion | * a 0..1 |
| CargoAnalysisLog - Porte | Asociacion debil | * a 0..1 |

## 5. Vehiculo

| Elemento | Detalle |
|---|---|
| Tabla JPA | `vehiculos` |
| Responsabilidad | Vehiculo usado por conductores para realizar portes. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | matricula | String |
| - privado | marca | String |
| - privado | modelo | String |
| - privado | tipo | TipoVehiculo |
| - privado | estado | EstadoVehiculo |
| - privado | capacidadCargaKg | Integer |
| - privado | largoUtilMm | Integer |
| - privado | anchoUtilMm | Integer |
| - privado | altoUtilMm | Integer |
| - privado | volumenM3 | Double |
| - privado | conductor | Conductor |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| + publico | setMatricula(String) | Normaliza la matricula a mayusculas. |
| + publico | calcularVolumenAutomatico() | Calcula el volumen util antes de guardar o actualizar. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Conductor - Vehiculo | Agregacion | 1 a 0..* |

## 6. Factura

| Elemento | Detalle |
|---|---|
| Tabla JPA | `facturas` |
| Responsabilidad | Documento economico generado para un porte. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | numeroSerie | String |
| - privado | baseImponible | Double |
| - privado | iva | Double |
| - privado | importeTotal | Double |
| - privado | fechaEmision | LocalDate |
| - privado | pagada | boolean |
| - privado | fechaPago | LocalDate |
| - privado | formaPago | String |
| - privado | condicionesPago | String |
| - privado | observaciones | String |
| - privado | porte | Porte |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| + publico | calcularTotales() | Calcula IVA e importe total. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Porte - Factura | Composicion | 1 a 0..1 |

## 7. Incidencia

| Elemento | Detalle |
|---|---|
| Tabla JPA | `incidencias` |
| Responsabilidad | Problema operativo asociado a un porte. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | titulo | String |
| - privado | descripcion | String |
| - privado | fechaReporte | LocalDateTime |
| - privado | estado | EstadoIncidencia |
| - privado | severidad | SeveridadIncidencia |
| - privado | prioridad | PrioridadIncidencia |
| - privado | fechaLimiteSla | LocalDateTime |
| - privado | resolucion | String |
| - privado | fechaResolucion | LocalDateTime |
| - privado | admin | Usuario |
| - privado | porte | Porte |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| No aplica | Sin metodos de dominio | Gestionada mediante servicios. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Porte - Incidencia | Agregacion | 1 a 0..* |
| Incidencia - IncidenciaEvento | Composicion | 1 a 0..* |
| Incidencia - Usuario | Asociacion | * a 0..1 |

## 8. IncidenciaEvento

| Elemento | Detalle |
|---|---|
| Tabla JPA | `incidencia_eventos` |
| Responsabilidad | Registro historico de cambios sobre una incidencia. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | incidencia | Incidencia |
| - privado | actor | Usuario |
| - privado | estadoAnterior | EstadoIncidencia |
| - privado | estadoNuevo | EstadoIncidencia |
| - privado | fecha | LocalDateTime |
| - privado | accion | String |
| - privado | comentario | String |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| No aplica | Sin metodos de dominio | Entidad historica. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Incidencia - IncidenciaEvento | Composicion | 1 a 0..* |
| IncidenciaEvento - Usuario | Asociacion | * a 0..1 |

## 9. TrackingSession

| Elemento | Detalle |
|---|---|
| Tabla JPA | `tracking_sessions` |
| Responsabilidad | Sesion de seguimiento GPS de un conductor y opcionalmente de un porte. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | conductor | Conductor |
| - privado | porte | Porte |
| - privado | status | TrackingSessionStatus |
| - privado | currentPhase | TrackingSessionPhase |
| - privado | startedAt | LocalDateTime |
| - privado | endedAt | LocalDateTime |
| - privado | lastSampleAt | LocalDateTime |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| No aplica | Sin metodos de dominio | Gestionada mediante servicios. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Conductor - TrackingSession | Agregacion | 1 a 0..* |
| TrackingSession - Porte | Asociacion | * a 0..1 |
| TrackingSession - TrackingPause | Composicion | 1 a 0..* |
| TrackingSession - LocationSample | Agregacion | 1 a 0..* |

## 10. TrackingPause

| Elemento | Detalle |
|---|---|
| Tabla JPA | `tracking_pauses` |
| Responsabilidad | Pausa temporal dentro de una sesion de tracking. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | session | TrackingSession |
| - privado | motivo | String |
| - privado | nota | String |
| - privado | startedAt | LocalDateTime |
| - privado | endedAt | LocalDateTime |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| TrackingSession - TrackingPause | Composicion | 1 a 0..* |

## 11. LocationSample

| Elemento | Detalle |
|---|---|
| Tabla JPA | `location_samples` |
| Responsabilidad | Muestra puntual de ubicacion GPS. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | session | TrackingSession |
| - privado | conductor | Conductor |
| - privado | porte | Porte |
| - privado | lat | Double |
| - privado | lon | Double |
| - privado | recordedAt | LocalDateTime |
| - privado | receivedAt | LocalDateTime |
| - privado | speedKph | Double |
| - privado | headingDeg | Integer |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| TrackingSession - LocationSample | Agregacion | 1 a 0..* |
| LocationSample - Conductor | Agregacion | * a 1 |
| LocationSample - Porte | Asociacion | * a 0..1 |

## 12. FotoCarga

| Elemento | Detalle |
|---|---|
| Tabla JPA | `fotos_carga` |
| Responsabilidad | Evidencia fotografica asociada a un porte. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | porte | Porte |
| - privado | tipo | TipoFotoCarga |
| - privado | fotoBase64 | String |
| - privado | descripcion | String |
| - privado | fechaCaptura | LocalDateTime |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Porte - FotoCarga | Composicion | 1 a 0..* |

## 13. BloqueoAgenda

| Elemento | Detalle |
|---|---|
| Tabla JPA | `agenda_bloqueos` |
| Responsabilidad | Bloqueo puntual de disponibilidad de conductor. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | fechaInicio | LocalDateTime |
| - privado | fechaFin | LocalDateTime |
| - privado | tipo | TipoBloqueoAgenda |
| - privado | titulo | String |
| - privado | conductor | Conductor |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Conductor - BloqueoAgenda | Agregacion | 1 a 0..* |

## 14. BloqueoRecurrente

| Elemento | Detalle |
|---|---|
| Tabla JPA | `bloqueos_recurrentes` |
| Responsabilidad | Bloqueo semanal recurrente de disponibilidad. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | conductor | Conductor |
| - privado | diaSemana | int |
| - privado | activo | boolean |
| - privado | createdAt | LocalDateTime |
| - privado | updatedAt | LocalDateTime |

| Visibilidad UML | Metodo | Descripcion |
|---|---|---|
| + publico | onCreate() | Inicializa fechas de creacion y actualizacion. |
| + publico | onUpdate() | Actualiza la fecha de modificacion. |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Conductor - BloqueoRecurrente | Agregacion | 1 a 0..* |

## 15. Notificacion

| Elemento | Detalle |
|---|---|
| Tabla JPA | `notificaciones` |
| Responsabilidad | Registro de avisos del sistema. Fuera del alcance defendido. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | usuarioId | Long |
| - privado | titulo | String |
| - privado | mensaje | String |
| - privado | tipo | TipoNotificacion |
| - privado | leida | boolean |
| - privado | fechaCreacion | LocalDateTime |
| - privado | referenciaId | Long |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| Notificacion - Usuario | Asociacion debil sin FK real | * a 1 |

## 16. CargoAnalysisLog

| Elemento | Detalle |
|---|---|
| Tabla JPA | `cargo_analysis_logs` |
| Responsabilidad | Log tecnico de analisis de carga. Soporte tecnico, no funcionalidad principal defendida. |

| Visibilidad UML | Atributo | Tipo |
|---|---|---|
| - privado | id | Long |
| - privado | requestData | String |
| - privado | requestTimestamp | LocalDateTime |
| - privado | responseData | String |
| - privado | responseTimestamp | LocalDateTime |
| - privado | success | Boolean |
| - privado | errorMessage | String |
| - privado | pesoTotalKg | Double |
| - privado | volumenTotalM3 | Double |
| - privado | largoMaxPaquete | Double |
| - privado | tipoVehiculoRequerido | String |
| - privado | revisionManual | Boolean |
| - privado | porte | Porte |

| Relacion | Tipo UML | Cardinalidad |
|---|---|---|
| CargoAnalysisLog - Porte | Asociacion debil | * a 0..1 |

## Enumeraciones

| Enumeracion | Valores |
|---|---|
| RolUsuario | ADMIN, SUPERADMIN, CONDUCTOR, CLIENTE |
| EstadoPorte | PENDIENTE, ASIGNADO, EN_RECOGIDA, EN_TRANSITO, ENTREGADO, CANCELADO, FACTURADO |
| TipoVehiculo | FURGONETA, CAMION_PEQUENO, CAMION_MEDIANO, CAMION_GRANDE, TRAILER |
| EstadoVehiculo | DISPONIBLE, EN_SERVICIO, MANTENIMIENTO, AVERIADO, RETIRADO |
| EstadoIncidencia | ABIERTA, EN_PROCESO, RESUELTA, CERRADA |
| SeveridadIncidencia | BAJA, MEDIA, ALTA, CRITICA |
| PrioridadIncidencia | BAJA, MEDIA, ALTA, URGENTE |
| TrackingSessionStatus | ACTIVE, PAUSED, COMPLETED |
| TrackingSessionPhase | PRE_TRIP, EN_ROUTE, POST_TRIP |
| TipoFotoCarga | RECOGIDA, ENTREGA, INCIDENCIA, OTRO |
| TipoBloqueoAgenda | VACACIONES, DESCANSO, MANTENIMIENTO_VEHICULO, PERSONAL, OTRO |
| TipoNotificacion | INFO, ALERTA, OFERTA, INCIDENCIA, SISTEMA |
