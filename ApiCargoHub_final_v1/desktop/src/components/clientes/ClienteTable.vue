<script setup lang="ts">
import { ref, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import type { Cliente } from '@/stores/clientes'

interface Props {
  clientes: Cliente[]
  loading?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'view', cliente: Cliente): void
  (e: 'edit', cliente: Cliente): void
  (e: 'view-portes', cliente: Cliente): void
}>()

// --- Filters ---
const globalFilter = ref('')

const filteredClientes = computed(() => {
  let result = props.clientes

  if (globalFilter.value) {
    const query = globalFilter.value.toLowerCase()
    result = result.filter(
      (c) =>
        String(c.id).includes(query) ||
        c.nombreEmpresa.toLowerCase().includes(query) ||
        c.cif.toLowerCase().includes(query) ||
        c.emailContacto.toLowerCase().includes(query) ||
        c.telefono.toLowerCase().includes(query) ||
        (c.direccion ?? '').toLowerCase().includes(query) ||
        c.ciudad.toLowerCase().includes(query)
    )
  }

  return result
})

// --- Helpers ---

function getInitials(cliente: Cliente): string {
  const parts = cliente.nombreEmpresa.split(' ')
  return parts
    .slice(0, 2)
    .map((s) => s.charAt(0).toUpperCase())
    .join('')
}
</script>

<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
    <!-- Table Header with Filters -->
    <div class="p-5 border-b border-gray-100 dark:border-gray-700">
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100">Lista de Clientes</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
            {{ filteredClientes.length }} clientes encontrados
          </p>
        </div>

        <div class="flex items-center gap-3">
          <!-- Global Search -->
          <div class="relative">
            <i class="pi pi-search absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm"></i>
            <InputText
              v-model="globalFilter"
              placeholder="Buscar clientes..."
              class="pl-4 pr-9 py-2 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary w-64"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- DataTable -->
    <DataTable
      :value="filteredClientes"
      :loading="loading"
      :paginator="true"
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20, 50]"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
      stripedRows
      class="clientes-table"
      responsiveLayout="scroll"
      :rowHover="true"
    >
      <!-- Empresa -->
      <Column header="Empresa" :sortable="true" field="nombreEmpresa" style="min-width: 220px">
        <template #body="slotProps">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 bg-teal-50">
              <span class="text-xs font-semibold text-teal-600">
                {{ getInitials(slotProps.data) }}
              </span>
            </div>
            <div>
              <p class="text-gray-800 dark:text-gray-100 font-medium text-sm">{{ slotProps.data.nombreEmpresa }}</p>
              <p class="text-gray-400 dark:text-gray-500 text-xs">{{ slotProps.data.ciudad || '—' }}</p>
            </div>
          </div>
        </template>
      </Column>

      <!-- CIF/NIF -->
      <Column field="cif" header="CIF/NIF" :sortable="true" style="min-width: 120px">
        <template #body="slotProps">
          <span class="text-gray-600 dark:text-gray-400 text-sm font-mono">{{ slotProps.data.cif || '—' }}</span>
        </template>
      </Column>

      <!-- Email -->
      <Column field="emailContacto" header="Email" :sortable="true" style="min-width: 200px">
        <template #body="slotProps">
          <div class="flex items-center gap-2">
            <i class="pi pi-envelope text-xs text-gray-400"></i>
            <span class="text-gray-700 dark:text-gray-300 text-sm">{{ slotProps.data.emailContacto || '—' }}</span>
          </div>
        </template>
      </Column>

      <!-- Teléfono -->
      <Column field="telefono" header="Teléfono" style="min-width: 130px">
        <template #body="slotProps">
          <span class="text-gray-600 dark:text-gray-400 text-sm">{{ slotProps.data.telefono || '—' }}</span>
        </template>
      </Column>

      <!-- Dirección -->
      <Column field="direccion" header="Dirección" style="min-width: 200px">
        <template #body="slotProps">
          <span class="text-gray-600 dark:text-gray-400 text-sm">{{ slotProps.data.direccion || '—' }}</span>
        </template>
      </Column>

      <!-- Acciones -->
      <Column header="Acciones" style="min-width: 170px; text-align: center" :exportable="false">
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
              icon="pi pi-truck"
              severity="secondary"
              text
              rounded
              size="small"
              v-tooltip.top="'Ver portes'"
              @click="emit('view-portes', slotProps.data)"
            />
          </div>
        </template>
      </Column>

      <!-- Empty state -->
      <template #empty>
        <div class="text-center py-12">
          <i class="pi pi-building text-4xl text-gray-300 dark:text-gray-600 mb-3"></i>
          <p class="text-gray-500 dark:text-gray-400">No se encontraron clientes</p>
          <p class="text-gray-400 dark:text-gray-500 text-sm mt-1">Intenta ajustar los filtros o crear un nuevo cliente</p>
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
