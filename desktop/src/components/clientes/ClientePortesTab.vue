<script setup lang="ts">
import { computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import type { ClientePorte } from '@/stores/clientes'

interface Props {
  portes: ClientePorte[]
  loading?: boolean
}

const props = defineProps<Props>()

// --- Helpers ---

type StyleConfig = {
  bg: string
  text: string
  ring: string
  label: string
}

const estadoConfig: Record<string, StyleConfig> = {
  PENDIENTE: { bg: 'bg-amber-50', text: 'text-amber-700', ring: 'ring-amber-600/20', label: 'Pendiente' },
  ASIGNADO: { bg: 'bg-blue-50', text: 'text-blue-700', ring: 'ring-blue-600/20', label: 'Asignado' },
  EN_TRANSITO: { bg: 'bg-indigo-50', text: 'text-indigo-700', ring: 'ring-indigo-600/20', label: 'En Tránsito' },
  ENTREGADO: { bg: 'bg-emerald-50', text: 'text-emerald-700', ring: 'ring-emerald-600/20', label: 'Entregado' },
  CANCELADO: { bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-600/20', label: 'Cancelado' },
  FACTURADO: { bg: 'bg-purple-50', text: 'text-purple-700', ring: 'ring-purple-600/20', label: 'Facturado' },
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

function formatDate(dateStr: string | undefined): string {
  if (!dateStr || dateStr === '—') return '—'
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

function formatPrice(price: number | undefined): string {
  if (price == null) return '—'
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(price)
}

// suppress unused props warning
const _loading = computed(() => props.loading)
</script>

<template>
  <DataTable
    :value="portes"
    :loading="_loading"
    :paginator="portes.length > 5"
    :rows="5"
    stripedRows
    class="portes-compact-table"
    responsiveLayout="scroll"
    :rowHover="true"
  >
    <!-- ID -->
    <Column field="id" header="ID" :sortable="true" style="min-width: 70px">
      <template #body="slotProps">
        <span class="font-semibold text-gray-800">#{{ slotProps.data.id }}</span>
      </template>
    </Column>

    <!-- Origen -->
    <Column field="origen" header="Origen" :sortable="true" style="min-width: 110px">
      <template #body="slotProps">
        <div class="flex items-center gap-2">
          <i class="pi pi-map-marker text-xs text-gray-400"></i>
          <span class="text-gray-700 text-sm">{{ slotProps.data.origen }}</span>
        </div>
      </template>
    </Column>

    <!-- Destino -->
    <Column field="destino" header="Destino" :sortable="true" style="min-width: 110px">
      <template #body="slotProps">
        <div class="flex items-center gap-2">
          <i class="pi pi-flag text-xs text-gray-400"></i>
          <span class="text-gray-700 text-sm">{{ slotProps.data.destino }}</span>
        </div>
      </template>
    </Column>

    <!-- Estado -->
    <Column field="estado" header="Estado" :sortable="true" style="min-width: 120px">
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

    <!-- Fecha -->
    <Column field="fechaRecogida" header="Fecha" :sortable="true" style="min-width: 120px">
      <template #body="slotProps">
        <span class="text-gray-600 text-sm">{{ formatDate(slotProps.data.fechaRecogida) }}</span>
      </template>
    </Column>

    <!-- Precio -->
    <Column field="precio" header="Precio" :sortable="true" style="min-width: 100px; text-align: right">
      <template #body="slotProps">
        <span class="text-gray-800 text-sm font-medium">{{ formatPrice(slotProps.data.precio) }}</span>
      </template>
    </Column>

    <!-- Empty state -->
    <template #empty>
      <div class="text-center py-8">
        <i class="pi pi-truck text-3xl text-gray-300 mb-2"></i>
        <p class="text-gray-500 text-sm">Este cliente no tiene portes registrados</p>
      </div>
    </template>
  </DataTable>
</template>

<style scoped>
:deep(.p-datatable) {
  font-size: 0.8125rem;
}
:deep(.p-datatable-thead > tr > th) {
  background: #F9FAFB;
  color: #6B7280;
  font-weight: 600;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-color: #F3F4F6;
  padding: 0.5rem 0.75rem;
}
:deep(.p-datatable-tbody > tr > td) {
  padding: 0.5rem 0.75rem;
  border-color: #F3F4F6;
}
</style>
