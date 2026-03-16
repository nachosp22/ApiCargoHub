<script setup lang="ts">
import { ref, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import type { Porte } from '@/stores/dashboard'

interface Props {
  portes: Porte[]
  loading?: boolean
}

const props = defineProps<Props>()

const globalFilter = ref('')

const filteredPortes = computed(() => {
  if (!globalFilter.value) return props.portes
  const query = globalFilter.value.toLowerCase()
  return props.portes.filter(
    (p) =>
      String(p.id).includes(query) ||
      p.origen.toLowerCase().includes(query) ||
      p.destino.toLowerCase().includes(query) ||
      p.conductor.toLowerCase().includes(query) ||
      p.estado.toLowerCase().includes(query) ||
      p.fecha.includes(query)
  )
})

type EstadoType = 'COMPLETADO' | 'ENTREGADO' | 'EN_RUTA' | 'PENDIENTE' | 'PROGRAMADO' | 'CANCELADO'

const estadoStyles: Record<EstadoType, string> = {
  COMPLETADO: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  ENTREGADO: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  EN_RUTA: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  PENDIENTE: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  PROGRAMADO: 'bg-blue-50 text-blue-700 ring-blue-600/20',
  CANCELADO: 'bg-gray-50 text-gray-600 ring-gray-500/20',
}

const estadoLabels: Record<string, string> = {
  COMPLETADO: 'Completado',
  ENTREGADO: 'Entregado',
  EN_RUTA: 'En Ruta',
  PENDIENTE: 'Pendiente',
  PROGRAMADO: 'Programado',
  CANCELADO: 'Cancelado',
}

function getEstadoClass(estado: string): string {
  return estadoStyles[estado as EstadoType] ?? 'bg-gray-50 text-gray-600 ring-gray-500/20'
}

function getEstadoLabel(estado: string): string {
  return estadoLabels[estado] ?? estado
}

function formatDate(fecha: string): string {
  if (!fecha || fecha === '—') return '—'
  try {
    const date = new Date(fecha)
    return date.toLocaleDateString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  } catch {
    return fecha
  }
}
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-5">
      <div>
        <h3 class="text-lg font-semibold text-gray-800">Portes Recientes</h3>
        <p class="text-sm text-gray-500 mt-0.5">Últimos portes registrados en el sistema</p>
      </div>

      <!-- Search -->
      <div class="relative">
        <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm"></i>
        <InputText
          v-model="globalFilter"
          placeholder="Buscar portes..."
          class="pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary w-64"
        />
      </div>
    </div>

    <!-- DataTable -->
    <DataTable
      :value="filteredPortes"
      :loading="loading"
      :paginator="true"
      :rows="5"
      :rowsPerPageOptions="[5, 10, 20]"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
      stripedRows
      class="dashboard-table"
      responsiveLayout="scroll"
    >
      <Column field="id" header="ID" :sortable="true" style="min-width: 80px">
        <template #body="{ data }">
          <span class="font-semibold text-gray-800">#{{ data.id }}</span>
        </template>
      </Column>

      <Column field="origen" header="Origen" :sortable="true" style="min-width: 130px">
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <i class="pi pi-map-marker text-xs text-gray-400"></i>
            <span class="text-gray-700">{{ data.origen }}</span>
          </div>
        </template>
      </Column>

      <Column field="destino" header="Destino" :sortable="true" style="min-width: 130px">
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <i class="pi pi-flag text-xs text-gray-400"></i>
            <span class="text-gray-700">{{ data.destino }}</span>
          </div>
        </template>
      </Column>

      <Column field="conductor" header="Conductor" :sortable="true" style="min-width: 150px">
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <div class="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
              <span class="text-xs font-semibold text-primary">{{ data.conductor.charAt(0) }}</span>
            </div>
            <span class="text-gray-700">{{ data.conductor }}</span>
          </div>
        </template>
      </Column>

      <Column field="estado" header="Estado" :sortable="true" style="min-width: 130px">
        <template #body="{ data }">
          <span
            class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
            :class="getEstadoClass(data.estado)"
          >
            {{ getEstadoLabel(data.estado) }}
          </span>
        </template>
      </Column>

      <Column field="fecha" header="Fecha" :sortable="true" style="min-width: 130px">
        <template #body="{ data }">
          <span class="text-gray-600 text-sm">{{ formatDate(data.fecha) }}</span>
        </template>
      </Column>

      <!-- Empty state -->
      <template #empty>
        <div class="text-center py-8">
          <i class="pi pi-inbox text-3xl text-gray-300 mb-2"></i>
          <p class="text-gray-500 text-sm">No se encontraron portes</p>
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
  padding: 0.75rem 0 0 0;
  justify-content: flex-end;
}
:deep(.p-paginator .p-paginator-page.p-highlight) {
  background: #2563EB;
  color: white;
  border-radius: 0.5rem;
}
</style>
