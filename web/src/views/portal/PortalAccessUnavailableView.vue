<template>
  <div class="max-w-3xl mx-auto">
    <div class="bg-white dark:bg-gray-800 rounded-xl border border-amber-200 dark:border-amber-800 p-6 md:p-8">
      <div class="w-12 h-12 rounded-xl bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 flex items-center justify-center mb-4">
        <i class="pi pi-info-circle text-xl"></i>
      </div>
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">
        {{ t('portal.accessUnavailable.title') }}
      </h2>
      <p class="text-gray-600 dark:text-gray-300 mb-6">
        {{ t('portal.accessUnavailable.description') }}
      </p>

      <div class="space-y-3 text-sm text-gray-600 dark:text-gray-300">
        <p>• {{ t('portal.accessUnavailable.reasonRole') }}</p>
        <p>• {{ t('portal.accessUnavailable.reasonMissingClient') }}</p>
      </div>

      <div class="mt-5 p-4 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800">
        <p class="text-sm text-blue-900 dark:text-blue-100 font-medium mb-3">
          {{ t('portal.accessUnavailable.mobileAppHint') }}
        </p>
        <a
          href="/downloads/cargohuhmobile.apk"
          download
          class="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors"
        >
          <i class="pi pi-download"></i>
          <span>{{ t('portal.accessUnavailable.downloadMobileApp') }}</span>
        </a>
      </div>

      <div class="mt-8 flex flex-wrap gap-3">
        <router-link
          to="/"
          class="inline-flex items-center px-4 py-2 rounded-lg bg-primary text-white hover:opacity-90 transition-opacity"
        >
          {{ t('portal.accessUnavailable.goHome') }}
        </router-link>
        <button
          type="button"
          class="inline-flex items-center px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          @click="handleLogout"
        >
          Cerrar sesión
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

function handleLogout(): void {
  authStore.logout()
  router.push('/login')
}
</script>
