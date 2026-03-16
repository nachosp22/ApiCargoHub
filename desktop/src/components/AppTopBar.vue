<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

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
    class="fixed top-0 left-60 right-0 h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 z-20"
  >
    <!-- Left: Search Bar -->
    <div class="relative w-full max-w-md">
      <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm"></i>
      <input
        type="text"
        placeholder="Buscar..."
        class="w-full pl-10 pr-4 py-2 bg-gray-100 border-0 rounded-lg text-sm text-gray-700 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-colors"
      />
    </div>

    <!-- Right: Notifications + User -->
    <div class="flex items-center gap-5 ml-4 shrink-0">
      <!-- Notification Bell -->
      <button
        type="button"
        class="relative p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
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
          <p class="text-sm font-medium text-gray-700 leading-tight">
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
