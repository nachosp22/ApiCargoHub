<script setup lang="ts">
import maplibregl, { type Map as MapLibreMap, type Marker, type StyleSpecification } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useFleetTrackingStore } from '@/stores/fleetTracking'
import { useConductoresStore } from '@/stores/conductores'
import type { DriverLocationPoint, DriverState } from '@/services/api'
import { computeMarkerDiff, getDriverRenderSignature } from './fleetMapMarkerDiff'

const fleetStore = useFleetTrackingStore()
const conductoresStore = useConductoresStore()

const mapContainer = ref<HTMLElement | null>(null)
const selectedDriverId = ref<string | null>(null)
type ReportingState = 'REPORTING' | 'NOT_REPORTING'

const stateFilter = ref<'ALL' | ReportingState>('ALL')
const driverSearch = ref('')
const userInteracting = ref(false)
const countdown = ref(10)
const hasAppliedInitialViewport = ref(false)

let map: MapLibreMap | null = null
const markersByDriverId = new Map<string, Marker>()
const lastRenderedRecordedAtByDriverId = new Map<string, string>()

const conductoresById = computed(() => {
  const map = new Map<string, { nombre: string; apellidos: string }>()
  for (const conductor of conductoresStore.conductores) {
    map.set(String(conductor.id), {
      nombre: conductor.nombre,
      apellidos: conductor.apellidos,
    })
  }
  return map
})

type DriverWithProfile = DriverLocationPoint & {
  driverName?: string
  driverLastName?: string
  nombre?: string
  apellidos?: string
}

function normalizeOptionalText(value: unknown): string | null {
  if (typeof value !== 'string') return null
  const normalized = value.trim()
  return normalized.length > 0 ? normalized : null
}

function getDriverIdentity(driver: DriverLocationPoint): { primary: string; secondary: string | null } {
  const typedDriver = driver as DriverWithProfile
  const fallbackFromStore = conductoresById.value.get(driver.driverId)

  const firstName =
    normalizeOptionalText(typedDriver.driverName) ??
    normalizeOptionalText(typedDriver.nombre) ??
    normalizeOptionalText(fallbackFromStore?.nombre)

  const lastName =
    normalizeOptionalText(typedDriver.driverLastName) ??
    normalizeOptionalText(typedDriver.apellidos) ??
    normalizeOptionalText(fallbackFromStore?.apellidos)

  const fullName = [firstName, lastName].filter((part): part is string => !!part).join(' ').trim()
  const hasName = fullName.length > 0
  const primary = hasName ? fullName : `Conductor #${driver.driverId}`
  return { primary, secondary: hasName ? `#${driver.driverId}` : null }
}

function getDriverLabel(driver: DriverLocationPoint): string {
  const identity = getDriverIdentity(driver)
  return identity.secondary ? `${identity.primary} · ${identity.secondary}` : identity.primary
}

const filteredDrivers = computed(() => {
  const search = driverSearch.value.trim().toLowerCase()
  return fleetStore.drivers.filter((driver) => {
    const reportingState = mapDriverStateToReportingState(driver.state)
    const matchesState = stateFilter.value === 'ALL' || reportingState === stateFilter.value
    const identity = getDriverIdentity(driver)
    const searchBlob = [driver.driverId, identity.primary, identity.secondary ?? ''].join(' ').toLowerCase()
    const matchesSearch = search.length === 0 || searchBlob.includes(search)
    return matchesState && matchesSearch
  })
})

const totalDriversCount = computed(() => fleetStore.drivers.length)
const reportingDriversCount = computed(
  () => fleetStore.drivers.filter((driver) => mapDriverStateToReportingState(driver.state) === 'REPORTING').length,
)
const notReportingDriversCount = computed(() => totalDriversCount.value - reportingDriversCount.value)

function stateLabel(state: DriverState): string {
  return mapDriverStateToReportingState(state) === 'REPORTING'
    ? 'Reporta ubicación'
    : 'No reporta ubicación'
}

function stateChipClass(state: DriverState): string {
  if (mapDriverStateToReportingState(state) === 'REPORTING') return 'bg-blue-100 text-blue-800 ring-blue-200'
  return 'bg-rose-100 text-rose-800 ring-rose-200'
}

function mapDriverStateToReportingState(state: DriverState): ReportingState {
  return state === 'ONLINE' ? 'REPORTING' : 'NOT_REPORTING'
}

