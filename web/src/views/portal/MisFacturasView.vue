<template>
  <div>
    <!-- Filters -->
    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 mb-6">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('portal.facturas.filterStatus') }}</label>
          <Select
            v-model="filterEstado"
            :options="estadoOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('portal.facturas.filterAll')"
            class="w-40"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('portal.facturas.filterFrom') }}</label>
          <DatePicker
            v-model="filterDesde"
            dateFormat="dd/mm/yy"
            :placeholder="t('portal.facturas.filterDateStart')"
            showIcon
            class="w-44"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('portal.facturas.filterTo') }}</label>
          <DatePicker
            v-model="filterHasta"
            dateFormat="dd/mm/yy"
            :placeholder="t('portal.facturas.filterDateEnd')"
            showIcon
            class="w-44"
          />
        </div>
        <Button
          v-if="hasActiveFilters"
          :label="t('portal.facturas.clearFilters')"
          icon="pi pi-filter-slash"
          severity="secondary"
          text
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
              text
              rounded
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
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'

const { t } = useI18n()
const facturasStore = useFacturasStore()

const filterEstado = ref<string | null>(null)
const filterDesde = ref<Date | null>(null)
const filterHasta = ref<Date | null>(null)

const estadoOptions = computed(() => [
  { label: t('portal.facturas.filterAll'), value: null },
  { label: t('portal.facturas.filterPaid'), value: 'pagada' },
  { label: t('portal.facturas.filterPending'), value: 'pendiente' },
])

const hasActiveFilters = computed(() =>
  filterEstado.value !== null || filterDesde.value !== null || filterHasta.value !== null
)

const filteredFacturas = computed(() => {
  let result = [...facturasStore.facturas]

  if (filterEstado.value === 'pagada') {
    result = result.filter((f) => f.pagada)
  } else if (filterEstado.value === 'pendiente') {
    result = result.filter((f) => !f.pagada)
  }

  if (filterDesde.value) {
    const desde = filterDesde.value.getTime()
    result = result.filter((f) => new Date(f.fechaEmision).getTime() >= desde)
  }

  if (filterHasta.value) {
    const hasta = filterHasta.value.getTime() + 86400000
    result = result.filter((f) => new Date(f.fechaEmision).getTime() <= hasta)
  }

  return result
})

function clearFilters() {
  filterEstado.value = null
  filterDesde.value = null
  filterHasta.value = null
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
