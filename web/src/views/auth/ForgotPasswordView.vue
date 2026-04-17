<template>
  <div class="min-h-screen flex items-center justify-center bg-canvas dark:bg-gray-900 px-4">
    <div class="w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow-lg dark:shadow-gray-900/50 p-8">
      <div class="text-center mb-8">
        <div class="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-700 rounded-xl flex items-center justify-center mx-auto mb-4">
          <i class="pi pi-lock text-white text-xl"></i>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Recuperar Contrasena</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Introduce tu email y te enviaremos instrucciones</p>
      </div>

      <div v-if="sent" class="text-center">
        <div class="w-16 h-16 bg-green-100 dark:bg-green-900/30 rounded-full flex items-center justify-center mx-auto mb-4">
          <i class="pi pi-check text-green-600 dark:text-green-400 text-2xl"></i>
        </div>
        <p class="text-gray-700 dark:text-gray-300 mb-4">Si el email existe en nuestro sistema, recibiras instrucciones para restablecer tu contrasena.</p>
        <router-link to="/login" class="text-sm text-primary hover:underline">Volver al inicio de sesion</router-link>
      </div>

      <form v-else @submit.prevent="handleSubmit" class="space-y-4">
        <div>
          <label for="email" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email</label>
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

        <Button
          type="submit"
          label="Enviar Instrucciones"
          :loading="loading"
          class="w-full"
          severity="primary"
        />
      </form>

      <div v-if="!sent" class="mt-6 text-center">
        <router-link to="/login" class="text-sm text-primary hover:underline">
          Volver al inicio de sesion
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'

const authStore = useAuthStore()
const email = ref('')
const loading = ref(false)
const sent = ref(false)

async function handleSubmit() {
  loading.value = true
  try {
    await authStore.forgotPassword(email.value)
    sent.value = true
  } catch {
    // Always show success for security
    sent.value = true
  } finally {
    loading.value = false
  }
}
</script>
