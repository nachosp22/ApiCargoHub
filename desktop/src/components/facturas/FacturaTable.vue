<script setup lang="ts">
import { ref, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import DatePicker from 'primevue/datepicker'
import type { Factura } from '@/stores/facturas'

interface Props {
  facturas: Factura[]
  loading?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'view', factura: Factura): void
}>()

// --- Filters ---
const globalFilter = ref('')
const estadoFilter = ref<string>('')
const fechaDesde = ref<Date | null>(null)
const fechaHasta = ref<Date | null>(null)

const estadoFilterOptions = [
  { label: 'Todas', value: '' },
  { label: 'Pagadas', value: 'pagada' },
  { label: 'Pendientes', value: 'pendiente' },
]

const filteredFacturas = computed(() => {
  let result = props.facturas

  // Estado filter
  if (estadoFilter.value === 'pagada') {
    result = result.filter((f) => f.pagada)
  } else if (estadoFilter.value === 'pendiente') {
    result = result.filter((f) => !f.pagada)
  }

  // Date range filter
  if (fechaDesde.value) {
    const desde = new Date(fechaDesde.value)
    desde.setHours(0, 0, 0, 0)
    result = result.filter((f) => new Date(f.fechaEmision) >= desde)
  }
  if (fechaHasta.value) {
    const hasta = new Date(fechaHasta.value)
    hasta.setHours(23, 59, 59, 999)
    result = result.filter((f) => new Date(f.fechaEmision) <= hasta)
  }

  // Global text search
  if (globalFilter.value) {
    const query = globalFilter.value.toLowerCase()
    result = result.filter(
      (f) =>
        f.numeroSerie.toLowerCase().includes(query) ||
        String(f.id).includes(query) ||
        (f.porte?.cliente?.nombreEmpresa ?? '').toLowerCase().includes(query) ||
        (f.porte?.origen ?? '').toLowerCase().includes(query) ||
        (f.porte?.destino ?? '').toLowerCase().includes(query),
    )
  }

  return result
})

function clearFilters(): void {
  globalFilter.value = ''
  estadoFilter.value = ''
  fechaDesde.value = null
  fechaHasta.value = null
}

const hasActiveFilters = computed(() =>
  globalFilter.value || estadoFilter.value || fechaDesde.value || fechaHasta.value,
)

// --- Row click ---
function onRowClick(event: { data: Factura }): void {
  emit('view', event.data)
}

// --- Helpers ---
function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  try {
    return new Date(dateStr).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  } catch {
    return dateStr
  }
}

function formatCurrency(value: number): string {
  return value.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' })
}

