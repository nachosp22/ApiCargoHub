<template>
  <div class="min-h-screen flex items-center justify-center bg-canvas dark:bg-gray-900 px-4">
    <div class="w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow-lg dark:shadow-gray-900/50 p-8">
      <div class="text-center mb-8">
        <div class="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-700 rounded-xl flex items-center justify-center mx-auto mb-4">
          <svg viewBox="0 0 512 512" fill="none" class="w-6 h-6"><path d="M256 296L88 199v148l168 97V296z" fill="#1E40AF"/><path d="M256 296l168-97v148l-168 97V296z" fill="#2563EB"/><path d="M256 102L88 199l168 97 168-97L256 102z" fill="#3B82F6"/><path d="M216 199l40-28 40 28-16 11v34h-48v-34L216 199z" fill="white" opacity=".92"/></svg>
        </div>
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
