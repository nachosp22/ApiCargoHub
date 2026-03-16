<script setup lang="ts">
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  type ChartData,
  type ChartOptions,
} from 'chart.js'

// Register Chart.js components
ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

interface Props {
  data: number[]
}

const props = defineProps<Props>()

const MONTHS = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']

const chartData = computed<ChartData<'bar'>>(() => ({
  labels: MONTHS,
  datasets: [
    {
      label: 'Portes',
      data: props.data,
      backgroundColor: 'rgba(37, 99, 235, 0.7)',
      hoverBackgroundColor: 'rgba(37, 99, 235, 0.9)',
      borderRadius: 6,
      borderSkipped: false,
      barThickness: 28,
    },
  ],
}))

const chartOptions = computed<ChartOptions<'bar'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      backgroundColor: '#1F2937',
      titleFont: { size: 13, weight: 'bold' },
      bodyFont: { size: 12 },
      padding: 10,
      cornerRadius: 8,
      callbacks: {
        label: (ctx) => ` ${ctx.parsed.y} portes`,
      },
    },
  },
  scales: {
    x: {
      grid: {
        display: false,
      },
      ticks: {
        color: '#9CA3AF',
        font: { size: 12 },
      },
      border: {
        display: false,
      },
    },
    y: {
      beginAtZero: true,
      grid: {
        color: 'rgba(243, 244, 246, 1)',
      },
      ticks: {
        color: '#9CA3AF',
        font: { size: 12 },
        stepSize: 10,
      },
      border: {
        display: false,
      },
    },
  },
}))
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h3 class="text-lg font-semibold text-gray-800">Portes por Mes</h3>
        <p class="text-sm text-gray-500 mt-0.5">Actividad de transporte en los últimos 12 meses</p>
      </div>
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <i class="pi pi-calendar text-xs"></i>
        <span>2025 – 2026</span>
      </div>
    </div>

    <!-- Chart -->
    <div class="h-72">
      <Bar :data="chartData" :options="chartOptions" />
    </div>
  </div>
</template>
