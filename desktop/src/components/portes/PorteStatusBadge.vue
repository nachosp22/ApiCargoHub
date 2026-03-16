<script setup lang="ts">
import type { EstadoPorte } from '@/stores/portes'

interface Props {
  estado: EstadoPorte | string
}

defineProps<Props>()

type StyleConfig = {
  bg: string
  text: string
  ring: string
  label: string
}

const estadoConfig: Record<string, StyleConfig> = {
  // Green: completed/delivered
  ENTREGADO: { bg: 'bg-emerald-50', text: 'text-emerald-700', ring: 'ring-emerald-600/20', label: 'Entregado' },
  COMPLETADO: { bg: 'bg-emerald-50', text: 'text-emerald-700', ring: 'ring-emerald-600/20', label: 'Completado' },

  // Yellow/Amber: in transit/loading
  EN_TRANSITO: { bg: 'bg-amber-50', text: 'text-amber-700', ring: 'ring-amber-600/20', label: 'En Tránsito' },
  EN_RUTA: { bg: 'bg-amber-50', text: 'text-amber-700', ring: 'ring-amber-600/20', label: 'En Ruta' },
  EN_CARGA: { bg: 'bg-amber-50', text: 'text-amber-700', ring: 'ring-amber-600/20', label: 'En Carga' },
  EN_DESCARGA: { bg: 'bg-amber-50', text: 'text-amber-700', ring: 'ring-amber-600/20', label: 'En Descarga' },

  // Blue: scheduled/assigned
  PROGRAMADO: { bg: 'bg-blue-50', text: 'text-blue-700', ring: 'ring-blue-600/20', label: 'Programado' },
  ASIGNADO: { bg: 'bg-blue-50', text: 'text-blue-700', ring: 'ring-blue-600/20', label: 'Asignado' },

  // Orange: pending
  PENDIENTE: { bg: 'bg-orange-50', text: 'text-orange-700', ring: 'ring-orange-600/20', label: 'Pendiente' },

  // Gray: cancelled
  CANCELADO: { bg: 'bg-gray-50', text: 'text-gray-600', ring: 'ring-gray-500/20', label: 'Cancelado' },

  // Red: incident
  INCIDENCIA: { bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-600/20', label: 'Incidencia' },

  // Indigo: billed
  FACTURADO: { bg: 'bg-indigo-50', text: 'text-indigo-700', ring: 'ring-indigo-600/20', label: 'Facturado' },
}

const defaultConfig: StyleConfig = {
  bg: 'bg-gray-50',
  text: 'text-gray-600',
  ring: 'ring-gray-500/20',
  label: '',
}

function getConfig(estado: string): StyleConfig {
  return estadoConfig[estado] ?? { ...defaultConfig, label: estado }
}
</script>

<template>
  <span
    class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
    :class="[getConfig(estado).bg, getConfig(estado).text, getConfig(estado).ring]"
  >
    {{ getConfig(estado).label }}
  </span>
</template>
