<template>
  <div>
    <!-- Filters -->
    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 mb-4">
      <div class="flex flex-wrap items-end gap-4">
        <div class="flex-1 min-w-[200px]">
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">Buscar</label>
          <input
            v-model.trim="filters.search"
            type="text"
            class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-900 text-gray-900 dark:text-white h-9"
            placeholder="Número de serie, ruta, importe..."
          >
        </div>
        <div class="flex gap-2">
          <div>
            <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">Desde</label>
            <input
              v-model="filters.fechaDesde"
              type="date"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-900 text-gray-900 dark:text-white h-9"
            >
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">Hasta</label>
            <input
              v-model="filters.fechaHasta"
              type="date"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-900 text-gray-900 dark:text-white h-9"
            >
          </div>
        </div>
        <div class="w-44">
          <label class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">Estado</label>
          <select
            v-model="filters.estado"
            class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-900 text-gray-900 dark:text-white h-9"
          >
            <option value="">Todos</option>
            <option value="pagada">Pagada</option>
            <option value="pendiente">Pendiente</option>
          </select>
        </div>
        <Button
          v-if="hasActiveFilters"
          icon="pi pi-filter-slash"
          severity="secondary"
          size="small"
          class="h-9 mt-auto"
          @click="clearFilters"
        />
      </div>
    </div>

    <!-- Loading -->
    <div v-if="facturasStore.loading" class="flex justify-center py-12">
      <i class="pi pi-spin pi-spinner text-3xl text-primary-500"></i>
    </div>

    <!-- Error -->
    <div v-else-if="facturasStore.error" class="text-center py-12">
      <i class="pi pi-exclamation-triangle text-4xl text-amber-400 mb-4"></i>
      <p class="text-gray-600">{{ facturasStore.error }}</p>
      <Button :label="t('portal.facturas.retry')" icon="pi pi-refresh" severity="secondary" class="mt-4" @click="loadData" />
    </div>

    <!-- Empty -->
    <div v-else-if="filteredFacturas.length === 0" class="text-center py-12">
      <i class="pi pi-file text-4xl text-gray-300 mb-4"></i>
      <h3 class="text-lg font-semibold text-gray-700">{{ t('portal.facturas.noInvoicesTitle') }}</h3>
      <p class="text-gray-400 mt-1">
        {{ hasActiveFilters ? t('portal.facturas.noInvoicesFiltered') : t('portal.facturas.noInvoicesYet') }}
      </p>
    </div>

    <!-- Table -->
    <div v-else>
      <!-- Summary bar -->
      <div class="grid md:grid-cols-3 gap-4 mb-4">
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 px-4 py-3 flex items-center justify-between">
          <span class="text-sm text-gray-500 dark:text-gray-400">{{ t('portal.facturas.totalInvoiced') }}</span>
          <span class="font-semibold text-gray-900 dark:text-white">{{ formatCurrency(facturasStore.totalFacturado) }}</span>
        </div>
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 px-4 py-3 flex items-center justify-between">
          <span class="text-sm text-gray-500 dark:text-gray-400">{{ t('portal.facturas.pendingPayment') }}</span>
          <span class="font-semibold text-amber-600 dark:text-amber-400">{{ formatCurrency(facturasStore.totalPendiente) }}</span>
        </div>
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 px-4 py-3 flex items-center justify-between">
          <span class="text-sm text-gray-500 dark:text-gray-400">{{ t('portal.facturas.invoices') }}</span>
          <span class="font-semibold text-gray-900 dark:text-white">{{ filteredFacturas.length }}</span>
        </div>
      </div>

      <DataTable
        :value="filteredFacturas"
        :paginator="filteredFacturas.length > 10"
        :rows="10"
        stripedRows
        class="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden"
      >
        <Column field="numeroSerie" :header="t('portal.facturas.serialNumber')">
          <template #body="{ data }">
            <span class="font-mono text-sm">{{ data.numeroSerie }}</span>
          </template>
        </Column>
        <Column field="porte" :header="t('portal.facturas.route')">
          <template #body="{ data }">
            <span v-if="data.porte" class="text-sm">{{ data.porte.origen }} → {{ data.porte.destino }}</span>
            <span v-else class="text-sm text-gray-400">—</span>
          </template>
        </Column>
        <Column field="importeTotal" :header="t('portal.facturas.amount')" sortable>
          <template #body="{ data }">
            <div class="text-sm">
              <span class="font-medium">{{ formatCurrency(data.importeTotal) }}</span>
              <span class="text-gray-400 ml-1">({{ t('portal.facturas.base') }}: {{ formatCurrency(data.baseImponible) }} + {{ t('portal.facturas.vat') }}: {{ formatCurrency(data.iva) }})</span>
            </div>
          </template>
        </Column>
        <Column field="fechaEmision" :header="t('portal.facturas.issued')" sortable>
          <template #body="{ data }">
            <span class="text-sm text-gray-600">{{ formatDate(data.fechaEmision) }}</span>
          </template>
        </Column>
        <Column field="pagada" :header="t('portal.facturas.status')">
          <template #body="{ data }">
            <span
              class="text-xs font-medium px-2.5 py-1 rounded-full"
              :class="data.pagada ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'"
            >
              {{ data.pagada ? t('portal.facturas.paid') : t('portal.facturas.pending') }}
            </span>
          </template>
        </Column>
        <Column :header="t('portal.facturas.actions')" style="width: 6rem">
          <template #body="{ data }">
            <Button
              icon="pi pi-download"
              severity="secondary"
              size="small"
              v-tooltip.top="t('portal.facturas.downloadPdf')"
              @click="handleDownloadPdf(data.id)"
            />
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFacturasStore } from '@/stores/facturas'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'

