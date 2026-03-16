<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useConductoresStore } from '@/stores/conductores'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import ConductorTable from '@/components/conductores/ConductorTable.vue'
import ConductorDialog from '@/components/conductores/ConductorDialog.vue'
import type { Conductor, CreateConductorRequest } from '@/stores/conductores'

const conductoresStore = useConductoresStore()
const toast = useToast()

// --- Dialog state ---
const showDialog = ref(false)
const editingConductor = ref<Conductor | null>(null)

// --- Detail panel state ---
const showDetail = ref(false)
const detailConductor = ref<Conductor | null>(null)

// --- Toggle confirmation ---
const showToggleConfirm = ref(false)
const togglingConductor = ref<Conductor | null>(null)

// --- Lifecycle ---
onMounted(async () => {
  await conductoresStore.fetchConductores()
})

// --- Handlers ---

function onNewConductor(): void {
  editingConductor.value = null
  showDialog.value = true
}

function onEditConductor(conductor: Conductor): void {
  editingConductor.value = conductor
  showDialog.value = true
}

function onViewConductor(conductor: Conductor): void {
  detailConductor.value = conductor
  showDetail.value = true
}

function onConfirmToggle(conductor: Conductor): void {
  togglingConductor.value = conductor
  showToggleConfirm.value = true
}

async function onSaveConductor(data: CreateConductorRequest): Promise<void> {
  try {
    if (editingConductor.value) {
      await conductoresStore.updateConductor(editingConductor.value.id, {
        nombre: data.nombre,
        apellidos: data.apellidos,
        telefono: data.telefono,
        ciudadBase: data.ciudadBase,
      })
      toast.add({
        severity: 'success',
        summary: 'Conductor actualizado',
        detail: `El conductor #${editingConductor.value.id} se ha actualizado correctamente.`,
        life: 3000,
      })
    } else {
      const created = await conductoresStore.createConductor(data)
      toast.add({
        severity: 'success',
        summary: 'Conductor creado',
        detail: `El conductor #${created.id} se ha creado correctamente.`,
        life: 3000,
      })
    }
    showDialog.value = false
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo guardar el conductor. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}

async function onToggleEstado(): Promise<void> {
  if (!togglingConductor.value) return
  try {
    const conductor = togglingConductor.value
    const wasActive = conductor.estado === 'ACTIVO'
    await conductoresStore.toggleEstado(conductor.id)
    toast.add({
      severity: 'success',
      summary: wasActive ? 'Conductor desactivado' : 'Conductor activado',
      detail: wasActive
        ? `${conductor.nombre} ${conductor.apellidos} ha sido desactivado.`
        : `${conductor.nombre} ${conductor.apellidos} ha sido activado.`,
      life: 3000,
    })
    showToggleConfirm.value = false
    togglingConductor.value = null
    // Also update detail if viewing the toggled conductor
    if (detailConductor.value?.id === conductor.id) {
      detailConductor.value = conductoresStore.conductores.find((c) => c.id === conductor.id) ?? null
    }
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'No se pudo cambiar el estado del conductor. Inténtalo de nuevo.',
      life: 5000,
    })
  }
}

function getFullName(conductor: Conductor): string {
  return `${conductor.nombre} ${conductor.apellidos}`.trim()
}

function getInitials(conductor: Conductor): string {
  const parts = getFullName(conductor).split(' ')
  return parts
    .slice(0, 2)
    .map((s) => s.charAt(0).toUpperCase())
    .join('')
}

function getRatingStars(rating: number): string {
  const rounded = Math.round(rating * 2) / 2
  const full = Math.floor(rounded)
  const half = rounded % 1 >= 0.5 ? 1 : 0
  return '★'.repeat(full) + (half ? '½' : '') + '☆'.repeat(5 - full - half)
}

type StyleConfig = {
  bg: string
  text: string
  ring: string
  label: string
}

