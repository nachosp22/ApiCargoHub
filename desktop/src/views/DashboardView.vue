<script setup lang="ts">
import { onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import StatMiniCard from '@/components/dashboard/StatMiniCard.vue'
import ActivityChart from '@/components/dashboard/ActivityChart.vue'
import RecentPortesTable from '@/components/dashboard/RecentPortesTable.vue'

const dashboardStore = useDashboardStore()

onMounted(() => {
  dashboardStore.fetchDashboardData()
  dashboardStore.fetchResumen()
  dashboardStore.fetchIncidenciasPendientes()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Page Header -->
    <div>
      <h1 class="text-2xl font-bold text-gray-800">Dashboard</h1>
      <p class="text-sm text-gray-500 mt-1">
        Resumen general de la plataforma de transporte
      </p>
    </div>

    <!-- Mock Data Banner -->
    <div
      v-if="dashboardStore.usingMockData"
      class="flex items-center gap-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg px-4 py-3 text-sm"
    >
      <i class="pi pi-info-circle text-amber-500"></i>
      <span>Mostrando datos de demostración — la API no está disponible en este momento.</span>
    </div>

    <!-- KPI Cards Row -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
      <KpiCard
        title="Portes Este Mes"
        :value="dashboardStore.resumen?.portesMes ?? 0"
        icon="pi-truck"
        icon-bg-color="bg-blue-50"
        icon-text-color="text-blue-600"
      />
      <KpiCard
        title="Portes Activos"
        :value="dashboardStore.resumen?.portesActivos ?? 0"
        icon="pi-send"
        icon-bg-color="bg-emerald-50"
        icon-text-color="text-emerald-600"
      />
      <KpiCard
        title="Portes Mañana"
        :value="dashboardStore.resumen?.portesManana ?? 0"
        icon="pi-calendar"
        icon-bg-color="bg-indigo-50"
        icon-text-color="text-indigo-600"
      />
      <KpiCard
        title="Incidencias Pendientes"
        :value="dashboardStore.incidenciasPendientes"
        icon="pi-exclamation-triangle"
        icon-bg-color="bg-amber-50"
        icon-text-color="text-amber-600"
      />
    </div>

    <!-- Floating Mini Stat Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <StatMiniCard
        label="Entregas Hoy"
        value="12"
        icon="pi-check-circle"
        icon-color="text-emerald-500"
      />
      <StatMiniCard
        label="SLA Cumplimiento"
        value="94%"
        icon="pi-chart-line"
        icon-color="text-blue-500"
      />
      <StatMiniCard
        label="Km Recorridos (Hoy)"
        value="2,847"
        icon="pi-map"
        icon-color="text-violet-500"
      />
    </div>

    <!-- Activity Chart -->
    <ActivityChart :data="dashboardStore.chartData" />

    <!-- Recent Portes Table -->
    <RecentPortesTable
      :portes="dashboardStore.recentPortes"
      :loading="dashboardStore.loading"
    />
  </div>
</template>
