<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useEstadisticasStore } from '@/stores/estadisticas'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import { Bar, Doughnut, Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler,
  type ChartData,
  type ChartOptions,
} from 'chart.js'

ChartJS.register(
  CategoryScale, LinearScale, BarElement, LineElement,
  PointElement, ArcElement, Title, Tooltip, Legend, Filler,
)

const store = useEstadisticasStore()

onMounted(() => {
  store.fetchEstadisticas()
})

// --- Chart: Portes por mes (Bar) ---
const barChartData = computed<ChartData<'bar'>>(() => {
  const items = store.data?.portesPorMes ?? []
  return {
    labels: items.map((m) => m.mes),
    datasets: [
      {
        label: 'Portes',
        data: items.map((m) => m.cantidad),
        backgroundColor: 'rgba(37, 99, 235, 0.7)',
        borderRadius: 6,
        borderSkipped: false,
        barThickness: 24,
      },
    ],
  }
})

const barChartOptions = computed<ChartOptions<'bar'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { backgroundColor: '#1F2937', cornerRadius: 8 } },
  scales: {
    x: { grid: { display: false }, border: { display: false }, ticks: { color: '#9CA3AF', font: { size: 11 } } },
    y: { beginAtZero: true, grid: { color: '#F3F4F6' }, border: { display: false }, ticks: { color: '#9CA3AF', font: { size: 11 } } },
  },
}))

// --- Chart: Ingresos por mes (Line) ---
const lineChartData = computed<ChartData<'line'>>(() => {
  const items = store.data?.portesPorMes ?? []
  return {
    labels: items.map((m) => m.mes),
    datasets: [
      {
        label: 'Ingresos (€)',
        data: items.map((m) => m.ingresos),
        borderColor: 'rgba(16, 185, 129, 1)',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        fill: true,
        tension: 0.4,
        pointRadius: 3,
        pointBackgroundColor: 'rgba(16, 185, 129, 1)',
      },
    ],
  }
})

const lineChartOptions = computed<ChartOptions<'line'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { backgroundColor: '#1F2937', cornerRadius: 8 } },
  scales: {
    x: { grid: { display: false }, border: { display: false }, ticks: { color: '#9CA3AF', font: { size: 11 } } },
    y: { beginAtZero: true, grid: { color: '#F3F4F6' }, border: { display: false }, ticks: { color: '#9CA3AF', font: { size: 11 } } },
  },
}))

// --- Chart: Portes por estado (Doughnut) ---
const ESTADO_COLORS: Record<string, string> = {
  PENDIENTE: '#F59E0B',
  ASIGNADO: '#3B82F6',
  EN_TRANSITO: '#8B5CF6',
  ENTREGADO: '#10B981',
  CANCELADO: '#EF4444',
  FACTURADO: '#06B6D4',
}

const doughnutChartData = computed<ChartData<'doughnut'>>(() => {
  const items = store.data?.portesPorEstado ?? []
  return {
    labels: items.map((e) => e.estado),
    datasets: [
      {
        data: items.map((e) => e.cantidad),
        backgroundColor: items.map((e) => ESTADO_COLORS[e.estado] ?? '#9CA3AF'),
        borderWidth: 2,
        borderColor: '#FFFFFF',
      },
    ],
  }
})

const doughnutChartOptions = computed<ChartOptions<'doughnut'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'bottom', labels: { usePointStyle: true, padding: 16, font: { size: 12 } } },
    tooltip: { backgroundColor: '#1F2937', cornerRadius: 8 },
  },
}))

function formatCurrency(val: number): string {
  return val.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' })
}

