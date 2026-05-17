<script setup lang="ts">
import { RouterView } from 'vue-router'
import Toast from 'primevue/toast'
import { useTheme } from '@/composables/useTheme'

// Initialize theme (reads localStorage / system preference, applies dark class)
useTheme()
</script>

<template>
  <Toast position="top-right" />
  <RouterView />
</template>
