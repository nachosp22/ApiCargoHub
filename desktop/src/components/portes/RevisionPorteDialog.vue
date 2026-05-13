<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { usePortesStore } from '@/stores/portes'
import { useToast } from 'primevue/usetoast'
import type { Porte, DimensionesRequest, ConductorCandidato } from '@/stores/portes'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PorteStatusBadge from './PorteStatusBadge.vue'

interface Props {
  visible: boolean
  porte: Porte | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  assigned: []
}>()

const portesStore = usePortesStore()
const toast = useToast()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val),
})

// Dimension form
const dimForm = ref<DimensionesRequest>({})
const showCandidatos = ref(false)

const tipoVehiculoOptions = [
  { label: 'Furgoneta', value: 'FURGONETA' },
  { label: 'Rígido', value: 'RIGIDO' },
  { label: 'Tráiler', value: 'TRAILER' },
  { label: 'Especial', value: 'ESPECIAL' },
]

// Reset form when porte changes
watch(
  () => props.porte,
  (p) => {
    if (p) {
      dimForm.value = {
        pesoTotalKg: p.pesoTotalKg ?? undefined,
        volumenTotalM3: p.volumenTotalM3 ?? undefined,
        largoMaxPaquete: p.largoMaxPaquete ?? undefined,
        anchoMaxPaquete: p.anchoMaxPaquete ?? undefined,
        altoMaxPaquete: p.altoMaxPaquete ?? undefined,
        tipoVehiculoRequerido: p.tipoVehiculoRequerido ?? undefined,
      }
      showCandidatos.value = false
      portesStore.conductorCandidatos = []
    }
  },
  { immediate: true },
)

