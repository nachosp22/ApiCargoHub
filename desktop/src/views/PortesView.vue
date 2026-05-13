<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import { usePortesStore } from '@/stores/portes'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import PorteTable from '@/components/portes/PorteTable.vue'
import PorteDialog from '@/components/portes/PorteDialog.vue'
import PorteStatusBadge from '@/components/portes/PorteStatusBadge.vue'
import PorteFotosSection from '@/components/portes/PorteFotosSection.vue'
import type { Porte, CreatePorteRequest, EstadoPorte } from '@/stores/portes'

const portesStore = usePortesStore()
const authStore = useAuthStore()
const toast = useToast()
const route = useRoute()
const router = useRouter()

const selectedClienteId = computed<number | null>(() => {
  const rawId = route.query.clienteId
  const clienteId = typeof rawId === 'string' ? Number.parseInt(rawId, 10) : NaN
  return Number.isNaN(clienteId) ? null : clienteId
})

const visiblePortes = computed(() => {
  if (selectedClienteId.value == null) return portesStore.portes
  return portesStore.portes.filter((porte) => porte.cliente?.id === selectedClienteId.value)
})

// --- Dialog state ---
const showDialog = ref(false)
const editingPorte = ref<Porte | null>(null)

// --- Detail panel state ---
const showDetail = ref(false)
const detailPorte = ref<Porte | null>(null)

// --- Delete confirmation ---
const showDeleteConfirm = ref(false)
const deletingPorte = ref<Porte | null>(null)

// --- Ajuste precio state ---
const showAjusteDialog = ref(false)
const ajustePorte = ref<Porte | null>(null)
const ajusteForm = ref({ precioAjustado: 0, motivo: '' })

// --- Facturar confirmation ---
const showFacturarConfirm = ref(false)
const facturandoPorte = ref<Porte | null>(null)

const portesPendientes = computed(
  () =>
    visiblePortes.value.filter((porte) => porte.estado === 'PENDIENTE' || porte.estado === 'ASIGNADO')
      .length,
)

const portesEnTransito = computed(
  () => visiblePortes.value.filter((porte) => porte.estado === 'EN_TRANSITO').length,
)

const completadosEsteMes = computed(() => {
  const now = new Date()
  const currentMonth = now.getMonth()
  const currentYear = now.getFullYear()

  return visiblePortes.value.filter((porte) => {
    if (porte.estado !== 'ENTREGADO' && porte.estado !== 'FACTURADO') return false
    if (!porte.fechaEntrega) return false

    const fechaEntrega = new Date(porte.fechaEntrega)
    if (Number.isNaN(fechaEntrega.getTime())) return false

    return fechaEntrega.getMonth() === currentMonth && fechaEntrega.getFullYear() === currentYear
  }).length
})

// --- Lifecycle ---
onMounted(async () => {
  await Promise.all([portesStore.fetchPortes(), portesStore.fetchReferenceData()])
  await openPorteFromQuery()
})

watch(() => route.query.porteId, () => {
  void openPorteFromQuery()
})

// --- Handlers ---

function onNewPorte(): void {
  editingPorte.value = null
  showDialog.value = true
}

function onEditPorte(porte: Porte): void {
  editingPorte.value = porte
  showDialog.value = true
}

function onViewPorte(porte: Porte): void {
  detailPorte.value = porte
  showDetail.value = true
}

async function clearClienteFilter(): Promise<void> {
  await clearQueryParam('clienteId')
}

async function clearQueryParam(param: string): Promise<void> {
  const nextQuery = { ...route.query }
  delete nextQuery[param]
  await router.replace({ query: nextQuery })
}

async function openPorteFromQuery(): Promise<void> {
  const rawId = route.query.porteId
  const porteId = typeof rawId === 'string' ? Number.parseInt(rawId, 10) : NaN

  if (Number.isNaN(porteId)) return

  const porte = await portesStore.fetchPorteById(porteId)
  if (porte) {
    onViewPorte(porte)
    await clearQueryParam('porteId')
  }
}

