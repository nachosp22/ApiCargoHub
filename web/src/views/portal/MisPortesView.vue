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
        :value="processedPortes"
        :paginator="processedPortes.length > 10"
        :rows="10"
        stripedRows
        class="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden"
        v-model:expandedRows="expandedRows"
        dataKey="id"
      >
        <Column expander style="width: 3rem" />
        <Column field="id" style="width: 7rem">
          <template #header>
            <div class="flex flex-col gap-2">
              <button type="button" class="text-left font-semibold hover:text-primary-600" @click="toggleSort('id')">
                {{ t('portal.portes.id') }}
                <i :class="sortIcon('id')" class="ml-1"></i>
              </button>
              <input
                v-model.trim="filters.id"
                type="text"
                class="w-full px-2 py-1 text-xs border border-gray-300 rounded-md"
                placeholder="#"
              >
            </div>
          </template>
          <template #body="{ data }">
            <span class="font-mono text-sm text-gray-500">#{{ data.id }}</span>
          </template>
        </Column>
        <Column field="origen">
          <template #header>
            <div class="flex flex-col gap-2">
              <button type="button" class="text-left font-semibold hover:text-primary-600" @click="toggleSort('origen')">
                {{ t('portal.portes.origin') }}
                <i :class="sortIcon('origen')" class="ml-1"></i>
              </button>
              <input
                v-model.trim="filters.origen"
                type="text"
                class="w-full px-2 py-1 text-xs border border-gray-300 rounded-md"
                :placeholder="t('portal.portes.origin')"
              >
            </div>
          </template>
          <template #body="{ data }">
            <span class="font-medium text-gray-800 dark:text-gray-200 break-words">
              {{ formatAddress(data.origen) }}
            </span>
          </template>
        </Column>
        <Column field="destino">
          <template #header>
            <div class="flex flex-col gap-2">
              <button type="button" class="text-left font-semibold hover:text-primary-600" @click="toggleSort('destino')">
                {{ t('portal.portes.destination') }}
                <i :class="sortIcon('destino')" class="ml-1"></i>
              </button>
              <input
                v-model.trim="filters.destino"
                type="text"
                class="w-full px-2 py-1 text-xs border border-gray-300 rounded-md"
                :placeholder="t('portal.portes.destination')"
              >
            </div>
          </template>
          <template #body="{ data }">
            <span class="font-medium text-gray-800 dark:text-gray-200 break-words">
              {{ formatAddress(data.destino) }}
            </span>
          </template>
        </Column>
        <Column field="estado" style="width: 12rem">
          <template #header>
            <div class="flex flex-col gap-2">
              <button type="button" class="text-left font-semibold hover:text-primary-600" @click="toggleSort('estado')">
                {{ t('portal.portes.status') }}
                <i :class="sortIcon('estado')" class="ml-1"></i>
              </button>
              <input
                v-model.trim="filters.estado"
                type="text"
                class="w-full px-2 py-1 text-xs border border-gray-300 rounded-md"
                :placeholder="t('portal.portes.status')"
              >
            </div>
          </template>
          <template #body="{ data }">
            <span
              class="text-xs font-medium px-2.5 py-1 rounded-full"
              :class="estadoBadgeClass(resolvePortalStatusKey(data))"
            >
              {{ resolvePortalStatusLabel(data) }}
            </span>
          </template>
        </Column>
        <Column field="fechaRecogida" style="width: 12rem">
          <template #header>
            <div class="flex flex-col gap-2">
              <button type="button" class="text-left font-semibold hover:text-primary-600" @click="toggleSort('fechaRecogida')">
                {{ t('portal.portes.date') }}
                <i :class="sortIcon('fechaRecogida')" class="ml-1"></i>
              </button>
              <input
                v-model="filters.fecha"
                type="date"
                class="w-full px-2 py-1 text-xs border border-gray-300 rounded-md"
              >
            </div>
          </template>
          <template #body="{ data }">
            <span class="text-sm text-gray-600">{{ formatDate(data.fechaRecogida) }}</span>
          </template>
        </Column>
        <Column field="precio" style="width: 10rem">
          <template #header>
            <button type="button" class="text-left font-semibold hover:text-primary-600" @click="toggleSort('precio')">
              {{ t('portal.portes.price') }}
              <i :class="sortIcon('precio')" class="ml-1"></i>
            </button>
          </template>
          <template #body="{ data }">
            <span class="text-sm font-medium">{{ data.precio ? formatCurrency(data.precio) : '—' }}</span>
          </template>
        </Column>
        <Column header="" style="width: 8rem">
          <template #body="{ data }">
            <Button
              v-if="data.estado === 'EN_TRANSITO'"
              :label="t('portal.portes.tracking')"
              icon="pi pi-map-marker"
              severity="info"
              text
              size="small"
              @click="openTrackingModal(data.id)"
            />
          </template>
        </Column>

        <!-- Expanded row detail -->
        <template #expansion="{ data }">
          <div class="p-4 bg-gray-50 dark:bg-gray-800/50">
            <div class="grid md:grid-cols-3 gap-4 text-sm">
              <div>
                <p class="text-gray-500 dark:text-gray-400 mb-1">{{ t('portal.portes.origin') }}</p>
                <p class="text-gray-900 dark:text-gray-100 font-medium">{{ data.origen }}</p>
              </div>
              <div>
                <p class="text-gray-500 dark:text-gray-400 mb-1">{{ t('portal.portes.destination') }}</p>
                <p class="text-gray-900 dark:text-gray-100 font-medium">{{ data.destino }}</p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">{{ t('portal.portes.description') }}</p>
                <p class="text-gray-900">{{ data.descripcionCliente ?? '—' }}</p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">{{ t('portal.portes.pickup') }}</p>
                <p class="text-gray-900">{{ formatDateTime(data.fechaRecogida) }}</p>
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
                <p class="text-gray-900">{{ hasPositiveValue(data.pesoTotalKg) ? `${data.pesoTotalKg} kg` : '—' }}</p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">Volumen</p>
                <p class="text-gray-900">{{ hasPositiveValue(data.volumenTotalM3) ? `${data.volumenTotalM3} m³` : '—' }}</p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">Dimensiones máximas</p>
                <p class="text-gray-900">{{ formatDimensions(data) || '—' }}</p>
              </div>
              <div>
                <p class="text-gray-500 mb-1">{{ t('portal.portes.requiredVehicle') }}</p>
                <p class="text-gray-900">{{ data.tipoVehiculoRequerido ?? '—' }}</p>
              </div>
            </div>
            <div v-if="data.revisionManual" class="mt-3 p-3 bg-amber-50 border border-amber-200 rounded-lg">
              <p class="text-sm text-amber-700">
                <i class="pi pi-exclamation-triangle mr-1"></i>
                {{ t('portal.portes.manualReview') }}
              </p>
            </div>
            <div v-else-if="resolvePendingConductorMessage(data)" class="mt-3 p-3 bg-sky-50 border border-sky-200 rounded-lg">
              <p class="text-sm text-sky-700">
                <i class="pi pi-info-circle mr-1"></i>
                {{ resolvePendingConductorMessage(data) }}
              </p>
            </div>

          </div>
        </template>
      </DataTable>
    </div>

    <TrackingModal
      v-if="selectedTrackingPorteId != null"
      v-model:visible="trackingModalVisible"
      :porte-id="selectedTrackingPorteId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { usePortesStore } from '@/stores/portes'
