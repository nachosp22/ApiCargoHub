<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'

// Initialize theme (reads localStorage / system preference, applies dark class)
useTheme()
</script>

<template>
  <router-view />
</template>
