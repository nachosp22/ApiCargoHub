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
        title="Portes Activos"
        :value="dashboardStore.kpis.portesActivos"
        icon="pi-truck"
        icon-bg-color="bg-blue-50"
        icon-text-color="text-blue-600"
        :trend="dashboardStore.trends.portesActivos?.value"
        :trend-positive="dashboardStore.trends.portesActivos?.positive"
      />
      <KpiCard
        title="Conductores"
        :value="dashboardStore.kpis.conductores"
        icon="pi-users"
        icon-bg-color="bg-emerald-50"
        icon-text-color="text-emerald-600"
        :trend="dashboardStore.trends.conductores?.value"
        :trend-positive="dashboardStore.trends.conductores?.positive"
      />
      <KpiCard
        title="Incidencias Abiertas"
        :value="dashboardStore.kpis.incidenciasAbiertas"
        icon="pi-exclamation-triangle"
        icon-bg-color="bg-amber-50"
        icon-text-color="text-amber-600"
        :trend="dashboardStore.trends.incidenciasAbiertas?.value"
        :trend-positive="dashboardStore.trends.incidenciasAbiertas?.positive"
      />
      <KpiCard
        title="Vehículos Disponibles"
        :value="dashboardStore.kpis.vehiculosDisponibles"
        icon="pi-car"
        icon-bg-color="bg-purple-50"
        icon-text-color="text-purple-600"
        :trend="dashboardStore.trends.vehiculosDisponibles?.value"
        :trend-positive="dashboardStore.trends.vehiculosDisponibles?.positive"
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
