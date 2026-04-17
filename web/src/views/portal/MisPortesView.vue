<template>
  <div>
    <!-- Loading -->
    <div v-if="portesStore.loading" class="flex justify-center py-12">
      <i class="pi pi-spin pi-spinner text-3xl text-primary-500"></i>
    </div>

    <!-- Error -->
    <div v-else-if="portesStore.error" class="text-center py-12">
      <i class="pi pi-exclamation-triangle text-4xl text-amber-400 mb-4"></i>
      <p class="text-gray-600">{{ portesStore.error }}</p>
      <Button :label="t('portal.portes.retry')" icon="pi pi-refresh" severity="secondary" class="mt-4" @click="loadData" />
    </div>

    <!-- Empty state -->
    <div v-else-if="portesStore.portes.length === 0" class="text-center py-12">
      <i class="pi pi-truck text-4xl text-gray-300 mb-4"></i>
      <h3 class="text-lg font-semibold text-gray-700">{{ t('portal.portes.noPortesTitle') }}</h3>
      <p class="text-gray-400 mt-1">{{ t('portal.portes.noPortesDesc') }}</p>
      <router-link to="/portal/solicitar-porte">
        <Button :label="t('portal.portes.requestPorte')" icon="pi pi-plus" class="mt-4" severity="primary" />
      </router-link>
    </div>

    <!-- Table -->
    <div v-else>
      <DataTable
        :value="portesStore.portes"
        :paginator="portesStore.portes.length > 10"
        :rows="10"
        stripedRows
        class="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden"
        v-model:expandedRows="expandedRows"
        dataKey="id"
      >
        <Column expander style="width: 3rem" />
        <Column field="id" :header="t('portal.portes.id')" style="width: 5rem">
          <template #body="{ data }">
            <span class="font-mono text-sm text-gray-500">#{{ data.id }}</span>
          </template>
        </Column>
        <Column field="origen" :header="t('portal.portes.origin')" />
        <Column field="destino" :header="t('portal.portes.destination')" />
        <Column field="estado" :header="t('portal.portes.status')">
          <template #body="{ data }">
            <span
              class="text-xs font-medium px-2.5 py-1 rounded-full"
              :class="estadoBadgeClass(data.estado)"
            >
              {{ data.estado.replace('_', ' ') }}
            </span>
          </template>
        </Column>
        <Column field="fechaCreacion" :header="t('portal.portes.date')">
          <template #body="{ data }">
            <span class="text-sm text-gray-600">{{ formatDate(data.fechaCreacion) }}</span>
          </template>
        </Column>
        <Column field="precio" :header="t('portal.portes.price')">
          <template #body="{ data }">
            <span class="text-sm font-medium">{{ data.precio ? formatCurrency(data.precio) : '—' }}</span>
          </template>
        </Column>
        <Column header="" style="width: 8rem">
          <template #body="{ data }">
            <router-link
              v-if="data.estado === 'EN_TRANSITO' || data.estado === 'ASIGNADO'"
              :to="`/portal/portes/${data.id}/tracking`"
            >
              <Button :label="t('portal.portes.tracking')" icon="pi pi-map-marker" severity="info" text size="small" />
            </router-link>
          </template>
        </Column>

        <!-- Expanded row detail -->
        <template #expansion="{ data }">
          <div class="p-4 bg-gray-50 dark:bg-gray-800/50">
            <div class="grid md:grid-cols-3 gap-4 text-sm">
              <div>
                <p class="text-gray-500 mb-1">{{ t('portal.portes.description') }}</p>
                <p class="text-gray-900">{{ data.descripcionCliente ?? '—' }}</p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">{{ t('portal.portes.pickup') }}</p>
                <p class="text-gray-900">{{ formatDate(data.fechaRecogida) }}</p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">{{ t('portal.portes.delivery') }}</p>
                <p class="text-gray-900">{{ formatDate(data.fechaEntrega) }}</p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">{{ t('portal.portes.driver') }}</p>
                <p class="text-gray-900">
                  {{ data.conductor ? `${data.conductor.nombre} ${data.conductor.apellidos ?? ''}`.trim() : t('portal.portes.unassigned') }}
                </p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">{{ t('portal.portes.weight') }}</p>
                <p class="text-gray-900">{{ data.pesoTotalKg ? `${data.pesoTotalKg} kg` : '—' }}</p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">{{ t('portal.portes.requiredVehicle') }}</p>
                <p class="text-gray-900">{{ data.tipoVehiculoRequerido ?? '—' }}</p>
              </div>
            </div>
            <div v-if="data.revisionManual" class="mt-3 p-3 bg-amber-50 border border-amber-200 rounded-lg">
              <p class="text-sm text-amber-700">
                <i class="pi pi-exclamation-triangle mr-1"></i>
                {{ t('portal.portes.manualReview', { reason: data.motivoRevision ?? t('portal.portes.manualReviewNoReason') }) }}
              </p>
            </div>
          </div>
        </template>
      </DataTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { usePortesStore } from '@/stores/portes'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'

const { t } = useI18n()
const authStore = useAuthStore()
const portesStore = usePortesStore()
const expandedRows = ref({})

onMounted(() => loadData())

async function loadData() {
  const cId = authStore.clienteId
  if (cId) {
    await portesStore.fetchOwn(cId)
  }
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(amount)
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '—'
  return new Intl.DateTimeFormat('es-ES', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(dateStr))
}

function estadoBadgeClass(estado: string): string {
  const map: Record<string, string> = {
    PENDIENTE: 'bg-yellow-100 text-yellow-700',
    SOLICITUD: 'bg-purple-100 text-purple-700',
    ASIGNADO: 'bg-blue-100 text-blue-700',
    EN_TRANSITO: 'bg-indigo-100 text-indigo-700',
    ENTREGADO: 'bg-green-100 text-green-700',
    CANCELADO: 'bg-red-100 text-red-700',
    FACTURADO: 'bg-gray-100 text-gray-700',
  }
  return map[estado] ?? 'bg-gray-100 text-gray-600'
}
</script>
