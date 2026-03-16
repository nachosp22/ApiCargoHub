<script setup lang="ts">
import { computed } from 'vue'
import Dialog from 'primevue/dialog'
import ProgressBar from 'primevue/progressbar'
import IncidenciaStatusBadge from './IncidenciaStatusBadge.vue'
import SeveridadBadge from './SeveridadBadge.vue'
import type { Incidencia, IncidenciaEvento, EstadoIncidencia } from '@/stores/incidencias'

interface Props {
  visible: boolean
  incidencia: Incidencia | null
  historial: IncidenciaEvento[]
  loadingHistorial?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  incidencia: null,
  loadingHistorial: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'resolve', incidencia: Incidencia): void
}>()

const isTerminal = computed(() => {
  if (!props.incidencia) return true
  return props.incidencia.estado === 'RESUELTA' || props.incidencia.estado === 'DESESTIMADA'
})

// --- SLA Computation ---
const slaData = computed(() => {
  const inc = props.incidencia
  if (!inc || !inc.fechaLimiteSla) {
    return { percentage: 0, label: '—', color: 'text-gray-400', barColor: '', show: false }
  }

  if (isTerminal.value) {
    return { percentage: 100, label: 'Finalizada', color: 'text-gray-400', barColor: '', show: false }
  }

  const now = new Date()
  const start = new Date(inc.fechaReporte)
  const limit = new Date(inc.fechaLimiteSla)
  const totalDuration = limit.getTime() - start.getTime()
  const elapsed = now.getTime() - start.getTime()

  if (totalDuration <= 0) {
    return { percentage: 100, label: 'VENCIDA', color: 'text-red-600', barColor: 'bg-red-500', show: true }
  }

  const consumed = Math.min(Math.max((elapsed / totalDuration) * 100, 0), 100)
  const remaining = limit.getTime() - now.getTime()

  if (remaining <= 0) {
    const overMs = Math.abs(remaining)
    return {
      percentage: 100,
      label: `VENCIDA · hace ${formatDuration(overMs)}`,
      color: 'text-red-600 font-bold',
      barColor: 'bg-red-500',
      show: true,
    }
  }

  const remainLabel = `${formatDuration(remaining)} restantes`

  if (consumed > 75) {
    return { percentage: Math.round(consumed), label: remainLabel, color: 'text-red-600', barColor: 'bg-red-500', show: true }
  }
  if (consumed > 50) {
    return { percentage: Math.round(consumed), label: remainLabel, color: 'text-amber-600', barColor: 'bg-amber-500', show: true }
  }

  return { percentage: Math.round(consumed), label: remainLabel, color: 'text-emerald-600', barColor: 'bg-emerald-500', show: true }
})

function formatDuration(ms: number): string {
  const totalMinutes = Math.floor(ms / (1000 * 60))
  const totalHours = Math.floor(totalMinutes / 60)
  const days = Math.floor(totalHours / 24)
  const hours = totalHours % 24
  const minutes = totalMinutes % 60

  if (days > 0) return `${days}d ${hours}h`
  if (hours > 0) return `${hours}h ${minutes}m`
  return `${minutes}m`
}

