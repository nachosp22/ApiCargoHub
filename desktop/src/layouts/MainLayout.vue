<script setup lang="ts">
import AppSidebar from '@/components/AppSidebar.vue'
import AppTopBar from '@/components/AppTopBar.vue'
</script>

<template>
  <div class="min-h-screen bg-canvas">
    <!-- Sidebar -->
    <AppSidebar />

    <!-- TopBar -->
    <AppTopBar />

    <!-- Canvas (main content area) -->
    <main class="ml-60 mt-16 p-6 min-h-[calc(100vh-4rem)]">
      <RouterView />
    </main>
  </div>
</template>
