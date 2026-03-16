<script setup lang="ts">
interface Props {
  label: string
  value: string
  icon: string
  iconColor: string
}

defineProps<Props>()
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 px-5 py-4 flex items-center gap-4">
    <!-- Icon -->
    <div class="flex-shrink-0 w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center">
      <i class="pi text-lg" :class="[icon, iconColor]"></i>
    </div>

    <!-- Content -->
    <div class="min-w-0">
      <p class="text-lg font-bold text-gray-800">{{ value }}</p>
      <p class="text-xs text-gray-500 truncate">{{ label }}</p>
    </div>
  </div>
</template>
