<template>
  <Dialog
    :visible="visible"
    @update:visible="emit('update:visible', $event)"
    :modal="true"
    :dismissableMask="true"
    :style="{ width: '50rem', maxWidth: '95vw' }"
    :breakpoints="{ '960px': '75vw', '640px': '90vw' }"
  >
    <template #header>
      <div class="flex items-center gap-3">
        <h3 class="text-lg font-bold text-gray-800 dark:text-white">
          {{ t('portal.tracking.title', { id: porteId }) }}
        </h3>
        <span
          v-if="portesStore.tracking"
          class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
          :class="estadoBadgeClass(portesStore.tracking.status)"
        >
          {{ portesStore.tracking.status.replace('_', ' ') }}
        </span>
        <i
          v-if="portesStore.trackingLoading && !portesStore.tracking"
          class="pi pi-spin pi-spinner text-primary-500"
        />
      </div>
    </template>

    <div v-if="portesStore.tracking" class="flex flex-col md:flex-row gap-4">
      <div class="flex-1 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden h-[320px] md:h-[420px]">
        <div ref="mapContainer" class="w-full h-full" />
      </div>

      <div class="md:w-72 space-y-3">
        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
          <h3 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase mb-2">
            {{ t('portal.tracking.estimatedTime') }}
          </h3>
          <div v-if="portesStore.tracking.etaMinutes != null" class="text-3xl font-bold text-primary-600">
            {{ portesStore.tracking.etaMinutes }} min
          </div>
          <div v-else class="text-lg text-gray-400">{{ t('portal.tracking.noEstimation') }}</div>
          <p v-if="portesStore.tracking.etaMinutes != null" class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            <i class="pi pi-clock mr-1 text-xs" />
            {{ t('portal.tracking.estimatedArrival') }}: {{ formatEtaClock(portesStore.tracking.etaMinutes) }} h
          </p>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
          <h3 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase mb-2">
            {{ t('portal.tracking.driver') }}
          </h3>
          <p class="text-gray-800 dark:text-white font-medium">
            {{ portesStore.tracking.driverName ?? t('portal.tracking.unassigned') }}
          </p>
          <p v-if="portesStore.tracking.vehicleInfo" class="text-sm text-gray-500">
            {{ portesStore.tracking.vehicleInfo }}
          </p>
          <p v-if="portesStore.tracking.speedKph != null" class="text-sm text-gray-500 mt-1">
            <i class="pi pi-gauge mr-1"></i>
            {{ Math.round(portesStore.tracking.speedKph) }} km/h
          </p>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
          <h3 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase mb-2">
            {{ t('portal.tracking.route') }}
          </h3>
          <div class="space-y-2">
            <div class="flex items-start gap-2">
              <span class="mt-1 w-3 h-3 rounded-full bg-green-500 flex-shrink-0" />
              <div>
                <p class="text-xs text-gray-400">{{ t('portal.tracking.origin') }}</p>
                <p class="text-sm text-gray-700 dark:text-gray-300">{{ portesStore.tracking.originName || '—' }}</p>
              </div>
            </div>
            <div class="flex items-start gap-2">
              <span class="mt-1 w-3 h-3 rounded-full bg-red-500 flex-shrink-0" />
              <div>
                <p class="text-xs text-gray-400">{{ t('portal.tracking.destination') }}</p>
                <p class="text-sm text-gray-700 dark:text-gray-300">{{ portesStore.tracking.destinationName || '—' }}</p>
              </div>
            </div>
          </div>
        </div>

        <div v-if="portesStore.tracking.lastUpdate" class="text-xs text-gray-400 text-center">
          {{ t('portal.tracking.lastUpdate', { time: formatTime(portesStore.tracking.lastUpdate) }) }}
          <br />
          <span class="text-gray-300">{{ t('portal.tracking.autoRefresh') }}</span>
        </div>
      </div>
    </div>

    <div v-else-if="portesStore.trackingLoading" class="flex items-center justify-center h-[320px] md:h-[420px]">
      <i class="pi pi-spin pi-spinner text-4xl text-primary-500"></i>
    </div>

    <div v-else class="flex flex-col items-center justify-center h-[320px] md:h-[420px] text-gray-400">
      <i class="pi pi-map-marker text-4xl mb-3"></i>
      <p>{{ t('portal.tracking.errorLoading') }}</p>
      <Button
        :label="t('portal.tracking.retry')"
        icon="pi pi-refresh"
        severity="secondary"
        class="mt-3"
        @click="portesStore.fetchTracking(porteId)"
      />
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePortesStore } from '@/stores/portes'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps<{
  visible: boolean
  porteId: number
}>()

const emit = defineEmits<{
  'update:visible': [boolean]
}>()

const { t } = useI18n()
const portesStore = usePortesStore()
const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null
let driverMarker: L.Marker | null = null

const OSRM_BASE = 'https://router.project-osrm.org'

interface OsrmRouteResponse {
  code: string
  routes: Array<{
    geometry: { coordinates: Array<[number, number]> }
    distance: number
    duration: number
  }>
}

