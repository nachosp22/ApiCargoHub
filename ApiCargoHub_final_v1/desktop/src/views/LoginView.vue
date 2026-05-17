<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AuthLoginError, useAuthStore } from '@/stores/auth'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
const loginBackgroundImage = `${import.meta.env.BASE_URL}assets/brand/login-bg.png`
const whiteLogo = `${import.meta.env.BASE_URL}assets/brand/logo-blanco.png`

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
  <div class="relative min-h-screen flex items-center justify-center overflow-hidden bg-blue-950 px-4 py-6">
    <img
      :src="loginBackgroundImage"
      alt=""
      aria-hidden="true"
      class="absolute inset-0 h-full w-full object-cover"
    />

    <div
      class="absolute inset-0"
      style="background: linear-gradient(120deg, rgba(8, 38, 112, 0.76) 0%, rgba(14, 73, 190, 0.58) 48%, rgba(8, 38, 112, 0.78) 100%)"
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

    <div class="absolute inset-0 bg-blue-950/20 backdrop-blur-[1.5px]"></div>

    <div class="relative z-10 flex w-full max-w-[440px] flex-col items-center">
      <!-- Logo / Brand -->
      <div class="mb-7 text-center text-white">
        <img :src="whiteLogo" alt="CargoHub" class="mx-auto h-20 w-auto object-contain drop-shadow-2xl" />
        <h1 class="mt-3 text-2xl font-bold leading-tight drop-shadow">CargoHub</h1>
        <p class="mt-1.5 text-base text-blue-50/95 drop-shadow-sm">Plataforma de Gestión Logística</p>
      </div>

      <!-- Login Card -->
      <div class="w-full rounded-[1.15rem] bg-white/95 p-8 shadow-2xl shadow-blue-950/45 ring-1 ring-white/70 backdrop-blur-md">
        <h2 class="mb-7 text-center text-2xl font-bold tracking-tight text-gray-950">Iniciar Sesión</h2>

        <form @submit.prevent="handleLogin" class="space-y-5">
          <!-- Email -->
          <div class="flex flex-col gap-2.5">
            <label for="email" class="text-sm font-semibold text-gray-700">
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
          <div class="flex flex-col gap-2.5">
            <label for="password" class="text-sm font-semibold text-gray-700">
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
            class="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
          >
            <i class="pi pi-exclamation-circle mt-0.5"></i>
            <span>{{ errorMessage }}</span>
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
      <p class="mt-6 text-center text-sm text-blue-50/95 drop-shadow-sm">
        CargoHub Desktop &copy; {{ new Date().getFullYear() }}
      </p>
    </div>
  </div>
</template>

<style scoped>
:deep(.p-inputtext),
:deep(.p-password-input) {
  height: 2.85rem;
  border-radius: 0.45rem;
  border-color: #cbd5e1;
  font-size: 0.98rem;
}

:deep(.p-inputtext:enabled:focus),
:deep(.p-password-input:enabled:focus) {
  border-color: #0d6efd;
  box-shadow: 0 0 0 1px #0d6efd;
}

:deep(.p-inputtext::placeholder),
:deep(.p-password-input::placeholder) {
  color: #66789f;
}

:deep(.p-button) {
  height: 2.95rem;
  border-radius: 0.45rem;
  border-color: #0d6efd;
  background: #0d6efd;
  font-size: 1rem;
  font-weight: 700;
}

:deep(.p-button:not(:disabled):hover) {
  border-color: #0b5ed7;
  background: #0b5ed7;
}
</style>