function onConfirmDelete(porte: Porte): void {
  if (porte.estado !== 'CANCELADO') {
    toast.add({
      severity: 'warn',
      summary: 'No se puede eliminar',
      detail: 'Solo se pueden eliminar portes cancelados.',
      life: 4000,
    })
    return
  }
  deletingPorte.value = porte
  showDeleteConfirm.value = true
}

function onAjustarPrecio(porte: Porte): void {
  ajustePorte.value = porte
  ajusteForm.value = { precioAjustado: porte.precio ?? 0, motivo: '' }
  showAjusteDialog.value = true
}

async function onSaveAjuste(): Promise<void> {
  if (!ajustePorte.value) return

  const precioAjustado = normalizeCurrencyInput(ajusteForm.value.precioAjustado)
  if (!Number.isFinite(precioAjustado)) {
    toast.add({
      severity: 'warn',
      summary: 'Precio inválido',
      detail: 'Ingresá un importe válido para ajustar el precio (ej: 116,35).',
      life: 5000,
    })
    return
  }

  const motivo = ajusteForm.value.motivo.trim()
  if (!motivo) {
    toast.add({
      severity: 'warn',
      summary: 'Motivo requerido',
      detail: 'Debés indicar el motivo del ajuste.',
      life: 5000,
    })
    return
  }

  try {
    await portesStore.ajustarPrecio(ajustePorte.value.id, {
      precioAjustado,
      motivo,
    })
    toast.add({
      severity: 'success',
      summary: 'Precio ajustado',
      detail: `El precio del porte #${ajustePorte.value.id} se ha ajustado correctamente.`,
      life: 3000,
    })
    showAjusteDialog.value = false
    ajustePorte.value = null
  } catch (error: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: resolveAjusteErrorMessage(error),
      life: 5000,
    })
  }
}

function normalizeCurrencyInput(value: unknown): number {
  if (typeof value === 'number') return value
  if (typeof value !== 'string') return Number.NaN

  const normalized = value
    .replace(/\s/g, '')
    .replace(/€/g, '')
    .replace(/\./g, '')
    .replace(/,/g, '.')

  return Number(normalized)
}

function resolveAjusteErrorMessage(error: unknown): string {
  if (!axios.isAxiosError(error)) {
    return 'No se pudo ajustar el precio. Inténtalo de nuevo.'
  }

  const data = error.response?.data
  if (typeof data === 'string' && data.trim()) {
    return data
  }

  if (data && typeof data === 'object') {
    const maybeMessage = (data as { message?: unknown }).message
    if (typeof maybeMessage === 'string' && maybeMessage.trim()) {
      return maybeMessage
    }
  }

  if (error.response?.status === 403) {
    return 'No tenés permisos para ajustar precios.'
  }

  return 'No se pudo ajustar el precio. Inténtalo de nuevo.'
}

function onConfirmFacturar(porte: Porte): void {
  facturandoPorte.value = porte
  showFacturarConfirm.value = true
}

async function onFacturar(): Promise<void> {
  if (!facturandoPorte.value) return
  try {
    const id = facturandoPorte.value.id
    await portesStore.facturarPorte(id)
    toast.add({
      severity: 'success',
      summary: 'Porte facturado',
      detail: `El porte #${id} se ha facturado correctamente.`,
      life: 3000,
    })
    showFacturarConfirm.value = false
    facturandoPorte.value = null
  } catch (error: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: resolveFacturarErrorMessage(error),
      life: 5000,
    })
  }
}

function resolveFacturarErrorMessage(error: unknown): string {
  const fallback = 'No se pudo facturar el porte. Inténtalo de nuevo.'

  if (!axios.isAxiosError(error)) {
    if (error instanceof Error && error.message.trim()) return error.message
    return fallback
  }

  const data = error.response?.data

  if (typeof data === 'string' && data.trim()) {
    return data
  }

  if (data && typeof data === 'object') {
    const payload = data as { message?: unknown; error?: unknown }

    if (typeof payload.message === 'string' && payload.message.trim()) {
      return payload.message
    }

    if (typeof payload.error === 'string' && payload.error.trim()) {
      return payload.error
    }
  }

  if (error.message?.trim()) {
    return error.message
  }

  return fallback
}

