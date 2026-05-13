<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { usePortesStore } from '@/stores/portes'
import { useToast } from 'primevue/usetoast'
import type { Porte } from '@/stores/portes'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import PorteStatusBadge from '@/components/portes/PorteStatusBadge.vue'
import RevisionPorteDialog from '@/components/portes/RevisionPorteDialog.vue'

const portesStore = usePortesStore()
const toast = useToast()

const showReviewDialog = ref(false)
const reviewingPorte = ref<Porte | null>(null)
const globalFilter = ref('')

const filteredPendientesRevision = computed(() => {
  const query = globalFilter.value.trim().toLowerCase()
  let result = portesStore.pendientesRevision

  // Mantiene compatibilidad para combinar con filtros actuales/futuros
  if (!query) return result

  result = result.filter((porte) => {
    const conductorNombre = [porte.conductor?.nombre, porte.conductor?.apellidos].filter(Boolean).join(' ').trim()
    const searchableFields = [
      String(porte.id),
      porte.origen,
      porte.destino,
      porte.ciudadOrigen,
      porte.ciudadDestino,
      porte.cliente?.nombreEmpresa,
      porte.motivoRevision,
      porte.descripcionCliente,
      porte.estado,
      porte.tipoVehiculoRequerido,
      conductorNombre,
      porte.fechaRecogida,
      porte.fechaEntrega,
      porte.fechaCreacion,
    ]

    return searchableFields
      .filter((value): value is string => Boolean(value))
      .some((value) => value.toLowerCase().includes(query))
  })

  return result
})

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

function getReviewKind(porte: Porte): string {
  return porte.revisionManual ? 'Revisión manual' : 'Atención operativa'
}

function getReviewKindClass(porte: Porte): string {
  return porte.revisionManual
    ? 'text-amber-700 bg-amber-50'
    : 'text-sky-700 bg-sky-50'
}

// Suppress lint
void toast
</script>

<template>
  <div class="h-full min-h-0 flex flex-col gap-6 overflow-hidden">
    <!-- Page Header -->
    <div class="shrink-0 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
          <i class="pi pi-eye text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Revisión de Portes</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
            Portes con revisión manual o atención operativa pendiente
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
      class="shrink-0 flex items-center gap-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-check-circle text-emerald-500"></i>
        <span>No hay portes pendientes de revisión. ¡Todo al día!</span>
    </div>

    <!-- Pending Review Table -->
    <div class="flex-1 min-h-0 flex flex-col overflow-hidden bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700">
      <div class="shrink-0 p-4 border-b border-gray-100 dark:border-gray-700">
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <p class="text-sm text-gray-500 dark:text-gray-400">
            {{ filteredPendientesRevision.length }} portes encontrados
          </p>
          <div class="relative w-full sm:w-80">
            <i class="pi pi-search absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm"></i>
            <InputText
              v-model="globalFilter"
              placeholder="Buscar por ID, ruta, cliente, motivo..."
              class="w-full pl-4 pr-9 py-2 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>
        </div>
      </div>

      <div class="flex-1 min-h-0 overflow-auto">
        <DataTable
          :value="filteredPendientesRevision"
          :loading="portesStore.loading"
          stripedRows
          :rows="20"
          :paginator="filteredPendientesRevision.length > 20"
          :rowsPerPageOptions="[10, 20, 50]"
          dataKey="id"
          emptyMessage="No hay portes pendientes de revisión."
          :rowHover="true"
          @row-click="onRowClick"
          class="cursor-pointer"
        >
        <Column field="id" header="ID" :sortable="true" style="width: 80px">
          <template #body="{ data }">
            <span class="font-mono font-bold text-gray-800 dark:text-gray-100">#{{ data.id }}</span>
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
              <span class="text-gray-800 dark:text-gray-100">{{ data.origen }}</span>
              <i class="pi pi-arrow-right text-gray-300 dark:text-gray-600 text-xs"></i>
              <span class="text-gray-800 dark:text-gray-100">{{ data.destino }}</span>
            </div>
          </template>
        </Column>
        <Column header="Cliente" :sortable="false">
          <template #body="{ data }">
            <span class="text-gray-700 dark:text-gray-300">{{ data.cliente?.nombreEmpresa ?? '—' }}</span>
          </template>
        </Column>
        <Column header="Motivo" :sortable="false">
          <template #body="{ data }">
            <div class="flex flex-col gap-1">
              <span class="text-xs font-medium px-2 py-0.5 rounded w-fit" :class="getReviewKindClass(data)">
                {{ getReviewKind(data) }}
              </span>
              <span class="text-sm text-gray-700 dark:text-gray-300">
                {{ data.motivoRevision ?? 'Revisión pendiente' }}
              </span>
            </div>
          </template>
        </Column>
        <Column field="fechaCreacion" header="Fecha" :sortable="true" style="width: 160px">
          <template #body="{ data }">
            <span class="text-sm text-gray-600 dark:text-gray-400">{{ formatDateTime(data.fechaCreacion) }}</span>
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
    </div>

    <!-- Review Dialog -->
    <RevisionPorteDialog
      v-model:visible="showReviewDialog"
      :porte="reviewingPorte"
      @assigned="onAssigned"
    />
  </div>
</template>