function getClienteName(factura: Factura): string {
  return factura.porte?.cliente?.nombreEmpresa ?? '—'
}
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
    <!-- Table Header with Filters -->
    <div class="p-5 border-b border-gray-100 dark:border-gray-700">
      <div class="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100">Lista de Facturas</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
            {{ filteredFacturas.length }} facturas encontradas
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <!-- Estado Filter -->
          <Select
            v-model="estadoFilter"
            :options="estadoFilterOptions"
            optionLabel="label"
            optionValue="value"
            class="w-40"
          />

          <!-- Fecha Desde -->
          <DatePicker
            v-model="fechaDesde"
            placeholder="Desde"
            dateFormat="dd/mm/yy"
            showIcon
            class="w-40"
          />

          <!-- Fecha Hasta -->
          <DatePicker
            v-model="fechaHasta"
            placeholder="Hasta"
            dateFormat="dd/mm/yy"
            showIcon
            class="w-40"
          />

          <!-- Clear filters -->
          <Button
            v-if="hasActiveFilters"
            icon="pi pi-filter-slash"
            severity="secondary"
            text
            rounded
            size="small"
            v-tooltip.top="'Limpiar filtros'"
            @click="clearFilters"
          />

          <!-- Global Search -->
          <div class="relative">
            <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm"></i>
            <InputText
              v-model="globalFilter"
              placeholder="Buscar facturas..."
              class="pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary w-64"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- DataTable -->
    <DataTable
      :value="filteredFacturas"
      :loading="loading"
      :paginator="true"
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20, 50]"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
      stripedRows
      class="facturas-table"
      responsiveLayout="scroll"
      :rowHover="true"
      @row-click="onRowClick"
      :pt="{ bodyRow: { style: 'cursor: pointer' } }"
    >
      <!-- Nº Serie -->
      <Column field="numeroSerie" header="Nº Serie" :sortable="true" style="min-width: 150px">
        <template #body="slotProps">
          <span class="font-semibold text-gray-800 dark:text-gray-100">{{ slotProps.data.numeroSerie }}</span>
        </template>
      </Column>

      <!-- Fecha Emisión -->
      <Column field="fechaEmision" header="Fecha Emisión" :sortable="true" style="min-width: 130px">
        <template #body="slotProps">
          <span class="text-gray-600 dark:text-gray-400 text-sm">{{ formatDate(slotProps.data.fechaEmision) }}</span>
        </template>
      </Column>

      <!-- Cliente -->
      <Column header="Cliente" :sortable="false" style="min-width: 180px">
        <template #body="slotProps">
          <span class="text-gray-700 dark:text-gray-300">{{ getClienteName(slotProps.data) }}</span>
        </template>
      </Column>

      <!-- Base Imponible -->
      <Column field="baseImponible" header="Base Imponible" :sortable="true" style="min-width: 130px">
        <template #body="slotProps">
          <span class="text-gray-600 dark:text-gray-400 text-sm">{{ formatCurrency(slotProps.data.baseImponible) }}</span>
        </template>
      </Column>

      <!-- IVA -->
      <Column field="iva" header="IVA" :sortable="true" style="min-width: 100px">
        <template #body="slotProps">
          <span class="text-gray-600 dark:text-gray-400 text-sm">{{ formatCurrency(slotProps.data.iva) }}</span>
        </template>
      </Column>

      <!-- Total -->
      <Column field="importeTotal" header="Total" :sortable="true" style="min-width: 120px">
        <template #body="slotProps">
          <span class="font-semibold text-gray-800 dark:text-gray-100">{{ formatCurrency(slotProps.data.importeTotal) }}</span>
        </template>
      </Column>

      <!-- Estado -->
      <Column field="pagada" header="Estado" :sortable="true" style="min-width: 120px">
        <template #body="slotProps">
          <Tag
            :value="slotProps.data.pagada ? 'Pagada' : 'Pendiente'"
            :severity="slotProps.data.pagada ? 'success' : 'warn'"
          />
        </template>
      </Column>

      <!-- Acciones -->
      <Column header="Acciones" style="min-width: 80px; text-align: center" :exportable="false">
        <template #body="slotProps">
          <div class="flex items-center justify-center" @click.stop>
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
          <i class="pi pi-file text-4xl text-gray-300 dark:text-gray-600 mb-3"></i>
          <p class="text-gray-500 dark:text-gray-400">No se encontraron facturas</p>
          <p class="text-gray-400 dark:text-gray-500 text-sm mt-1">Intenta ajustar los filtros de búsqueda</p>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<style scoped>
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

/* Dark mode overrides */
.dark :deep(.p-datatable-thead > tr > th) {
  background: #1F2937;
  color: #9CA3AF;
  border-color: #374151;
}
.dark :deep(.p-datatable-tbody > tr > td) {
  border-color: #374151;
  color: #D1D5DB;
}
.dark :deep(.p-datatable-tbody > tr) {
  background: #1F2937;
}
.dark :deep(.p-datatable-tbody > tr:nth-child(even)) {
  background: #111827;
}
.dark :deep(.p-datatable-tbody > tr:hover) {
  background-color: #374151 !important;
}
.dark :deep(.p-paginator) {
  background: #1F2937;
  color: #9CA3AF;
  border-color: #374151;
}
</style>
