<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { usePortesStore } from '@/stores/portes'
import { useToast } from 'primevue/usetoast'
import type { Porte } from '@/stores/portes'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PorteStatusBadge from '@/components/portes/PorteStatusBadge.vue'
import RevisionPorteDialog from '@/components/portes/RevisionPorteDialog.vue'

const portesStore = usePortesStore()
const toast = useToast()

const showReviewDialog = ref(false)
const reviewingPorte = ref<Porte | null>(null)

onMounted(async () => {
  await portesStore.fetchPendientesRevision()
})

function onReviewPorte(porte: Porte): void {
  reviewingPorte.value = porte
  showReviewDialog.value = true
}

function onRowClick(event: { data: Porte }): void {
  onReviewPorte(event.data)
}

async function onAssigned(): Promise<void> {
  toast.add({
    severity: 'success',
    summary: 'Porte asignado correctamente',
    detail: 'El porte ha sido retirado de la lista de revisión.',
    life: 3000,
  })
  await portesStore.fetchPendientesRevision()
}

function formatDateTime(dateStr: string | undefined): string {
  if (!dateStr) return '—'
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateStr
  }
}

// Suppress lint
void toast
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
          <i class="pi pi-eye text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800">Revisión de Portes</h1>
          <p class="text-sm text-gray-500 mt-0.5">
            Portes pendientes de revisión manual — ajusta dimensiones y asigna conductores
          </p>
        </div>
      </div>
      <Button
        label="Actualizar"
        icon="pi pi-refresh"
        severity="secondary"
        :loading="portesStore.loading"
        @click="portesStore.fetchPendientesRevision()"
      />
    </div>

    <!-- Info Banner -->
    <div
      v-if="!portesStore.loading && portesStore.pendientesRevision.length === 0"
      class="flex items-center gap-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-check-circle text-emerald-500"></i>
      <span>No hay portes pendientes de revisión. ¡Todo al día!</span>
    </div>

    <!-- Pending Review Table -->
    <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
      <DataTable
        :value="portesStore.pendientesRevision"
        :loading="portesStore.loading"
        stripedRows
        :rows="20"
        :paginator="portesStore.pendientesRevision.length > 20"
        :rowsPerPageOptions="[10, 20, 50]"
        dataKey="id"
        emptyMessage="No hay portes pendientes de revisión."
        :rowHover="true"
        @row-click="onRowClick"
        class="cursor-pointer"
      >
        <Column field="id" header="ID" :sortable="true" style="width: 80px">
          <template #body="{ data }">
            <span class="font-mono font-bold text-gray-800">#{{ data.id }}</span>
          </template>
        </Column>
        <Column field="estado" header="Estado" :sortable="true" style="width: 130px">
          <template #body="{ data }">
            <PorteStatusBadge :estado="data.estado" />
          </template>
        </Column>
        <Column header="Ruta" :sortable="false">
          <template #body="{ data }">
            <div class="flex items-center gap-2">
              <span class="text-gray-800">{{ data.origen }}</span>
              <i class="pi pi-arrow-right text-gray-300 text-xs"></i>
              <span class="text-gray-800">{{ data.destino }}</span>
            </div>
          </template>
        </Column>
        <Column header="Cliente" :sortable="false">
          <template #body="{ data }">
            <span class="text-gray-700">{{ data.cliente?.nombreEmpresa ?? '—' }}</span>
          </template>
        </Column>
        <Column header="Motivo" :sortable="false">
          <template #body="{ data }">
            <span class="text-sm text-amber-700 bg-amber-50 px-2 py-0.5 rounded">
              {{ data.motivoRevision ?? 'Revisión pendiente' }}
            </span>
          </template>
        </Column>
        <Column field="fechaCreacion" header="Fecha" :sortable="true" style="width: 160px">
          <template #body="{ data }">
            <span class="text-sm text-gray-600">{{ formatDateTime(data.fechaCreacion) }}</span>
          </template>
        </Column>
        <Column header="" style="width: 100px">
          <template #body="{ data }">
            <Button
              label="Revisar"
              icon="pi pi-eye"
              size="small"
              severity="warning"
              @click.stop="onReviewPorte(data)"
            />
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Review Dialog -->
    <RevisionPorteDialog
      v-model:visible="showReviewDialog"
      :porte="reviewingPorte"
      @assigned="onAssigned"
    />
  </div>
</template>
