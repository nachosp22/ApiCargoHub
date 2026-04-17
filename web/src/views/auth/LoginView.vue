<template>
  <div class="min-h-screen flex items-center justify-center bg-canvas px-4">
    <div class="w-full max-w-md bg-white rounded-2xl shadow-lg p-8">
      <div class="text-center mb-8">
        <div class="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-700 rounded-xl flex items-center justify-center mx-auto mb-4">
          <i class="pi pi-truck text-white text-xl"></i>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">Iniciar Sesión</h1>
        <p class="text-sm text-gray-500 mt-1">Accede a tu portal de cliente</p>
      </div>

      <!-- Error message -->
      <div v-if="errorMessage" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-700">{{ errorMessage }}</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label for="email" class="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <InputText
            id="email"
            v-model="email"
            type="email"
            placeholder="tu@email.com"
            class="w-full"
            :disabled="loading"
            required
          />
        </div>

        <div>
          <label for="password" class="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
          <InputText
            id="password"
            v-model="password"
            type="password"
            placeholder="••••••••"
            class="w-full"
            :disabled="loading"
            required
          />
        </div>

        <Button
          type="submit"
          label="Iniciar Sesión"
          :loading="loading"
          class="w-full"
          severity="primary"
        />
      </form>

      <div class="mt-6 text-center">
        <router-link to="/register" class="text-sm text-primary hover:underline">
          ¿No tienes cuenta? Regístrate
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore, AuthLoginError } from '@/stores/auth'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'

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
      errorMessage.value = 'Error inesperado al iniciar sesión.'
    }
  } finally {
    loading.value = false
  }
}
</script>
