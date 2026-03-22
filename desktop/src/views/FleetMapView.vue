<script setup lang="ts">
import maplibregl, { type Map as MapLibreMap, type Marker, type StyleSpecification } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useFleetTrackingStore } from '@/stores/fleetTracking'
import type { DriverLocationPoint, DriverState } from '@/services/api'
import { computeMarkerDiff } from './fleetMapMarkerDiff'

const fleetStore = useFleetTrackingStore()

const mapContainer = ref<HTMLElement | null>(null)
const selectedDriverId = ref<string | null>(null)
const stateFilter = ref<'ALL' | DriverState>('ALL')
const driverSearch = ref('')
const autoFitEnabled = ref(true)
const userInteracting = ref(false)
const countdown = ref(10)

let map: MapLibreMap | null = null
const markersByDriverId = new Map<string, Marker>()
const lastRenderedRecordedAtByDriverId = new Map<string, string>()

const selectedDriver = computed<DriverLocationPoint | null>(() => {
  if (!selectedDriverId.value) return null
  return fleetStore.drivers.find((driver) => driver.driverId === selectedDriverId.value) ?? null
})

const filteredDrivers = computed(() => {
  const search = driverSearch.value.trim().toLowerCase()
  return fleetStore.drivers.filter((driver) => {
    const matchesState = stateFilter.value === 'ALL' || driver.state === stateFilter.value
    const matchesSearch = search.length === 0 || driver.driverId.toLowerCase().includes(search)
    return matchesState && matchesSearch
  })
})

const connectionLabel = computed(() => {
  if (fleetStore.connectionState === 'ONLINE') return 'En línea'
  if (fleetStore.connectionState === 'DEGRADED') return 'Degradado'
  return 'Sin conexión'
})

const connectionBadgeClass = computed(() => {
  if (fleetStore.connectionState === 'ONLINE') return 'bg-emerald-100 text-emerald-700'
  if (fleetStore.connectionState === 'DEGRADED') return 'bg-amber-100 text-amber-700'
  return 'bg-red-100 text-red-700'
})

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

watch(selectedDriver, (driver) => {
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
      markerElement.title = `Conductor #${driver.driverId}`
      markerElement.addEventListener('click', () => {
        selectedDriverId.value = driver.driverId
      })

      const marker = new maplibregl.Marker({ element: markerElement })
        .setLngLat([driver.lon, driver.lat])
        .setPopup(new maplibregl.Popup({ closeButton: false, offset: 18 }).setDOMContent(buildPopupContent(driver)))
        .addTo(map)
      markersByDriverId.set(driver.driverId, marker)
    }

    lastRenderedRecordedAtByDriverId.set(driver.driverId, driver.recordedAt)
  }

  if (autoFitEnabled.value && !userInteracting.value) fitMapToDrivers(drivers)
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
  if (state === 'ONLINE') return '#10b981'
  if (state === 'STALE') return '#f59e0b'
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

  const title = document.createElement('p')
  title.className = 'fleet-popup-title'
  title.textContent = `Conductor #${driver.driverId}`

  const status = document.createElement('span')
  status.className = 'fleet-popup-status'
  status.style.backgroundColor = colorForState(driver.state)
  status.textContent = driver.state

  const details = document.createElement('div')
  details.className = 'fleet-popup-details'
  details.innerHTML = `<p>Coords: ${driver.lat.toFixed(5)}, ${driver.lon.toFixed(5)}</p><p>Velocidad: ${driver.speedKph ?? 'N/D'} km/h</p><p>Rumbo: ${driver.headingDeg ?? 'N/D'}°</p><p>Actualizado: ${driver.recordedAt}</p>`

  popup.append(title, status, details)
  return popup
}
</script>

