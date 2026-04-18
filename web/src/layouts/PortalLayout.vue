<template>
  <div class="min-h-screen flex bg-canvas dark:bg-gray-900">
    <!-- Sidebar -->
    <aside class="w-64 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 flex flex-col fixed inset-y-0 left-0 z-40">
      <!-- Logo -->
      <div class="h-16 flex items-center px-6 border-b border-gray-100 dark:border-gray-700">
        <router-link to="/portal/dashboard" class="flex items-center gap-2">
          <div class="w-8 h-8 bg-gradient-to-br from-primary-500 to-primary-700 rounded-lg flex items-center justify-center">
            <svg viewBox="0 0 512 512" fill="none" class="w-5 h-5"><path d="M256 296L88 199v148l168 97V296z" fill="#1E40AF"/><path d="M256 296l168-97v148l-168 97V296z" fill="#2563EB"/><path d="M256 102L88 199l168 97 168-97L256 102z" fill="#3B82F6"/><path d="M216 199l40-28 40 28-16 11v34h-48v-34L216 199z" fill="white" opacity=".92"/></svg>
          </div>
          <span class="text-lg font-bold text-gray-900 dark:text-white">Cargo<span class="text-primary">Hub</span></span>
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
    <div class="flex-1 ml-64">
      <!-- Top bar -->
      <header class="h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-6 sticky top-0 z-30">
        <h1 class="text-lg font-semibold text-gray-900 dark:text-white">{{ pageTitle }}</h1>
        <div class="flex items-center gap-3">
          <ThemeToggle />
          <LanguageSwitcher />
          <span class="text-sm text-gray-500 dark:text-gray-400">{{ t('layout.portal.clientPortal') }}</span>
          <div class="w-8 h-8 bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-400 rounded-full flex items-center justify-center text-sm font-semibold">
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
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  if (!authStore.token) {
    authStore.loadFromStorage()
  }
})

const navItems = computed(() => [
  { to: '/portal/dashboard', label: t('layout.portal.dashboard'), icon: 'pi pi-home' },
  { to: '/portal/solicitar-porte', label: t('layout.portal.requestPorte'), icon: 'pi pi-plus-circle' },
  { to: '/portal/mis-portes', label: t('layout.portal.myPortes'), icon: 'pi pi-list' },
  { to: '/portal/mis-facturas', label: t('layout.portal.myInvoices'), icon: 'pi pi-file' },
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
</script>
