<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

interface NavItem {
  label: string
  icon: string
  route: string
}

const navItems: NavItem[] = [
  { label: 'Dashboard', icon: 'pi-chart-bar', route: '/dashboard' },
  { label: 'Portes', icon: 'pi-truck', route: '/portes' },
  { label: 'Conductores', icon: 'pi-users', route: '/conductores' },
  { label: 'Vehículos', icon: 'pi-car', route: '/vehiculos' },
  { label: 'Incidencias', icon: 'pi-exclamation-triangle', route: '/incidencias' },
  { label: 'Facturas', icon: 'pi-file', route: '/facturas' },
  { label: 'Clientes', icon: 'pi-building', route: '/clientes' },
]

async function handleLogout(): Promise<void> {
  authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <aside class="fixed top-0 left-0 h-screen w-60 bg-white border-r border-gray-200 flex flex-col z-30">
    <!-- Logo -->
    <div class="h-16 flex items-center gap-3 px-5 border-b border-gray-200 shrink-0">
      <div class="flex items-center justify-center w-8 h-8 bg-primary rounded-lg">
        <i class="pi pi-truck text-white text-sm"></i>
      </div>
      <span class="text-lg font-semibold text-gray-800">CargoHub</span>
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
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
              "
              @click="navigate"
            >
              <i class="pi text-base" :class="item.icon"></i>
              <span>{{ item.label }}</span>
            </button>
          </router-link>
        </li>
      </ul>
    </nav>

    <!-- Logout -->
    <div class="border-t border-gray-200 p-3 shrink-0">
      <button
        type="button"
        class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-100 hover:text-gray-900 transition-colors duration-150"
        @click="handleLogout"
      >
        <i class="pi pi-sign-out text-base"></i>
        <span>Cerrar sesión</span>
      </button>
    </div>
  </aside>
</template>
