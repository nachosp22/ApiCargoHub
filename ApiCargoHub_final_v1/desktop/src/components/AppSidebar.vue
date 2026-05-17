<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useConductoresStore } from '@/stores/conductores'
import { usePortesStore } from '@/stores/portes'

const router = useRouter()
const authStore = useAuthStore()
const conductoresStore = useConductoresStore()
const portesStore = usePortesStore()

interface NavItem {
  label: string
  icon: string
  route: string
  badge?: () => number
}

const fleetRealtimeEnabled = import.meta.env.VITE_FEATURE_FLEET_REALTIME === 'true'
const brandLogo = `${import.meta.env.BASE_URL}assets/brand/logo.png`

const navItems: NavItem[] = [
  { label: 'Dashboard', icon: 'pi-chart-bar', route: '/dashboard' },
  { label: 'Estadísticas', icon: 'pi-chart-line', route: '/estadisticas' },
  { label: 'Portes', icon: 'pi-truck', route: '/portes' },
  { label: 'Revisión Portes', icon: 'pi-eye', route: '/revision-portes', badge: () => portesStore.pendientesRevision.length },
  { label: 'Conductores', icon: 'pi-users', route: '/conductores' },
  { label: 'Aprobaciones', icon: 'pi-user-plus', route: '/aprobacion-conductores', badge: () => conductoresStore.pendientesAprobacion.length },
  { label: 'Vehículos', icon: 'pi-car', route: '/vehiculos' },
  { label: 'Incidencias', icon: 'pi-exclamation-triangle', route: '/incidencias' },
  { label: 'Facturas', icon: 'pi-file', route: '/facturas' },
  { label: 'Clientes', icon: 'pi-building', route: '/clientes' },
  ...(fleetRealtimeEnabled
    ? [{ label: 'Mapa flota', icon: 'pi-map', route: '/fleet-map' }]
    : []),
]

// Fetch pending counts on sidebar mount
onMounted(() => {
  conductoresStore.fetchPendientesAprobacion()
  portesStore.fetchPendientesRevision()
})

async function handleLogout(): Promise<void> {
  authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <aside class="fixed top-0 left-0 h-screen w-60 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 flex flex-col z-30">
    <!-- Logo -->
    <div class="h-16 flex items-center gap-3 px-5 border-b border-gray-200 dark:border-gray-700 shrink-0">
      <img
        :src="brandLogo"
        alt="CargoHub"
        class="w-9 h-9 object-contain"
      />
      <span class="text-lg font-semibold text-gray-800 dark:text-white">CargoHub</span>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto py-4 px-3">
      <ul class="space-y-1">
        <li v-for="item in navItems" :key="item.route">
          <router-link
            :to="item.route"
            custom
            v-slot="{ isActive, navigate }"
          >
            <button
              type="button"
              class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors duration-150"
              :class="
                isActive
                  ? 'bg-primary text-white'
                  : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-gray-200'
              "
              @click="navigate"
            >
              <i class="pi text-base" :class="item.icon"></i>
              <span>{{ item.label }}</span>
              <span
                v-if="item.badge && item.badge() > 0"
                class="ml-auto inline-flex items-center justify-center min-w-[1.25rem] h-5 px-1.5 text-xs font-bold rounded-full"
                :class="isActive ? 'bg-white/25 text-white' : 'bg-red-500 text-white'"
              >
                {{ item.badge() }}
              </span>
            </button>
          </router-link>
        </li>
      </ul>
    </nav>

    <!-- Logout -->
    <div class="border-t border-gray-200 dark:border-gray-700 p-3 shrink-0">
      <button
        type="button"
        class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-gray-200 transition-colors duration-150"
        @click="handleLogout"
      >
        <i class="pi pi-sign-out text-base"></i>
        <span>Cerrar sesión</span>
      </button>
    </div>
  </aside>
</template>
