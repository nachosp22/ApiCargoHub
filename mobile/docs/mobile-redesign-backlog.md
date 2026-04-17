# Backlog de Rediseño Minimalista Mobile

Alcance: app Android Java con fragments + drawer. Objetivo: mejorar UX visual sin romper flujos funcionales actuales.

## Progreso de fases
- **Fase completada (actual)**: Viajes + Detalle de viaje con lenguaje visual unificado respecto a ofertas.
- **Estado tickets impactados**:
  - Ticket 4 (Viajes listas): **Completado**
  - Ticket 5 (Detalle de viaje): **Completado**
  - Ticket 2 y Ticket 3 (Ofertas): **Completado previamente**

## Ticket 1 - Login minimalista y claro
- **Pantalla**: `activity_login`
- **Objetivo**: simplificar jerarquia visual y feedback de acceso.
- **Criterios de aceptacion**:
  - Al abrir login, el usuario identifica en menos de 2 niveles visuales: titulo, campos y CTA principal.
  - Al enviar credenciales, el CTA cambia a estado de carga y se bloquea doble toque.
  - Errores de validacion muestran mensaje junto al campo correspondiente sin tapar otros elementos.

## Ticket 2 - Ofertas (bandeja) rediseño minimalista
- **Pantalla**: `fragment_offer_inbox`
- **Objetivo**: mejorar escaneabilidad de ofertas y visibilidad de accion principal.
- **Criterios de aceptacion**:
  - Cada tarjeta muestra titulo, estado, agenda/carga, precio y un CTA visible sin truncar en pantallas compactas.
  - El usuario puede abrir detalle tocando tarjeta completa o CTA y ambos caminos llevan al mismo detalle.
  - Estados `loading`, `empty` y `error` se ven con layout consistente y boton de reintento funcional.

## Ticket 3 - Detalle de oferta con CTA principal/secundario
- **Pantalla**: `fragment_offer_detail`
- **Objetivo**: reforzar decision rapida de aceptar/rechazar con jerarquia clara.
- **Criterios de aceptacion**:
  - El CTA primario (`Aceptar oferta`) se visualiza primero y el secundario (`Rechazar oferta`) como menor enfasis.
  - Si la oferta no admite accion, ambos botones quedan deshabilitados y el texto helper explica el motivo.
  - Estados `loading`, `empty` y `error` reutilizan el mismo patron visual de bandeja de ofertas.

## Ticket 4 - Viajes (listas activo/proximo/historico) coherencia visual
- **Pantallas**: `fragment_trip_list` (3 modos)
- **Objetivo**: unificar encabezados, tarjetas y mensajes de estado con el lenguaje minimalista.
- **Estado**: Completado
- **Criterios de aceptacion**:
  - Los 3 modos conservan mismo layout base y solo cambian textos de contexto y filtro.
  - En cada modo, `loading`, `empty` y `error` usan el mismo patron visual y boton de reintento.
  - Abrir detalle desde tarjeta sigue funcionando sin cambios en navegacion.

## Ticket 5 - Detalle de viaje enfocado en accion operativa
- **Pantalla**: `fragment_trip_detail`
- **Objetivo**: priorizar estado del viaje y accion siguiente.
- **Estado**: Completado
- **Criterios de aceptacion**:
  - El usuario identifica estado, horario y accion principal antes de hacer scroll.
  - Tracking e incidencias se muestran como bloques secundarios visualmente separados.
  - Acciones de viaje mantienen exactamente la misma logica de habilitacion/deshabilitacion actual.

## Ticket 6 - Incidencias (opciones, activas, historial, nueva)
- **Pantallas**: `fragment_incidencias_options`, `fragment_incidencias_activas`, `fragment_historial_incidencias`, `fragment_nueva_incidencia`
- **Objetivo**: reducir friccion al reportar y revisar incidencias.
- **Criterios de aceptacion**:
  - La pantalla de nueva incidencia mantiene orden lineal: porte, titulo, descripcion, severidad, prioridad, CTA.
  - Listas de incidencias muestran estado legible y separacion clara entre items.
  - Mensajes de exito/error se muestran en contexto y no bloquean volver a operar.

## Ticket 7 - Tracking con estados operativos claros
- **Pantalla**: `fragment_tracking_status`
- **Objetivo**: hacer evidente si tracking esta listo, corriendo o bloqueado por permisos.
- **Criterios de aceptacion**:
  - El estado actual aparece en primer bloque con copy corto y accion sugerida.
  - Botones de permiso/inicio/detencion cambian habilitacion segun estado real del tracking.
  - Si no hay viaje activo, se comunica claramente y el usuario puede abrir viaje cuando exista.

## Ticket 8 - Perfil util y accionable
- **Pantalla**: `fragment_profile`
- **Objetivo**: ordenar datos del conductor y accesos rapidos a agenda/vehiculo.
- **Criterios de aceptacion**:
  - Se muestran datos principales del conductor con jerarquia visual legible.
  - Accesos a agenda y vehiculos son visibles y alcanzables sin scroll largo.
  - Si falta informacion remota, se muestra placeholder claro sin romper la pantalla.

## Ticket 9 - QA manual E2E conductor (opcion 1)
- **Objetivo**: ejecutar cierre funcional manual sobre los flujos criticos del conductor.
- **Detalle operativo**: ver `mobile/README.md` en la seccion `QA manual E2E conductor (opcion 1)` para pasos y resultados esperados por caso.
- **Casos incluidos**: login, ofertas, viajes, detalle de viaje, incidencias, perfil, agenda, vehiculo, tracking, logout.

### Matriz de ejecucion

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
