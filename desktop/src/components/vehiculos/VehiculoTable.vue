<script setup lang="ts">
import { ref, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import type { Vehiculo, EstadoVehiculo, TipoVehiculo } from '@/stores/vehiculos'

interface Props {
  vehiculos: Vehiculo[]
  loading?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'view', vehiculo: Vehiculo): void
}>()

// --- Filters ---
const globalFilter = ref('')
const estadoFilter = ref<EstadoVehiculo | ''>('')
const tipoFilter = ref<TipoVehiculo | ''>('')

const estadoFilterOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Disponible', value: 'DISPONIBLE' },
  { label: 'En Mantenimiento', value: 'EN_MANTENIMIENTO' },
  { label: 'Baja', value: 'BAJA' },
]

const tipoFilterOptions = [
  { label: 'Todos los tipos', value: '' },
  { label: 'Furgoneta', value: 'FURGONETA' },
  { label: 'Rígido', value: 'RIGIDO' },
  { label: 'Tráiler', value: 'TRAILER' },
  { label: 'Especial', value: 'ESPECIAL' },
]

const filteredVehiculos = computed(() => {
  let result = props.vehiculos

  // Status filter
  if (estadoFilter.value) {
    result = result.filter((v) => v.estado === estadoFilter.value)
  }

  // Type filter
  if (tipoFilter.value) {
    result = result.filter((v) => v.tipo === tipoFilter.value)
  }

  // Global text search
  if (globalFilter.value) {
    const query = globalFilter.value.toLowerCase()
    result = result.filter(
      (v) =>
        String(v.id).includes(query) ||
        v.matricula.toLowerCase().includes(query) ||
        v.marca.toLowerCase().includes(query) ||
        v.modelo.toLowerCase().includes(query) ||
        v.tipo.toLowerCase().includes(query) ||
        getConductorName(v).toLowerCase().includes(query)
    )
  }

  return result
})

// --- Row Click Handler ---
function onRowClick(event: { data: Vehiculo }): void {
  emit('view', event.data)
}

// --- Helpers ---

function getConductorName(vehiculo: Vehiculo): string {
  if (!vehiculo.conductor) return 'Sin asignar'
  return `${vehiculo.conductor.nombre} ${vehiculo.conductor.apellidos}`.trim()
}

function getConductorInitials(vehiculo: Vehiculo): string {
  if (!vehiculo.conductor) return '?'
  const parts = getConductorName(vehiculo).split(' ')
  return parts
    .slice(0, 2)
    .map((s) => s.charAt(0).toUpperCase())
    .join('')
}