function formatRecordedAt(value: string): string {
  const parsed = Date.parse(value)
  if (!Number.isNaN(parsed)) {
    return new Intl.DateTimeFormat('es-ES', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      day: '2-digit',
      month: '2-digit',
    }).format(new Date(parsed))
  }
  return value
}

watch(
  () => fleetStore.drivers,
  (drivers) => {
    updateMarkersIncrementally(drivers)
    if (selectedDriverId.value && !drivers.some((driver) => driver.driverId === selectedDriverId.value)) {
      selectedDriverId.value = null
    }
    countdown.value = Math.ceil(fleetStore.pollingMs / 1000)
  },
  { deep: false }
)

let countdownInterval: number | null = null

onMounted(() => {
  initializeMap()
  if (conductoresStore.conductores.length === 0 && !conductoresStore.loading) {
    void conductoresStore.fetchConductores()
  }
  fleetStore.startPolling()
  countdown.value = Math.ceil(fleetStore.pollingMs / 1000)
  countdownInterval = window.setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    }
  }, 1000)
})

onUnmounted(() => {
  fleetStore.stopPolling()
  if (countdownInterval !== null) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }
  clearAllMarkers()
  if (map) {
    map.remove()
    map = null
  }
})

watch(selectedDriverId, (driverId) => {
  if (!driverId) return
  const driver = fleetStore.drivers.find((item) => item.driverId === driverId)
  if (!map || !driver) return

  const marker = markersByDriverId.get(driver.driverId)
  map.easeTo({ center: [driver.lon, driver.lat], duration: 450, zoom: Math.max(map.getZoom(), 11) })
  marker?.togglePopup()
})

function initializeMap(): void {
  if (!mapContainer.value || map) return

  map = new maplibregl.Map({
    container: mapContainer.value,
    style: buildRasterStyle(),
    center: [-3.7038, 40.4168],
    zoom: 6,
    attributionControl: {},
  })

  map.addControl(new maplibregl.NavigationControl({ showCompass: true }), 'top-right')

  map.on('movestart', () => {
    userInteracting.value = true
  })
  map.on('moveend', () => {
    setTimeout(() => {
      userInteracting.value = false
    }, 500)
  })
}

function updateMarkersIncrementally(drivers: DriverLocationPoint[]): void {
  if (!map) return

  const diff = computeMarkerDiff(drivers, lastRenderedRecordedAtByDriverId)

  for (const driverId of diff.toRemove) {
    const marker = markersByDriverId.get(driverId)
    if (!marker) continue
    marker.remove()
    markersByDriverId.delete(driverId)
    lastRenderedRecordedAtByDriverId.delete(driverId)
  }

  for (const driver of diff.toUpsert) {
    const existing = markersByDriverId.get(driver.driverId)
    if (existing) {
      existing.setLngLat([driver.lon, driver.lat])
      decorateMarkerElement(existing.getElement(), driver.state)
      existing.setPopup(new maplibregl.Popup({ closeButton: false, offset: 18 }).setDOMContent(buildPopupContent(driver)))
    } else {
      const markerElement = document.createElement('button')
      markerElement.type = 'button'
      decorateMarkerElement(markerElement, driver.state)
      const identity = getDriverIdentity(driver)
      markerElement.title = identity.secondary
        ? `${identity.primary} (${identity.secondary})`
        : identity.primary
      markerElement.addEventListener('click', () => {
        selectedDriverId.value = driver.driverId
      })

      const marker = new maplibregl.Marker({ element: markerElement })
        .setLngLat([driver.lon, driver.lat])
        .setPopup(new maplibregl.Popup({ closeButton: false, offset: 18 }).setDOMContent(buildPopupContent(driver)))
        .addTo(map)
      markersByDriverId.set(driver.driverId, marker)
    }

    lastRenderedRecordedAtByDriverId.set(driver.driverId, getDriverRenderSignature(driver))
  }

  if (!hasAppliedInitialViewport.value && !userInteracting.value) {
    fitMapToDrivers(drivers)
    hasAppliedInitialViewport.value = true
  }
}