import type { Porte } from '@/stores/portes'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import TrackingModal from '@/components/TrackingModal.vue'

const { t } = useI18n()
const authStore = useAuthStore()
const portesStore = usePortesStore()
const expandedRows = ref({})
const trackingModalVisible = ref(false)
const selectedTrackingPorteId = ref<number | null>(null)

function openTrackingModal(porteId: number) {
  selectedTrackingPorteId.value = porteId
  trackingModalVisible.value = true
}
type SortField = 'id' | 'origen' | 'destino' | 'estado' | 'fechaRecogida' | 'precio'

const sortField = ref<SortField>('fechaRecogida')
const sortDirection = ref<'asc' | 'desc'>('asc')
const filters = ref({
  id: '',
  origen: '',
  destino: '',
  estado: '',
  fecha: '',
})

const processedPortes = computed(() => {
  const filtered = portesStore.portes.filter((porte) => {
    const idValue = String(porte.id ?? '')
    const origenValue = normalizeText(porte.origen)
    const destinoValue = normalizeText(porte.destino)
    const estadoValue = normalizeText(resolvePortalStatusLabel(porte))
    const fechaIso = toIsoDate(porte.fechaRecogida)

    return (
      (!filters.value.id || idValue.includes(filters.value.id)) &&
      (!filters.value.origen || origenValue.includes(normalizeText(filters.value.origen))) &&
      (!filters.value.destino || destinoValue.includes(normalizeText(filters.value.destino))) &&
      (!filters.value.estado || estadoValue.includes(normalizeText(filters.value.estado))) &&
      (!filters.value.fecha || fechaIso === filters.value.fecha)
    )
  })

  return [...filtered].sort((a, b) => comparePortes(a, b))
})