async function onSaveDimensiones(): Promise<void> {
  if (!props.porte) return
  try {
    await portesStore.updateDimensiones(props.porte.id, dimForm.value)
    toast.add({ severity: 'success', summary: 'Dimensiones actualizadas', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error al actualizar dimensiones', life: 5000 })
  }
}

async function onBuscarConductores(): Promise<void> {
  if (!props.porte) return
  try {
    showCandidatos.value = true
    await portesStore.buscarConductores(props.porte.id)
  } catch {
    toast.add({ severity: 'error', summary: 'Error al buscar conductores compatibles', life: 5000 })
  }
}

async function onAsignar(candidato: ConductorCandidato): Promise<void> {
  if (!props.porte) return
  try {
    await portesStore.asignarConductor(props.porte.id, candidato.id)
    toast.add({
      severity: 'success',
      summary: 'Conductor asignado',
      detail: `${candidato.nombre} ${candidato.apellidos ?? ''} asignado al porte #${props.porte.id}`,
      life: 3000,
    })
    emit('update:visible', false)
    emit('assigned')
  } catch {
    toast.add({ severity: 'error', summary: 'Error al asignar conductor', life: 5000 })
  }
}

function copyToClipboard(text: string, label: string): void {
  navigator.clipboard.writeText(text)
  toast.add({ severity: 'info', summary: `${label} copiado`, life: 2000 })
}

async function onRetryMatching(): Promise<void> {
  if (!props.porte) return
  try {
    await portesStore.retryMatching(props.porte.id)
    toast.add({ severity: 'success', summary: 'Matching automático reintentado', life: 3000 })
    emit('assigned')
  } catch {
    toast.add({ severity: 'error', summary: 'Error al reintentar matching automático', life: 5000 })
  }
}

const reviewLabel = computed(() =>
  props.porte?.revisionManual ? 'Revisión Manual' : 'Atención Operativa',
)

const reviewBannerClass = computed(() =>
  props.porte?.revisionManual
    ? 'bg-amber-50 border-amber-200 text-amber-700'
    : 'bg-sky-50 border-sky-200 text-sky-700',
)

const reviewBadgeClass = computed(() =>
  props.porte?.revisionManual
    ? 'bg-red-50 text-red-700 ring-red-600/20'
    : 'bg-sky-50 text-sky-700 ring-sky-600/20',
)
</script>

<template>
  <Dialog
    v-model:visible="dialogVisible"
    :header="`Revisión Porte #${porte?.id ?? ''}`"
    :modal="true"
    :closable="true"
    :style="{ width: '900px' }"
    :contentStyle="{ maxHeight: '80vh', overflow: 'auto' }"
  >
    <div v-if="porte" class="space-y-6 pt-2">
      <!-- Status & Route Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="text-lg font-bold text-gray-800 dark:text-gray-100">#{{ porte.id }}</span>
          <PorteStatusBadge :estado="porte.estado" />
          <span
            v-if="porte.revisionManual || porte.motivoRevision"
            class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
            :class="reviewBadgeClass"
          >
            <i class="pi pi-exclamation-circle mr-1"></i>
            {{ reviewLabel }}
          </span>
        </div>
      </div>

      <!-- Motivo de revisión o atención -->
      <div
        v-if="porte.motivoRevision"
        class="border rounded-lg px-4 py-3"
        :class="reviewBannerClass"
      >
        <div class="flex items-center gap-2 mb-1">
          <i class="pi pi-info-circle"></i>
          <span class="text-sm font-semibold">{{ reviewLabel }}</span>
        </div>
        <p class="text-sm">{{ porte.motivoRevision }}</p>
      </div>

      <!-- Route Info -->
      <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-4">
        <div class="flex items-center gap-4">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <i class="pi pi-map-marker text-blue-500"></i>
              <span class="text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide">Origen</span>
            </div>
            <p class="text-gray-800 dark:text-gray-100 font-medium">{{ porte.origen }}</p>
          </div>
          <i class="pi pi-arrow-right text-gray-300 dark:text-gray-600 text-xl flex-shrink-0"></i>
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <i class="pi pi-flag text-emerald-500"></i>
              <span class="text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide">Destino</span>
            </div>
            <p class="text-gray-800 dark:text-gray-100 font-medium">{{ porte.destino }}</p>
          </div>
          <div v-if="porte.distanciaKm" class="text-right flex-shrink-0">
            <span class="text-xs text-gray-500 dark:text-gray-400">Distancia</span>
            <p class="text-gray-800 dark:text-gray-100 font-medium">{{ porte.distanciaKm?.toFixed(0) }} km</p>
          </div>
        </div>
      </div>

      <!-- Client Info Card -->
      <div class="bg-blue-50/50 dark:bg-blue-950/30 rounded-xl p-5 border border-blue-100 dark:border-blue-900">
        <h4 class="text-sm font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-3 flex items-center gap-2">
          <i class="pi pi-building text-blue-500"></i>
          Datos del Cliente
        </h4>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <span class="text-xs text-gray-500 dark:text-gray-400">Empresa</span>
            <p class="text-gray-800 dark:text-gray-100 font-medium">{{ porte.cliente?.nombreEmpresa ?? 'Sin cliente' }}</p>
          </div>
          <div>
            <span class="text-xs text-gray-500 dark:text-gray-400">CIF</span>
            <p class="text-gray-800 dark:text-gray-100 font-medium">{{ porte.cliente?.cif ?? '—' }}</p>
          </div>
          <div>
            <span class="text-xs text-gray-500 dark:text-gray-400">Teléfono</span>
            <p v-if="porte.cliente?.telefono" class="text-gray-800 dark:text-gray-100 font-medium flex items-center gap-2">
              {{ porte.cliente.telefono }}
              <button
                class="text-blue-500 hover:text-blue-700 transition-colors"
                @click="copyToClipboard(porte.cliente!.telefono!, 'Teléfono')"
              >
                <i class="pi pi-copy text-xs"></i>
              </button>
            </p>
            <p v-else class="text-gray-400">—</p>
          </div>
          <div>
            <span class="text-xs text-gray-500 dark:text-gray-400">Email</span>
            <p v-if="porte.cliente?.emailContacto" class="text-gray-800 dark:text-gray-100 font-medium flex items-center gap-2">
              {{ porte.cliente.emailContacto }}
              <button
                class="text-blue-500 hover:text-blue-700 transition-colors"
                @click="copyToClipboard(porte.cliente!.emailContacto!, 'Email')"
              >
                <i class="pi pi-copy text-xs"></i>
              </button>
            </p>
            <p v-else class="text-gray-400">—</p>
          </div>
        </div>
      </div>

      <!-- Description -->
      <div v-if="porte.descripcionCliente" class="bg-gray-50 dark:bg-gray-900 rounded-xl p-4">
        <h4 class="text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">Descripción del cliente</h4>
        <p class="text-gray-700 dark:text-gray-300 text-sm">{{ porte.descripcionCliente }}</p>
      </div>

      <!-- Cargo Dimensions - Editable -->
      <div class="bg-white dark:bg-gray-800 rounded-xl p-5 border border-gray-200 dark:border-gray-700">
        <h4 class="text-sm font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-4 flex items-center gap-2">
          <i class="pi pi-box text-orange-500"></i>
          Dimensiones de Carga
        </h4>
        <div class="grid grid-cols-3 gap-4 mb-4">
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Peso (kg)</label>
            <InputNumber
              v-model="dimForm.pesoTotalKg"
              :minFractionDigits="1"
              :maxFractionDigits="2"
              placeholder="0.0"
              class="w-full"
              :min="0"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Volumen (m³)</label>
            <InputNumber
              v-model="dimForm.volumenTotalM3"
              :minFractionDigits="1"
              :maxFractionDigits="3"
              placeholder="0.0"
              class="w-full"
              :min="0"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Tipo Vehículo</label>
            <Select
              v-model="dimForm.tipoVehiculoRequerido"
              :options="tipoVehiculoOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Seleccionar"
              class="w-full"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Largo máx. (m)</label>
            <InputNumber
              v-model="dimForm.largoMaxPaquete"
              :minFractionDigits="1"
              :maxFractionDigits="2"
              placeholder="0.0"
              class="w-full"
              :min="0"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Ancho máx. (m)</label>
            <InputNumber
              v-model="dimForm.anchoMaxPaquete"
              :minFractionDigits="1"
              :maxFractionDigits="2"
              placeholder="0.0"
              class="w-full"
              :min="0"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Alto máx. (m)</label>
            <InputNumber
              v-model="dimForm.altoMaxPaquete"
              :minFractionDigits="1"
              :maxFractionDigits="2"
              placeholder="0.0"
              class="w-full"
              :min="0"
            />
          </div>
        </div>
        <div class="flex items-center gap-3">
          <Button
            label="Guardar Dimensiones"
            icon="pi pi-save"
            size="small"
            severity="secondary"
            :loading="portesStore.saving"
            @click="onSaveDimensiones"
          />
          <Button
            label="Buscar conductores compatibles"
            icon="pi pi-search"
            size="small"
            :loading="portesStore.loadingCandidatos"
            @click="onBuscarConductores"
          />
          <Button
            label="Reintentar matching automático"
            icon="pi pi-refresh"
            size="small"
            severity="contrast"
            :loading="portesStore.saving"
            @click="onRetryMatching"
          />
        </div>
      </div>

      <!-- Conductor Candidates Table -->
      <div v-if="showCandidatos" class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div class="px-5 py-3 bg-gray-50 dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700">
          <h4 class="text-sm font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide flex items-center gap-2">
            <i class="pi pi-users text-primary"></i>
            Conductores compatibles para asignación manual
            <span class="text-xs font-normal text-gray-400">({{ portesStore.conductorCandidatos.length }} encontrados)</span>
          </h4>
        </div>

        <DataTable
          :value="portesStore.conductorCandidatos"
          :loading="portesStore.loadingCandidatos"
          stripedRows
          size="small"
          :rows="10"
          :paginator="portesStore.conductorCandidatos.length > 10"
          emptyMessage="No se encontraron conductores con vehículo compatible para las medidas actuales."
        >
          <Column header="Conductor" :sortable="false">
            <template #body="{ data }">
              <div>
                <span class="font-medium text-gray-800 dark:text-gray-100">{{ data.nombre }} {{ data.apellidos ?? '' }}</span>
                <span v-if="data.ciudadBase" class="block text-xs text-gray-500 dark:text-gray-400">{{ data.ciudadBase }}</span>
              </div>
            </template>
          </Column>
          <Column field="vehiculoInfo" header="Vehículo" :sortable="false">
            <template #body="{ data }">
              <span class="text-sm text-gray-700 dark:text-gray-300">{{ data.vehiculoInfo ?? '—' }}</span>
            </template>
          </Column>
          <Column field="score" header="Score" :sortable="true" style="width: 90px">
            <template #body="{ data }">
              <span
                class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold"
                :class="data.score > 50 ? 'bg-emerald-50 text-emerald-700' : data.score > 0 ? 'bg-amber-50 text-amber-700' : 'bg-gray-100 text-gray-500'"
              >
                {{ data.score }}
              </span>
            </template>
          </Column>
          <Column header="" style="width: 100px">
            <template #body="{ data }">
              <Button
                label="Asignar manualmente (forzar)"
                icon="pi pi-check"
                size="small"
                severity="success"
                :loading="portesStore.saving"
                @click="onAsignar(data)"
              />
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- Pricing -->
      <div v-if="porte.precio" class="bg-gray-50 dark:bg-gray-900 rounded-xl p-4 flex items-center gap-6">
        <div>
          <span class="text-xs text-gray-500 dark:text-gray-400">Precio estimado</span>
          <p class="text-lg font-bold text-primary">{{ porte.precio?.toFixed(2) }} €</p>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
/* Dark mode for candidates DataTable */
.dark :deep(.p-datatable-thead > tr > th) {
  background: #1F2937;
  color: #9CA3AF;
  border-color: #374151;
}
.dark :deep(.p-datatable-tbody > tr > td) {
  border-color: #374151;
  color: #D1D5DB;
}
.dark :deep(.p-datatable-tbody > tr) {
  background: #111827;
}
.dark :deep(.p-datatable-tbody > tr:nth-child(even)) {
  background: #1F2937;
}
.dark :deep(.p-datatable-tbody > tr:hover) {
  background: #374151 !important;
}
</style>