function fitMapToDrivers(drivers: DriverLocationPoint[]): void {
  if (!map || drivers.length === 0) return
  if (drivers.length === 1) {
    map.easeTo({ center: [drivers[0].lon, drivers[0].lat], zoom: 11, duration: 500 })
    return
  }

  const bounds = new maplibregl.LngLatBounds()
  for (const driver of drivers) {
    bounds.extend([driver.lon, driver.lat])
  }
  map.fitBounds(bounds, { padding: 80, duration: 500, maxZoom: 12 })
}

function clearAllMarkers(): void {
  for (const marker of markersByDriverId.values()) marker.remove()
  markersByDriverId.clear()
  lastRenderedRecordedAtByDriverId.clear()
}

function colorForState(state: DriverState): string {
  if (mapDriverStateToReportingState(state) === 'REPORTING') return '#2563eb'
  return '#ef4444'
}

function buildRasterStyle(): StyleSpecification {
  return {
    version: 8,
    sources: {
      cartoVoyager: {
        type: 'raster',
        tiles: [
          'https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
          'https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
          'https://c.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
          'https://d.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
        ],
        tileSize: 256,
        attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
      },
    },
    layers: [
      {
        id: 'carto-basemap',
        type: 'raster',
        source: 'cartoVoyager',
        minzoom: 0,
        maxzoom: 20,
      },
    ],
  }
}

function decorateMarkerElement(element: HTMLElement, state: DriverState): void {
  element.className = 'fleet-map-marker'
  element.style.backgroundColor = colorForState(state)
  element.dataset.state = state
}

function buildPopupContent(driver: DriverLocationPoint): HTMLElement {
  const popup = document.createElement('div')
  popup.className = 'fleet-popup'

  const identity = getDriverIdentity(driver)

  const status = document.createElement('span')
  status.className = 'fleet-popup-status'
  status.style.backgroundColor = colorForState(driver.state)
  status.textContent = stateLabel(driver.state)

  const details = document.createElement('div')
  details.className = 'fleet-popup-details'

  const appendRow = (label: string, value: string, options: { strong?: boolean } = {}) => {
    const row = document.createElement('div')
    row.className = 'fleet-popup-row'

    const labelEl = document.createElement('span')
    labelEl.className = 'fleet-popup-row-label'
    labelEl.textContent = label

    const valueEl = document.createElement('span')
    valueEl.className = options.strong ? 'fleet-popup-row-value fleet-popup-row-value-strong' : 'fleet-popup-row-value'
    valueEl.textContent = value

    row.append(labelEl, valueEl)
    details.append(row)
  }

  appendRow('Conductor:', identity.secondary ? `${identity.primary} (${identity.secondary})` : identity.primary, { strong: true })
  appendRow('Porte:', driver.activePorteId ? `#${driver.activePorteId}` : 'Sin porte activo', { strong: !!driver.activePorteId })
  appendRow('Destino:', normalizeOptionalText(driver.activePorteDestination) ?? 'No asignado')
  appendRow('Estado porte:', formatPorteStatus(driver.activePorteStatus))
  appendRow('Última señal:', formatRecordedAt(driver.recordedAt), { strong: true })

  popup.append(status, details)
  return popup
}

function formatPorteStatus(status?: string): string {
  if (!status) return 'Sin estado'
  const labels: Record<string, string> = {
    ASIGNADO: 'Asignado',
    EN_TRANSITO: 'En tránsito',
    ENTREGADO: 'Entregado',
    FACTURADO: 'Facturado',
    CANCELADO: 'Cancelado',
    PENDIENTE: 'Pendiente',
  }
  return labels[status] ?? status.split('_').join(' ').toLowerCase()
}
</script>

