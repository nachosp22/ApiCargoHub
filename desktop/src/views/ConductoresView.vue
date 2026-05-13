<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConductoresStore } from '@/stores/conductores'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import ConductorTable from '@/components/conductores/ConductorTable.vue'
import ConductorDialog from '@/components/conductores/ConductorDialog.vue'
import ConductorStatsChart from '@/components/conductores/ConductorStatsChart.vue'
import type { Conductor, CreateConductorRequest } from '@/stores/conductores'

const conductoresStore = useConductoresStore()
const toast = useToast()
const route = useRoute()
const router = useRouter()

// --- Dialog state ---
const showDialog = ref(false)
const editingConductor = ref<Conductor | null>(null)

// --- Detail panel state ---
const showDetail = ref(false)
const detailConductor = ref<Conductor | null>(null)
const activeTab = ref(0)
const dateSpecificBlocks = computed(() =>
  conductoresStore.detailAgenda
    .filter((bloqueo) => bloqueo.diaSemana == null)
    .map((bloqueo) => {
      const isSingleDay = isSameCalendarDay(bloqueo.fechaInicio, bloqueo.fechaFin)
      return {
        ...bloqueo,
        blockKindLabel: isSingleDay ? 'Día puntual' : 'Intervalo',
        blockDateLabel: isSingleDay
          ? formatDisplayDate(bloqueo.fechaInicio)
          : `${formatDisplayDate(bloqueo.fechaInicio)} — ${formatDisplayDate(bloqueo.fechaFin)}`,
      }
    })
)

// --- Load tab data when detail opens or tab changes ---
watch([showDetail, activeTab], ([visible, tab]) => {
  if (!visible || !detailConductor.value) return
  const id = detailConductor.value.id
  switch (tab) {
    case 1: conductoresStore.fetchAgenda(id); break
    case 2: conductoresStore.fetchVehiculos(id); break
    case 3: conductoresStore.fetchEstadisticas(id); break
    case 4: conductoresStore.fetchPortesConductor(id); break
  }
})

// --- Toggle confirmation ---
const showToggleConfirm = ref(false)
const togglingConductor = ref<Conductor | null>(null)

// --- Lifecycle ---
onMounted(async () => {
  await conductoresStore.fetchConductores()
  await openConductorFromQuery()
})

