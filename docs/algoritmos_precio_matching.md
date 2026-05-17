# ApiCargoHub — Algoritmos de Precio y Matching

---

## 1. Algoritmo de Cálculo de Precio

### Fórmula

```
precioTotal = (distanciaKm × precioBase) + 20€ (arranque)
precioTotal = precioTotal × multiplicadorSuplementos
Si precioTotal < mínimoVehiculo → precioTotal = mínimoVehiculo
```

### Precios Base por Tipo de Vehículo

| Vehículo   | €/km  | Mínimo |
|------------|-------|--------|
| Furgoneta  | 0,90€ | 40€   |
| Rígido     | 1,40€ | 70€   |
| Tráiler    | 1,65€ | 90€   |

### Suplementos (acumulativos)

| Condición                    | Incremento |
|------------------------------|------------|
| Recogida o entrega nocturna (22:00–06:00) | +20% |
| Recogida o entrega en fin de semana (sáb/dom) | +25% |

Ambos suplementos pueden acumularse (ej: nocturno + finde = ×1.45).

### Ejemplo: Madrid → Barcelona (606 km)

| Vehículo   | Base   | +Nocturno | +Finde    | +Ambos   |
|------------|--------|-----------|-----------|----------|
| Furgoneta  | 565 €  | 678 €     | 706 €     | 820 €    |
| Rígido     | 868 €  | 1.042 €   | 1.085 €   | 1.259 €  |
| Tráiler    | 1.020 €| 1.224 €   | 1.275 €   | 1.479 €  |

---

## 2. Algoritmo de Búsqueda de Conductores (Matching)

### Flujo de 8 filtros secuenciales

Partiendo de todos los conductores con `disponible = true` que trabajan el día de la recogida:

| # | Filtro                 | Regla                                              |
|---|------------------------|----------------------------------------------------|
| 1 | Usuario activo         | `usuario.activo = true`                            |
| 2 | Disponible             | `conductor.disponible = true`                      |
| 3 | Agenda libre           | Sin `BloqueoAgenda` solapado con la ventana del porte |
| 4 | Bloqueos recurrentes   | Sin `BloqueoRecurrente` activo en ningún día del rango |
| 5 | Sin otro viaje         | No tiene portes en `EN_RECOGIDA` / `EN_TRANSITO` simultáneos |
| 6 | Radio de acción        | `distancia(conductor.base, porte.origen) ≤ conductor.radioAccionKm` |
| 7 | Vehículo compatible    | Al menos 1 vehículo del tipo requerido en estado `DISPONIBLE` |
| 8 | Orden determinístico   | Por ID de conductor (menor a mayor)                |

### Cálculo de distancia: Fórmula de Haversine

```
a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlon/2)
c = 2 × atan2(√a, √(1−a))
distancia = 6371 × c  (km)
```

### Cálculo de distancia del porte

```
distanciaPorte = haversine(origen, destino) × 1.2
```

El factor 1.2 corrige la distancia en línea recta (≈120% de la distancia real por carretera).

---

## 3. Resumen del Flujo de Creación de Porte

```
Cliente describe carga
        │
        ▼
   ┌──────────┐    ¿datos     ┌──────────────────┐
   │ Gemini AI│─── suficientes?─→│ NO: revisionManual │
   └──────────┘   suficientes  │   = true           │
        │         ←────────────│                    │
        │ SÍ                   └──────────────────┘
        ▼
   Calcular distancia (Haversine × 1.2)
        │
        ▼
   Calcular precio (algoritmo arriba)
        │
        ▼
   Mostrar presupuesto al cliente
        │
        ▼
   ┌──────────────┐
   │Cliente confirma?│──NO──→ Porte se cancela
   └──────────────┘
        │ SÍ
        ▼
   Matching (8 filtros)
        │
        ▼
   Ofertas enviadas a conductores
```

---

*Documento generado para la defensa del TFG — ApiCargoHub*
