<template>
  <button
    @click="toggleLocale"
    class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-lg border transition-colors"
    :class="variant === 'dark'
      ? 'border-white/20 text-white/80 hover:bg-white/10 hover:text-white'
      : 'border-gray-300 text-gray-600 hover:bg-gray-100 hover:text-gray-900'"
  >
    <i class="pi pi-globe text-xs"></i>
    {{ locale === 'es' ? 'ES' : 'EN' }}
  </button>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  variant?: 'light' | 'dark'
}>()

const { locale } = useI18n()

function toggleLocale() {
  const next = locale.value === 'es' ? 'en' : 'es'
  locale.value = next
  localStorage.setItem('locale', next)
}
</script>
