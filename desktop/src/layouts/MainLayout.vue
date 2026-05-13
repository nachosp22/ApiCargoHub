<script setup lang="ts">
import AppSidebar from '@/components/AppSidebar.vue'
import AppTopBar from '@/components/AppTopBar.vue'
</script>

<template>
  <div class="h-dvh overflow-hidden bg-canvas dark:bg-gray-900">
    <!-- Sidebar -->
    <AppSidebar />

    <!-- TopBar -->
    <AppTopBar />

    <!-- Canvas (main content area) -->
    <main class="ml-60 mt-16 h-[calc(100dvh-4rem)] min-h-0 overflow-hidden p-6">
      <RouterView v-slot="{ Component }">
        <component :is="Component" class="h-full min-h-0" />
      </RouterView>
    </main>
  </div>
</template>