async function fetchOsrmRoute(
  originLat: number, originLng: number,
  destLat: number, destLng: number,
): Promise<Array<[number, number]> | null> {
  try {
    const url = `${OSRM_BASE}/route/v1/driving/${originLng},${originLat};${destLng},${destLat}?overview=full&geometries=geojson`
    const res = await fetch(url)
    const data: OsrmRouteResponse = await res.json()
    if (data.code === 'Ok' && data.routes.length > 0) {
      return data.routes[0].geometry.coordinates.map(([lng, lat]) => [lat, lng])
    }
    return null
  } catch {
    return null
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
delete (L.Icon.Default.prototype as any)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

const driverIcon = L.divIcon({
  html: '<div style="background:#4F46E5;width:16px;height:16px;border-radius:50%;border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,.3)"></div>',
  className: '',
  iconSize: [16, 16],
  iconAnchor: [8, 8],
})

const originIcon = L.divIcon({
  html: '<div style="background:#22C55E;width:12px;height:12px;border-radius:50%;border:2px solid white;box-shadow:0 2px 4px rgba(0,0,0,.2)"></div>',
  className: '',
  iconSize: [12, 12],
  iconAnchor: [6, 6],
})

const destinationIcon = L.divIcon({
  html: '<div style="background:#EF4444;width:12px;height:12px;border-radius:50%;border:2px solid white;box-shadow:0 2px 4px rgba(0,0,0,.2)"></div>',
  className: '',
  iconSize: [12, 12],
  iconAnchor: [6, 6],
})

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      portesStore.startTrackingPolling(props.porteId, 10000)
    } else {
      portesStore.stopTrackingPolling()
      cleanupMap()
    }
  },
  { immediate: true }
)

watch(
  () => portesStore.tracking,
  async (tracking) => {
    if (!tracking || !props.visible) return
    await nextTick()
    if (!mapContainer.value) return

    if (!map) {
      initMap(tracking)
    } else {
      updateMap(tracking)
    }
  }
)

onBeforeUnmount(() => {
  portesStore.stopTrackingPolling()
  cleanupMap()
})

function initMap(tracking: NonNullable<typeof portesStore.tracking>) {
  if (!mapContainer.value) return
  map = L.map(mapContainer.value).setView([40.416, -3.703], 6)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 18,
  }).addTo(map)

  const bounds: L.LatLngExpression[] = []

  if (tracking.originLat != null && tracking.originLng != null) {
    L.marker([tracking.originLat, tracking.originLng], { icon: originIcon })
      .bindPopup(`<b>${t('portal.tracking.mapOrigin')}</b><br/>${tracking.originName || ''}`)
      .addTo(map)
    bounds.push([tracking.originLat, tracking.originLng])
  }

  if (tracking.destinationLat != null && tracking.destinationLng != null) {
    L.marker([tracking.destinationLat, tracking.destinationLng], { icon: destinationIcon })
      .bindPopup(`<b>${t('portal.tracking.mapDestination')}</b><br/>${tracking.destinationName || ''}`)
      .addTo(map)
    bounds.push([tracking.destinationLat, tracking.destinationLng])
  }

  let routeLoaded = false
  if (tracking.originLat != null && tracking.originLng != null && tracking.destinationLat != null && tracking.destinationLng != null) {
    fetchOsrmRoute(tracking.originLat, tracking.originLng, tracking.destinationLat, tracking.destinationLng)
      .then((coords) => {
        if (!map) return
        const path = coords ?? [[tracking.originLat!, tracking.originLng!], [tracking.destinationLat!, tracking.destinationLng!]]
        L.polyline(path, { color: '#3B82F6', weight: 4, opacity: 0.8 }).addTo(map)
        const allBounds = L.latLngBounds(path)
        if (driverMarker) allBounds.extend(driverMarker.getLatLng())
        map.fitBounds(allBounds, { padding: [40, 40] })
      })
    routeLoaded = true
  }

  if (tracking.driverLat != null && tracking.driverLng != null) {
    driverMarker = L.marker([tracking.driverLat, tracking.driverLng], { icon: driverIcon })
      .bindPopup(`<b>${tracking.driverName ?? t('portal.tracking.driver')}</b>`)
      .addTo(map)
    bounds.push([tracking.driverLat, tracking.driverLng])
  }

  if (!routeLoaded) {
    if (bounds.length >= 2) {
      map.fitBounds(L.latLngBounds(bounds), { padding: [40, 40] })
    } else if (bounds.length === 1) {
      map.setView(bounds[0], 12)
    }
  }
}

function updateMap(tracking: NonNullable<typeof portesStore.tracking>) {
  if (!map) return
  if (tracking.driverLat != null && tracking.driverLng != null) {
    if (driverMarker) {
      driverMarker.setLatLng([tracking.driverLat, tracking.driverLng])
    } else {
      driverMarker = L.marker([tracking.driverLat, tracking.driverLng], { icon: driverIcon })
        .bindPopup(`<b>${tracking.driverName ?? t('portal.tracking.driver')}</b>`)
        .addTo(map)
    }
  }
}

function cleanupMap() {
  if (map) {
    map.remove()
    map = null
  }
  driverMarker = null
}

function estadoBadgeClass(estado: string): string {
  const m: Record<string, string> = {
    PENDIENTE: 'bg-yellow-50 text-yellow-700',
    SOLICITUD: 'bg-purple-50 text-purple-700',
    ASIGNADO: 'bg-blue-50 text-blue-700',
    EN_TRANSITO: 'bg-indigo-50 text-indigo-700',
    ENTREGADO: 'bg-green-50 text-green-700',
    CANCELADO: 'bg-red-50 text-red-700',
    FACTURADO: 'bg-gray-50 text-gray-600',
  }
  return m[estado] ?? 'bg-gray-50 text-gray-600'
}

function formatTime(dateStr: string): string {
  return new Intl.DateTimeFormat('es-ES', {
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    day: '2-digit', month: 'short',
  }).format(new Date(dateStr))
}

function formatEtaClock(etaMinutes: number): string {
  const arrival = new Date(Date.now() + etaMinutes * 60_000)
  return new Intl.DateTimeFormat('es-ES', {
    hour: '2-digit', minute: '2-digit',
  }).format(arrival)
}
</script>
