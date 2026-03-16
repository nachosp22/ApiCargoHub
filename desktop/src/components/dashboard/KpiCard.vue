<script setup lang="ts">
interface Props {
  title: string
  value: number | string
  icon: string
  iconBgColor: string
  iconTextColor: string
  trend?: string
  trendPositive?: boolean
}

defineProps<Props>()

function formatNumber(val: number | string): string {
  const num = typeof val === 'string' ? parseInt(val, 10) : val
  if (isNaN(num)) return String(val)
  return num.toLocaleString('es-ES')
}
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 transition-all duration-200 hover:shadow-md">
    <div class="flex items-start justify-between">
      <!-- Text Content -->
      <div class="flex-1 min-w-0">
        <p class="text-sm font-medium text-gray-500 truncate">{{ title }}</p>
        <p class="text-3xl font-bold text-gray-800 mt-1">{{ formatNumber(value) }}</p>

        <!-- Trend Indicator -->
        <div v-if="trend" class="flex items-center gap-1 mt-2">
          <i
            class="pi text-xs"
            :class="trendPositive ? 'pi-arrow-up text-emerald-500' : 'pi-arrow-down text-red-500'"
          ></i>
          <span
            class="text-xs font-medium"
            :class="trendPositive ? 'text-emerald-600' : 'text-red-600'"
          >
            {{ trend }} vs mes anterior
          </span>
        </div>
      </div>

      <!-- Icon -->
      <div
        class="flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center ml-4"
        :class="[iconBgColor]"
      >
        <i class="pi text-xl" :class="[icon, iconTextColor]"></i>
      </div>
    </div>
  </div>
</template>
