<script setup lang="ts">
import { ref, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import IncidenciaStatusBadge from './IncidenciaStatusBadge.vue'
import SeveridadBadge from './SeveridadBadge.vue'
import SlaIndicator from './SlaIndicator.vue'
import type { Incidencia, EstadoIncidencia, SeveridadIncidencia } from '@/stores/incidencias'

interface Props {
  incidencias: Incidencia[]
  loading?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'view', incidencia: Incidencia): void
  (e: 'resolve', incidencia: Incidencia): void
}>()

// --- Filters ---
const globalFilter = ref('')
const estadoFilter = ref<EstadoIncidencia | ''>('')
const severidadFilter = ref<SeveridadIncidencia | ''>('')

const estadoFilterOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Abierta', value: 'ABIERTA' },
  { label: 'En Revisión', value: 'EN_REVISION' },
  { label: 'Resuelta', value: 'RESUELTA' },
  { label: 'Desestimada', value: 'DESESTIMADA' },
]

const severidadFilterOptions = [
  { label: 'Todas las severidades', value: '' },
  { label: 'Alta', value: 'ALTA' },
  { label: 'Media', value: 'MEDIA' },
  { label: 'Baja', value: 'BAJA' },
]

const filteredIncidencias = computed(() => {
  let result = props.incidencias

  // Status filter
  if (estadoFilter.value) {
    result = result.filter((i) => i.estado === estadoFilter.value)
  }

  // Severity filter
  if (severidadFilter.value) {
    result = result.filter((i) => i.severidad === severidadFilter.value)
  }

  // Global text search
  if (globalFilter.value) {
    const query = globalFilter.value.toLowerCase()
    result = result.filter(
      (i) =>
        String(i.id).includes(query) ||
        String(i.porteId).includes(query) ||
        i.titulo.toLowerCase().includes(query) ||
        i.descripcion.toLowerCase().includes(query) ||
        i.estado.toLowerCase().includes(query) ||
        i.severidad.toLowerCase().includes(query) ||
        i.prioridad.toLowerCase().includes(query)
    )
  }

  return result
})

function isTerminalState(estado: EstadoIncidencia): boolean {
  return estado === 'RESUELTA' || estado === 'DESESTIMADA'
}

// --- Helpers ---

function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  } catch {
    return dateStr
  }
}

function truncate(text: string, maxLen: number): string {
  if (text.length <= maxLen) return text
  return text.slice(0, maxLen) + '…'
}