async function onSavePorte(data: CreatePorteRequest & { estado?: EstadoPorte }): Promise<void> {
  try {
    if (editingPorte.value) {
      await portesStore.updatePorte(editingPorte.value.id, {
        origen: data.origen,
        destino: data.destino,
        descripcionCliente: data.descripcionCliente,
        fechaRecogida: data.fechaRecogida,
        fechaEntrega: data.fechaEntrega,
        estado: data.estado,
      })
      toast.add({
        severity: 'success',
        summary: 'Porte actualizado',
        detail: `El porte #${editingPorte.value.id} se ha actualizado correctamente.`,
        life: 3000,
      })
    } else {
      const created = await portesStore.createPorte(data)
      toast.add({
        severity: 'success',
        summary: 'Porte creado',
        detail: `El porte #${created.id} se ha creado correctamente.`,
        life: 3000,
      })
    }
    showDialog.value = false
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo guardar el porte. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}

async function onDeletePorte(): Promise<void> {
  if (!deletingPorte.value) return
  try {
    const id = deletingPorte.value.id
    await portesStore.deletePorte(id)
    toast.add({
      severity: 'success',
      summary: 'Porte eliminado',
      detail: `El porte #${id} se ha eliminado correctamente.`,
      life: 3000,
    })
    showDeleteConfirm.value = false
    deletingPorte.value = null
    // Also close detail if viewing the deleted porte
    if (detailPorte.value?.id === id) {
      showDetail.value = false
      detailPorte.value = null
    }
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo eliminar el porte. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}

function formatDateTime(dateStr: string | undefined): string {
  if (!dateStr || dateStr === '—') return '—'
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

function getConductorFullName(porte: Porte): string {
  if (!porte.conductor) return 'Sin asignar'
  const c = porte.conductor
  return `${c.nombre}${c.apellidos ? ' ' + c.apellidos : ''}`
}

function hasPositiveValue(value: number | undefined): boolean {
  return value != null && value > 0
}

function formatDimensionLabel(porte: Porte): string {
  const parts: string[] = []
  if (hasPositiveValue(porte.largoMaxPaquete)) parts.push(`L ${porte.largoMaxPaquete} m`)
  if (hasPositiveValue(porte.anchoMaxPaquete)) parts.push(`A ${porte.anchoMaxPaquete} m`)
  if (hasPositiveValue(porte.altoMaxPaquete)) parts.push(`H ${porte.altoMaxPaquete} m`)
  return parts.join(' × ')
}

// Suppress unused variable warnings — authStore is used in template
void authStore
</script>

<template>
  <div class="h-full min-h-0 flex flex-col gap-6 overflow-hidden">
    <!-- Page Header -->
    <div class="shrink-0 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
          <i class="pi pi-truck text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Portes</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Gestión de portes y envíos</p>
        </div>
      </div>
      <Button
        label="Nuevo Porte"
        icon="pi pi-plus"
        @click="onNewPorte"
      />
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="portesStore.usingMockData"
      class="shrink-0 flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <div
      v-if="selectedClienteId !== null"
      class="shrink-0 flex items-center justify-between gap-3 bg-blue-50 border border-blue-200 text-blue-800 rounded-lg px-4 py-3 text-sm"
    >
      <span>Filtrando por cliente #{{ selectedClienteId }}.</span>
      <Button
        label="Quitar filtro"
        icon="pi pi-times"
        size="small"
        text
        @click="clearClienteFilter"
      />
    </div>

    <!-- Stats -->
    <div class="shrink-0 grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-amber-50 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400 flex items-center justify-center">
            <i class="pi pi-inbox text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800 dark:text-gray-100">{{ portesPendientes }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">Pendientes de gestión</p>
          </div>
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 flex items-center justify-center">
            <i class="pi pi-send text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800 dark:text-gray-100">{{ portesEnTransito }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">En tránsito</p>
          </div>
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-emerald-50 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
            <i class="pi pi-check-circle text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800 dark:text-gray-100">{{ completadosEsteMes }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">Completados este mes</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Data Table -->
    <div class="flex-1 min-h-0 overflow-auto">
      <PorteTable
        :portes="visiblePortes"
        :loading="portesStore.loading"
        @view="onViewPorte"
        @edit="onEditPorte"
        @delete="onConfirmDelete"
        @ajustar-precio="onAjustarPrecio"
        @facturar="onConfirmFacturar"
      />
    </div>

    <!-- Create/Edit Dialog -->
    <PorteDialog
      v-model:visible="showDialog"
      :porte="editingPorte"
      :conductores="portesStore.conductores"
      :vehiculos="portesStore.vehiculos"
      :clientes="portesStore.clientes"
      :saving="portesStore.saving"
      @save="onSavePorte"
    />

    <!-- Detail Slide Panel -->
    <Dialog
      v-model:visible="showDetail"
      :header="`Detalle del Porte #${detailPorte?.id ?? ''}`"
      :modal="true"
      :closable="true"
      :style="{ width: '700px' }"
    >
      <div v-if="detailPorte" class="space-y-6 pt-2">
        <!-- Status & ID Header -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="text-lg font-bold text-gray-800 dark:text-gray-100">#{{ detailPorte.id }}</span>
            <PorteStatusBadge :estado="detailPorte.estado" />
          </div>
          <div class="flex items-center gap-2">
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              size="small"
              v-tooltip.top="'Editar'"
              @click="showDetail = false; onEditPorte(detailPorte!)"
            />
          </div>
        </div>

        <!-- Route Info -->
        <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-5">
          <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-3">Ruta</h4>
          <div class="flex items-center gap-4">
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <i class="pi pi-map-marker text-blue-500"></i>
                <span class="text-sm text-gray-500 dark:text-gray-400">Origen</span>
              </div>
              <p class="text-gray-800 dark:text-gray-100 font-medium">{{ detailPorte.origen }}</p>
            </div>
            <div class="flex-shrink-0">
              <i class="pi pi-arrow-right text-gray-300 dark:text-gray-600 text-xl"></i>
            </div>
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <i class="pi pi-flag text-emerald-500"></i>
                <span class="text-sm text-gray-500 dark:text-gray-400">Destino</span>
              </div>
              <p class="text-gray-800 dark:text-gray-100 font-medium">{{ detailPorte.destino }}</p>
            </div>
          </div>
          <div v-if="detailPorte.distanciaKm" class="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
            <span class="text-sm text-gray-500 dark:text-gray-400">Distancia:</span>
            <span class="text-sm font-medium text-gray-700 dark:text-gray-300 ml-1">{{ detailPorte.distanciaKm }} km</span>
          </div>
        </div>

        <!-- Details Grid -->
        <div class="grid grid-cols-2 gap-5">
          <!-- Conductor -->
          <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-4">
            <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">Conductor</h4>
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 rounded-full flex items-center justify-center"
                :class="detailPorte.conductor ? 'bg-primary/10' : 'bg-gray-200 dark:bg-gray-700'"
              >
                <span
                  class="text-sm font-semibold"
                  :class="detailPorte.conductor ? 'text-primary' : 'text-gray-400'"
                >
                  {{ detailPorte.conductor ? detailPorte.conductor.nombre.charAt(0).toUpperCase() : '?' }}
                </span>
              </div>
              <div>
                <p class="text-gray-800 dark:text-gray-100 font-medium">{{ getConductorFullName(detailPorte) }}</p>
                <p v-if="detailPorte.conductor?.telefono" class="text-sm text-gray-500 dark:text-gray-400">
                  {{ detailPorte.conductor.telefono }}
                </p>
              </div>
            </div>
          </div>

          <!-- Cliente -->
          <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-4">
            <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">Cliente</h4>
            <p class="text-gray-800 dark:text-gray-100 font-medium">{{ detailPorte.cliente?.nombreEmpresa ?? 'Sin cliente' }}</p>
            <p v-if="detailPorte.cliente?.emailContacto" class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
              {{ detailPorte.cliente.emailContacto }}
            </p>
          </div>
        </div>

        <!-- Dates -->
        <div class="grid grid-cols-3 gap-4">
          <div>
            <span class="text-sm text-gray-500 dark:text-gray-400">Creación</span>
            <p class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ formatDateTime(detailPorte.fechaCreacion) }}</p>
          </div>
          <div>
            <span class="text-sm text-gray-500 dark:text-gray-400">Recogida</span>
            <p class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ formatDateTime(detailPorte.fechaRecogida) }}</p>
          </div>
          <div>
            <span class="text-sm text-gray-500 dark:text-gray-400">Entrega</span>
            <p class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ formatDateTime(detailPorte.fechaEntrega) }}</p>
          </div>
        </div>

        <!-- Pricing -->
        <div v-if="detailPorte.precio" class="bg-gray-50 dark:bg-gray-900 rounded-xl p-4">
          <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">Económico</h4>
          <div class="grid grid-cols-3 gap-4">
            <div>
              <span class="text-sm text-gray-500 dark:text-gray-400">Precio Base</span>
              <p class="text-gray-800 dark:text-gray-100 font-medium">{{ detailPorte.precio?.toFixed(2) }} €</p>
            </div>
            <div v-if="detailPorte.ajustePrecio">
              <span class="text-sm text-gray-500 dark:text-gray-400">Ajuste</span>
              <p class="text-gray-800 dark:text-gray-100 font-medium">{{ detailPorte.ajustePrecio.toFixed(2) }} €</p>
            </div>
            <div>
              <span class="text-sm text-gray-500 dark:text-gray-400">Total</span>
              <p class="text-lg font-bold text-primary">
                {{ ((detailPorte.precio ?? 0) + (detailPorte.ajustePrecio ?? 0)).toFixed(2) }} €
              </p>
            </div>
          </div>
        </div>

        <!-- Cargo -->
        <div
          v-if="
            hasPositiveValue(detailPorte.pesoTotalKg) ||
            hasPositiveValue(detailPorte.volumenTotalM3) ||
            formatDimensionLabel(detailPorte) ||
            detailPorte.tipoVehiculoRequerido
          "
          class="bg-gray-50 dark:bg-gray-900 rounded-xl p-4"
        >
          <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">Carga</h4>
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div v-if="hasPositiveValue(detailPorte.pesoTotalKg)">
              <span class="text-gray-500 dark:text-gray-400">Peso total</span>
              <p class="text-gray-800 dark:text-gray-100 font-medium">{{ detailPorte.pesoTotalKg }} kg</p>
            </div>
            <div v-if="hasPositiveValue(detailPorte.volumenTotalM3)">
              <span class="text-gray-500 dark:text-gray-400">Volumen total</span>
              <p class="text-gray-800 dark:text-gray-100 font-medium">{{ detailPorte.volumenTotalM3 }} m³</p>
            </div>
            <div v-if="formatDimensionLabel(detailPorte)">
              <span class="text-gray-500 dark:text-gray-400">Dimensiones máximas</span>
              <p class="text-gray-800 dark:text-gray-100 font-medium">{{ formatDimensionLabel(detailPorte) }}</p>
            </div>
            <div v-if="detailPorte.tipoVehiculoRequerido">
              <span class="text-gray-500 dark:text-gray-400">Vehículo requerido</span>
              <p class="text-gray-800 dark:text-gray-100 font-medium">{{ detailPorte.tipoVehiculoRequerido }}</p>
            </div>
          </div>
        </div>

        <!-- Description -->
        <div v-if="detailPorte.descripcionCliente">
          <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">Descripción</h4>
          <p class="text-gray-700 dark:text-gray-300 text-sm bg-gray-50 dark:bg-gray-900 rounded-xl p-4">{{ detailPorte.descripcionCliente }}</p>
        </div>

        <!-- Fotos de carga -->
        <PorteFotosSection :porteId="detailPorte.id ?? null" />
      </div>
    </Dialog>

    <!-- Delete Confirmation Dialog -->
    <Dialog
      v-model:visible="showDeleteConfirm"
      header="Confirmar eliminación"
      :modal="true"
      :closable="true"
      :style="{ width: '450px' }"
    >
      <div class="flex items-start gap-4 py-2">
        <div class="w-10 h-10 rounded-full bg-red-50 flex items-center justify-center flex-shrink-0">
          <i class="pi pi-exclamation-triangle text-red-500"></i>
        </div>
        <div>
          <p class="text-gray-800 dark:text-gray-100 font-medium">
            ¿Eliminar el porte #{{ deletingPorte?.id }}?
          </p>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Esta acción no se puede deshacer. Se eliminará permanentemente el porte
            de {{ deletingPorte?.origen }} a {{ deletingPorte?.destino }}.
          </p>
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3">
          <Button
            label="Cancelar"
            severity="secondary"
            text
            @click="showDeleteConfirm = false"
          />
          <Button
            label="Eliminar"
            severity="danger"
            icon="pi pi-trash"
            :loading="portesStore.saving"
            @click="onDeletePorte"
          />
        </div>
      </template>
    </Dialog>

    <!-- Ajuste Precio Dialog -->
    <Dialog
      v-model:visible="showAjusteDialog"
      header="Ajustar Precio"
      :modal="true"
      :closable="true"
      :style="{ width: '450px' }"
    >
      <div class="space-y-4 pt-2">
        <p class="text-sm text-gray-500 dark:text-gray-400">
          Ajustar precio del porte <span class="font-semibold text-gray-800 dark:text-gray-100">#{{ ajustePorte?.id }}</span>
          ({{ ajustePorte?.origen }} → {{ ajustePorte?.destino }})
        </p>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nuevo precio (€)</label>
          <InputNumber
            v-model="ajusteForm.precioAjustado"
            mode="currency"
            currency="EUR"
            locale="es-ES"
            class="w-full"
            :min="0"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Motivo del ajuste</label>
          <Textarea
            v-model="ajusteForm.motivo"
            rows="3"
            placeholder="Explica el motivo del ajuste de precio..."
            class="w-full"
            autoResize
          />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3">
          <Button
            label="Cancelar"
            severity="secondary"
            text
            @click="showAjusteDialog = false"
          />
          <Button
            label="Aplicar Ajuste"
            icon="pi pi-check"
            :loading="portesStore.saving"
            @click="onSaveAjuste"
          />
        </div>
      </template>
    </Dialog>

    <!-- Facturar Confirmation Dialog -->
    <Dialog
      v-model:visible="showFacturarConfirm"
      header="Confirmar facturación"
      :modal="true"
      :closable="true"
      :style="{ width: '450px' }"
    >
      <div class="flex items-start gap-4 py-2">
        <div class="w-10 h-10 rounded-full bg-blue-50 flex items-center justify-center flex-shrink-0">
          <i class="pi pi-file text-blue-500"></i>
        </div>
        <div>
          <p class="text-gray-800 dark:text-gray-100 font-medium">
            ¿Facturar el porte #{{ facturandoPorte?.id }}?
          </p>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Se marcará como facturado el porte de {{ facturandoPorte?.origen }} a {{ facturandoPorte?.destino }}.
          </p>
          <p v-if="facturandoPorte?.precio" class="text-sm text-gray-600 dark:text-gray-400 mt-2">
            Importe: <span class="font-semibold">{{ ((facturandoPorte.precio ?? 0) + (facturandoPorte.ajustePrecio ?? 0)).toFixed(2) }} €</span>
          </p>
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3">
          <Button
            label="Cancelar"
            severity="secondary"
            text
            @click="showFacturarConfirm = false"
          />
          <Button
            label="Facturar"
            severity="success"
            icon="pi pi-file"
            :loading="portesStore.saving"
            @click="onFacturar"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
