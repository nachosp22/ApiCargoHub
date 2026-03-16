<script setup lang="ts">
import { ref, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import PorteStatusBadge from './PorteStatusBadge.vue'
import type { Porte, EstadoPorte } from '@/stores/portes'
import { useAuthStore } from '@/stores/auth'

interface Props {
  portes: Porte[]
  loading?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'view', porte: Porte): void
  (e: 'edit', porte: Porte): void
  (e: 'delete', porte: Porte): void
}>()

const authStore = useAuthStore()
const isAdmin = computed(() => {
  const role = authStore.user?.role?.toUpperCase() ?? ''
  return role === 'ADMIN' || role === 'SUPERADMIN' || role === 'ROLE_ADMIN' || role === 'ROLE_SUPERADMIN'
})

// --- Filters ---
const globalFilter = ref('')
const estadoFilter = ref<EstadoPorte | ''>('')

const estadoFilterOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Pendiente', value: 'PENDIENTE' },
  { label: 'Asignado', value: 'ASIGNADO' },
  { label: 'En Tránsito', value: 'EN_TRANSITO' },
  { label: 'Entregado', value: 'ENTREGADO' },
  { label: 'Cancelado', value: 'CANCELADO' },
  { label: 'Facturado', value: 'FACTURADO' },
]

const filteredPortes = computed(() => {
  let result = props.portes

  // Status filter
  if (estadoFilter.value) {
    result = result.filter((p) => p.estado === estadoFilter.value)
  }

  // Global text search
  if (globalFilter.value) {
    const query = globalFilter.value.toLowerCase()
    result = result.filter(
      (p) =>
        String(p.id).includes(query) ||
        p.origen.toLowerCase().includes(query) ||
        p.destino.toLowerCase().includes(query) ||
        getConductorName(p).toLowerCase().includes(query) ||
        p.estado.toLowerCase().includes(query) ||
        (p.descripcionCliente ?? '').toLowerCase().includes(query)
    )
  }

  return result
})

// --- Row Click Handler ---
function onRowClick(event: { data: Porte }): void {
  emit('view', event.data)
}

// --- Helpers ---

function getConductorName(porte: Porte): string {
  if (!porte.conductor) return '—'
  const c = porte.conductor
  return `${c.nombre}${c.apellidos ? ' ' + c.apellidos : ''}`
}

function getConductorInitials(porte: Porte): string {
  if (!porte.conductor) return '?'
  const parts = getConductorName(porte).split(' ')
  return parts
    .slice(0, 2)
    .map((s) => s.charAt(0).toUpperCase())
    .join('')
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
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100">
    <!-- Table Header with Filters -->
    <div class="p-5 border-b border-gray-100">
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h3 class="text-lg font-semibold text-gray-800">Lista de Portes</h3>
          <p class="text-sm text-gray-500 mt-0.5">
            {{ filteredPortes.length }} portes encontrados
          </p>
        </div>

        <div class="flex items-center gap-3">
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
              placeholder="Buscar portes..."
              class="pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary w-64"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- DataTable -->
    <DataTable
      :value="filteredPortes"
      :loading="loading"
      :paginator="true"
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20, 50]"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
      stripedRows
      class="portes-table"
      responsiveLayout="scroll"
      :rowHover="true"
      @row-click="onRowClick"
      :pt="{ bodyRow: { style: 'cursor: pointer' } }"
    >
      <!-- ID -->
      <Column field="id" header="ID" :sortable="true" style="min-width: 80px">
        <template #body="slotProps">
          <span class="font-semibold text-gray-800">#{{ slotProps.data.id }}</span>
        </template>
      </Column>

      <!-- Origen -->
      <Column field="origen" header="Origen" :sortable="true" style="min-width: 130px">
        <template #body="slotProps">
          <div class="flex items-center gap-2">
            <i class="pi pi-map-marker text-xs text-gray-400"></i>
            <span class="text-gray-700">{{ slotProps.data.origen }}</span>
          </div>
        </template>
      </Column>

      <!-- Destino -->
      <Column field="destino" header="Destino" :sortable="true" style="min-width: 130px">
        <template #body="slotProps">
          <div class="flex items-center gap-2">
            <i class="pi pi-flag text-xs text-gray-400"></i>
            <span class="text-gray-700">{{ slotProps.data.destino }}</span>
          </div>
        </template>
      </Column>

      <!-- Conductor -->
      <Column header="Conductor" :sortable="false" style="min-width: 170px">
        <template #body="slotProps">
          <div class="flex items-center gap-2">
            <div
              class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
              :class="slotProps.data.conductor ? 'bg-primary/10' : 'bg-gray-100'"
            >
              <span
                class="text-xs font-semibold"
                :class="slotProps.data.conductor ? 'text-primary' : 'text-gray-400'"
              >
                {{ getConductorInitials(slotProps.data) }}
              </span>
            </div>
            <span class="text-gray-700">{{ getConductorName(slotProps.data) }}</span>
          </div>
        </template>
      </Column>

      <!-- Estado -->
      <Column field="estado" header="Estado" :sortable="true" style="min-width: 130px">
        <template #body="slotProps">
          <PorteStatusBadge :estado="slotProps.data.estado" />
        </template>
      </Column>

      <!-- Fecha Recogida -->
      <Column field="fechaRecogida" header="Fecha Programada" :sortable="true" style="min-width: 140px">
        <template #body="slotProps">
          <span class="text-gray-600 text-sm">{{ formatDate(slotProps.data.fechaRecogida) }}</span>
        </template>
      </Column>

      <!-- Acciones -->
      <Column header="Acciones" style="min-width: 120px; text-align: center" :exportable="false">
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
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              size="small"
              v-tooltip.top="'Editar'"
              @click="emit('edit', slotProps.data)"
            />
            <Button
              v-if="isAdmin"
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              size="small"
              v-tooltip.top="'Eliminar'"
              @click="emit('delete', slotProps.data)"
            />
          </div>
        </template>
      </Column>

      <!-- Empty state -->
      <template #empty>
        <div class="text-center py-12">
          <i class="pi pi-truck text-4xl text-gray-300 mb-3"></i>
          <p class="text-gray-500">No se encontraron portes</p>
          <p class="text-gray-400 text-sm mt-1">Intenta ajustar los filtros o crear un nuevo porte</p>
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