const estadoConfig: Record<string, StyleConfig> = {
  ACTIVO: { bg: 'bg-emerald-50', text: 'text-emerald-700', ring: 'ring-emerald-600/20', label: 'Activo' },
  INACTIVO: { bg: 'bg-gray-50', text: 'text-gray-600', ring: 'ring-gray-500/20', label: 'Inactivo' },
  SUSPENDIDO: { bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-600/20', label: 'Suspendido' },
}

const defaultConfig: StyleConfig = {
  bg: 'bg-gray-50',
  text: 'text-gray-600',
  ring: 'ring-gray-500/20',
  label: '',
}

function getEstadoConfig(estado: string): StyleConfig {
  return estadoConfig[estado] ?? { ...defaultConfig, label: estado }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-green-50 text-green-600 flex items-center justify-center">
          <i class="pi pi-users text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800">Conductores</h1>
          <p class="text-sm text-gray-500 mt-0.5">Gestión de conductores y asignaciones</p>
        </div>
      </div>
      <Button
        label="Nuevo Conductor"
        icon="pi pi-plus"
        @click="onNewConductor"
      />
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="conductoresStore.usingMockData"
      class="flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-4 gap-4">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
            <i class="pi pi-users text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ conductoresStore.totalConductores }}</p>
            <p class="text-xs text-gray-500">Total</p>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <i class="pi pi-check-circle text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ conductoresStore.activos.length }}</p>
            <p class="text-xs text-gray-500">Activos</p>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-green-50 text-green-600 flex items-center justify-center">
            <i class="pi pi-map-marker text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ conductoresStore.disponibles.length }}</p>
            <p class="text-xs text-gray-500">Disponibles</p>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-gray-50 text-gray-600 flex items-center justify-center">
            <i class="pi pi-ban text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800">{{ (conductoresStore.conductoresByEstado['INACTIVO'] ?? 0) + (conductoresStore.conductoresByEstado['SUSPENDIDO'] ?? 0) }}</p>
            <p class="text-xs text-gray-500">Inactivos</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Data Table -->
    <ConductorTable
      :conductores="conductoresStore.conductores"
      :loading="conductoresStore.loading"
      @view="onViewConductor"
      @edit="onEditConductor"
      @toggle-estado="onConfirmToggle"
    />

    <!-- Create/Edit Dialog -->
    <ConductorDialog
      v-model:visible="showDialog"
      :conductor="editingConductor"
      :saving="conductoresStore.saving"
      @save="onSaveConductor"
    />

    <!-- Detail Panel Dialog -->
    <Dialog
      v-model:visible="showDetail"
      :header="`Detalle del Conductor #${detailConductor?.id ?? ''}`"
      :modal="true"
      :closable="true"
      :style="{ width: '700px' }"
    >
      <div v-if="detailConductor" class="space-y-6 pt-2">
        <!-- Header with Avatar + Status -->
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <div
              class="w-16 h-16 rounded-full flex items-center justify-center"
              :class="detailConductor.estado === 'ACTIVO' ? 'bg-primary/10' : 'bg-gray-100'"
            >
              <span
                class="text-xl font-bold"
                :class="detailConductor.estado === 'ACTIVO' ? 'text-primary' : 'text-gray-400'"
              >
                {{ getInitials(detailConductor) }}
              </span>
            </div>
            <div>
              <h3 class="text-lg font-bold text-gray-800">{{ getFullName(detailConductor) }}</h3>
              <p class="text-sm text-gray-500">{{ detailConductor.ciudadBase || 'Sin ciudad base' }}</p>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <span
              class="inline-flex items-center px-3 py-1.5 rounded-full text-xs font-medium ring-1 ring-inset"
              :class="[
                getEstadoConfig(detailConductor.estado).bg,
                getEstadoConfig(detailConductor.estado).text,
                getEstadoConfig(detailConductor.estado).ring,
              ]"
            >
              {{ getEstadoConfig(detailConductor.estado).label }}
            </span>
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              size="small"
              v-tooltip.top="'Editar'"
              @click="showDetail = false; onEditConductor(detailConductor!)"
            />
          </div>
        </div>

        <!-- Contact Info -->
        <div class="bg-gray-50 rounded-xl p-5">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Información de Contacto</h4>
          <div class="grid grid-cols-2 gap-4">
            <div class="flex items-center gap-3">
              <i class="pi pi-envelope text-gray-400"></i>
              <div>
                <span class="text-xs text-gray-500">Email</span>
                <p class="text-gray-800 text-sm font-medium">{{ detailConductor.email || '—' }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <i class="pi pi-phone text-gray-400"></i>
              <div>
                <span class="text-xs text-gray-500">Teléfono</span>
                <p class="text-gray-800 text-sm font-medium">{{ detailConductor.telefono || '—' }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <i class="pi pi-id-card text-gray-400"></i>
              <div>
                <span class="text-xs text-gray-500">DNI / Licencia</span>
                <p class="text-gray-800 text-sm font-medium font-mono">{{ detailConductor.dni || '—' }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <i class="pi pi-map-marker text-gray-400"></i>
              <div>
                <span class="text-xs text-gray-500">Ciudad Base</span>
                <p class="text-gray-800 text-sm font-medium">{{ detailConductor.ciudadBase || '—' }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Stats -->
        <div class="grid grid-cols-3 gap-4">
          <div class="bg-blue-50 rounded-xl p-4 text-center">
            <p class="text-2xl font-bold text-blue-700">{{ detailConductor.portesAsignados }}</p>
            <p class="text-xs text-blue-600 mt-0.5">Portes Asignados</p>
          </div>
          <div class="bg-amber-50 rounded-xl p-4 text-center">
            <p class="text-2xl font-bold text-amber-700">{{ detailConductor.rating.toFixed(1) }}</p>
            <p class="text-xs text-amber-600 mt-0.5">
              {{ getRatingStars(detailConductor.rating) }}
              <span class="text-gray-400 ml-1">({{ detailConductor.numeroValoraciones }})</span>
            </p>
          </div>
          <div class="bg-green-50 rounded-xl p-4 text-center">
            <p class="text-2xl font-bold text-green-700">{{ detailConductor.radioAccionKm }} km</p>
            <p class="text-xs text-green-600 mt-0.5">Radio de Acción</p>
          </div>
        </div>

        <!-- Work Preferences -->
        <div class="bg-gray-50 rounded-xl p-5">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Preferencias</h4>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <span class="text-xs text-gray-500">Días Laborables</span>
              <div class="flex gap-1 mt-1">
                <span
                  v-for="day in ['L', 'M', 'X', 'J', 'V', 'S', 'D']"
                  :key="day"
                  class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-medium"
                  :class="
                    detailConductor.diasLaborables.includes(String(['L', 'M', 'X', 'J', 'V', 'S', 'D'].indexOf(day) + 1))
                      ? 'bg-primary text-white'
                      : 'bg-gray-200 text-gray-400'
                  "
                >
                  {{ day }}
                </span>
              </div>
            </div>
            <div>
              <span class="text-xs text-gray-500">Disponibilidad</span>
              <p class="mt-1">
                <span
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
                  :class="detailConductor.disponible
                    ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20'
                    : 'bg-red-50 text-red-700 ring-red-600/20'
                  "
                >
                  {{ detailConductor.disponible ? 'Disponible' : 'No disponible' }}
                </span>
              </p>
            </div>
          </div>
        </div>
      </div>
    </Dialog>

    <!-- Toggle Estado Confirmation Dialog -->
    <Dialog
      v-model:visible="showToggleConfirm"
      :header="togglingConductor?.estado === 'ACTIVO' ? 'Desactivar Conductor' : 'Activar Conductor'"
      :modal="true"
      :closable="true"
      :style="{ width: '450px' }"
    >
      <div class="flex items-start gap-4 py-2">
        <div
          class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
          :class="togglingConductor?.estado === 'ACTIVO' ? 'bg-red-50' : 'bg-emerald-50'"
        >
          <i
            :class="togglingConductor?.estado === 'ACTIVO'
              ? 'pi pi-ban text-red-500'
              : 'pi pi-check-circle text-emerald-500'
            "
          ></i>
        </div>
        <div>
          <p class="text-gray-800 font-medium">
            {{ togglingConductor?.estado === 'ACTIVO'
              ? `¿Desactivar a ${togglingConductor?.nombre} ${togglingConductor?.apellidos}?`
              : `¿Activar a ${togglingConductor?.nombre} ${togglingConductor?.apellidos}?`
            }}
          </p>
          <p class="text-sm text-gray-500 mt-1">
            {{ togglingConductor?.estado === 'ACTIVO'
              ? 'El conductor no podrá recibir nuevos portes y se bloqueará su acceso al sistema.'
              : 'El conductor podrá volver a recibir portes y acceder al sistema.'
            }}
          </p>
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3">
          <Button
            label="Cancelar"
            severity="secondary"
            text
            @click="showToggleConfirm = false"
          />
          <Button
            :label="togglingConductor?.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'"
            :severity="togglingConductor?.estado === 'ACTIVO' ? 'danger' : 'success'"
            :icon="togglingConductor?.estado === 'ACTIVO' ? 'pi pi-ban' : 'pi pi-check-circle'"
            :loading="conductoresStore.saving"
            @click="onToggleEstado"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
