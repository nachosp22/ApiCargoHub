<template>
  <div class="flex flex-col h-[calc(100vh-8rem)]">
    <!-- Header -->
    <div class="flex items-center gap-3 mb-4">
      <router-link to="/portal/mis-portes">
        <Button icon="pi pi-arrow-left" severity="secondary" text rounded />
      </router-link>
      <h2 class="text-xl font-bold text-gray-800">
        Tracking Porte #{{ porteId }}
      </h2>
      <span
        v-if="portesStore.tracking"
        class="text-xs font-medium px-2.5 py-1 rounded-full"
        :class="estadoBadgeClass(portesStore.tracking.status)"
      >
        {{ portesStore.tracking.status.replace('_', ' ') }}
      </span>
      <div v-if="portesStore.trackingLoading && !portesStore.tracking" class="ml-2">
        <i class="pi pi-spin pi-spinner text-primary-500"></i>
      </div>
    </div>

    <!-- Main content -->
    <div v-if="portesStore.tracking" class="flex flex-col lg:flex-row gap-4 flex-1 min-h-0">
      <!-- Map -->
      <div class="flex-1 rounded-xl border border-gray-200 overflow-hidden min-h-[300px]">
        <div ref="mapContainer" class="w-full h-full" />
      </div>

      <!-- Info panel -->
      <div class="lg:w-80 space-y-4">
        <!-- ETA -->
        <div class="bg-white rounded-xl border border-gray-200 p-4">
          <h3 class="text-sm font-semibold text-gray-500 uppercase mb-2">Tiempo estimado</h3>
          <div v-if="portesStore.tracking.etaMinutes != null" class="text-3xl font-bold text-primary-600">
            {{ portesStore.tracking.etaMinutes }} min
          </div>
          <div v-else class="text-lg text-gray-400">Sin estimación</div>
          <p v-if="portesStore.tracking.etaConfidence" class="text-xs text-gray-400 mt-1">
            Confianza: {{ portesStore.tracking.etaConfidence }}
          </p>
        </div>

        <!-- Driver -->
        <div class="bg-white rounded-xl border border-gray-200 p-4">
          <h3 class="text-sm font-semibold text-gray-500 uppercase mb-2">Conductor</h3>
          <p class="text-gray-800 font-medium">
            {{ portesStore.tracking.driverName ?? 'Sin asignar' }}
          </p>
          <p v-if="portesStore.tracking.vehicleInfo" class="text-sm text-gray-500">
            {{ portesStore.tracking.vehicleInfo }}
          </p>
          <p v-if="portesStore.tracking.speedKph != null" class="text-sm text-gray-500 mt-1">
            <i class="pi pi-gauge mr-1"></i>
            {{ Math.round(portesStore.tracking.speedKph) }} km/h
          </p>
        </div>

        <!-- Route -->
        <div class="bg-white rounded-xl border border-gray-200 p-4">
          <h3 class="text-sm font-semibold text-gray-500 uppercase mb-2">Ruta</h3>
          <div class="space-y-2">
            <div class="flex items-start gap-2">
              <span class="mt-1 w-3 h-3 rounded-full bg-green-500 flex-shrink-0" />
              <div>
                <p class="text-xs text-gray-400">Origen</p>
                <p class="text-sm text-gray-700">{{ portesStore.tracking.originName || '—' }}</p>
              </div>
            </div>
            <div class="flex items-start gap-2">
              <span class="mt-1 w-3 h-3 rounded-full bg-red-500 flex-shrink-0" />
              <div>
                <p class="text-xs text-gray-400">Destino</p>
                <p class="text-sm text-gray-700">{{ portesStore.tracking.destinationName || '—' }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Last update -->
        <div v-if="portesStore.tracking.lastUpdate" class="text-xs text-gray-400 text-center">
          Última actualización: {{ formatTime(portesStore.tracking.lastUpdate) }}
          <br />
          <span class="text-gray-300">Auto-refresh cada 10s</span>
        </div>
      </div>
    </div>

    <!-- Loading state -->
    <div v-else-if="portesStore.trackingLoading" class="flex-1 flex items-center justify-center">
      <i class="pi pi-spin pi-spinner text-4xl text-primary-500"></i>
    </div>

    <!-- Error state -->
    <div v-else class="flex-1 flex flex-col items-center justify-center text-gray-400">
      <i class="pi pi-map-marker text-4xl mb-3"></i>
      <p>No se pudo cargar la información de tracking</p>
      <Button label="Reintentar" icon="pi pi-refresh" severity="secondary" class="mt-3"
              @click="portesStore.fetchTracking(porteId)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { usePortesStore } from '@/stores/portes'
import Button from 'primevue/button'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const portesStore = usePortesStore()
const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null
let driverMarker: L.Marker | null = null

const porteId = Number(route.params.id)

// Fix default marker icons for Leaflet + bundlers
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

onMounted(() => {
  portesStore.startTrackingPolling(porteId, 10000)
})

onBeforeUnmount(() => {
  portesStore.stopTrackingPolling()
  if (map) {
    map.remove()
    map = null
  }
})

watch(
  () => portesStore.tracking,
  async (t) => {
    if (!t) return
    await nextTick()
    if (!mapContainer.value) return

    if (!map) {
      map = L.map(mapContainer.value).setView([40.416, -3.703], 6)
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 18,
      }).addTo(map)

      const bounds: L.LatLngExpression[] = []

      // Origin marker
      if (t.originLat != null && t.originLng != null) {
        L.marker([t.originLat, t.originLng], { icon: originIcon })
          .bindPopup(`<b>Origen</b><br/>${t.originName || ''}`)
          .addTo(map)
        bounds.push([t.originLat, t.originLng])
      }

      // Destination marker
      if (t.destinationLat != null && t.destinationLng != null) {
        L.marker([t.destinationLat, t.destinationLng], { icon: destinationIcon })
          .bindPopup(`<b>Destino</b><br/>${t.destinationName || ''}`)
          .addTo(map)
        bounds.push([t.destinationLat, t.destinationLng])
      }

      // Route line
      if (t.originLat != null && t.originLng != null && t.destinationLat != null && t.destinationLng != null) {
        L.polyline(
          [[t.originLat, t.originLng], [t.destinationLat, t.destinationLng]],
          { color: '#6366F1', weight: 3, dashArray: '8 4', opacity: 0.6 }
        ).addTo(map)
      }

      // Driver marker
      if (t.driverLat != null && t.driverLng != null) {
        driverMarker = L.marker([t.driverLat, t.driverLng], { icon: driverIcon })
          .bindPopup(`<b>${t.driverName ?? 'Conductor'}</b>`)
          .addTo(map)
        bounds.push([t.driverLat, t.driverLng])
      }

      if (bounds.length >= 2) {
        map.fitBounds(L.latLngBounds(bounds), { padding: [40, 40] })
      } else if (bounds.length === 1) {
        map.setView(bounds[0], 12)
      }
    } else {
      // Update driver marker position
      if (t.driverLat != null && t.driverLng != null) {
        if (driverMarker) {
          driverMarker.setLatLng([t.driverLat, t.driverLng])
        } else {
          driverMarker = L.marker([t.driverLat, t.driverLng], { icon: driverIcon })
            .bindPopup(`<b>${t.driverName ?? 'Conductor'}</b>`)
            .addTo(map)
        }
      }
    }
  },
  { immediate: true }
)

function estadoBadgeClass(estado: string): string {
  const map: Record<string, string> = {
    PENDIENTE: 'bg-yellow-100 text-yellow-700',
    SOLICITUD: 'bg-purple-100 text-purple-700',
    ASIGNADO: 'bg-blue-100 text-blue-700',
    EN_TRANSITO: 'bg-indigo-100 text-indigo-700',
    ENTREGADO: 'bg-green-100 text-green-700',
    CANCELADO: 'bg-red-100 text-red-700',
    FACTURADO: 'bg-gray-100 text-gray-700',
  }
  return map[estado] ?? 'bg-gray-100 text-gray-600'
}

function formatTime(dateStr: string): string {
  return new Intl.DateTimeFormat('es-ES', {
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    day: '2-digit', month: 'short',
  }).format(new Date(dateStr))
}
</script>