watch(() => route.query.conductorId, () => {
  void openConductorFromQuery()
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
  activeTab.value = 0
  conductoresStore.clearDetail()
  showDetail.value = true
}

async function openConductorFromQuery(): Promise<void> {
  const rawId = route.query.conductorId
  const conductorId = typeof rawId === 'string' ? Number.parseInt(rawId, 10) : NaN
  if (Number.isNaN(conductorId)) return

  const conductor = await conductoresStore.fetchConductorById(conductorId)
  if (conductor) {
    onViewConductor(conductor)
    await clearQueryParam('conductorId')
  }
}

async function clearQueryParam(param: string): Promise<void> {
  const nextQuery = { ...route.query }
  delete nextQuery[param]
  await router.replace({ query: nextQuery })
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

type StyleConfig = {
  bg: string
  text: string
  ring: string
  label: string
}

const estadoConfig: Record<string, StyleConfig> = {
  ACTIVO: { bg: 'bg-emerald-50 dark:bg-emerald-900/30', text: 'text-emerald-700 dark:text-emerald-400', ring: 'ring-emerald-600/20 dark:ring-emerald-500/30', label: 'Activo' },
  INACTIVO: { bg: 'bg-gray-50 dark:bg-gray-700', text: 'text-gray-600 dark:text-gray-300', ring: 'ring-gray-500/20 dark:ring-gray-400/30', label: 'Inactivo' },
  SUSPENDIDO: { bg: 'bg-red-50 dark:bg-red-900/30', text: 'text-red-700 dark:text-red-400', ring: 'ring-red-600/20 dark:ring-red-500/30', label: 'Suspendido' },
}

const defaultConfig: StyleConfig = {
  bg: 'bg-gray-50 dark:bg-gray-700',
  text: 'text-gray-600 dark:text-gray-300',
  ring: 'ring-gray-500/20 dark:ring-gray-400/30',
  label: '',
}

function getEstadoConfig(estado: string): StyleConfig {
  return estadoConfig[estado] ?? { ...defaultConfig, label: estado }
}

function getPortesSeverity(estado: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | undefined {
  const map: Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'> = {
    COMPLETADO: 'success',
    ENTREGADO: 'success',
    EN_RUTA: 'info',
    PENDIENTE: 'warn',
    PROGRAMADO: 'secondary',
    CANCELADO: 'danger',
  }
  return map[estado]
}

type DataTableRowClickEvent<T> = {
  data: T
}

async function onOpenVehiculoDetailFromConductor(event: DataTableRowClickEvent<{ id: number }>): Promise<void> {
  const vehiculoId = event.data?.id
  if (!Number.isFinite(vehiculoId)) return

  showDetail.value = false
  await router.push({ path: '/vehiculos', query: { vehiculoId: String(vehiculoId) } })
}

async function onOpenPorteDetailFromConductor(event: DataTableRowClickEvent<{ id: number }>): Promise<void> {
  const porteId = event.data?.id
  if (!Number.isFinite(porteId)) return

  showDetail.value = false
  await router.push({ path: '/portes', query: { porteId: String(porteId) } })
}

function formatDisplayDate(value: string): string {
  if (!value || value === '—') return '—'
  const parsed = parseCalendarDate(value)
  if (Number.isNaN(parsed.getTime())) return value

  return new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(parsed)
}

function parseCalendarDate(value: string): Date {
  const raw = value.trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return new Date(`${raw}T00:00:00`)
  const parsed = new Date(raw)
  if (Number.isNaN(parsed.getTime())) return parsed
  return new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate())
}

function isSameCalendarDay(start: string, end: string): boolean {
  const startDate = parseCalendarDate(start)
  const endDate = parseCalendarDate(end)
  if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) {
    return start === end
  }

  return startDate.getTime() === endDate.getTime()
}
</script>