onMounted(() => loadData())

async function loadData() {
  const cId = authStore.clienteId
  if (cId) {
    await portesStore.fetchOwn(cId)
  }
}

function formatAddress(addr: string): string {
  if (!addr || addr === '—') return '—'
  return addr
}

function normalizeText(value?: string | null): string {
  return (value ?? '').toLocaleLowerCase('es-ES').trim()
}

function resolvePortalStatusKey(porte: Porte): string {
  if (porte.estado !== 'PENDIENTE') return porte.estado
  return porte.revisionManual ? 'PENDIENTE_REVISION' : 'PENDIENTE_CONDUCTOR'
}

function resolvePortalStatusLabel(porte: Porte): string {
  const key = resolvePortalStatusKey(porte)
  if (key === 'PENDIENTE_REVISION') return t('portal.portes.pendingReview')
  if (key === 'PENDIENTE_CONDUCTOR') return t('portal.portes.pendingDriver')
  return porte.estado.replace('_', ' ')
}

function resolvePendingConductorMessage(porte: Porte): string | null {
  if (porte.estado !== 'PENDIENTE' || porte.revisionManual) return null
  return t('portal.portes.pendingDriverAwaitingAcceptance')
}

function toIsoDate(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return ''
  return date.toISOString().slice(0, 10)
}

function toggleSort(field: SortField) {
  if (sortField.value === field) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
    return
  }
  sortField.value = field
  sortDirection.value = 'asc'
}

function sortIcon(field: SortField): string {
  if (sortField.value !== field) return 'pi pi-sort-alt text-gray-400'
  return sortDirection.value === 'asc' ? 'pi pi-sort-amount-up-alt text-primary-600' : 'pi pi-sort-amount-down text-primary-600'
}

function comparePortes(a: Porte, b: Porte): number {
  const direction = sortDirection.value === 'asc' ? 1 : -1
  const field = sortField.value

  if (field === 'id') return compareNumbers(a.id, b.id) * direction
  if (field === 'fechaRecogida') return compareDates(a.fechaRecogida, b.fechaRecogida, sortDirection.value)
  if (field === 'precio') return compareNumbers(a.precio ?? 0, b.precio ?? 0) * direction
  if (field === 'estado') {
    const aStatus = normalizeText(resolvePortalStatusLabel(a))
    const bStatus = normalizeText(resolvePortalStatusLabel(b))
    return aStatus.localeCompare(bStatus, 'es') * direction
  }

  const aText = normalizeText(String(a[field] ?? ''))
  const bText = normalizeText(String(b[field] ?? ''))
  return aText.localeCompare(bText, 'es') * direction
}

function compareNumbers(a: number, b: number): number {
  return a - b
}

function compareDates(a?: string, b?: string, direction: 'asc' | 'desc' = 'asc'): number {
  const aTime = a ? new Date(a).getTime() : Number.NaN
  const bTime = b ? new Date(b).getTime() : Number.NaN
  const aMissing = Number.isNaN(aTime)
  const bMissing = Number.isNaN(bTime)

  if (aMissing && bMissing) return 0
  if (aMissing) return 1
  if (bMissing) return -1

  const diff = aTime - bTime
  return direction === 'desc' ? -diff : diff
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(amount)
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '—'
  return new Intl.DateTimeFormat('es-ES', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(dateStr))
}

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '—'
  return new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(dateStr))
}

function hasPositiveValue(value?: number): boolean {
  return value != null && value > 0
}

function formatDimensions(porte: {
  largoMaxPaquete?: number
  anchoMaxPaquete?: number
  altoMaxPaquete?: number
}): string {
  const parts: string[] = []
  if (hasPositiveValue(porte.largoMaxPaquete)) parts.push(`L ${porte.largoMaxPaquete} m`)
  if (hasPositiveValue(porte.anchoMaxPaquete)) parts.push(`A ${porte.anchoMaxPaquete} m`)
  if (hasPositiveValue(porte.altoMaxPaquete)) parts.push(`H ${porte.altoMaxPaquete} m`)
  return parts.join(' × ')
}

function estadoBadgeClass(estado: string): string {
  const map: Record<string, string> = {
    PENDIENTE: 'bg-yellow-100 text-yellow-700',
    PENDIENTE_REVISION: 'bg-amber-100 text-amber-700',
    PENDIENTE_CONDUCTOR: 'bg-sky-100 text-sky-700',
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
