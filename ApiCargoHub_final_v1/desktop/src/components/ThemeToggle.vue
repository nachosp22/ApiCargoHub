<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'

const { isDark, toggleTheme } = useTheme()
</script>

<template>
  <button
    type="button"
    class="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors"
    :aria-label="isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
    @click="toggleTheme"
  >
    <!-- Sun icon (shown in dark mode) -->
    <i v-if="isDark" class="pi pi-sun text-lg"></i>
    <!-- Moon icon (shown in light mode) -->
    <i v-else class="pi pi-moon text-lg"></i>
  </button>
</template>
