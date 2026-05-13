<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import GlobalSearchBar from '@/components/GlobalSearchBar.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import ProfileDialog from '@/components/ProfileDialog.vue'
import { getProfilePhoto } from '@/services/api'

const authStore = useAuthStore()
const showProfileDialog = ref(false)
const isMaximized = ref(false)

function getElectronBridge() {
  return window.electronAPI ?? window.electron
}

const isDesktopElectron = !!getElectronBridge()

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

function handleAvatarError(): void {
  authStore.setFotoUrl(null)
}

async function minimizeWindow(): Promise<void> {
  await getElectronBridge()?.minimizeWindow()
}

async function toggleMaximizeWindow(): Promise<void> {
  const maxState = await getElectronBridge()?.toggleMaximizeWindow()
  if (typeof maxState === 'boolean') {
    isMaximized.value = maxState
  }
}

async function closeWindow(): Promise<void> {
  await getElectronBridge()?.closeWindow()
}

// Load profile photo on mount
onMounted(async () => {
  const electronBridge = getElectronBridge()
  if (electronBridge) {
    isMaximized.value = await electronBridge.isMaximized()
  }

  if (authStore.user && !authStore.user.fotoUrl) {
    try {
      const response = await getProfilePhoto()
      if (response.status === 200 && response.data?.url) {
        authStore.setFotoUrl(response.data.url)
      }
    } catch {
      // No photo — keep initials
    }
  }
})
</script>

<template>
  <header
    class="fixed top-0 left-60 right-0 h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-4 z-20 select-none"
    style="-webkit-app-region: drag"
  >
    <div class="flex items-center min-w-0">
      <!-- Left: Global Search Bar -->
      <div class="w-[28rem] max-w-[40vw]" style="-webkit-app-region: no-drag">
        <GlobalSearchBar />
      </div>
    </div>

    <div class="flex-1 h-full" style="-webkit-app-region: drag"></div>

    <!-- Right: Theme + Notifications + User -->
    <div class="flex items-center gap-5 ml-4 shrink-0" style="-webkit-app-region: no-drag">
      <!-- Theme Toggle -->
      <div style="-webkit-app-region: no-drag">
        <ThemeToggle />
      </div>

      <!-- Notification Bell -->
      <button
        type="button"
        class="relative p-2 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
        aria-label="Notificaciones"
        style="-webkit-app-region: no-drag"
      >
        <i class="pi pi-bell text-lg"></i>
      </button>

      <!-- User Profile (clickable) -->
      <button
        type="button"
        class="flex items-center gap-3 p-1 -m-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
        @click="showProfileDialog = true"
        style="-webkit-app-region: no-drag"
      >
        <div
          class="w-9 h-9 rounded-full overflow-hidden flex items-center justify-center text-sm font-semibold"
          :class="authStore.user?.fotoUrl ? '' : 'bg-primary/10 text-primary'"
        >
          <img
            v-if="authStore.user?.fotoUrl"
            :src="authStore.user.fotoUrl"
            alt="Avatar"
            class="w-full h-full object-cover"
            @error="handleAvatarError"
          />
          <span v-else>{{ userInitials() }}</span>
        </div>
        <div class="hidden sm:block text-left">
          <p class="text-sm font-medium text-gray-700 dark:text-gray-200 leading-tight">
            {{ authStore.user?.nombre ?? 'Usuario' }}
          </p>
          <p class="text-xs text-gray-400 leading-tight">
            {{ authStore.user?.role ?? 'N/A' }}
          </p>
        </div>
      </button>

      <!-- Window Controls (client desktop only) -->
      <div
        v-if="isDesktopElectron"
        class="flex items-center gap-1 pl-2 border-l border-gray-200 dark:border-gray-700"
        style="-webkit-app-region: no-drag"
      >
        <button
          type="button"
          class="w-8 h-8 inline-flex items-center justify-center rounded-md text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-200 transition-colors"
          aria-label="Minimizar ventana"
          @click="minimizeWindow"
        >
          <i class="pi pi-minus text-sm"></i>
        </button>
        <button
          type="button"
          class="w-8 h-8 inline-flex items-center justify-center rounded-md text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-200 transition-colors"
          :aria-label="isMaximized ? 'Restaurar ventana' : 'Maximizar ventana'"
          @click="toggleMaximizeWindow"
        >
          <i :class="isMaximized ? 'pi pi-window-minimize' : 'pi pi-window-maximize'" class="text-sm"></i>
        </button>
        <button
          type="button"
          class="w-8 h-8 inline-flex items-center justify-center rounded-md text-gray-500 hover:bg-red-500 hover:text-white dark:text-gray-400 dark:hover:bg-red-500 dark:hover:text-white transition-colors"
          aria-label="Cerrar ventana"
          @click="closeWindow"
        >
          <i class="pi pi-times text-sm"></i>
        </button>
      </div>
    </div>
  </header>

  <!-- Profile Dialog -->
  <ProfileDialog v-model:visible="showProfileDialog" />
</template>