<template>
  <div class="h-full min-h-0 flex flex-col gap-6 overflow-hidden">
    <!-- Page Header -->
    <div class="shrink-0 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl bg-green-50 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center">
          <i class="pi pi-users text-2xl"></i>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Conductores</h1>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Gestión de conductores y asignaciones</p>
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
      class="shrink-0 flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- Stats Cards -->
    <div class="shrink-0 grid grid-cols-4 gap-4">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 flex items-center justify-center">
            <i class="pi pi-users text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800 dark:text-gray-100">{{ conductoresStore.totalConductores }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">Total</p>
          </div>
        </div>
      </div>
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-emerald-50 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
            <i class="pi pi-check-circle text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800 dark:text-gray-100">{{ conductoresStore.activos.length }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">Activos</p>
          </div>
        </div>
      </div>
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-green-50 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center">
            <i class="pi pi-map-marker text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800 dark:text-gray-100">{{ conductoresStore.disponibles.length }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">Disponibles</p>
          </div>
        </div>
      </div>
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-300 flex items-center justify-center">
            <i class="pi pi-ban text-lg"></i>
          </div>
          <div>
            <p class="text-2xl font-bold text-gray-800 dark:text-gray-100">{{ (conductoresStore.conductoresByEstado['INACTIVO'] ?? 0) + (conductoresStore.conductoresByEstado['SUSPENDIDO'] ?? 0) }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">Inactivos</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Data Table -->
    <div class="flex-1 min-h-0 overflow-auto">
      <ConductorTable
        :conductores="conductoresStore.conductores"
        :loading="conductoresStore.loading"
        @view="onViewConductor"
        @edit="onEditConductor"
        @toggle-estado="onConfirmToggle"
      />
    </div>

    <!-- Create/Edit Dialog -->
    <ConductorDialog
      v-model:visible="showDialog"
      :conductor="editingConductor"
      :saving="conductoresStore.saving"
      @save="onSaveConductor"
    />

    <!-- Detail Panel Dialog with Tabs -->
    <Dialog
      v-model:visible="showDetail"
      :header="`Detalle del Conductor #${detailConductor?.id ?? ''}`"
      :modal="true"
      :closable="true"
      :style="{ width: '850px' }"
    >
      <div v-if="detailConductor" class="space-y-4 pt-2">
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
              <h3 class="text-lg font-bold text-gray-800 dark:text-gray-100">{{ getFullName(detailConductor) }}</h3>
              <p class="text-sm text-gray-500 dark:text-gray-400">{{ detailConductor.ciudadBase || 'Sin ciudad base' }}</p>
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

        <!-- Tabs -->
        <TabView v-model:activeIndex="activeTab">
          <!-- Tab 0: Info general -->
          <TabPanel value="0" header="Información">
            <div class="space-y-5 pt-3">
              <!-- Contact Info -->
              <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-5">
                <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-3">Información de Contacto</h4>
                <div class="grid grid-cols-2 gap-4">
                  <div class="flex items-center gap-3">
                    <i class="pi pi-envelope text-gray-400 dark:text-gray-500"></i>
                    <div>
                      <span class="text-xs text-gray-500 dark:text-gray-400">Email</span>
                      <p class="text-gray-800 dark:text-gray-100 text-sm font-medium">{{ detailConductor.email || '—' }}</p>
                    </div>
                  </div>
                  <div class="flex items-center gap-3">
                    <i class="pi pi-phone text-gray-400 dark:text-gray-500"></i>
                    <div>
                      <span class="text-xs text-gray-500 dark:text-gray-400">Teléfono</span>
                      <p class="text-gray-800 dark:text-gray-100 text-sm font-medium">{{ detailConductor.telefono || '—' }}</p>
                    </div>
                  </div>
                  <div class="flex items-center gap-3">
                    <i class="pi pi-id-card text-gray-400 dark:text-gray-500"></i>
                    <div>
                      <span class="text-xs text-gray-500 dark:text-gray-400">DNI / Licencia</span>
                      <p class="text-gray-800 dark:text-gray-100 text-sm font-medium font-mono">{{ detailConductor.dni || '—' }}</p>
                    </div>
                  </div>
                  <div class="flex items-center gap-3">
                    <i class="pi pi-map-marker text-gray-400 dark:text-gray-500"></i>
                    <div>
                      <span class="text-xs text-gray-500 dark:text-gray-400">Ciudad Base</span>
                      <p class="text-gray-800 dark:text-gray-100 text-sm font-medium">{{ detailConductor.ciudadBase || '—' }}</p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Stats -->
              <div class="grid grid-cols-2 gap-4">
                <div class="bg-blue-50 rounded-xl p-4 text-center">
                  <p class="text-2xl font-bold text-blue-700">{{ detailConductor.portesAsignados }}</p>
                  <p class="text-xs text-blue-600 mt-0.5">Portes Asignados</p>
                </div>
                <div class="bg-green-50 rounded-xl p-4 text-center">
                  <p class="text-2xl font-bold text-green-700">{{ detailConductor.radioAccionKm }} km</p>
                  <p class="text-xs text-green-600 mt-0.5">Radio de Acción</p>
                </div>
              </div>

              <!-- Work Preferences -->
              <div class="bg-gray-50 dark:bg-gray-900 rounded-xl p-5">
                <h4 class="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-3">Preferencias</h4>
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <span class="text-xs text-gray-500 dark:text-gray-400">Días Laborables</span>
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
                    <span class="text-xs text-gray-500 dark:text-gray-400">Disponibilidad</span>
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
          </TabPanel>

          <!-- Tab 1: Agenda -->
          <TabPanel value="1" header="Agenda">
            <div class="pt-3">
              <div v-if="conductoresStore.detailLoading" class="flex items-center justify-center py-8">
                <i class="pi pi-spin pi-spinner text-2xl text-gray-400"></i>
              </div>
              <div v-else-if="dateSpecificBlocks.length === 0" class="text-center py-8 text-gray-400">
                <i class="pi pi-calendar-times text-3xl mb-2"></i>
                <p class="text-sm">No hay vacaciones, descansos ni bloqueos por fecha programados</p>
              </div>
              <div v-else class="space-y-4">
                <section class="space-y-2">
                  <div class="flex items-center justify-between">
                    <h4 class="text-sm font-semibold text-gray-800 dark:text-gray-100">Vacaciones, descansos y ausencias</h4>
                    <span class="text-xs text-gray-500 dark:text-gray-400">{{ dateSpecificBlocks.length }} registro(s)</span>
                  </div>

                  <div v-if="dateSpecificBlocks.length === 0" class="rounded-lg border border-dashed border-gray-300 dark:border-gray-700 py-4 px-3 text-center text-sm text-gray-500 dark:text-gray-400">
                    No hay vacaciones, descansos ni bloqueos por fecha programados
                  </div>

                  <div v-else class="space-y-2">
                    <article
                      v-for="bloqueo in dateSpecificBlocks"
                      :key="`date-${bloqueo.id}`"
                      class="rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900/70 px-4 py-3"
                    >
                      <div class="flex items-start justify-between gap-3">
                        <div class="min-w-0">
                          <div class="flex items-center gap-2 flex-wrap">
                            <Tag :value="bloqueo.blockKindLabel" severity="secondary" class="text-[11px]" />
                            <Tag :value="bloqueo.tipo" severity="warn" class="text-[11px]" />
                          </div>
                          <p class="mt-1 text-sm font-medium text-gray-800 dark:text-gray-100">{{ bloqueo.blockDateLabel }}</p>
                          <p class="text-xs text-gray-500 dark:text-gray-400 truncate">{{ bloqueo.descripcion }}</p>
                        </div>
                      </div>
                    </article>
                  </div>
                </section>
              </div>
            </div>
          </TabPanel>

          <!-- Tab 2: Vehículos -->
          <TabPanel value="2" header="Vehículos">
            <div class="pt-3">
              <div v-if="conductoresStore.detailLoading" class="flex items-center justify-center py-8">
                <i class="pi pi-spin pi-spinner text-2xl text-gray-400"></i>
              </div>
              <div v-else-if="conductoresStore.detailVehiculos.length === 0" class="text-center py-8 text-gray-400">
                <i class="pi pi-car text-3xl mb-2"></i>
                <p class="text-sm">Sin vehículos asignados</p>
              </div>
              <DataTable
                v-else
                :value="conductoresStore.detailVehiculos"
                size="small"
                stripedRows
                class="text-sm"
                :rowHover="true"
                @row-click="onOpenVehiculoDetailFromConductor"
                :pt="{ bodyRow: { style: 'cursor: pointer' } }"
              >
                <Column field="matricula" header="Matrícula">
                  <template #body="{ data }">
                    <span class="font-medium text-primary hover:underline" title="Abrir detalle del vehículo">
                      {{ data.matricula }}
                    </span>
                  </template>
                </Column>
                <Column field="marca" header="Marca" />
                <Column field="modelo" header="Modelo" />
                <Column field="tipoVehiculo" header="Tipo" />
                <Column header="Estado">
                  <template #body="{ data }">
                    <Tag
                      :value="data.activo ? 'Activo' : 'Inactivo'"
                      :severity="data.activo ? 'success' : 'danger'"
                      class="text-xs"
                    />
                  </template>
                </Column>
              </DataTable>
            </div>
          </TabPanel>

          <!-- Tab 3: Estadísticas -->
          <TabPanel value="3" header="Estadísticas">
            <div class="pt-3">
              <div v-if="conductoresStore.detailLoading" class="flex items-center justify-center py-8">
                <i class="pi pi-spin pi-spinner text-2xl text-gray-400"></i>
              </div>
              <div v-else-if="!conductoresStore.detailEstadisticas" class="text-center py-8 text-gray-400">
                <i class="pi pi-chart-bar text-3xl mb-2"></i>
                <p class="text-sm">Sin datos de estadísticas</p>
              </div>
              <div v-else class="space-y-5">
                <!-- KPI Cards -->
                <div class="grid grid-cols-3 gap-4">
                  <div class="bg-blue-50 rounded-xl p-4 text-center">
                    <p class="text-2xl font-bold text-blue-700">{{ conductoresStore.detailEstadisticas.portesCompletados }}</p>
                    <p class="text-xs text-blue-600 mt-0.5">Portes Completados</p>
                  </div>
                  <div class="bg-emerald-50 rounded-xl p-4 text-center">
                    <p class="text-2xl font-bold text-emerald-700">{{ conductoresStore.detailEstadisticas.ingresoTotal.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' }) }}</p>
                    <p class="text-xs text-emerald-600 mt-0.5">Ingreso Total</p>
                  </div>
                  <div class="bg-indigo-50 rounded-xl p-4 text-center">
                    <p class="text-2xl font-bold text-indigo-700">{{ conductoresStore.detailEstadisticas.mediaPorPorte.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' }) }}</p>
                    <p class="text-xs text-indigo-600 mt-0.5">Media por Porte</p>
                  </div>
                </div>

                <!-- Mini chart -->
                <ConductorStatsChart :ingreso-por-mes="conductoresStore.detailEstadisticas.ingresoPorMes" />
              </div>
            </div>
          </TabPanel>

          <!-- Tab 4: Portes -->
          <TabPanel value="4" header="Portes">
            <div class="pt-3">
              <div v-if="conductoresStore.detailLoading" class="flex items-center justify-center py-8">
                <i class="pi pi-spin pi-spinner text-2xl text-gray-400"></i>
              </div>
              <div v-else-if="conductoresStore.detailPortes.length === 0" class="text-center py-8 text-gray-400">
                <i class="pi pi-truck text-3xl mb-2"></i>
                <p class="text-sm">Sin portes registrados</p>
              </div>
              <DataTable
                v-else
                :value="conductoresStore.detailPortes"
                size="small"
                stripedRows
                class="text-sm"
                :paginator="conductoresStore.detailPortes.length > 10"
                :rows="10"
                :rowHover="true"
                @row-click="onOpenPorteDetailFromConductor"
                :pt="{ bodyRow: { style: 'cursor: pointer' } }"
              >
                <Column field="id" header="ID" style="width: 60px">
                  <template #body="{ data }">
                    <span class="font-medium text-primary hover:underline" title="Abrir detalle del porte">
                      #{{ data.id }}
                    </span>
                  </template>
                </Column>
                <Column header="Estado" style="width: 120px">
                  <template #body="{ data }">
                    <Tag :value="data.estado" :severity="getPortesSeverity(data.estado)" class="text-xs" />
                  </template>
                </Column>
                <Column field="origen" header="Origen" />
                <Column field="destino" header="Destino" />
                <Column field="fecha" header="Fecha" style="width: 110px" />
                <Column header="Precio" style="width: 100px">
                  <template #body="{ data }">
                    {{ data.precio.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' }) }}
                  </template>
                </Column>
              </DataTable>
            </div>
          </TabPanel>
        </TabView>
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
          <p class="text-gray-800 dark:text-gray-100 font-medium">
            {{ togglingConductor?.estado === 'ACTIVO'
              ? `¿Desactivar a ${togglingConductor?.nombre} ${togglingConductor?.apellidos}?`
              : `¿Activar a ${togglingConductor?.nombre} ${togglingConductor?.apellidos}?`
            }}
          </p>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
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