function getPrioridadConfig(prioridad: string): { bg: string; text: string; ring: string; label: string } {
  const configs: Record<string, { bg: string; text: string; ring: string; label: string }> = {
    ALTA: { bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-600/20', label: 'Alta' },
    MEDIA: { bg: 'bg-orange-50', text: 'text-orange-700', ring: 'ring-orange-600/20', label: 'Media' },
    BAJA: { bg: 'bg-emerald-50', text: 'text-emerald-700', ring: 'ring-emerald-600/20', label: 'Baja' },
  }
  return configs[prioridad] ?? { bg: 'bg-gray-50', text: 'text-gray-600', ring: 'ring-gray-500/20', label: prioridad }
}
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100">
    <!-- Table Header with Filters -->
    <div class="p-5 border-b border-gray-100">
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h3 class="text-lg font-semibold text-gray-800">Lista de Incidencias</h3>
          <p class="text-sm text-gray-500 mt-0.5">
            {{ filteredIncidencias.length }} incidencias encontradas
          </p>
        </div>

        <div class="flex items-center gap-3 flex-wrap">
          <!-- Status Filter -->
          <Select
            v-model="estadoFilter"
            :options="estadoFilterOptions"
            optionLabel="label"
            optionValue="value"
            class="w-44"
          />

          <!-- Severity Filter -->
          <Select
            v-model="severidadFilter"
            :options="severidadFilterOptions"
            optionLabel="label"
            optionValue="value"
            class="w-48"
          />

          <!-- Global Search -->
          <div class="relative">
            <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm"></i>
            <InputText
              v-model="globalFilter"
              placeholder="Buscar incidencias..."
              class="pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary w-64"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- DataTable -->
    <DataTable
      :value="filteredIncidencias"
      :loading="loading"
      :paginator="true"
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20]"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
      stripedRows
      class="incidencias-table"
      responsiveLayout="scroll"
      :rowHover="true"
    >
      <!-- ID -->
      <Column field="id" header="ID" :sortable="true" style="min-width: 70px">
        <template #body="slotProps">
          <span class="font-semibold text-gray-800">#{{ slotProps.data.id }}</span>
        </template>
      </Column>

      <!-- Porte ID -->
      <Column field="porteId" header="Porte" :sortable="true" style="min-width: 90px">
        <template #body="slotProps">
          <span class="text-blue-600 font-medium cursor-pointer hover:underline">
            #{{ slotProps.data.porteId }}
          </span>
        </template>
      </Column>

      <!-- Descripción / Título -->
      <Column field="titulo" header="Descripción" :sortable="true" style="min-width: 220px">
        <template #body="slotProps">
          <div>
            <p class="text-gray-800 text-sm font-medium">{{ truncate(slotProps.data.titulo, 50) }}</p>
            <p class="text-gray-400 text-xs mt-0.5">{{ truncate(slotProps.data.descripcion, 60) }}</p>
          </div>
        </template>
      </Column>

      <!-- Severidad -->
      <Column field="severidad" header="Severidad" :sortable="true" style="min-width: 100px">
        <template #body="slotProps">
          <SeveridadBadge :severidad="slotProps.data.severidad" />
        </template>
      </Column>

      <!-- Prioridad -->
      <Column field="prioridad" header="Prioridad" :sortable="true" style="min-width: 100px">
        <template #body="slotProps">
          <span
            class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
            :class="[
              getPrioridadConfig(slotProps.data.prioridad).bg,
              getPrioridadConfig(slotProps.data.prioridad).text,
              getPrioridadConfig(slotProps.data.prioridad).ring,
            ]"
          >
            {{ getPrioridadConfig(slotProps.data.prioridad).label }}
          </span>
        </template>
      </Column>

      <!-- Estado -->
      <Column field="estado" header="Estado" :sortable="true" style="min-width: 120px">
        <template #body="slotProps">
          <IncidenciaStatusBadge :estado="slotProps.data.estado" />
        </template>
      </Column>

      <!-- Fecha Creación -->
      <Column field="fechaReporte" header="Fecha Creación" :sortable="true" style="min-width: 120px">
        <template #body="slotProps">
          <span class="text-gray-600 text-sm">{{ formatDate(slotProps.data.fechaReporte) }}</span>
        </template>
      </Column>

      <!-- SLA -->
      <Column field="fechaLimiteSla" header="SLA" :sortable="true" style="min-width: 140px">
        <template #body="slotProps">
          <SlaIndicator :fecha-limite-sla="slotProps.data.fechaLimiteSla" :estado="slotProps.data.estado" />
        </template>
      </Column>

      <!-- Acciones -->
      <Column header="Acciones" style="min-width: 100px; text-align: center" :exportable="false">
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
            <Button
              v-if="!isTerminalState(slotProps.data.estado)"
              icon="pi pi-check"
              severity="success"
              text
              rounded
              size="small"
              v-tooltip.top="'Resolver'"
              @click="emit('resolve', slotProps.data)"
            />
          </div>
        </template>
      </Column>

      <!-- Empty state -->
      <template #empty>
        <div class="text-center py-12">
          <i class="pi pi-exclamation-triangle text-4xl text-gray-300 mb-3"></i>
          <p class="text-gray-500">No se encontraron incidencias</p>
          <p class="text-gray-400 text-sm mt-1">Intenta ajustar los filtros o crear una nueva incidencia</p>
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
