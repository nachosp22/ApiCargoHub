<script setup lang="ts">
import { ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import { useToast } from 'primevue/usetoast'
import { useFacturasStore, type Factura } from '@/stores/facturas'

interface Props {
  factura: Factura | null
  visible: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const toast = useToast()
const facturasStore = useFacturasStore()
const marcandoPagada = ref(false)
const badgeAnimating = ref(false)
const prevPagada = ref<boolean | null>(null)

watch(
  () => props.factura,
  (nueva) => {
    // Trigger badge animation when factura goes from pendiente → pagada
    if (nueva && prevPagada.value === false && nueva.pagada === true) {
      badgeAnimating.value = true
      setTimeout(() => { badgeAnimating.value = false }, 1200)
    }
    prevPagada.value = nueva?.pagada ?? null
  },
  { immediate: true },
)

function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  try {
    return new Date(dateStr).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  } catch {
    return dateStr
  }
}

function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  try {
    return new Date(dateStr).toLocaleDateString('es-ES', {
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

function formatCurrency(value: number | null | undefined): string {
  if (value == null) return '0,00 €'
  return value.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' })
}

function getConductorName(): string {
  const c = props.factura?.porte?.conductor
  if (!c) return '—'
  return `${c.nombre}${c.apellidos ? ' ' + c.apellidos : ''}`
}

async function handleDownloadPdf() {
  if (props.factura) {
    await facturasStore.downloadPdf(props.factura.id)
  }
}

async function handlePagar() {
  if (!props.factura || props.factura.pagada) return
  marcandoPagada.value = true
  const result = await facturasStore.pagarFactura(props.factura.id)
  marcandoPagada.value = false
  if (result) {
    toast.add({ severity: 'success', summary: 'Factura pagada', detail: `La factura ${props.factura.numeroSerie} ha sido marcada como pagada.`, life: 4000 })
  } else {
    toast.add({ severity: 'error', summary: 'Error', detail: facturasStore.error ?? 'No se pudo marcar como pagada', life: 5000 })
  }
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="emit('update:visible', $event)"
    :header="`Factura ${factura?.numeroSerie ?? ''}`"
    :modal="true"
    :closable="true"
    :style="{ width: '750px' }"
  >
    <div v-if="factura" class="space-y-5 pt-2">
      <!-- Invoice Header -->
      <div class="flex items-center justify-between border-b border-gray-200 dark:border-gray-700 pb-4">
        <div class="flex items-center gap-4">
          <img
            src="/assets/brand/logo.png"
            alt="CargoHub"
            class="h-10 w-10 object-contain"
          />
          <div>
            <h2 class="text-xl font-bold text-gray-800 dark:text-gray-100">{{ factura.numeroSerie }}</h2>
            <p class="text-sm text-gray-500 dark:text-gray-400">Emitida el {{ formatDate(factura.fechaEmision) }}</p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <span
            class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset transition-all duration-700"
            :class="[
              factura.pagada
                ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20'
                : 'bg-orange-50 text-orange-700 ring-orange-600/20',
              badgeAnimating ? 'scale-110 shadow-[0_0_12px_rgba(16,185,129,0.4)]' : '',
            ]"
          >
            <i
              v-if="factura.pagada"
              class="pi pi-check-circle mr-1 text-xs"
            />
            {{ factura.pagada ? 'Pagada' : 'Pendiente' }}
          </span>
          <Button
            v-if="!factura.pagada"
            label="Marcar como pagada"
            icon="pi pi-check"
            severity="success"
            size="small"
            :loading="marcandoPagada"
            @click="handlePagar"
          />
          <Button
            label="Descargar PDF"
            icon="pi pi-file-pdf"
            severity="info"
            size="small"
            @click="handleDownloadPdf"
          />
        </div>
      </div>

      <!-- Two-column: Issuer / Client -->
      <div class="grid grid-cols-2 gap-6">
        <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-4">
          <h4 class="text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wide mb-2">Emisor</h4>
          <p class="font-semibold text-gray-800 dark:text-gray-100">CargoHub S.L.</p>
          <p class="text-sm text-gray-600 dark:text-gray-400">CIF: B12345678</p>
          <p class="text-sm text-gray-600 dark:text-gray-400">Calle Logística 1, 28001 Madrid</p>
        </div>
        <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-4">
          <h4 class="text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wide mb-2">Cliente</h4>
          <p class="font-semibold text-gray-800 dark:text-gray-100">{{ factura.porte?.cliente?.nombreEmpresa ?? '—' }}</p>
          <p class="text-sm text-gray-600 dark:text-gray-400">CIF: {{ factura.porte?.cliente?.cif ?? '—' }}</p>
          <p class="text-sm text-gray-600 dark:text-gray-400">{{ factura.porte?.cliente?.direccionFiscal ?? '—' }}</p>
        </div>
      </div>

      <!-- Invoice Meta -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <span class="text-xs text-gray-400 dark:text-gray-500 uppercase">Fecha Emisión</span>
          <p class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ formatDate(factura.fechaEmision) }}</p>
        </div>
        <div>
          <span class="text-xs text-gray-400 dark:text-gray-500 uppercase">Fecha Pago</span>
          <p class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ formatDate(factura.fechaPago) }}</p>
        </div>
      </div>

      <!-- Porte Details -->
      <div v-if="factura.porte" class="bg-gray-50 dark:bg-gray-900 rounded-xl p-5">
        <h4 class="text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wide mb-3">Detalle del Porte</h4>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <div>
            <span class="text-xs text-gray-400 dark:text-gray-500">Nº Porte</span>
            <p class="text-sm font-medium text-gray-800 dark:text-gray-100">#{{ factura.porte.id }}</p>
          </div>
          <div>
            <span class="text-xs text-gray-400 dark:text-gray-500">Ruta</span>
            <p class="text-sm font-medium text-gray-800 dark:text-gray-100">
              {{ factura.porte.origen }} → {{ factura.porte.destino }}
            </p>
          </div>
          <div>
            <span class="text-xs text-gray-400 dark:text-gray-500">Conductor</span>
            <p class="text-sm font-medium text-gray-800 dark:text-gray-100">{{ getConductorName() }}</p>
          </div>
          <div>
            <span class="text-xs text-gray-400 dark:text-gray-500">F. Recogida</span>
            <p class="text-sm font-medium text-gray-800 dark:text-gray-100">{{ formatDateTime(factura.porte.fechaRecogida) }}</p>
          </div>
          <div>
            <span class="text-xs text-gray-400 dark:text-gray-500">F. Entrega</span>
            <p class="text-sm font-medium text-gray-800 dark:text-gray-100">{{ formatDateTime(factura.porte.fechaEntrega) }}</p>
          </div>
          <div>
            <span class="text-xs text-gray-400 dark:text-gray-500">Peso / Volumen</span>
            <p class="text-sm font-medium text-gray-800 dark:text-gray-100">
              {{ factura.porte.pesoTotalKg != null ? factura.porte.pesoTotalKg + ' kg' : '—' }}
              /
              {{ factura.porte.volumenTotalM3 != null ? factura.porte.volumenTotalM3 + ' m³' : '—' }}
            </p>
          </div>
        </div>
        <div v-if="factura.porte.descripcionCliente" class="mt-3">
          <span class="text-xs text-gray-400 dark:text-gray-500">Descripción</span>
          <p class="text-sm text-gray-700 dark:text-gray-300">{{ factura.porte.descripcionCliente }}</p>
        </div>
      </div>

      <!-- Amounts -->
      <div class="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-5">
        <h4 class="text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wide mb-3">Importes</h4>
        <div class="space-y-2">
          <div class="flex justify-between text-sm">
            <span class="text-gray-600 dark:text-gray-400">Base Imponible</span>
            <span class="text-gray-800 dark:text-gray-100 font-medium">{{ formatCurrency(factura.baseImponible) }}</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-gray-600 dark:text-gray-400">IVA (21%)</span>
            <span class="text-gray-800 dark:text-gray-100 font-medium">{{ formatCurrency(factura.iva) }}</span>
          </div>
          <div class="flex justify-between pt-2 border-t border-gray-200 dark:border-gray-700">
            <span class="text-base font-bold text-gray-800 dark:text-gray-100">Total</span>
            <span class="text-lg font-bold text-blue-600">{{ formatCurrency(factura.importeTotal) }}</span>
          </div>
        </div>
      </div>

      <!-- Observaciones -->
      <div v-if="factura.observaciones" class="bg-yellow-50 border border-yellow-200 rounded-xl p-4">
        <h4 class="text-xs font-semibold text-yellow-600 uppercase tracking-wide mb-1">Observaciones</h4>
        <p class="text-sm text-yellow-800">{{ factura.observaciones }}</p>
      </div>
    </div>
  </Dialog>
</template>