function formatTrend(val: number): string {
  return `${val >= 0 ? '+' : ''}${val.toFixed(1)}%`
}
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div>
      <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">Estadísticas Globales</h1>
      <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
        Panel de análisis y métricas de la plataforma
      </p>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="flex items-center justify-center py-20">
      <i class="pi pi-spin pi-spinner text-3xl text-blue-500"></i>
    </div>

    <!-- Error -->
    <div
      v-else-if="store.error"
      class="flex items-center gap-3 bg-red-50 border border-red-200 text-red-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-times-circle text-red-500"></i>
      <span>{{ store.error }}</span>
    </div>

    <template v-else-if="store.data">
      <!-- KPI Cards Row -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <KpiCard
          title="Total Portes"
          :value="store.data.totalPortes"
          icon="pi-truck"
          icon-bg-color="bg-blue-50"
          icon-text-color="text-blue-600"
          :trend="formatTrend(store.data.portesTendencia)"
          :trend-positive="store.data.portesTendencia >= 0"
        />
        <KpiCard
          title="Ingresos Totales"
          :value="formatCurrency(store.data.totalIngresos)"
          icon="pi-euro"
          icon-bg-color="bg-emerald-50"
          icon-text-color="text-emerald-600"
          :trend="formatTrend(store.data.ingresosTendencia)"
          :trend-positive="store.data.ingresosTendencia >= 0"
        />
        <KpiCard
          title="Conductores Activos"
          :value="store.data.totalConductoresActivos"
          icon="pi-users"
          icon-bg-color="bg-indigo-50"
          icon-text-color="text-indigo-600"
        />
        <KpiCard
          title="Clientes"
          :value="store.data.totalClientes"
          icon="pi-building"
          icon-bg-color="bg-amber-50"
          icon-text-color="text-amber-600"
        />
      </div>

      <!-- Charts Row -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <!-- Bar Chart: Portes por mes -->
        <div class="lg:col-span-2 bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-6">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-1">Portes por Mes</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">Últimos 12 meses</p>
          <div class="h-64">
            <Bar :data="barChartData" :options="barChartOptions" />
          </div>
        </div>

        <!-- Doughnut: Portes por estado -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-6">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-1">Portes por Estado</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">Distribución actual</p>
          <div class="h-64">
            <Doughnut :data="doughnutChartData" :options="doughnutChartOptions" />
          </div>
        </div>
      </div>

      <!-- Line Chart: Ingresos por mes -->
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-6">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-1">Ingresos por Mes</h3>
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">Evolución de facturación (últimos 12 meses)</p>
        <div class="h-64">
          <Line :data="lineChartData" :options="lineChartOptions" />
        </div>
      </div>

      <!-- Facturas Summary Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-5 flex items-center gap-4">
          <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
            <i class="pi pi-file text-blue-600"></i>
          </div>
          <div>
            <p class="text-sm text-gray-500 dark:text-gray-400">Facturas Emitidas</p>
            <p class="text-xl font-bold text-gray-800 dark:text-gray-100">{{ store.data.facturasEmitidas }}</p>
          </div>
        </div>
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-5 flex items-center gap-4">
          <div class="w-10 h-10 rounded-lg bg-emerald-50 flex items-center justify-center">
            <i class="pi pi-check-circle text-emerald-600"></i>
          </div>
          <div>
            <p class="text-sm text-gray-500 dark:text-gray-400">Facturas Pagadas</p>
            <p class="text-xl font-bold text-gray-800 dark:text-gray-100">{{ store.data.facturasPagadas }}</p>
          </div>
        </div>
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-5 flex items-center gap-4">
          <div class="w-10 h-10 rounded-lg bg-amber-50 flex items-center justify-center">
            <i class="pi pi-clock text-amber-600"></i>
          </div>
          <div>
            <p class="text-sm text-gray-500 dark:text-gray-400">Facturas Pendientes</p>
            <p class="text-xl font-bold text-gray-800 dark:text-gray-100">{{ store.data.facturasPendientes }}</p>
          </div>
        </div>
      </div>

      <!-- Top Tables Row -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <!-- Top 5 Conductores -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-6">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-4">Top 5 Conductores</h3>
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-700">
                <th class="text-left py-2 text-gray-500 dark:text-gray-400 font-medium">#</th>
                <th class="text-left py-2 text-gray-500 dark:text-gray-400 font-medium">Nombre</th>
                <th class="text-right py-2 text-gray-500 dark:text-gray-400 font-medium">Portes</th>
                <th class="text-right py-2 text-gray-500 dark:text-gray-400 font-medium">Rating</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(c, i) in store.data.topConductores"
                :key="i"
                class="border-b border-gray-50 dark:border-gray-700/50 last:border-0"
              >
                <td class="py-2.5 text-gray-400 dark:text-gray-500 font-medium">{{ i + 1 }}</td>
                <td class="py-2.5 text-gray-800 dark:text-gray-100 font-medium">{{ c.nombre }}</td>
                <td class="py-2.5 text-right text-gray-600 dark:text-gray-400">{{ c.portes }}</td>
                <td class="py-2.5 text-right">
                  <span class="inline-flex items-center gap-1 text-amber-600">
                    <i class="pi pi-star-fill text-xs"></i>
                    {{ c.rating.toFixed(1) }}
                  </span>
                </td>
              </tr>
              <tr v-if="!store.data.topConductores.length">
                <td colspan="4" class="py-4 text-center text-gray-400 dark:text-gray-500">Sin datos</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Top 5 Clientes -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-6">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-4">Top 5 Clientes</h3>
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-700">
                <th class="text-left py-2 text-gray-500 dark:text-gray-400 font-medium">#</th>
                <th class="text-left py-2 text-gray-500 dark:text-gray-400 font-medium">Empresa</th>
                <th class="text-right py-2 text-gray-500 dark:text-gray-400 font-medium">Facturado</th>
                <th class="text-right py-2 text-gray-500 dark:text-gray-400 font-medium">Portes</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(c, i) in store.data.topClientes"
                :key="i"
                class="border-b border-gray-50 dark:border-gray-700/50 last:border-0"
              >
                <td class="py-2.5 text-gray-400 dark:text-gray-500 font-medium">{{ i + 1 }}</td>
                <td class="py-2.5 text-gray-800 dark:text-gray-100 font-medium">{{ c.nombreEmpresa }}</td>
                <td class="py-2.5 text-right text-gray-600 dark:text-gray-400">{{ formatCurrency(c.totalFacturado) }}</td>
                <td class="py-2.5 text-right text-gray-600 dark:text-gray-400">{{ c.portes }}</td>
              </tr>
              <tr v-if="!store.data.topClientes.length">
                <td colspan="4" class="py-4 text-center text-gray-400 dark:text-gray-500">Sin datos</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
