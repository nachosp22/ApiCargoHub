<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useVehiculosStore } from '@/stores/vehiculos'
import { useConductoresStore } from '@/stores/conductores'
import Dialog from 'primevue/dialog'
import VehiculoTable from '@/components/vehiculos/VehiculoTable.vue'
import type { Vehiculo } from '@/stores/vehiculos'

const vehiculosStore = useVehiculosStore()
const conductoresStore = useConductoresStore()

// --- Detail panel state ---
const showDetail = ref(false)
const detailVehiculo = ref<Vehiculo | null>(null)

// --- Lifecycle ---
onMounted(async () => {
  await Promise.all([
    vehiculosStore.fetchVehiculos(),
    conductoresStore.fetchConductores(),
  ])
})

// --- Handlers ---

function onViewVehiculo(vehiculo: Vehiculo): void {
  detailVehiculo.value = vehiculo
  showDetail.value = true
}



// --- Helpers ---

function getConductorName(vehiculo: Vehiculo): string {
  if (!vehiculo.conductor) return 'Sin asignar'
  return `${vehiculo.conductor.nombre} ${vehiculo.conductor.apellidos}`.trim()
}

function formatCapacidad(kg: number | null): string {
  if (kg == null) return '--'
  if (kg >= 1000) return `${(kg / 1000).toFixed(1)} t`
  return `${kg} kg`
}

function formatDimension(mm: number | null): string {
  if (mm == null) return '--'
  return `${(mm / 1000).toFixed(2)} m`
}

type StyleConfig = {
  bg: string
  text: string
  ring: string
  label: string
}

const estadoConfig: Record<string, StyleConfig> = {
  DISPONIBLE: { bg: 'bg-emerald-50', text: 'text-emerald-700', ring: 'ring-emerald-600/20', label: 'Disponible' },
  EN_MANTENIMIENTO: { bg: 'bg-amber-50', text: 'text-amber-700', ring: 'ring-amber-600/20', label: 'En Mantenimiento' },
  BAJA: { bg: 'bg-gray-50', text: 'text-gray-600', ring: 'ring-gray-500/20', label: 'Baja' },
}

const defaultConfig: StyleConfig = {
  bg: 'bg-gray-50',
  text: 'text-gray-600',
  ring: 'ring-gray-500/20',
  label: '',
}

function getEstadoConfig(estado: string): StyleConfig {
  return estadoConfig[estado] ?? { ...defaultConfig, label: estado }
}

const tipoConfig: Record<string, StyleConfig> = {
  FURGONETA: { bg: 'bg-blue-50', text: 'text-blue-700', ring: 'ring-blue-600/20', label: 'Furgoneta' },
  RIGIDO: { bg: 'bg-indigo-50', text: 'text-indigo-700', ring: 'ring-indigo-600/20', label: 'Rígido' },
  TRAILER: { bg: 'bg-purple-50', text: 'text-purple-700', ring: 'ring-purple-600/20', label: 'Tráiler' },
  ESPECIAL: { bg: 'bg-orange-50', text: 'text-orange-700', ring: 'ring-orange-600/20', label: 'Especial' },
}