<template>
  <section class="fleet-map-view text-gray-800 dark:text-gray-100">
    <header class="shrink-0 flex items-center justify-between gap-4">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-sky-50 dark:bg-sky-900/30 text-sky-600 dark:text-sky-400 flex items-center justify-center">
          <i class="pi pi-map text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-semibold text-gray-800 dark:text-gray-100">Mapa de flota</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 flex flex-wrap items-center gap-x-3 gap-y-1">
            <span>Total conductores: <strong class="text-slate-700 dark:text-slate-200">{{ totalDriversCount }}</strong></span>
            <span>Reportan ubicación: <strong class="text-blue-700 dark:text-blue-300">{{ reportingDriversCount }}</strong></span>
            <span>No reportan ubicación: <strong class="text-rose-700 dark:text-rose-300">{{ notReportingDriversCount }}</strong></span>
            <span v-if="fleetStore.lastSnapshotAt">Último snapshot: {{ fleetStore.lastSnapshotAt }}</span>
          </p>
        </div>
      </div>
      <button
        type="button"
        class="px-3 py-2 text-sm rounded-lg bg-primary text-white hover:opacity-90"
        @click="fleetStore.fetchSnapshot"
      >
        Actualizar
      </button>
    </header>

    <div class="fleet-map-layout grid grid-cols-1 xl:grid-cols-[1fr_320px] gap-4">
      <div class="flex min-h-0 flex-col rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 shadow-[0_12px_30px_-18px_rgba(15,23,42,0.45)] dark:shadow-[0_14px_34px_-20px_rgba(0,0,0,0.75)]">
        <div class="mb-3 text-sm text-gray-500 dark:text-gray-400 flex flex-wrap items-center gap-3">
          <span class="text-sky-600 dark:text-sky-400 font-medium">Actualiza en {{ countdown }}s</span>
          <span>Conductores: {{ totalDriversCount }}</span>
          <span class="inline-flex items-center gap-1">
            <span class="inline-block h-2 w-2 rounded-full bg-blue-600"></span>Reporta ubicación
          </span>
          <span class="inline-flex items-center gap-1">
            <span class="inline-block h-2 w-2 rounded-full bg-red-500"></span>No reporta ubicación
          </span>
        </div>

        <div ref="mapContainer" class="fleet-map-canvas w-full rounded-lg border border-gray-200 dark:border-gray-700"></div>
      </div>

      <aside
        class="h-full min-h-0 overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/70 dark:bg-slate-900/60 p-4 shadow-[0_12px_30px_-18px_rgba(15,23,42,0.35)] dark:shadow-[0_14px_34px_-20px_rgba(0,0,0,0.75)]"
        aria-label="Panel lateral de seguimiento"
      >
        <div class="flex h-full min-h-0 flex-col gap-4">
          <div class="shrink-0 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-3">
            <h2 class="mb-3 text-xs font-semibold uppercase tracking-[0.08em] text-slate-500 dark:text-slate-300">Filtros de seguimiento</h2>
            <div class="space-y-3">
              <div>
                <label for="driver-search" class="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-300">Buscar conductor</label>
                <div class="relative">
                  <input
                    id="driver-search"
                    v-model="driverSearch"
                    type="text"
                    placeholder="Ej: Juan Pérez o 1024"
                    aria-label="Buscar conductor por nombre o ID"
                    class="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 pl-3 pr-9 py-2.5 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 focus-visible:ring-offset-1 dark:focus-visible:ring-offset-slate-800"
                  />
                  <i class="pi pi-search pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 dark:text-slate-500"></i>
                </div>
              </div>
              <div>
                <label for="state-filter" class="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-300">Estado</label>
                <select
                  id="state-filter"
                  v-model="stateFilter"
                  aria-label="Filtrar por estado del conductor"
                  class="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 px-3 py-2.5 text-sm text-slate-900 dark:text-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 focus-visible:ring-offset-1 dark:focus-visible:ring-offset-slate-800"
                >
                  <option value="ALL">Todos los estados</option>
                  <option value="REPORTING">Reporta ubicación</option>
                  <option value="NOT_REPORTING">No reporta ubicación</option>
                </select>
              </div>
            </div>
          </div>

          <div class="flex min-h-0 flex-1 flex-col rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-3">
            <div class="mb-3 flex items-center justify-between gap-2">
              <h3 class="text-xs font-semibold uppercase tracking-[0.08em] text-slate-500 dark:text-slate-300">Conductores visibles</h3>
              <span class="rounded-full bg-slate-100 dark:bg-slate-700 px-2 py-1 text-xs font-semibold text-slate-700 dark:text-slate-100">{{ filteredDrivers.length }}</span>
            </div>

            <p v-if="!fleetStore.hasDrivers" class="text-sm text-slate-500 dark:text-slate-400">No hay conductores activos</p>

            <ul
              v-else
              class="min-h-0 flex-1 space-y-2 overflow-y-auto pr-1"
              role="listbox"
              aria-label="Lista de conductores"
            >
              <li v-for="driver in filteredDrivers" :key="driver.driverId">
                <button
                  type="button"
                  class="w-full rounded-xl border px-3 py-3 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 focus-visible:ring-offset-1"
                  :class="selectedDriverId === driver.driverId
                    ? 'border-sky-300 dark:border-sky-500 bg-sky-50 dark:bg-sky-900/30 shadow-sm'
                    : 'border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 hover:border-slate-300 dark:hover:border-slate-500 hover:bg-slate-50 dark:hover:bg-slate-800'"
                  role="option"
                  :aria-selected="selectedDriverId === driver.driverId"
                  :aria-label="`Seleccionar conductor ${getDriverLabel(driver)}`"
                  @click="selectedDriverId = driver.driverId"
                >
                  <div class="flex items-center justify-between gap-3">
                    <span class="font-semibold text-slate-900 dark:text-slate-100">{{ getDriverLabel(driver) }}</span>
                    <span
                      class="rounded-full px-2.5 py-1 text-[11px] font-semibold ring-1"
                      :class="stateChipClass(driver.state)"
                    >
                      {{ stateLabel(driver.state) }}
                    </span>
                  </div>
                  <div class="mt-2 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
                    <span>Última señal</span>
                    <span class="font-medium text-slate-600 dark:text-slate-300">{{ formatRecordedAt(driver.recordedAt) }}</span>
                  </div>
                </button>
              </li>
            </ul>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
