<template>
  <div class="min-h-screen flex bg-canvas dark:bg-gray-900">
    <div
      v-if="sidebarOpen"
      class="fixed inset-0 bg-black/40 z-30 lg:hidden"
      @click="sidebarOpen = false"
    ></div>

    <!-- Sidebar -->
    <aside
      class="w-64 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 flex flex-col fixed inset-y-0 left-0 z-40 transform transition-transform duration-200 lg:translate-x-0"
      :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
    >
      <!-- Logo -->
      <div class="h-16 flex items-center px-6 border-b border-gray-100 dark:border-gray-700">
        <router-link to="/portal/dashboard" class="flex items-center gap-3">
          <img
            src="/assets/brand/logo.png"
            alt="CargoHub"
            class="w-9 h-9 object-contain"
          />
          <span class="text-lg font-bold text-gray-900 dark:text-white">Cargo<span class="text-primary">Hub</span></span>
        </router-link>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 px-3 py-4 space-y-1">
          <router-link
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            @click="sidebarOpen = false"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors"
          :class="[
            $route.path === item.to
              ? 'bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-400'
              : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-gray-200'
          ]"
        >
          <i :class="item.icon" class="text-base"></i>
          {{ item.label }}
        </router-link>
      </nav>

      <!-- User info + Logout -->
      <div class="p-3 border-t border-gray-100 dark:border-gray-700">
        <div v-if="authStore.user" class="px-3 py-2 mb-2">
          <p class="text-sm font-medium text-gray-900 dark:text-white truncate">{{ authStore.user.nombre }}</p>
          <p class="text-xs text-gray-500 dark:text-gray-400 truncate">{{ authStore.user.email }}</p>
        </div>
        <button
          @click="handleLogout"
          class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-red-50 dark:hover:bg-red-900/20 hover:text-red-600 dark:hover:text-red-400 transition-colors w-full"
        >
          <i class="pi pi-sign-out text-base"></i>
          {{ t('layout.portal.logout') }}
        </button>
      </div>
    </aside>

    <!-- Main content -->
    <div class="flex-1 lg:ml-64">
      <!-- Top bar -->
      <header class="h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-6 sticky top-0 z-30">
        <div class="flex items-center gap-3 min-w-0">
          <button
            class="lg:hidden inline-flex items-center justify-center w-9 h-9 rounded-lg border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300"
            @click="sidebarOpen = !sidebarOpen"
            :aria-label="t('layout.portal.openMenu')"
          >
            <i class="pi pi-bars"></i>
          </button>
          <h1 class="text-lg font-semibold text-gray-900 dark:text-white truncate">{{ pageTitle }}</h1>
        </div>
        <div class="flex items-center gap-3">
          <ThemeToggle />
          <LanguageSwitcher />
          <span class="text-sm text-gray-500 dark:text-gray-400 hidden md:inline">{{ t('layout.portal.clientPortal') }}</span>
          <div class="w-8 h-8 bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-400 rounded-full flex items-center justify-center text-sm font-semibold">
            {{ userInitial }}
          </div>
        </div>
      </header>

      <!-- Page content -->
      <main class="p-4 md:p-6">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const sidebarOpen = ref(false)

onMounted(() => {
  if (!authStore.token) {
    authStore.loadFromStorage()
  }

  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

const navItems = computed(() => [
  { to: '/portal/dashboard', label: t('layout.portal.dashboard'), icon: 'pi pi-home' },
  { to: '/portal/solicitar-porte', label: t('layout.portal.requestPorte'), icon: 'pi pi-plus-circle' },
  { to: '/portal/mis-portes', label: t('layout.portal.myPortes'), icon: 'pi pi-list' },
  { to: '/portal/mis-facturas', label: t('layout.portal.myInvoices'), icon: 'pi pi-file' },
  { to: '/portal/perfil', label: t('layout.portal.profile'), icon: 'pi pi-user' },
])

const pageTitle = computed(() => {
  const item = navItems.value.find((n) => n.to === route.path)
  return item?.label ?? t('layout.portal.portal')
})

const userInitial = computed(() => {
  const name = authStore.user?.nombre ?? 'C'
  return name.charAt(0).toUpperCase()
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Escape') return
  if (window.innerWidth >= 1024) return
  if (!sidebarOpen.value) return

  sidebarOpen.value = false
}
</script>