function getTipoConfig(tipo: string): StyleConfig {
  return tipoConfig[tipo] ?? { ...defaultConfig, label: tipo }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
          <i class="pi pi-car text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800">Vehículos</h1>
          <p class="text-sm text-gray-500 mt-0.5">Gestión de flota y mantenimiento</p>
        </div>
      </div>
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="vehiculosStore.usingMockData"
      class="flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-4 gap-4">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
            <i class="pi pi-car text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ vehiculosStore.totalVehiculos }}</p>
            <p class="text-xs text-gray-500">Total Flota</p>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <i class="pi pi-check-circle text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ vehiculosStore.disponibles.length }}</p>
            <p class="text-xs text-gray-500">Disponibles</p>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
            <i class="pi pi-wrench text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ vehiculosStore.enMantenimiento.length }}</p>
            <p class="text-xs text-gray-500">En Mantenimiento</p>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-gray-50 text-gray-600 flex items-center justify-center">
            <i class="pi pi-ban text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ vehiculosStore.enBaja.length }}</p>
            <p class="text-xs text-gray-500">De Baja</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Data Table -->
    <VehiculoTable
      :vehiculos="vehiculosStore.vehiculos"
      :loading="vehiculosStore.loading"
      @view="onViewVehiculo"
    />

    <!-- Detail Panel Dialog -->
    <Dialog
      v-model:visible="showDetail"
      :header="`Detalle del Vehículo — ${detailVehiculo?.matricula ?? ''}`"
      :modal="true"
      :closable="true"
      :style="{ width: '700px' }"
    >
      <div v-if="detailVehiculo" class="space-y-6 pt-2">
        <!-- Header with Status -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <div
              class="w-16 h-16 rounded-full flex items-center justify-center"
              :class="detailVehiculo.estado === 'DISPONIBLE' ? 'bg-primary/10' : 'bg-gray-100'"
            >
              <i
                class="pi pi-car text-2xl"
                :class="detailVehiculo.estado === 'DISPONIBLE' ? 'text-primary' : 'text-gray-400'"
              ></i>
            </div>
            <div>
              <h3 class="text-lg font-bold text-gray-800 font-mono">{{ detailVehiculo.matricula }}</h3>
              <p class="text-sm text-gray-500">{{ detailVehiculo.marca }} {{ detailVehiculo.modelo }}</p>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <span
              class="inline-flex items-center px-3 py-1.5 rounded-full text-xs font-medium ring-1 ring-inset"
              :class="[
                getEstadoConfig(detailVehiculo.estado).bg,
                getEstadoConfig(detailVehiculo.estado).text,
                getEstadoConfig(detailVehiculo.estado).ring,
              ]"
            >
              {{ getEstadoConfig(detailVehiculo.estado).label }}
            </span>
            <span
              class="inline-flex items-center px-3 py-1.5 rounded-full text-xs font-medium ring-1 ring-inset"
              :class="[
                getTipoConfig(detailVehiculo.tipo).bg,
                getTipoConfig(detailVehiculo.tipo).text,
                getTipoConfig(detailVehiculo.tipo).ring,
              ]"
            >
              {{ getTipoConfig(detailVehiculo.tipo).label }}
            </span>
          </div>
        </div>

        <!-- Vehicle Info -->
        <div class="bg-gray-50 rounded-xl p-5">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Información del Vehículo</h4>
          <div class="grid grid-cols-2 gap-4">
            <div class="flex items-center gap-3">
              <i class="pi pi-id-card text-gray-400"></i>
              <div>
                <span class="text-xs text-gray-500">Matrícula</span>
                <p class="text-gray-800 text-sm font-medium font-mono">{{ detailVehiculo.matricula }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <i class="pi pi-tag text-gray-400"></i>
              <div>
                <span class="text-xs text-gray-500">Marca / Modelo</span>
                <p class="text-gray-800 text-sm font-medium">{{ detailVehiculo.marca }} {{ detailVehiculo.modelo }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <i class="pi pi-box text-gray-400"></i>
              <div>
                <span class="text-xs text-gray-500">Capacidad de Carga</span>
                <p class="text-gray-800 text-sm font-medium">{{ formatCapacidad(detailVehiculo.capacidadCargaKg) }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <i class="pi pi-user text-gray-400"></i>
              <div>
                <span class="text-xs text-gray-500">Conductor Asignado</span>
                <p class="text-sm font-medium" :class="detailVehiculo.conductor ? 'text-gray-800' : 'text-gray-400 italic'">
                  {{ getConductorName(detailVehiculo) }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Dimensions -->
        <div class="bg-gray-50 rounded-xl p-5">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Dimensiones y Equipamiento</h4>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <span class="text-xs text-gray-500">Largo útil</span>
              <p class="text-gray-800 text-sm font-medium">{{ formatDimension(detailVehiculo.largoUtilMm) }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500">Ancho útil</span>
              <p class="text-gray-800 text-sm font-medium">{{ formatDimension(detailVehiculo.anchoUtilMm) }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500">Alto útil</span>
              <p class="text-gray-800 text-sm font-medium">{{ formatDimension(detailVehiculo.altoUtilMm) }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500">Volumen</span>
              <p class="text-gray-800 text-sm font-medium">{{ detailVehiculo.volumenM3 != null ? `${detailVehiculo.volumenM3} m³` : '--' }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500">Trampilla elevadora</span>
              <p class="mt-1">
                <span
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
                  :class="detailVehiculo.trampillaElevadora
                    ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20'
                    : 'bg-gray-50 text-gray-500 ring-gray-400/20'
                  "
                >
                  {{ detailVehiculo.trampillaElevadora ? 'Sí' : 'No' }}
                </span>
              </p>
            </div>
          </div>
        </div>

        <!-- Stats (Mock) -->
        <div class="grid grid-cols-3 gap-4">
          <div class="bg-blue-50 rounded-xl p-4 text-center">
            <p class="text-2xl font-bold text-blue-700">{{ Math.floor(Math.random() * 50000 + 10000).toLocaleString() }}</p>
            <p class="text-xs text-blue-600 mt-0.5">Km Recorridos</p>
          </div>
          <div class="bg-amber-50 rounded-xl p-4 text-center">
            <p class="text-2xl font-bold text-amber-700">{{ Math.floor(Math.random() * 80 + 5) }}</p>
            <p class="text-xs text-amber-600 mt-0.5">Portes Completados</p>
          </div>
          <div class="bg-green-50 rounded-xl p-4 text-center">
            <p class="text-2xl font-bold text-green-700">{{ Math.floor(Math.random() * 12 + 1) }}</p>
            <p class="text-xs text-green-600 mt-0.5">Meses desde último mantenimiento</p>
          </div>
        </div>

        <!-- Maintenance History (Mock) -->
        <div class="bg-gray-50 rounded-xl p-5">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Historial de Mantenimiento</h4>
          <div class="space-y-3">
            <div class="flex items-center gap-3 bg-white rounded-lg p-3 border border-gray-100">
              <div class="w-8 h-8 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center flex-shrink-0">
                <i class="pi pi-wrench text-sm"></i>
              </div>
              <div class="flex-1">
                <p class="text-sm text-gray-800 font-medium">Revisión ITV</p>
                <p class="text-xs text-gray-400">15/01/2026</p>
              </div>
              <span class="text-xs text-emerald-600 font-medium">Aprobado</span>
            </div>
            <div class="flex items-center gap-3 bg-white rounded-lg p-3 border border-gray-100">
              <div class="w-8 h-8 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center flex-shrink-0">
                <i class="pi pi-cog text-sm"></i>
              </div>
              <div class="flex-1">
                <p class="text-sm text-gray-800 font-medium">Cambio de aceite y filtros</p>
                <p class="text-xs text-gray-400">28/11/2025</p>
              </div>
              <span class="text-xs text-gray-500 font-medium">Completado</span>
            </div>
            <div class="flex items-center gap-3 bg-white rounded-lg p-3 border border-gray-100">
              <div class="w-8 h-8 rounded-full bg-red-50 text-red-600 flex items-center justify-center flex-shrink-0">
                <i class="pi pi-exclamation-triangle text-sm"></i>
              </div>
              <div class="flex-1">
                <p class="text-sm text-gray-800 font-medium">Reparación frenos</p>
                <p class="text-xs text-gray-400">03/09/2025</p>
              </div>
              <span class="text-xs text-gray-500 font-medium">Completado</span>
            </div>
          </div>
        </div>
      </div>
    </Dialog>
  </div>
</template>
