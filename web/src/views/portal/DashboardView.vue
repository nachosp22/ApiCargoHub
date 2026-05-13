<template>
  <div>
    <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-6">
      {{ t('portal.dashboard.welcome', { name: authStore.user?.nombre ?? 'Cliente' }) }}
    </h2>

    <!-- Summary Cards -->
    <div class="grid md:grid-cols-4 gap-6 mb-8">
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 bg-blue-100 dark:bg-blue-900/30 rounded-lg flex items-center justify-center">
            <i class="pi pi-truck text-blue-600 dark:text-blue-400"></i>
          </div>
          <span class="text-sm text-gray-500 dark:text-gray-400">{{ t('portal.dashboard.activePortes') }}</span>
        </div>
        <p class="text-3xl font-bold text-gray-900 dark:text-white">{{ portesStore.portesActivos.length }}</p>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 bg-green-100 dark:bg-green-900/30 rounded-lg flex items-center justify-center">
            <i class="pi pi-check-circle text-green-600 dark:text-green-400"></i>
          </div>
          <span class="text-sm text-gray-500 dark:text-gray-400">{{ t('portal.dashboard.completed') }}</span>
        </div>
        <p class="text-3xl font-bold text-gray-900 dark:text-white">{{ portesStore.portesCompletados.length }}</p>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 bg-amber-100 dark:bg-amber-900/30 rounded-lg flex items-center justify-center">
            <i class="pi pi-file text-amber-600 dark:text-amber-400"></i>
          </div>
          <span class="text-sm text-gray-500 dark:text-gray-400">{{ t('portal.dashboard.pendingInvoices') }}</span>
        </div>
        <p class="text-3xl font-bold text-gray-900 dark:text-white">{{ facturasStore.facturasPendientes }}</p>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 bg-violet-100 dark:bg-violet-900/30 rounded-lg flex items-center justify-center">
            <i class="pi pi-euro text-violet-600 dark:text-violet-400"></i>
          </div>
          <span class="text-sm text-gray-500 dark:text-gray-400">{{ t('portal.dashboard.totalPending') }}</span>
        </div>
        <p class="text-3xl font-bold text-gray-900 dark:text-white">{{ formatCurrency(facturasStore.totalPendiente) }}</p>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="grid md:grid-cols-2 gap-6 mb-8">
      <router-link
        to="/portal/solicitar-porte"
        class="flex items-center gap-4 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 hover:border-primary-300 dark:hover:border-primary-700 hover:shadow-sm transition-all group"
      >
        <div class="w-12 h-12 bg-primary-100 rounded-xl flex items-center justify-center group-hover:bg-primary-200 transition-colors">
          <i class="pi pi-plus text-primary-600 text-xl"></i>
        </div>
        <div>
          <h3 class="font-semibold text-gray-900 dark:text-white">{{ t('portal.dashboard.requestPorte') }}</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">{{ t('portal.dashboard.requestPorteDesc') }}</p>
        </div>
      </router-link>

      <router-link
        to="/portal/mis-portes"
        class="flex items-center gap-4 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 hover:border-primary-300 dark:hover:border-primary-700 hover:shadow-sm transition-all group"
      >
        <div class="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center group-hover:bg-blue-200 transition-colors">
          <i class="pi pi-list text-blue-600 text-xl"></i>
        </div>
        <div>
          <h3 class="font-semibold text-gray-900 dark:text-white">{{ t('portal.dashboard.viewMyPortes') }}</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">{{ t('portal.dashboard.viewMyPortesDesc') }}</p>
        </div>
      </router-link>
    </div>

    <!-- Recent Activity -->
    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700">
      <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700">
        <h3 class="font-semibold text-gray-900 dark:text-white">{{ t('portal.dashboard.recentActivity') }}</h3>
      </div>
      <div v-if="portesStore.loading" class="p-6 text-center text-gray-400">
        <i class="pi pi-spin pi-spinner text-xl"></i>
      </div>
      <div v-else-if="recentPortes.length === 0" class="p-6 text-center text-gray-400 text-sm">
        {{ t('portal.dashboard.noRecentActivity') }}
      </div>
      <div v-else class="divide-y divide-gray-100 dark:divide-gray-700">
        <div
          v-for="porte in recentPortes"
          :key="porte.id"
          class="px-6 py-4 flex items-center justify-between"
        >
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center">
              <i class="pi pi-arrow-right text-gray-500 dark:text-gray-400 text-xs"></i>
            </div>
            <div>
              <p class="text-sm font-medium text-gray-900 dark:text-white">{{ porte.origen }} → {{ porte.destino }}</p>
              <p class="text-xs text-gray-500 dark:text-gray-400">{{ formatDate(porte.fechaCreacion) }}</p>
            </div>
          </div>
          <span
            class="text-xs font-medium px-2.5 py-1 rounded-full"
            :class="estadoBadgeClass(porte.estado)"
          >
            {{ porte.estado.replace('_', ' ') }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { usePortesStore } from '@/stores/portes'
import { useFacturasStore } from '@/stores/facturas'

const { t } = useI18n()
const authStore = useAuthStore()
const portesStore = usePortesStore()
const facturasStore = useFacturasStore()

onMounted(async () => {
  const cId = authStore.clienteId
  if (cId) {
    await Promise.allSettled([
      portesStore.fetchOwn(cId),
      facturasStore.fetchMisFacturas(),
    ])
  }
})

const recentPortes = computed(() =>
  [...portesStore.portes]
    .sort((a, b) => {
      const dateA = a.fechaCreacion ? new Date(a.fechaCreacion).getTime() : 0
      const dateB = b.fechaCreacion ? new Date(b.fechaCreacion).getTime() : 0
      return dateB - dateA
    })
    .slice(0, 5)
)

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(amount)
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '—'
  return new Intl.DateTimeFormat('es-ES', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(dateStr))
}

function estadoBadgeClass(estado: string): string {
  const map: Record<string, string> = {
    PENDIENTE: 'bg-yellow-100 text-yellow-700',
    SOLICITUD: 'bg-purple-100 text-purple-700',
    ASIGNADO: 'bg-blue-100 text-blue-700',
    EN_TRANSITO: 'bg-indigo-100 text-indigo-700',
    ENTREGADO: 'bg-green-100 text-green-700',
    CANCELADO: 'bg-red-100 text-red-700',
    FACTURADO: 'bg-gray-100 text-gray-700',
  }
  return map[estado] ?? 'bg-gray-100 text-gray-600'
}
</script>
