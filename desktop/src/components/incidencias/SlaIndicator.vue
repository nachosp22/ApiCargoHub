<script setup lang="ts">
import { computed } from 'vue'
import type { EstadoIncidencia } from '@/stores/incidencias'

interface Props {
  fechaLimiteSla: string | null
  estado: EstadoIncidencia
}

const props = defineProps<Props>()

const isTerminal = computed(() => props.estado === 'RESUELTA' || props.estado === 'DESESTIMADA')

const slaInfo = computed(() => {
  if (isTerminal.value) {
    return { label: '—', color: 'text-gray-400', severity: 'none' as const }
  }

  if (!props.fechaLimiteSla) {
    return { label: '—', color: 'text-gray-400', severity: 'none' as const }
  }

  const now = new Date()
  const limit = new Date(props.fechaLimiteSla)
  const diffMs = limit.getTime() - now.getTime()

  if (diffMs <= 0) {
    // Overdue
    const overMs = Math.abs(diffMs)
    const overLabel = formatDuration(overMs)
    return {
      label: `VENCIDA · hace ${overLabel}`,
      color: 'text-red-600 font-bold',
      severity: 'danger' as const,
    }
  }

  // Remaining time
  const remainLabel = formatDuration(diffMs)

  // Calculate total SLA duration from report to limit (approximate)
  // We use percentage-based coloring:
  // >50% remaining = green, <50% = yellow, <25% = red
  const totalHours = 24 // fallback - we use thresholds directly
  void totalHours

  const hoursRemaining = diffMs / (1000 * 60 * 60)

  if (hoursRemaining < 6) {
    return {
      label: `${remainLabel} restantes`,
      color: 'text-red-600 font-semibold',
      severity: 'danger' as const,
    }
  }

  if (hoursRemaining < 24) {
    return {
      label: `${remainLabel} restantes`,
      color: 'text-amber-600 font-medium',
      severity: 'warning' as const,
    }
  }

  return {
    label: `${remainLabel} restantes`,
    color: 'text-emerald-600',
    severity: 'ok' as const,
  }
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
</script>

<template>
  <span class="text-xs whitespace-nowrap" :class="slaInfo.color">
    {{ slaInfo.label }}
  </span>
</template>