const { t } = useI18n()
const facturasStore = useFacturasStore()

const filters = ref({
  search: '',
  fechaDesde: '',
  fechaHasta: '',
  estado: '',
})

const hasActiveFilters = computed(() =>
  filters.value.search !== '' || filters.value.fechaDesde !== '' || filters.value.fechaHasta !== '' || filters.value.estado !== ''
)

function normalizeText(value?: string | null): string {
  return (value ?? '').toLocaleLowerCase('es-ES').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim()
}

const filteredFacturas = computed(() => {
  let result = [...facturasStore.facturas]

  // Global search
  const search = normalizeText(filters.value.search)
  if (search) {
    result = result.filter((f) => {
      const serieMatch = normalizeText(f.numeroSerie).includes(search)
      const rutaMatch = f.porte ? normalizeText(`${f.porte.origen} ${f.porte.destino}`).includes(search) : false
      const importeMatch = String(f.importeTotal).includes(search)
      return serieMatch || rutaMatch || importeMatch
    })
  }

  // Estado filter
  if (filters.value.estado === 'pagada') {
    result = result.filter((f) => f.pagada)
  } else if (filters.value.estado === 'pendiente') {
    result = result.filter((f) => !f.pagada)
  }

  // Fecha desde
  if (filters.value.fechaDesde) {
    const desde = new Date(filters.value.fechaDesde).getTime()
    result = result.filter((f) => new Date(f.fechaEmision).getTime() >= desde)
  }

  // Fecha hasta
  if (filters.value.fechaHasta) {
    const hasta = new Date(filters.value.fechaHasta).getTime() + 86400000
    result = result.filter((f) => new Date(f.fechaEmision).getTime() <= hasta)
  }

  return result
})

function clearFilters() {
  filters.value.search = ''
  filters.value.fechaDesde = ''
  filters.value.fechaHasta = ''
  filters.value.estado = ''
}

onMounted(() => loadData())

async function loadData() {
  await facturasStore.fetchMisFacturas()
}

async function handleDownloadPdf(facturaId: number) {
  try {
    await facturasStore.downloadPdf(facturaId)
  } catch (err) {
    console.error('Error downloading PDF:', err)
  }
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(amount)
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '—'
  return new Intl.DateTimeFormat('es-ES', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(dateStr))
}
</script>
