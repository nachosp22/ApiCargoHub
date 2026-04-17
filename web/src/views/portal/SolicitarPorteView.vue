<template>
  <div class="max-w-2xl mx-auto">
    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
      <div class="mb-6">
        <h2 class="text-xl font-bold text-gray-900 dark:text-white">{{ t('portal.solicitar.title') }}</h2>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
          {{ t('portal.solicitar.subtitle') }}
        </p>
      </div>

      <!-- Success message -->
      <div v-if="successMessage" class="mb-6 p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
        <div class="flex items-center gap-2">
          <i class="pi pi-check-circle text-green-600"></i>
          <p class="text-sm text-green-700 font-medium">{{ successMessage }}</p>
        </div>
        <div class="mt-3">
          <router-link to="/portal/mis-portes" class="text-sm text-green-700 hover:underline font-medium">
            {{ t('portal.solicitar.viewMyPortes') }}
          </router-link>
        </div>
      </div>

      <!-- Error message -->
      <div v-if="portesStore.error" class="mb-6 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
        <p class="text-sm text-red-700 dark:text-red-400">{{ portesStore.error }}</p>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-5">
        <!-- Origen / Destino -->
        <div class="grid md:grid-cols-2 gap-4">
          <div>
            <label for="origen" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('portal.solicitar.origin') }} <span class="text-red-500">*</span>
            </label>
            <InputText
              id="origen"
              v-model="form.origen"
              :placeholder="t('portal.solicitar.originPlaceholder')"
              class="w-full"
              :disabled="portesStore.submitting"
              required
            />
          </div>
          <div>
            <label for="destino" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('portal.solicitar.destination') }} <span class="text-red-500">*</span>
            </label>
            <InputText
              id="destino"
              v-model="form.destino"
              :placeholder="t('portal.solicitar.destinationPlaceholder')"
              class="w-full"
              :disabled="portesStore.submitting"
              required
            />
          </div>
        </div>

        <!-- Descripción de carga -->
        <div>
          <label for="descripcion" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            {{ t('portal.solicitar.cargoDescription') }} <span class="text-red-500">*</span>
          </label>
          <Textarea
            id="descripcion"
            v-model="form.descripcionCliente"
            :placeholder="t('portal.solicitar.cargoPlaceholder')"
            class="w-full"
            rows="4"
            :disabled="portesStore.submitting"
            required
          />
          <p class="text-xs text-gray-400 mt-1">
            <i class="pi pi-sparkles text-primary-400"></i>
            {{ t('portal.solicitar.aiNote') }}
          </p>
        </div>

        <!-- Fecha de recogida -->
        <div>
          <label for="fechaRecogida" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            {{ t('portal.solicitar.pickupDate') }}
          </label>
          <DatePicker
            id="fechaRecogida"
            v-model="fechaRecogida"
            dateFormat="dd/mm/yy"
            :minDate="minDate"
            :placeholder="t('portal.solicitar.pickupDatePlaceholder')"
            class="w-full"
            :disabled="portesStore.submitting"
            showIcon
          />
        </div>

        <!-- Submit -->
        <div class="pt-2">
          <Button
            type="submit"
            :label="t('portal.solicitar.submit')"
            icon="pi pi-send"
            :loading="portesStore.submitting"
            class="w-full"
            severity="primary"
          />
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePortesStore } from '@/stores/portes'
import type { SolicitudPorteRequest } from '@/stores/portes'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'

const { t } = useI18n()
const portesStore = usePortesStore()

const form = reactive({
  origen: '',
  destino: '',
  descripcionCliente: '',
})

const fechaRecogida = ref<Date | null>(null)
const successMessage = ref('')

const minDate = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return d
})

async function handleSubmit() {
  successMessage.value = ''

  const request: SolicitudPorteRequest = {
    origen: form.origen,
    destino: form.destino,
    descripcionCliente: form.descripcionCliente,
    fechaRecogida: fechaRecogida.value?.toISOString(),
  }

  try {
    await portesStore.createSolicitud(request)
    successMessage.value = t('portal.solicitar.successMessage')

    form.origen = ''
    form.destino = ''
    form.descripcionCliente = ''
    fechaRecogida.value = null
  } catch {
    // Error is shown via portesStore.error
  }
}
</script>
