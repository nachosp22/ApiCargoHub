<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'

const { locale } = useI18n()

function toggleLocale() {
  const newLocale = locale.value === 'es' ? 'en' : 'es'
  locale.value = newLocale
  localStorage.setItem('locale', newLocale)
}
</script>

<template>
  <Button
    :label="locale.toUpperCase()"
    severity="secondary"
    text
    rounded
    size="small"
    @click="toggleLocale"
  />
</template>
