<template>
  <div class="relative min-h-screen flex items-center justify-center px-4 py-8 overflow-hidden">
    <img
      src="/assets/brand/login-bg.png"
      alt=""
      aria-hidden="true"
      class="absolute inset-0 h-full w-full object-cover"
    />
    <div class="absolute inset-0 bg-gradient-to-br from-slate-950/70 via-slate-900/60 to-primary-950/70"></div>

    <div class="relative w-full max-w-md bg-white/95 dark:bg-gray-900/90 backdrop-blur-sm rounded-2xl shadow-xl dark:shadow-black/40 p-8 border border-white/20 dark:border-gray-700/50">
      <div class="text-center mb-8">
        <img
          src="/assets/brand/logo.png"
          alt="CargoHub"
          class="w-12 h-12 object-contain mx-auto mb-4"
        />
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ t('auth.login.title') }}</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ t('auth.login.subtitle') }}</p>
      </div>

      <!-- Error message -->
      <div v-if="errorMessage" class="mb-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
        <p class="text-sm text-red-700 dark:text-red-400">{{ errorMessage }}</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label for="email" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ t('auth.login.email') }}</label>
          <InputText
            id="email"
            v-model="email"
            type="email"
            :placeholder="t('auth.login.emailPlaceholder')"
            class="w-full"
            :disabled="loading"
            required
          />
        </div>

        <div>
          <label for="password" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ t('auth.login.password') }}</label>
          <InputText
            id="password"
            v-model="password"
            type="password"
            :placeholder="t('auth.login.passwordPlaceholder')"
            class="w-full"
            :disabled="loading"
            required
          />
        </div>

        <Button
          type="submit"
          :label="t('auth.login.submit')"
          :loading="loading"
          class="w-full"
          severity="primary"
        />
      </form>

      <div class="mt-6 text-center">
        <router-link to="/register" class="text-sm text-primary hover:underline">
          {{ t('auth.login.noAccount') }}
        </router-link>
        <p class="text-xs text-gray-500 dark:text-gray-400 mt-3">
          {{ t('auth.login.passwordRecoveryUnavailable') }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore, AuthLoginError } from '@/stores/auth'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function handleLogin() {
  errorMessage.value = ''
  loading.value = true

  try {
    await authStore.login(email.value, password.value)

    const redirect = route.query.redirect as string | undefined
    await router.push(redirect || '/portal/dashboard')
  } catch (err) {
    if (err instanceof AuthLoginError) {
      errorMessage.value = err.message
    } else {
      errorMessage.value = t('auth.login.unexpectedError')
    }
  } finally {
    loading.value = false
  }
}
</script>
