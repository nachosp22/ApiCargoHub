<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import GlobalSearchBar from '@/components/GlobalSearchBar.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const authStore = useAuthStore()

/** Build initials from user name (e.g. "Juan García" → "JG") */
function userInitials(): string {
  const name = authStore.user?.nombre
  if (!name) return '?'
  return name
    .split(' ')
    .filter(Boolean)
    .map((w) => w[0].toUpperCase())
    .slice(0, 2)
    .join('')
}
</script>

<template>
  <header
    class="fixed top-0 left-60 right-0 h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-6 z-20"
  >
    <!-- Left: Global Search Bar -->
    <GlobalSearchBar />

    <!-- Right: Theme + Notifications + User -->
    <div class="flex items-center gap-5 ml-4 shrink-0">
      <!-- Theme Toggle -->
      <ThemeToggle />

      <!-- Notification Bell -->
      <button
        type="button"
        class="relative p-2 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
        aria-label="Notificaciones"
      >
        <i class="pi pi-bell text-lg"></i>
      </button>

      <!-- User Profile -->
      <div class="flex items-center gap-3">
        <div
          class="w-9 h-9 rounded-full bg-primary/10 text-primary flex items-center justify-center text-sm font-semibold"
        >
          {{ userInitials() }}
        </div>
        <div class="hidden sm:block">
          <p class="text-sm font-medium text-gray-700 dark:text-gray-200 leading-tight">
            {{ authStore.user?.nombre ?? 'Usuario' }}
          </p>
          <p class="text-xs text-gray-400 leading-tight">
            {{ authStore.user?.role ?? 'N/A' }}
          </p>
        </div>
      </div>
    </div>
  </header>
</template>