function formatCapacidad(kg: number | null): string {
  if (kg == null) return '--'
  if (kg >= 1000) {
    return `${(kg / 1000).toFixed(1)} t`
  }
  return `${kg} kg`
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

const defaultEstadoConfig: StyleConfig = {
  bg: 'bg-gray-50',
  text: 'text-gray-600',
  ring: 'ring-gray-500/20',
  label: '',
}

function getEstadoConfig(estado: string): StyleConfig {
  return estadoConfig[estado] ?? { ...defaultEstadoConfig, label: estado }
}

const tipoConfig: Record<string, StyleConfig> = {
  FURGONETA: { bg: 'bg-blue-50', text: 'text-blue-700', ring: 'ring-blue-600/20', label: 'Furgoneta' },
  RIGIDO: { bg: 'bg-indigo-50', text: 'text-indigo-700', ring: 'ring-indigo-600/20', label: 'Rígido' },
  TRAILER: { bg: 'bg-purple-50', text: 'text-purple-700', ring: 'ring-purple-600/20', label: 'Tráiler' },
  ESPECIAL: { bg: 'bg-orange-50', text: 'text-orange-700', ring: 'ring-orange-600/20', label: 'Especial' },
}

const defaultTipoConfig: StyleConfig = {
  bg: 'bg-gray-50',
  text: 'text-gray-600',
  ring: 'ring-gray-500/20',
  label: '',
}

function getTipoConfig(tipo: string): StyleConfig {
  return tipoConfig[tipo] ?? { ...defaultTipoConfig, label: tipo }
}
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100">
    <!-- Table Header with Filters -->
    <div class="p-5 border-b border-gray-100">
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h3 class="text-lg font-semibold text-gray-800">Flota de Vehículos</h3>
          <p class="text-sm text-gray-500 mt-0.5">
            {{ filteredVehiculos.length }} vehículos encontrados
          </p>
        </div>

        <div class="flex items-center gap-3">
          <!-- Type Filter -->
          <Select
            v-model="tipoFilter"
            :options="tipoFilterOptions"
            optionLabel="label"
            optionValue="value"
            class="w-44"
          />

          <!-- Status Filter -->
          <Select
            v-model="estadoFilter"
            :options="estadoFilterOptions"
            optionLabel="label"
            optionValue="value"
            class="w-48"
          />

          <!-- Global Search -->
          <div class="relative">
            <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm"></i>
            <InputText
              v-model="globalFilter"
              placeholder="Buscar vehículos..."
              class="pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary w-64"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- DataTable -->
    <DataTable
      :value="filteredVehiculos"
      :loading="loading"
      :paginator="true"
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20]"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
      stripedRows
      class="vehiculos-table"
      responsiveLayout="scroll"
      :rowHover="true"
      @row-click="onRowClick"
      :pt="{ bodyRow: { style: 'cursor: pointer' } }"
    >
      <!-- Matrícula -->
      <Column header="Matrícula" :sortable="true" field="matricula" style="min-width: 130px">
        <template #body="slotProps">
          <span class="text-gray-800 font-bold text-sm font-mono">{{ slotProps.data.matricula }}</span>
        </template>
      </Column>

      <!-- Marca / Modelo -->
      <Column header="Marca / Modelo" :sortable="true" field="marca" style="min-width: 200px">
        <template #body="slotProps">
          <div>
            <p class="text-gray-800 font-medium text-sm">{{ slotProps.data.marca }}</p>
            <p class="text-gray-400 text-xs">{{ slotProps.data.modelo }}</p>
          </div>
        </template>
      </Column>

      <!-- Tipo -->
      <Column field="tipo" header="Tipo" :sortable="true" style="min-width: 120px">
        <template #body="slotProps">
          <span
            class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
            :class="[
              getTipoConfig(slotProps.data.tipo).bg,
              getTipoConfig(slotProps.data.tipo).text,
              getTipoConfig(slotProps.data.tipo).ring,
            ]"
          >
            {{ getTipoConfig(slotProps.data.tipo).label }}
          </span>
        </template>
      </Column>

      <!-- Capacidad -->
      <Column field="capacidadCargaKg" header="Capacidad" :sortable="true" style="min-width: 110px">
        <template #body="slotProps">
          <span class="text-gray-700 text-sm">{{ formatCapacidad(slotProps.data.capacidadCargaKg) }}</span>
        </template>
      </Column>

      <!-- Conductor Asignado -->
      <Column header="Conductor" field="conductor" style="min-width: 200px">
        <template #body="slotProps">
          <div class="flex items-center gap-2">
            <div
              class="w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0"
              :class="slotProps.data.conductor ? 'bg-primary/10' : 'bg-gray-100'"
            >
              <span
                class="text-xs font-semibold"
                :class="slotProps.data.conductor ? 'text-primary' : 'text-gray-400'"
              >
                {{ getConductorInitials(slotProps.data) }}
              </span>
            </div>
            <span
              class="text-sm"
              :class="slotProps.data.conductor ? 'text-gray-700' : 'text-gray-400 italic'"
            >
              {{ getConductorName(slotProps.data) }}
            </span>
          </div>
        </template>
      </Column>

      <!-- Estado -->
      <Column field="estado" header="Estado" :sortable="true" style="min-width: 150px">
        <template #body="slotProps">
          <span
            class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
            :class="[
              getEstadoConfig(slotProps.data.estado).bg,
              getEstadoConfig(slotProps.data.estado).text,
              getEstadoConfig(slotProps.data.estado).ring,
            ]"
          >
            {{ getEstadoConfig(slotProps.data.estado).label }}
          </span>
        </template>
      </Column>

      <!-- Acciones -->
      <Column header="Acciones" style="min-width: 80px; text-align: center" :exportable="false">
        <template #body="slotProps">
          <div class="flex items-center justify-center gap-1" @click.stop>
            <Button
              icon="pi pi-eye"
              severity="secondary"
              text
              rounded
              size="small"
              v-tooltip.top="'Ver detalle'"
              @click="emit('view', slotProps.data)"
            />
          </div>
        </template>
      </Column>

      <!-- Empty state -->
      <template #empty>
        <div class="text-center py-12">
          <i class="pi pi-car text-4xl text-gray-300 mb-3"></i>
          <p class="text-gray-500">No se encontraron vehículos</p>
          <p class="text-gray-400 text-sm mt-1">Intenta ajustar los filtros o dar de alta un nuevo vehículo</p>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<style scoped>
/* Override PrimeVue DataTable styles for cleaner look */
:deep(.p-datatable) {
  font-size: 0.875rem;
}
:deep(.p-datatable-header) {
  background: transparent;
  border: none;
  padding: 0;
}
:deep(.p-datatable-thead > tr > th) {
  background: #F9FAFB;
  color: #6B7280;
  font-weight: 600;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-color: #F3F4F6;
  padding: 0.75rem 1rem;
}
:deep(.p-datatable-tbody > tr > td) {
  padding: 0.75rem 1rem;
  border-color: #F3F4F6;
}
:deep(.p-datatable-tbody > tr:hover) {
  background-color: #F9FAFB !important;
}
:deep(.p-paginator) {
  border: none;
  padding: 0.75rem 1rem;
  justify-content: flex-end;
}
:deep(.p-paginator .p-paginator-page.p-highlight) {
  background: #2563EB;
  color: white;
  border-radius: 0.5rem;
}
</style>