<template>
  <section class="space-y-4 text-gray-800">
    <header class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-semibold text-gray-800">Mapa de flota</h1>
        <p class="text-sm text-gray-500 flex items-center gap-2">
          <span>Estado:</span>
          <span class="text-xs px-2 py-1 rounded-full font-medium" :class="connectionBadgeClass">
            {{ connectionLabel }}
          </span>
          <span v-if="fleetStore.lastSnapshotAt"> · Último snapshot: {{ fleetStore.lastSnapshotAt }}</span>
        </p>
      </div>
      <button
        type="button"
        class="px-3 py-2 text-sm rounded-lg bg-primary text-white hover:opacity-90"
        @click="fleetStore.fetchSnapshot"
      >
        Actualizar
      </button>
    </header>

    <div
      v-if="fleetStore.degradedReason"
      class="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
    >
      Operación degradada: {{ fleetStore.degradedReason }}
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-[1fr_320px] gap-4">
      <div class="rounded-xl border border-gray-200 bg-white p-4 shadow-[0_12px_30px_-18px_rgba(15,23,42,0.45)]">
        <div class="mb-3 text-sm text-gray-500 flex flex-wrap items-center gap-3">
          <span class="text-sky-600 font-medium">Actualiza en {{ countdown }}s</span>
          <span>Conductores: {{ fleetStore.drivers.length }}</span>
          <button
            type="button"
            class="rounded-md px-2 py-1 text-xs font-medium transition"
            :class="autoFitEnabled ? 'bg-sky-100 text-sky-700' : 'bg-gray-100 text-gray-700'"
            @click="autoFitEnabled = !autoFitEnabled"
          >
            Autoajustar: {{ autoFitEnabled ? 'ON' : 'OFF' }}
          </button>
          <span class="inline-flex items-center gap-1">
            <span class="inline-block h-2 w-2 rounded-full bg-emerald-500"></span>ONLINE
          </span>
          <span class="inline-flex items-center gap-1">
            <span class="inline-block h-2 w-2 rounded-full bg-amber-500"></span>STALE
          </span>
          <span class="inline-flex items-center gap-1">
            <span class="inline-block h-2 w-2 rounded-full bg-red-500"></span>OFFLINE
          </span>
        </div>

        <div ref="mapContainer" class="h-[520px] w-full rounded-lg border border-gray-200"></div>
      </div>

      <aside class="rounded-xl border border-gray-200 bg-white p-4 shadow-[0_12px_30px_-18px_rgba(15,23,42,0.35)]">
        <div class="space-y-3">
          <input
            v-model="driverSearch"
            type="text"
            placeholder="Buscar conductor por ID"
            class="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-800"
          />
          <select
            v-model="stateFilter"
            class="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-800"
          >
            <option value="ALL">Todos los estados</option>
            <option value="ONLINE">ONLINE</option>
            <option value="STALE">STALE</option>
            <option value="OFFLINE">OFFLINE</option>
          </select>
        </div>

        <p v-if="!fleetStore.hasDrivers" class="mt-4 text-sm text-gray-500">sin conductores activos</p>

        <ul v-else class="mt-4 max-h-56 overflow-auto divide-y divide-gray-100">
          <li
            v-for="driver in filteredDrivers"
            :key="driver.driverId"
            class="-mx-2 cursor-pointer rounded px-2 py-2 text-sm"
            :class="selectedDriverId === driver.driverId ? 'bg-gray-100' : ''"
            @click="selectedDriverId = driver.driverId"
          >
            <div class="flex items-center justify-between">
              <span class="font-medium text-gray-800">#{{ driver.driverId }}</span>
              <span class="rounded bg-gray-100 px-2 py-1 text-xs text-gray-700">
                {{ driver.state }}
              </span>
            </div>
            <div class="mt-1 text-xs text-gray-500">{{ driver.recordedAt }}</div>
          </li>
        </ul>

        <div v-if="selectedDriver" class="mt-4 rounded-lg border border-gray-200 bg-gray-50 p-3 text-sm">
          <h2 class="mb-2 font-semibold text-gray-800">Detalle conductor #{{ selectedDriver.driverId }}</h2>
          <p><span class="text-gray-500">Estado:</span> {{ selectedDriver.state }}</p>
          <p><span class="text-gray-500">Coords:</span> {{ selectedDriver.lat }}, {{ selectedDriver.lon }}</p>
          <p><span class="text-gray-500">Actualizado:</span> {{ selectedDriver.recordedAt }}</p>
          <p><span class="text-gray-500">Velocidad:</span> {{ selectedDriver.speedKph ?? 'N/D' }}</p>
          <p><span class="text-gray-500">Rumbo:</span> {{ selectedDriver.headingDeg ?? 'N/D' }}</p>
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
  box-shadow: 0 14px 34px -18px rgba(15, 23, 42, 0.7);
}

:deep(.maplibregl-popup-tip) {
  border-top-color: #ffffff;
}

.fleet-popup {
  min-width: 220px;
  background: #ffffff;
  color: #0f172a;
  border-radius: 12px;
  padding: 10px 12px;
}

.fleet-popup-title {
  font-size: 0.9rem;
  font-weight: 700;
  margin-bottom: 0.35rem;
}

.fleet-popup-status {
  display: inline-block;
  color: #ffffff;
  border-radius: 9999px;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  padding: 2px 8px;
  margin-bottom: 0.45rem;
}

.fleet-popup-details {
  font-size: 0.76rem;
  line-height: 1.35;
  opacity: 0.92;
}

.fleet-popup-details p + p {
  margin-top: 2px;
}
</style>
