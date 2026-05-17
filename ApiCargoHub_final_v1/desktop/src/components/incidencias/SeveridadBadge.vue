<script setup lang="ts">
import type { SeveridadIncidencia } from '@/stores/incidencias'

interface Props {
  severidad: SeveridadIncidencia | string
}

defineProps<Props>()

type StyleConfig = {
  bg: string
  text: string
  ring: string
  label: string
}

const severidadConfig: Record<string, StyleConfig> = {
  ALTA: { bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-600/20', label: 'Alta' },
  MEDIA: { bg: 'bg-orange-50', text: 'text-orange-700', ring: 'ring-orange-600/20', label: 'Media' },
  BAJA: { bg: 'bg-yellow-50', text: 'text-yellow-700', ring: 'ring-yellow-600/20', label: 'Baja' },
}

const defaultConfig: StyleConfig = {
  bg: 'bg-gray-50',
  text: 'text-gray-600',
  ring: 'ring-gray-500/20',
  label: '',
}

function getConfig(severidad: string): StyleConfig {
  return severidadConfig[severidad] ?? { ...defaultConfig, label: severidad }
}
</script>

<template>
  <span
    class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ring-1 ring-inset"
    :class="[getConfig(severidad).bg, getConfig(severidad).text, getConfig(severidad).ring]"
  >
    {{ getConfig(severidad).label }}
  </span>
</template>
