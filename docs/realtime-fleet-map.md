# Realtime Fleet Map (Desktop MVP)

## Objetivo

Habilitar visibilidad casi en tiempo real de conductores en desktop con coste 0€, contratos reutilizables para móvil y degradación controlada.

---

## Contratos API v1

Base path: `/api/v1`

### 1) Fleet snapshot

`GET /fleet/snapshot`

Respuesta:

```json
{
  "snapshotAt": "2026-03-16T10:00:00Z",
  "drivers": [
    {
      "driverId": "7",
      "lat": 40.416,
      "lon": -3.703,
      "recordedAt": "2026-03-16T09:59:50Z",
      "speedKph": 63.5,
      "headingDeg": 180,
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

### 2) Tracking write

`POST /tracking/drivers/{driverId}/locations`

Body:

```json
{
  "lat": 40.416,
  "lon": -3.703,
  "recordedAt": "2026-03-16T10:00:00Z",
  "speedKph": 63.5,
  "headingDeg": 180
}
```

Compatibilidad: se mantiene endpoint legacy `POST /api/conductores/{id}/ubicacion`.

### 3) ETA estimate

`GET /eta/estimate?driverId={id}&jobId={id}`

Respuesta:

```json
{
  "etaMinutes": 12,
  "method": "ROUTE_PROVIDER",
  "estimatedAt": "2026-03-16T10:00:00Z",
  "confidence": "MEDIUM"
}
```

Fallback:
- Si OSRM falla/timeout/rate-limit, `method=HAVERSINE_FALLBACK` y no se devuelve 5xx por esta causa.

---

## Reglas TTL de estado de conductor

Configurables en backend:

- `ONLINE`: edad `<= 30s`
- `STALE`: edad `> 30s` y `<= 180s`
- `OFFLINE`: edad `> 180s`

Props:

- `feature.fleet-realtime.ttl-online-sec`
- `feature.fleet-realtime.ttl-stale-sec`

---

## Degradación y continuidad

Backend:
- Excluye coordenadas inválidas del snapshot.
- Marca `meta.degraded=true` + `meta.degradedReason`.
- Usa caché corta de snapshot (60s configurable) como fallback.

Frontend:
- Mantiene último snapshot válido hasta 60s en timeout puntual.
- Estados de conexión: `ONLINE | DEGRADED | OFFLINE`.
- Reintento automático con backoff.

---

## Observabilidad mínima (MVP)

Se implementan contadores/timers en memoria (sin coste extra):

- `fleet.snapshot.requests`
- `fleet.snapshot.degraded`
- `eta.fallback.count`
- `fleet.tracking.writes`
- latencia media snapshot/eta

Logs estructurados en endpoints/servicios (`fleet.snapshot.*`, `fleet.eta.*`, `fleet.tracking.*`) con:

- `requestId`
- `durationMs`
- `driverCount` / `method` / `degradedReason` / etc.

---

## Feature flags y defaults

### Backend (application.properties)

```properties
feature.fleet-realtime.enabled=${FEATURE_FLEET_REALTIME_ENABLED:false}
feature.fleet-realtime.ttl-online-sec=${FEATURE_FLEET_REALTIME_TTL_ONLINE_SEC:30}
feature.fleet-realtime.ttl-stale-sec=${FEATURE_FLEET_REALTIME_TTL_STALE_SEC:180}
feature.fleet-realtime.polling-suggested-sec=${FEATURE_FLEET_REALTIME_POLLING_SUGGESTED_SEC:10}
feature.fleet-realtime.snapshot-cache-sec=${FEATURE_FLEET_REALTIME_SNAPSHOT_CACHE_SEC:60}
feature.fleet-realtime.max-drivers=${FEATURE_FLEET_REALTIME_MAX_DRIVERS:300}
```

### Frontend (`desktop/.env.example`)

```bash
VITE_FEATURE_FLEET_REALTIME=false
```

---

## Rollout demo escolar

1. Activar backend: `FEATURE_FLEET_REALTIME_ENABLED=true`
2. Activar frontend: `VITE_FEATURE_FLEET_REALTIME=true`
3. Verificar ruta `#/fleet-map` y flujo snapshot/polling.

## Rollback

1. Poner flags en `false`.
2. Reiniciar servicios.
3. La ruta de mapa se oculta y los endpoints v1 quedan desactivados sin romper legacy.

---

## Limitaciones MVP

- Polling (no WS/SSE obligatorio en esta release).
- ETA simple con fallback Haversine.
- Métricas in-memory (sin backend de observabilidad externa).

---

## Evidencia reproducible NFR snapshot (300 drivers)

Comando ejecutado:

```bash
./mvnw -q -Dtest=FleetTrackingServiceTest test
```

Test de evidencia: `buildSnapshot_p95Evidence_for300Drivers_underNfrTarget`

- Simula 300 conductores en snapshot.
- Ejecuta 40 iteraciones y calcula p95 local.
- Criterio: `p95 <= 800ms`.

Resultado observado en ejecución local de referencia:

- `Fleet snapshot performance evidence (300 drivers) p95=0ms`

> Nota: valor orientativo de entorno local de test (sin I/O real de red/DB). Se usa como evidencia reproducible de no regresión para verify del MVP.
