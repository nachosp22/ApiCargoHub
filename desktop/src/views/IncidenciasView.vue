<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIncidenciasStore } from '@/stores/incidencias'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import IncidenciaTable from '@/components/incidencias/IncidenciaTable.vue'
import IncidenciaDialog from '@/components/incidencias/IncidenciaDialog.vue'
import ResolverDialog from '@/components/incidencias/ResolverDialog.vue'
import IncidenciaDetail from '@/components/incidencias/IncidenciaDetail.vue'
import type { Incidencia, CrearIncidenciaRequest, ResolverIncidenciaRequest } from '@/stores/incidencias'

const incidenciasStore = useIncidenciasStore()
const toast = useToast()
const route = useRoute()
const router = useRouter()

// --- Dialog state ---
const showCreateDialog = ref(false)
const showResolverDialog = ref(false)
const showDetail = ref(false)
const detailIncidencia = ref<Incidencia | null>(null)
const resolvingIncidencia = ref<Incidencia | null>(null)


// --- Lifecycle ---
onMounted(async () => {
  await Promise.all([incidenciasStore.fetchIncidencias(), incidenciasStore.fetchPorteOptions()])
  await openIncidenciaFromQuery()
})

watch(() => route.query.incidenciaId, () => {
  void openIncidenciaFromQuery()
})

// --- Handlers ---

function onNewIncidencia(): void {
  showCreateDialog.value = true
}

function onViewIncidencia(incidencia: Incidencia): void {
  detailIncidencia.value = incidencia
  showDetail.value = true
  incidenciasStore.fetchHistorial(incidencia.id)
}

function onResolveIncidencia(incidencia: Incidencia): void {
  resolvingIncidencia.value = incidencia
  showResolverDialog.value = true
}

function onResolveFromDetail(incidencia: Incidencia): void {
  showDetail.value = false
  resolvingIncidencia.value = incidencia
  showResolverDialog.value = true
}

async function onSaveIncidencia(porteId: number, data: CrearIncidenciaRequest): Promise<void> {
  try {
    const created = await incidenciasStore.crearIncidencia(porteId, data)
    toast.add({
      severity: 'success',
      summary: 'Incidencia creada',
      detail: `La incidencia #${created.id} se ha creado correctamente.`,
      life: 3000,
    })
    showCreateDialog.value = false
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo crear la incidencia. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}

async function onResolverIncidencia(id: number, data: ResolverIncidenciaRequest): Promise<void> {
  try {
    await incidenciasStore.resolverIncidencia(id, data)
    toast.add({
      severity: 'success',
      summary: 'Incidencia actualizada',
      detail: `La incidencia #${id} se ha actualizado correctamente.`,
      life: 3000,
    })
    showResolverDialog.value = false
    resolvingIncidencia.value = null
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo guardar la edición de la incidencia. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}

async function openIncidenciaFromQuery(): Promise<void> {
  const rawId = route.query.incidenciaId
  const incidenciaId = typeof rawId === 'string' ? Number.parseInt(rawId, 10) : NaN
  if (Number.isNaN(incidenciaId)) return

  const incidencia = await incidenciasStore.fetchIncidenciaById(incidenciaId)
  if (incidencia) {
    onViewIncidencia(incidencia)
    await clearQueryParam('incidenciaId')
  }
}

async function clearQueryParam(param: string): Promise<void> {
  const nextQuery = { ...route.query }
  delete nextQuery[param]
  await router.replace({ query: nextQuery })
}

async function onOpenPorteFromIncidencia(porteId: number): Promise<void> {
  await router.push({ path: '/portes', query: { porteId: String(porteId) } })
}
</script>

<template>
  <div class="h-full min-h-0 flex flex-col gap-6 overflow-hidden">
    <!-- Page Header -->
    <div class="shrink-0 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-amber-50 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400 flex items-center justify-center">
          <i class="pi pi-exclamation-triangle text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Incidencias</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Gestión de incidencias</p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <Button
          label="Nueva Incidencia"
          icon="pi pi-plus"
          @click="onNewIncidencia"
        />
      </div>
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="incidenciasStore.usingMockData"
      class="shrink-0 flex items-center gap-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800/40 text-amber-800 dark:text-amber-300 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500 dark:text-amber-400"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Data Table -->
    <div class="flex-1 min-h-0 overflow-auto">
      <IncidenciaTable
        :incidencias="incidenciasStore.incidencias"
        :loading="incidenciasStore.loading"
        @view="onViewIncidencia"
        @resolve="onResolveIncidencia"
        @open-porte="onOpenPorteFromIncidencia"
      />
    </div>

    <!-- Create Incidencia Dialog -->
    <IncidenciaDialog
      v-model:visible="showCreateDialog"
      :porte-options="incidenciasStore.porteOptions"
      :saving="incidenciasStore.saving"
      @save="onSaveIncidencia"
    />

    <!-- Resolve Incidencia Dialog -->
    <ResolverDialog
      v-model:visible="showResolverDialog"
      :incidencia="resolvingIncidencia"
      :saving="incidenciasStore.saving"
      @resolve="onResolverIncidencia"
    />

    <!-- Detail Panel -->
    <IncidenciaDetail
      v-model:visible="showDetail"
      :incidencia="detailIncidencia"
      :historial="incidenciasStore.historial"
      :loading-historial="incidenciasStore.loadingHistorial"
      @resolve="onResolveFromDetail"
    />
  </div>
</template>