/* Use :global() so programmatically created markers receive these styles */
:global(.fleet-map-marker) {
  width: 18px;
  height: 18px;
  border-radius: 9999px;
  border: 2px solid #ffffff;
  box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.2), 0 8px 18px -8px rgba(15, 23, 42, 0.6);
  cursor: pointer;
  transition: box-shadow 140ms ease;
}

:global(.fleet-map-marker:hover) {
  box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.35), 0 12px 22px -8px rgba(15, 23, 42, 0.75);
}

:deep(.maplibregl-ctrl-group) {
  border: none;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 10px 24px -16px rgba(15, 23, 42, 0.55);
}

:deep(.maplibregl-ctrl-group button) {
  width: 32px;
  height: 32px;
}

:deep(.maplibregl-popup-content) {
  border-radius: 12px;
  padding: 0;
  background: #ffffff;
  box-shadow: 0 14px 34px -18px rgba(15, 23, 42, 0.7);
}

:deep(.maplibregl-popup-tip) {
  border-top-color: #ffffff;
}

:global(.dark) :deep(.maplibregl-popup-tip) {
  border-top-color: #0f172a;
}

.fleet-popup {
  min-width: 260px;
  background: #ffffff;
  color: #0f172a;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  padding: 12px 14px;
}

.fleet-popup-title {
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin-bottom: 0.4rem;
}

.fleet-popup-status {
  display: inline-block;
  color: #ffffff;
  border-radius: 9999px;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  padding: 3px 9px;
  margin-bottom: 0.65rem;
}

.fleet-popup-details {
  display: grid;
  gap: 6px;
  font-size: 0.76rem;
  line-height: 1.35;
}

.fleet-popup-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
  align-items: baseline;
}

.fleet-popup-row-label {
  color: #64748b;
  font-weight: 650;
}

.fleet-popup-row-value {
  color: #0f172a;
  min-width: 0;
  overflow-wrap: anywhere;
  text-align: left;
}

.fleet-popup-row-value-strong {
  font-weight: 800;
}

:global(.dark) .fleet-popup {
  background: #0f172a;
  color: #e2e8f0;
  border-color: rgba(148, 163, 184, 0.18);
}

:global(.dark) :deep(.maplibregl-popup-content) {
  background: #0f172a;
}

:global(.dark) .fleet-popup-row-label {
  color: #94a3b8;
}

:global(.dark) .fleet-popup-row-value {
  color: #e2e8f0;
}

:global(.dark) :deep(.maplibregl-ctrl-group) {
  box-shadow: 0 10px 24px -16px rgba(0, 0, 0, 0.85);
}

:global(.dark) :deep(.maplibregl-ctrl-group button) {
  background: #1e293b;
  color: #e2e8f0;
}

:global(.dark) :deep(.maplibregl-ctrl-group button + button) {
  border-top-color: rgba(148, 163, 184, 0.22);
}

.fleet-map-view {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.fleet-map-layout {
  flex: 1;
  min-height: 0;
}

.fleet-map-canvas {
  flex: 1;
  min-height: 360px;
}

@media (min-width: 1280px) {
  .fleet-map-layout {
    grid-template-rows: minmax(0, 1fr);
  }
}
</style>
