<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useFacturasStore } from '@/stores/facturas'
import type { Factura } from '@/stores/facturas'
import FacturaTable from '@/components/facturas/FacturaTable.vue'
import FacturaDetail from '@/components/facturas/FacturaDetail.vue'

const facturasStore = useFacturasStore()

// --- Detail dialog state ---
const showDetail = ref(false)
const detailFactura = ref<Factura | null>(null)

// --- Lifecycle ---
onMounted(async () => {
  await facturasStore.fetchFacturas()
})

// --- Handlers ---
function onViewFactura(factura: Factura): void {
  detailFactura.value = factura
  showDetail.value = true
}

function formatCurrency(value: number): string {
  return value.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' })
}
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex items-center gap-4">
      <div class="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
        <i class="pi pi-file text-2xl"></i>
      </div>
      <div>
        <h1 class="text-2xl font-bold text-gray-800">Facturas</h1>
        <p class="text-sm text-gray-500 mt-0.5">Gestión de facturación</p>
      </div>
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="facturasStore.usingMockData"
      class="flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- KPI Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Total Facturas -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
            <i class="pi pi-file text-lg"></i>
          </div>
          <span class="text-sm font-medium text-gray-500">Total Facturas</span>
        </div>
        <p class="text-2xl font-bold text-gray-800">{{ facturasStore.totalFacturas }}</p>
      </div>

      <!-- Total Facturado -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
            <i class="pi pi-euro text-lg"></i>
          </div>
          <span class="text-sm font-medium text-gray-500">Total Facturado</span>
        </div>
        <p class="text-2xl font-bold text-gray-800">{{ formatCurrency(facturasStore.totalFacturado) }}</p>
      </div>

      <!-- Pagado -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <i class="pi pi-check-circle text-lg"></i>
          </div>
          <span class="text-sm font-medium text-gray-500">Pagado</span>
        </div>
        <p class="text-2xl font-bold text-emerald-600">{{ formatCurrency(facturasStore.totalPagado) }}</p>
        <p class="text-xs text-gray-400 mt-1">{{ facturasStore.facturasPagadas }} facturas</p>
      </div>

      <!-- Pendiente -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
            <i class="pi pi-clock text-lg"></i>
          </div>
          <span class="text-sm font-medium text-gray-500">Pendiente</span>
        </div>
        <p class="text-2xl font-bold text-amber-600">{{ formatCurrency(facturasStore.totalPendiente) }}</p>
        <p class="text-xs text-gray-400 mt-1">{{ facturasStore.facturasPendientes }} facturas</p>
      </div>
    </div>

    <!-- Data Table -->
    <FacturaTable
      :facturas="facturasStore.facturas"
      :loading="facturasStore.loading"
      @view="onViewFactura"
    />

    <!-- Detail Dialog -->
    <FacturaDetail
      v-model:visible="showDetail"
      :factura="detailFactura"
    />
  </div>
</template>
