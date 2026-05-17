<script setup lang="ts">
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Tooltip,
  type ChartData,
  type ChartOptions,
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip)

interface Props {
  ingresoPorMes: Record<string, number>
}

const props = defineProps<Props>()

const MONTH_NAMES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']

const sortedEntries = computed(() => {
  return Object.entries(props.ingresoPorMes)
    .sort(([a], [b]) => a.localeCompare(b))
})

const chartLabels = computed(() =>
  sortedEntries.value.map(([key]) => {
    const parts = key.split('-')
    if (parts.length === 2) {
      const monthIdx = parseInt(parts[1], 10) - 1
      return `${MONTH_NAMES[monthIdx] ?? key} ${parts[0].slice(2)}`
    }
    return key
  })
)

const chartValues = computed(() =>
  sortedEntries.value.map(([, v]) => v)
)

const chartData = computed<ChartData<'bar'>>(() => ({
  labels: chartLabels.value,
  datasets: [
    {
      label: 'Ingresos',
      data: chartValues.value,
      backgroundColor: 'rgba(16, 185, 129, 0.7)',
      borderRadius: 4,
      barThickness: 20,
    },
  ],
}))

const chartOptions = computed<ChartOptions<'bar'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      backgroundColor: '#1F2937',
      callbacks: {
        label: (ctx) => ` ${(ctx.parsed.y ?? 0).toLocaleString('es-ES', { style: 'currency', currency: 'EUR' })}`,
      },
    },
  },
  scales: {
    x: { grid: { display: false }, border: { display: false } },
    y: { beginAtZero: true, grid: { color: 'rgba(243,244,246,1)' }, border: { display: false } },
  },
}))
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-100 p-4">
    <h4 class="text-sm font-semibold text-gray-700 mb-3">Ingresos por Mes</h4>
    <div v-if="sortedEntries.length === 0" class="text-center py-6 text-gray-400 text-sm">
      Sin datos de ingresos
    </div>
    <div v-else class="h-48">
      <Bar :data="chartData" :options="chartOptions" />
    </div>
  </div>
</template>
