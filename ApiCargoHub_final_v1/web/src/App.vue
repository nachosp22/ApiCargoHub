<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'
import Toast from 'primevue/toast'

// Initialize theme (reads localStorage / system preference, applies dark class)
useTheme()
</script>

<template>
  <Toast />
  <router-view />
</template>
