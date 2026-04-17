<template>
  <div class="min-h-screen flex items-center justify-center bg-canvas dark:bg-gray-900 px-4">
    <div class="w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow-lg dark:shadow-gray-900/50 p-8">
      <div class="text-center mb-8">
        <div class="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-700 rounded-xl flex items-center justify-center mx-auto mb-4">
          <i class="pi pi-key text-white text-xl"></i>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Restablecer Contrasena</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Introduce tu nueva contrasena</p>
      </div>

      <div v-if="!token" class="text-center">
        <p class="text-red-600 dark:text-red-400 mb-4">Token no proporcionado. Solicita un nuevo enlace de recuperacion.</p>
        <router-link to="/forgot-password" class="text-sm text-primary hover:underline">Solicitar recuperacion</router-link>
      </div>

      <div v-else-if="success" class="text-center">
        <div class="w-16 h-16 bg-green-100 dark:bg-green-900/30 rounded-full flex items-center justify-center mx-auto mb-4">
          <i class="pi pi-check text-green-600 dark:text-green-400 text-2xl"></i>
        </div>
        <p class="text-gray-700 dark:text-gray-300 mb-4">Tu contrasena se ha restablecido correctamente.</p>
        <router-link to="/login" class="text-sm text-primary hover:underline">Iniciar sesion</router-link>
      </div>

      <form v-else @submit.prevent="handleSubmit" class="space-y-4">
        <div v-if="errorMessage" class="p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
          <p class="text-sm text-red-700 dark:text-red-400">{{ errorMessage }}</p>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nueva Contrasena</label>
          <InputText v-model="newPassword" type="password" placeholder="Minimo 6 caracteres" class="w-full" :disabled="loading" required />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Confirmar Contrasena</label>
          <InputText v-model="confirmPassword" type="password" placeholder="Repite la contrasena" class="w-full" :disabled="loading" required />
        </div>

        <Button type="submit" label="Restablecer Contrasena" :loading="loading" class="w-full" severity="primary" />
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'

const route = useRoute()
const authStore = useAuthStore()

const token = computed(() => route.query.token as string | undefined)
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const success = ref(false)
const errorMessage = ref('')

async function handleSubmit() {
  errorMessage.value = ''

  if (newPassword.value.length < 6) {
    errorMessage.value = 'La contrasena debe tener al menos 6 caracteres.'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'Las contrasenas no coinciden.'
    return
  }

  loading.value = true
  try {
    await authStore.resetPassword(token.value!, newPassword.value)
    success.value = true
  } catch {
    errorMessage.value = 'Token invalido o expirado. Solicita un nuevo enlace.'
  } finally {
    loading.value = false
  }
}
</script>
