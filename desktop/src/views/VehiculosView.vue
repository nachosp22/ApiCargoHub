<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useVehiculosStore } from '@/stores/vehiculos'
import { useConductoresStore } from '@/stores/conductores'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import VehiculoTable from '@/components/vehiculos/VehiculoTable.vue'
import VehiculoDialog from '@/components/vehiculos/VehiculoDialog.vue'
import type { Vehiculo, CreateVehiculoRequest } from '@/stores/vehiculos'

const vehiculosStore = useVehiculosStore()
const conductoresStore = useConductoresStore()
const toast = useToast()
const route = useRoute()
const router = useRouter()

// --- Dialog state ---
const showDialog = ref(false)
const editingVehiculo = ref<Vehiculo | null>(null)

// --- Detail panel state ---
const showDetail = ref(false)
const detailVehiculo = ref<Vehiculo | null>(null)

// --- Lifecycle ---
onMounted(async () => {
  await Promise.all([
    vehiculosStore.fetchVehiculos(),
    conductoresStore.fetchConductores(),
  ])
  await openVehiculoFromQuery()
})

watch(() => route.query.vehiculoId, () => {
  void openVehiculoFromQuery()
})

// --- Handlers ---

function onViewVehiculo(vehiculo: Vehiculo): void {
  detailVehiculo.value = vehiculo
  showDetail.value = true
}

function onEditVehiculo(vehiculo: Vehiculo): void {
  editingVehiculo.value = vehiculo
  showDialog.value = true
}

function onNewVehiculo(): void {
  editingVehiculo.value = null
  showDialog.value = true
}

async function openVehiculoFromQuery(): Promise<void> {
  const rawId = route.query.vehiculoId
  const vehiculoId = typeof rawId === 'string' ? Number.parseInt(rawId, 10) : NaN
  if (Number.isNaN(vehiculoId)) return

  const vehiculo = await vehiculosStore.fetchVehiculoById(vehiculoId)
  if (vehiculo) {
    onViewVehiculo(vehiculo)
    await clearQueryParam('vehiculoId')
  }
}

async function clearQueryParam(param: string): Promise<void> {
  const nextQuery = { ...route.query }
  delete nextQuery[param]
  await router.replace({ query: nextQuery })
}

async function onSaveVehiculo(data: CreateVehiculoRequest): Promise<void> {
  try {
    if (editingVehiculo.value) {
      await vehiculosStore.updateVehiculo(editingVehiculo.value.id, data)
      toast.add({
        severity: 'success',
        summary: 'Vehículo actualizado',
        detail: `El vehículo ${data.matricula} se ha actualizado correctamente.`,
        life: 3000,
      })
    } else {
      const created = await vehiculosStore.createVehiculo(data)
      toast.add({
        severity: 'success',
        summary: 'Vehículo creado',
        detail: `El vehículo ${created.matricula} se ha creado correctamente.`,
        life: 3000,
      })
    }
    showDialog.value = false
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo guardar el vehículo. Inténtalo de nuevo.',
      life: 5000,
    })
  }
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
  DISPONIBLE: { bg: 'bg-emerald-50 dark:bg-emerald-900/30', text: 'text-emerald-700 dark:text-emerald-400', ring: 'ring-emerald-600/20 dark:ring-emerald-500/30', label: 'Disponible' },
  EN_MANTENIMIENTO: { bg: 'bg-amber-50 dark:bg-amber-900/30', text: 'text-amber-700 dark:text-amber-400', ring: 'ring-amber-600/20 dark:ring-amber-500/30', label: 'En Mantenimiento' },
  BAJA: { bg: 'bg-gray-50 dark:bg-gray-700', text: 'text-gray-600 dark:text-gray-300', ring: 'ring-gray-500/20 dark:ring-gray-400/30', label: 'Baja' },
}

const defaultConfig: StyleConfig = {
  bg: 'bg-gray-50 dark:bg-gray-700',
  text: 'text-gray-600 dark:text-gray-300',
  ring: 'ring-gray-500/20 dark:ring-gray-400/30',
  label: '',
}

function getEstadoConfig(estado: string): StyleConfig {
  return estadoConfig[estado] ?? { ...defaultConfig, label: estado }
}

const tipoConfig: Record<string, StyleConfig> = {
  FURGONETA: { bg: 'bg-blue-50 dark:bg-blue-900/30', text: 'text-blue-700 dark:text-blue-400', ring: 'ring-blue-600/20 dark:ring-blue-500/30', label: 'Furgoneta' },
  RIGIDO: { bg: 'bg-indigo-50 dark:bg-indigo-900/30', text: 'text-indigo-700 dark:text-indigo-400', ring: 'ring-indigo-600/20 dark:ring-indigo-500/30', label: 'Rígido' },
  TRAILER: { bg: 'bg-purple-50 dark:bg-purple-900/30', text: 'text-purple-700 dark:text-purple-400', ring: 'ring-purple-600/20 dark:ring-purple-500/30', label: 'Tráiler' },
  ESPECIAL: { bg: 'bg-orange-50 dark:bg-orange-900/30', text: 'text-orange-700 dark:text-orange-400', ring: 'ring-orange-600/20 dark:ring-orange-500/30', label: 'Especial' },
}

