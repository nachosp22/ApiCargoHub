<script setup lang="ts">
import { computed } from 'vue'
import Dialog from 'primevue/dialog'
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

type StyleConfig = { bg: string; text: string; ring: string; label: string }

function getPrioridadConfig(prioridad: string): StyleConfig {
  const configs: Record<string, StyleConfig> = {
    ALTA: { bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-600/20', label: 'Alta' },
    MEDIA: { bg: 'bg-orange-50', text: 'text-orange-700', ring: 'ring-orange-600/20', label: 'Media' },
    BAJA: { bg: 'bg-yellow-50', text: 'text-yellow-700', ring: 'ring-yellow-600/20', label: 'Baja' },
  }
  return configs[prioridad] ?? { bg: 'bg-gray-50', text: 'text-gray-600', ring: 'ring-gray-500/20', label: prioridad }
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
            class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-amber-700 bg-amber-50 rounded-lg hover:bg-amber-100 transition-colors"
            @click="emit('resolve', incidencia!)"
          >
            <i class="pi pi-pencil text-xs"></i>
            Editar
          </button>
        </div>
      </div>

      <!-- Title & Description -->
      <div class="bg-gray-50 rounded-xl p-5">
        <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Descripción</h4>
        <p class="text-gray-800 font-medium mb-2">{{ incidencia.titulo }}</p>
        <p class="text-gray-600 text-sm leading-relaxed">{{ incidencia.descripcion }}</p>
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
              <span
                class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
                :class="[getPrioridadConfig(incidencia.prioridad).bg, getPrioridadConfig(incidencia.prioridad).text, getPrioridadConfig(incidencia.prioridad).ring]"
              >
                {{ getPrioridadConfig(incidencia.prioridad).label }}
              </span>
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