function formatDateTime(dateStr: string | null | undefined): string {
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

function getPrioridadLabel(prioridad: string): string {
  const labels: Record<string, string> = { ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja' }
  return labels[prioridad] ?? prioridad
}

function getEstadoLabel(estado: string): string {
  const labels: Record<string, string> = {
    ABIERTA: 'Abierta',
    EN_REVISION: 'En Revisión',
    RESUELTA: 'Resuelta',
    DESESTIMADA: 'Desestimada',
  }
  return labels[estado] ?? estado
}

function getTimelineIcon(accion: string): string {
  if (accion === 'CREACION') return 'pi pi-plus-circle'
  if (accion === 'RESOLUCION') return 'pi pi-check-circle'
  return 'pi pi-arrow-right-arrow-left'
}

function getTimelineColor(estadoNuevo: EstadoIncidencia): string {
  const colors: Record<string, string> = {
    ABIERTA: 'bg-red-500',
    EN_REVISION: 'bg-amber-500',
    RESUELTA: 'bg-emerald-500',
    DESESTIMADA: 'bg-gray-400',
  }
  return colors[estadoNuevo] ?? 'bg-gray-400'
}

function onClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    :header="`Detalle de Incidencia #${incidencia?.id ?? ''}`"
    :modal="true"
    :closable="true"
    :style="{ width: '750px' }"
    @update:visible="onClose"
  >
    <div v-if="incidencia" class="space-y-6 pt-2">
      <!-- Header Row -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="text-lg font-bold text-gray-800">#{{ incidencia.id }}</span>
          <IncidenciaStatusBadge :estado="incidencia.estado" />
          <SeveridadBadge :severidad="incidencia.severidad" />
        </div>
        <div v-if="!isTerminal" class="flex items-center gap-2">
          <button
            class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-emerald-700 bg-emerald-50 rounded-lg hover:bg-emerald-100 transition-colors"
            @click="emit('resolve', incidencia!)"
          >
            <i class="pi pi-check text-xs"></i>
            Resolver
          </button>
        </div>
      </div>

      <!-- Title & Description -->
      <div class="bg-gray-50 rounded-xl p-5">
        <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Descripción</h4>
        <p class="text-gray-800 font-medium mb-2">{{ incidencia.titulo }}</p>
        <p class="text-gray-600 text-sm leading-relaxed">{{ incidencia.descripcion }}</p>
      </div>

      <!-- SLA Indicator Panel -->
      <div v-if="slaData.show" class="bg-gray-50 rounded-xl p-5">
        <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Estado SLA</h4>
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm" :class="slaData.color">{{ slaData.label }}</span>
          <span class="text-xs text-gray-400">{{ slaData.percentage }}% consumido</span>
        </div>
        <ProgressBar
          :value="slaData.percentage"
          :showValue="false"
          style="height: 8px"
          :pt="{
            value: { class: slaData.percentage > 75 ? 'bg-red-500' : slaData.percentage > 50 ? 'bg-amber-500' : 'bg-emerald-500' },
          }"
        />
        <div class="flex items-center justify-between mt-2 text-xs text-gray-400">
          <span>Reporte: {{ formatDateTime(incidencia.fechaReporte) }}</span>
          <span>Límite: {{ formatDateTime(incidencia.fechaLimiteSla) }}</span>
        </div>
      </div>

      <!-- Details Grid -->
      <div class="grid grid-cols-2 gap-5">
        <div class="bg-gray-50 rounded-xl p-4">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Información</h4>
          <div class="space-y-2">
            <div class="flex justify-between">
              <span class="text-sm text-gray-500">Porte</span>
              <span class="text-sm font-medium text-blue-600">#{{ incidencia.porteId }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-sm text-gray-500">Prioridad</span>
              <span class="text-sm font-medium text-gray-700">{{ getPrioridadLabel(incidencia.prioridad) }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-sm text-gray-500">Fecha Reporte</span>
              <span class="text-sm text-gray-700">{{ formatDateTime(incidencia.fechaReporte) }}</span>
            </div>
          </div>
        </div>

        <div class="bg-gray-50 rounded-xl p-4">
          <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Resolución</h4>
          <div v-if="incidencia.resolucion" class="space-y-2">
            <p class="text-sm text-gray-700">{{ incidencia.resolucion }}</p>
            <div class="flex justify-between">
              <span class="text-sm text-gray-500">Fecha</span>
              <span class="text-sm text-gray-700">{{ formatDateTime(incidencia.fechaResolucion) }}</span>
            </div>
          </div>
          <p v-else class="text-sm text-gray-400 italic">Sin resolución aún</p>
        </div>
      </div>

      <!-- Historial / Timeline -->
      <div>
        <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-4">Historial</h4>

        <!-- Loading -->
        <div v-if="loadingHistorial" class="flex items-center justify-center py-8">
          <i class="pi pi-spin pi-spinner text-2xl text-gray-400"></i>
        </div>

        <!-- Empty -->
        <div v-else-if="historial.length === 0" class="text-center py-6">
          <i class="pi pi-history text-3xl text-gray-300 mb-2"></i>
          <p class="text-sm text-gray-400">No hay eventos registrados</p>
        </div>

        <!-- Timeline -->
        <div v-else class="relative">
          <!-- Vertical line -->
          <div class="absolute left-[15px] top-2 bottom-2 w-0.5 bg-gray-200"></div>

          <div
            v-for="(evento, index) in historial"
            :key="evento.id"
            class="relative flex gap-4 pb-6 last:pb-0"
          >
            <!-- Dot -->
            <div class="relative z-10 flex-shrink-0">
              <div
                class="w-8 h-8 rounded-full flex items-center justify-center text-white"
                :class="getTimelineColor(evento.estadoNuevo)"
              >
                <i :class="getTimelineIcon(evento.accion)" class="text-xs"></i>
              </div>
            </div>

            <!-- Content -->
            <div class="flex-1 min-w-0 pt-0.5">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-medium text-gray-800">{{ evento.accion }}</span>
                  <template v-if="evento.estadoAnterior">
                    <span class="text-xs text-gray-400">
                      {{ getEstadoLabel(evento.estadoAnterior) }}
                    </span>
                    <i class="pi pi-arrow-right text-xs text-gray-300"></i>
                  </template>
                  <span class="text-xs font-medium" :class="{
                    'text-red-600': evento.estadoNuevo === 'ABIERTA',
                    'text-amber-600': evento.estadoNuevo === 'EN_REVISION',
                    'text-emerald-600': evento.estadoNuevo === 'RESUELTA',
                    'text-gray-500': evento.estadoNuevo === 'DESESTIMADA',
                  }">
                    {{ getEstadoLabel(evento.estadoNuevo) }}
                  </span>
                </div>
                <span class="text-xs text-gray-400 flex-shrink-0">
                  {{ formatDateTime(evento.fecha) }}
                </span>
              </div>
              <p v-if="evento.comentario" class="text-sm text-gray-600 mt-1">
                {{ evento.comentario }}
              </p>
              <p v-if="evento.actorId" class="text-xs text-gray-400 mt-0.5">
                Actor ID: {{ evento.actorId }}
              </p>
            </div>

            <!-- Suppress unused index warning -->
            <span class="hidden">{{ index }}</span>
          </div>
        </div>
      </div>
    </div>
  </Dialog>
</template>
