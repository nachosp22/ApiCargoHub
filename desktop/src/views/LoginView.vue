<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AuthLoginError, useAuthStore } from '@/stores/auth'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
const loginBackgroundImage = '/assets/brand/login-bg.png'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')
const isMaximized = ref(false)

function getElectronBridge() {
  return window.electronAPI ?? window.electron
}

const isDesktopElectron = !!getElectronBridge()

async function minimizeWindow(): Promise<void> {
  await getElectronBridge()?.minimizeWindow()
}

async function toggleMaximizeWindow(): Promise<void> {
  const maxState = await getElectronBridge()?.toggleMaximizeWindow()
  if (typeof maxState === 'boolean') {
    isMaximized.value = maxState
  }
}

async function closeWindow(): Promise<void> {
  await getElectronBridge()?.closeWindow()
}

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

onMounted(async () => {
  const electronBridge = getElectronBridge()
  if (electronBridge) {
    isMaximized.value = await electronBridge.isMaximized()
  }
})
</script>

<template>
  <div class="relative min-h-screen flex items-center justify-center px-4 py-8 overflow-hidden">
    <img
      :src="loginBackgroundImage"
      alt=""
      aria-hidden="true"
      class="absolute inset-0 h-full w-full object-cover"
    />

    <div
      class="absolute inset-0"
      style="background: linear-gradient(120deg, rgba(15, 23, 42, 0.74) 0%, rgba(30, 64, 175, 0.62) 45%, rgba(15, 23, 42, 0.8) 100%)"
    ></div>

    <div
      v-if="isDesktopElectron"
      class="absolute top-0 left-0 right-0 h-12 px-3 flex items-center justify-end z-20 select-none"
      style="-webkit-app-region: drag"
    >
      <div class="flex items-center gap-1" style="-webkit-app-region: no-drag">
        <button
          type="button"
          class="w-8 h-8 inline-flex items-center justify-center rounded-md text-white/90 hover:bg-white/20 hover:text-white transition-colors"
          aria-label="Minimizar ventana"
          @click="minimizeWindow"
        >
          <i class="pi pi-minus text-sm"></i>
        </button>
        <button
          type="button"
          class="w-8 h-8 inline-flex items-center justify-center rounded-md text-white/90 hover:bg-white/20 hover:text-white transition-colors"
          :aria-label="isMaximized ? 'Restaurar ventana' : 'Maximizar ventana'"
          @click="toggleMaximizeWindow"
        >
          <i :class="isMaximized ? 'pi pi-window-minimize' : 'pi pi-window-maximize'" class="text-sm"></i>
        </button>
        <button
          type="button"
          class="w-8 h-8 inline-flex items-center justify-center rounded-md text-white/90 hover:bg-red-500 hover:text-white transition-colors"
          aria-label="Cerrar ventana"
          @click="closeWindow"
        >
          <i class="pi pi-times text-sm"></i>
        </button>
      </div>
    </div>

    <div class="absolute inset-0 bg-slate-950/20 backdrop-blur-[1.5px]"></div>

    <div class="relative z-10 w-full max-w-md">
      <!-- Logo / Brand -->
      <div class="text-center mb-8">
        <div
          class="inline-flex items-center justify-center w-16 h-16 bg-white/95 dark:bg-gray-800/95 shadow-lg rounded-2xl mb-4"
        >
          <img
            src="/assets/brand/logo.png"
            alt="CargoHub"
            class="w-10 h-10 object-contain"
          />
        </div>
        <h1 class="text-2xl font-bold text-white drop-shadow">CargoHub</h1>
        <p class="text-blue-100/95 mt-1">Plataforma de Gestión Logística</p>
      </div>

      <!-- Login Card -->
      <div class="bg-white/95 dark:bg-gray-800/95 backdrop-blur-md rounded-2xl shadow-2xl ring-1 ring-white/30 dark:ring-slate-600/60 p-8">
        <h2 class="text-xl font-semibold text-gray-800 dark:text-white mb-6">Iniciar Sesión</h2>

        <form @submit.prevent="handleLogin" class="space-y-5">
          <!-- Email -->
          <div class="flex flex-col gap-2">
            <label for="email" class="text-sm font-medium text-gray-700 dark:text-gray-300">
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
            <label for="password" class="text-sm font-medium text-gray-700 dark:text-gray-300">
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
            class="bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm px-4 py-3 rounded-lg flex items-center gap-2"
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
      <p class="text-center text-blue-100/85 text-xs mt-6">
        CargoHub Desktop &copy; {{ new Date().getFullYear() }}
      </p>
    </div>
  </div>
</template>
