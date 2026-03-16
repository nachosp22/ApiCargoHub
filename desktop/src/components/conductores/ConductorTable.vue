<script setup lang="ts">
import { ref, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import type { Conductor, EstadoConductor } from '@/stores/conductores'

interface Props {
  conductores: Conductor[]
  loading?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'view', conductor: Conductor): void
  (e: 'edit', conductor: Conductor): void
  (e: 'toggle-estado', conductor: Conductor): void
}>()

// --- Filters ---
const globalFilter = ref('')
const estadoFilter = ref<EstadoConductor | ''>('')

const estadoFilterOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Activo', value: 'ACTIVO' },
  { label: 'Inactivo', value: 'INACTIVO' },
  { label: 'Suspendido', value: 'SUSPENDIDO' },
]

const filteredConductores = computed(() => {
  let result = props.conductores

  // Status filter
  if (estadoFilter.value) {
    result = result.filter((c) => c.estado === estadoFilter.value)
  }

  // Global text search
  if (globalFilter.value) {
    const query = globalFilter.value.toLowerCase()
    result = result.filter(
      (c) =>
        String(c.id).includes(query) ||
        c.nombre.toLowerCase().includes(query) ||
        c.apellidos.toLowerCase().includes(query) ||
        c.email.toLowerCase().includes(query) ||
        c.telefono.toLowerCase().includes(query) ||
        c.dni.toLowerCase().includes(query) ||
        c.ciudadBase.toLowerCase().includes(query)
    )
  }

  return result
})

// --- Row Click Handler ---
function onRowClick(event: { data: Conductor }): void {
  emit('view', event.data)
}

// --- Helpers ---

function getFullName(conductor: Conductor): string {
  return `${conductor.nombre} ${conductor.apellidos}`.trim()
}

function getInitials(conductor: Conductor): string {
  const parts = getFullName(conductor).split(' ')
  return parts
    .slice(0, 2)
    .map((s) => s.charAt(0).toUpperCase())
    .join('')
}

type StyleConfig = {
  bg: string
  text: string
  ring: string
  label: string
}

const estadoConfig: Record<string, StyleConfig> = {
  ACTIVO: { bg: 'bg-emerald-50', text: 'text-emerald-700', ring: 'ring-emerald-600/20', label: 'Activo' },
  INACTIVO: { bg: 'bg-gray-50', text: 'text-gray-600', ring: 'ring-gray-500/20', label: 'Inactivo' },
  SUSPENDIDO: { bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-600/20', label: 'Suspendido' },
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
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100">
    <!-- Table Header with Filters -->
    <div class="p-5 border-b border-gray-100">
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h3 class="text-lg font-semibold text-gray-800">Lista de Conductores</h3>
          <p class="text-sm text-gray-500 mt-0.5">
            {{ filteredConductores.length }} conductores encontrados
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
              placeholder="Buscar conductores..."
              class="pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary w-64"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- DataTable -->
    <DataTable
      :value="filteredConductores"
      :loading="loading"
      :paginator="true"
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20]"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
      stripedRows
      class="conductores-table"
      responsiveLayout="scroll"
      :rowHover="true"
      @row-click="onRowClick"
      :pt="{ bodyRow: { style: 'cursor: pointer' } }"
    >
      <!-- Avatar + Nombre -->
      <Column header="Conductor" :sortable="true" field="nombre" style="min-width: 220px">
        <template #body="slotProps">
          <div class="flex items-center gap-3">
            <div
              class="w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0"
              :class="slotProps.data.estado === 'ACTIVO' ? 'bg-primary/10' : 'bg-gray-100'"
            >
              <span
                class="text-xs font-semibold"
                :class="slotProps.data.estado === 'ACTIVO' ? 'text-primary' : 'text-gray-400'"
              >
                {{ getInitials(slotProps.data) }}
              </span>
            </div>
            <div>
              <p class="text-gray-800 font-medium text-sm">{{ getFullName(slotProps.data) }}</p>
              <p class="text-gray-400 text-xs">{{ slotProps.data.ciudadBase || '—' }}</p>
            </div>
          </div>
        </template>
      </Column>

      <!-- Email -->
      <Column field="email" header="Email" :sortable="true" style="min-width: 200px">
        <template #body="slotProps">
          <div class="flex items-center gap-2">
            <i class="pi pi-envelope text-xs text-gray-400"></i>
            <span class="text-gray-700 text-sm">{{ slotProps.data.email || '—' }}</span>
          </div>
        </template>
      </Column>

      <!-- Teléfono -->
      <Column field="telefono" header="Teléfono" style="min-width: 130px">
        <template #body="slotProps">
          <span class="text-gray-600 text-sm">{{ slotProps.data.telefono || '—' }}</span>
        </template>
      </Column>

      <!-- Licencia / DNI -->
      <Column field="dni" header="Licencia / DNI" style="min-width: 130px">
        <template #body="slotProps">
          <span class="text-gray-600 text-sm font-mono">{{ slotProps.data.dni || '—' }}</span>
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

      <!-- Portes Asignados -->
      <Column field="portesAsignados" header="Portes" :sortable="true" style="min-width: 90px; text-align: center">
        <template #body="slotProps">
          <span
            class="inline-flex items-center justify-center w-8 h-8 rounded-full text-xs font-semibold"
            :class="slotProps.data.portesAsignados > 0 ? 'bg-blue-50 text-blue-700' : 'bg-gray-50 text-gray-400'"
          >
            {{ slotProps.data.portesAsignados }}
          </span>
        </template>
      </Column>

      <!-- Acciones -->
      <Column header="Acciones" style="min-width: 140px; text-align: center" :exportable="false">
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
              :icon="slotProps.data.estado === 'ACTIVO' ? 'pi pi-ban' : 'pi pi-check-circle'"
              :severity="slotProps.data.estado === 'ACTIVO' ? 'danger' : 'success'"
              text
              rounded
              size="small"
              v-tooltip.top="slotProps.data.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'"
              @click="emit('toggle-estado', slotProps.data)"
            />
          </div>
        </template>
      </Column>

      <!-- Empty state -->
      <template #empty>
        <div class="text-center py-12">
          <i class="pi pi-users text-4xl text-gray-300 mb-3"></i>
          <p class="text-gray-500">No se encontraron conductores</p>
          <p class="text-gray-400 text-sm mt-1">Intenta ajustar los filtros o crear un nuevo conductor</p>
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
