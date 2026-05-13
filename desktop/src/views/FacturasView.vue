<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useFacturasStore } from '@/stores/facturas'
import type { Factura } from '@/stores/facturas'
import FacturaTable from '@/components/facturas/FacturaTable.vue'
import FacturaDetail from '@/components/facturas/FacturaDetail.vue'

const facturasStore = useFacturasStore()
const route = useRoute()
const router = useRouter()

// --- Detail dialog state ---
const showDetail = ref(false)
const detailFactura = ref<Factura | null>(null)

// --- Lifecycle ---
onMounted(async () => {
  await facturasStore.fetchFacturas()
  await openFacturaFromQuery()
})

watch(() => route.query.facturaId, () => {
  void openFacturaFromQuery()
})

// --- Handlers ---
function onViewFactura(factura: Factura): void {
  detailFactura.value = factura
  showDetail.value = true
}

async function openFacturaFromQuery(): Promise<void> {
  const rawId = route.query.facturaId
  const facturaId = typeof rawId === 'string' ? Number.parseInt(rawId, 10) : NaN
  if (Number.isNaN(facturaId)) return

  const factura = await facturasStore.fetchFactura(facturaId)
  if (factura) {
    onViewFactura(factura)
    await clearQueryParam('facturaId')
  }
}

async function clearQueryParam(param: string): Promise<void> {
  const nextQuery = { ...route.query }
  delete nextQuery[param]
  await router.replace({ query: nextQuery })
}

</script>

<template>
  <div class="h-full min-h-0 flex flex-col gap-6 overflow-hidden">
    <!-- Page Header -->
    <div class="shrink-0 flex items-center gap-4">
      <div class="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
        <i class="pi pi-file text-2xl"></i>
      </div>
      <div>
        <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Facturas</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Gestión de facturación</p>
      </div>
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="facturasStore.usingMockData"
      class="shrink-0 flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Data Table -->
    <div class="flex-1 min-h-0 overflow-auto">
      <FacturaTable
        :facturas="facturasStore.facturas"
        :loading="facturasStore.loading"
        @view="onViewFactura"
      />
    </div>

    <!-- Detail Dialog -->
    <FacturaDetail
      v-model:visible="showDetail"
      :factura="detailFactura"
    />
  </div>
</template>
