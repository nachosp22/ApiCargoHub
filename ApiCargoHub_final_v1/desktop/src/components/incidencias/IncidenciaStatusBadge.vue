<script setup lang="ts">
import type { EstadoIncidencia } from '@/stores/incidencias'

interface Props {
  estado: EstadoIncidencia | string
}

defineProps<Props>()

type StyleConfig = {
  bg: string
  text: string
  ring: string
  label: string
}

const estadoConfig: Record<string, StyleConfig> = {
  ABIERTA: { bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-600/20', label: 'Abierta' },
  EN_REVISION: { bg: 'bg-amber-50', text: 'text-amber-700', ring: 'ring-amber-600/20', label: 'En Revisión' },
  RESUELTA: { bg: 'bg-emerald-50', text: 'text-emerald-700', ring: 'ring-emerald-600/20', label: 'Resuelta' },
  DESESTIMADA: { bg: 'bg-gray-50', text: 'text-gray-600', ring: 'ring-gray-500/20', label: 'Desestimada' },
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