function getTipoConfig(tipo: string): StyleConfig {
  return tipoConfig[tipo] ?? { ...defaultConfig, label: tipo }
}
</script>

<template>
  <div class="h-full min-h-0 flex flex-col gap-6 overflow-hidden">
    <!-- Page Header -->
    <div class="shrink-0 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 flex items-center justify-center">
          <i class="pi pi-car text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Vehículos</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Gestión de flota y mantenimiento</p>
        </div>
      </div>
      <Button
        label="Nuevo Vehículo"
        icon="pi pi-plus"
        @click="onNewVehiculo"
      />
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="vehiculosStore.usingMockData"
      class="shrink-0 flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Data Table -->
    <div class="flex-1 min-h-0 overflow-auto">
      <VehiculoTable
        :vehiculos="vehiculosStore.vehiculos"
        :loading="vehiculosStore.loading"
        @view="onViewVehiculo"
        @edit="onEditVehiculo"
      />
    </div>

    <!-- Create/Edit Dialog -->
    <VehiculoDialog
      v-model:visible="showDialog"
      :vehiculo="editingVehiculo"
      :saving="vehiculosStore.saving"
      @save="onSaveVehiculo"
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
              <h3 class="text-lg font-bold text-gray-800 dark:text-gray-100 font-mono">{{ detailVehiculo.matricula }}</h3>
              <p class="text-sm text-gray-500 dark:text-gray-400">{{ detailVehiculo.marca }} {{ detailVehiculo.modelo }}</p>
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
        <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-5">
          <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-3">Información del Vehículo</h4>
          <div class="grid grid-cols-2 gap-4">
            <div class="flex items-center gap-3">
              <i class="pi pi-id-card text-gray-400 dark:text-gray-500"></i>
              <div>
                <span class="text-xs text-gray-500 dark:text-gray-400">Matrícula</span>
                <p class="text-gray-800 dark:text-gray-100 text-sm font-medium font-mono">{{ detailVehiculo.matricula }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <i class="pi pi-tag text-gray-400 dark:text-gray-500"></i>
              <div>
                <span class="text-xs text-gray-500 dark:text-gray-400">Marca / Modelo</span>
                <p class="text-gray-800 dark:text-gray-100 text-sm font-medium">{{ detailVehiculo.marca }} {{ detailVehiculo.modelo }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <i class="pi pi-box text-gray-400 dark:text-gray-500"></i>
              <div>
                <span class="text-xs text-gray-500 dark:text-gray-400">Capacidad de Carga</span>
                <p class="text-gray-800 dark:text-gray-100 text-sm font-medium">{{ formatCapacidad(detailVehiculo.capacidadCargaKg) }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <i class="pi pi-user text-gray-400 dark:text-gray-500"></i>
              <div>
                <span class="text-xs text-gray-500 dark:text-gray-400">Conductor Asignado</span>
                <p class="text-sm font-medium" :class="detailVehiculo.conductor ? 'text-gray-800 dark:text-gray-100' : 'text-gray-400 italic'">
                  {{ getConductorName(detailVehiculo) }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Dimensions -->
        <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-5">
          <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-3">Dimensiones y Equipamiento</h4>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <span class="text-xs text-gray-500 dark:text-gray-400">Largo útil</span>
              <p class="text-gray-800 dark:text-gray-100 text-sm font-medium">{{ formatDimension(detailVehiculo.largoUtilMm) }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500 dark:text-gray-400">Ancho útil</span>
              <p class="text-gray-800 dark:text-gray-100 text-sm font-medium">{{ formatDimension(detailVehiculo.anchoUtilMm) }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500 dark:text-gray-400">Alto útil</span>
              <p class="text-gray-800 dark:text-gray-100 text-sm font-medium">{{ formatDimension(detailVehiculo.altoUtilMm) }}</p>
            </div>
            <div>
              <span class="text-xs text-gray-500 dark:text-gray-400">Volumen</span>
              <p class="text-gray-800 dark:text-gray-100 text-sm font-medium">{{ detailVehiculo.volumenM3 != null ? `${detailVehiculo.volumenM3} m³` : '--' }}</p>
            </div>
          </div>
        </div>


      </div>
    </Dialog>
  </div>
</template>
