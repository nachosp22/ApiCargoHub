<script setup lang="ts">
import { onMounted, ref } from 'vue'
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

// --- Dialog state ---
const showCreateDialog = ref(false)
const showResolverDialog = ref(false)
const showDetail = ref(false)
const detailIncidencia = ref<Incidencia | null>(null)
const resolvingIncidencia = ref<Incidencia | null>(null)

// --- Overdue toggle ---
const showingVencidas = ref(false)

// --- Lifecycle ---
onMounted(async () => {
  await Promise.all([incidenciasStore.fetchIncidencias(), incidenciasStore.fetchPorteOptions()])
})

// --- Handlers ---

function onNewIncidencia(): void {
  showCreateDialog.value = true
}

async function onToggleVencidas(): Promise<void> {
  showingVencidas.value = !showingVencidas.value
  if (showingVencidas.value) {
    await incidenciasStore.fetchVencidas()
  } else {
    await incidenciasStore.fetchIncidencias()
  }
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
      detail: 'No se pudo resolver la incidencia. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
          <i class="pi pi-exclamation-triangle text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800">Incidencias</h1>
          <p class="text-sm text-gray-500 mt-0.5">Gestión de incidencias y seguimiento SLA</p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <Button
          :label="showingVencidas ? 'Ver Todas' : 'Ver Vencidas'"
          icon="pi pi-clock"
          :severity="showingVencidas ? 'primary' : 'secondary'"
          :outlined="!showingVencidas"
          @click="onToggleVencidas"
        />
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
      class="flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Overdue Banner -->
    <div
      v-if="showingVencidas"
      class="flex items-center gap-3 bg-red-50 border border-red-200 text-red-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-clock text-red-500"></i>
      <span>Mostrando solo incidencias con SLA vencido.</span>
    </div>

    <!-- Data Table -->
    <IncidenciaTable
      :incidencias="incidenciasStore.incidencias"
      :loading="incidenciasStore.loading"
      @view="onViewIncidencia"
      @resolve="onResolveIncidencia"
    />

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
