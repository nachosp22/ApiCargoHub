<template>
  <div class="min-h-screen flex bg-canvas">
    <!-- Sidebar -->
    <aside class="w-64 bg-white border-r border-gray-200 flex flex-col fixed inset-y-0 left-0 z-40">
      <!-- Logo -->
      <div class="h-16 flex items-center px-6 border-b border-gray-100">
        <router-link to="/portal/dashboard" class="flex items-center gap-2">
          <div class="w-8 h-8 bg-gradient-to-br from-primary-500 to-primary-700 rounded-lg flex items-center justify-center">
            <i class="pi pi-truck text-white text-sm"></i>
          </div>
          <span class="text-lg font-bold text-gray-900">Cargo<span class="text-primary">Hub</span></span>
        </router-link>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 px-3 py-4 space-y-1">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors"
          :class="[
            $route.path === item.to
              ? 'bg-primary-50 text-primary-700'
              : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
          ]"
        >
          <i :class="item.icon" class="text-base"></i>
          {{ item.label }}
        </router-link>
      </nav>

      <!-- User info + Logout -->
      <div class="p-3 border-t border-gray-100">
        <div v-if="authStore.user" class="px-3 py-2 mb-2">
          <p class="text-sm font-medium text-gray-900 truncate">{{ authStore.user.nombre }}</p>
          <p class="text-xs text-gray-500 truncate">{{ authStore.user.email }}</p>
        </div>
        <button
          @click="handleLogout"
          class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-600 hover:bg-red-50 hover:text-red-600 transition-colors w-full"
        >
          <i class="pi pi-sign-out text-base"></i>
          Cerrar Sesión
        </button>
      </div>
    </aside>

    <!-- Main content -->
    <div class="flex-1 ml-64">
      <!-- Top bar -->
      <header class="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 sticky top-0 z-30">
        <h1 class="text-lg font-semibold text-gray-900">{{ pageTitle }}</h1>
        <div class="flex items-center gap-3">
          <span class="text-sm text-gray-500">Portal Cliente</span>
          <div class="w-8 h-8 bg-primary-100 text-primary-700 rounded-full flex items-center justify-center text-sm font-semibold">
            {{ userInitial }}
          </div>
        </div>
      </header>

      <!-- Page content -->
      <main class="p-6">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  if (!authStore.token) {
    authStore.loadFromStorage()
  }
})

const navItems = [
  { to: '/portal/dashboard', label: 'Dashboard', icon: 'pi pi-home' },
  { to: '/portal/solicitar-porte', label: 'Solicitar Porte', icon: 'pi pi-plus-circle' },
  { to: '/portal/mis-portes', label: 'Mis Portes', icon: 'pi pi-list' },
  { to: '/portal/mis-facturas', label: 'Mis Facturas', icon: 'pi pi-file' },
]

const pageTitle = computed(() => {
  const item = navItems.find((n) => n.to === route.path)
  return item?.label ?? 'Portal'
})

const userInitial = computed(() => {
  const name = authStore.user?.nombre ?? 'C'
  return name.charAt(0).toUpperCase()
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
