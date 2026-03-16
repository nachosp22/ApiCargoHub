<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { AuthLoginError, useAuthStore } from '@/stores/auth'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function handleLogin(): Promise<void> {
  if (!email.value || !password.value) {
    errorMessage.value = 'Por favor, completa todos los campos.'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    await authStore.login(email.value, password.value)
    const redirectTarget =
      typeof router.currentRoute.value.query.redirect === 'string'
        ? router.currentRoute.value.query.redirect
        : '/dashboard'

    await router.replace(redirectTarget)
  } catch (error: unknown) {
    if (error instanceof AuthLoginError) {
      switch (error.kind) {
        case 'invalid_credentials':
          errorMessage.value = 'Credenciales inválidas. Verifica tu correo y contraseña.'
          break
        case 'network':
          errorMessage.value =
            'No se pudo conectar con el servidor. Verifica que la API esté ejecutándose.'
          break
        case 'server':
          errorMessage.value =
            'El servidor respondió con un error. Inténtalo de nuevo en unos minutos.'
          break
        default:
          errorMessage.value = 'Error inesperado. Inténtalo de nuevo.'
      }
    } else {
      errorMessage.value = 'Error inesperado. Inténtalo de nuevo.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-canvas flex items-center justify-center px-4">
    <div class="w-full max-w-md">
      <!-- Logo / Brand -->
      <div class="text-center mb-8">
        <div
          class="inline-flex items-center justify-center w-16 h-16 bg-primary rounded-2xl mb-4"
        >
          <i class="pi pi-truck text-white text-3xl"></i>
        </div>
        <h1 class="text-2xl font-bold text-gray-800">CargoHub</h1>
        <p class="text-gray-500 mt-1">Plataforma de Gestión Logística</p>
      </div>

      <!-- Login Card -->
      <div class="bg-white rounded-2xl shadow-lg p-8">
        <h2 class="text-xl font-semibold text-gray-800 mb-6">Iniciar Sesión</h2>

        <form @submit.prevent="handleLogin" class="space-y-5">
          <!-- Email -->
          <div class="flex flex-col gap-2">
            <label for="email" class="text-sm font-medium text-gray-700">
              Correo electrónico
            </label>
            <InputText
              id="email"
              v-model="email"
              type="email"
              placeholder="tu@correo.com"
              class="w-full"
              :disabled="loading"
            />
          </div>

          <!-- Password -->
          <div class="flex flex-col gap-2">
            <label for="password" class="text-sm font-medium text-gray-700">
              Contraseña
            </label>
            <Password
              id="password"
              v-model="password"
              placeholder="••••••••"
              :feedback="false"
              toggle-mask
              class="w-full"
              input-class="w-full"
              :disabled="loading"
            />
          </div>

          <!-- Error message -->
          <div
            v-if="errorMessage"
            class="bg-red-50 text-red-600 text-sm px-4 py-3 rounded-lg flex items-center gap-2"
          >
            <i class="pi pi-exclamation-circle"></i>
            {{ errorMessage }}
          </div>

          <!-- Submit -->
          <Button
            type="submit"
            label="Iniciar Sesión"
            icon="pi pi-sign-in"
            class="w-full"
            :loading="loading"
          />
        </form>
      </div>

      <!-- Footer -->
      <p class="text-center text-gray-400 text-xs mt-6">
        CargoHub Desktop &copy; {{ new Date().getFullYear() }}
      </p>
    </div>
  </div>
</template>
