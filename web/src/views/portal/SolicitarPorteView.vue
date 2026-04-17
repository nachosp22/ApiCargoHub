<template>
  <div class="max-w-2xl mx-auto">
    <div class="bg-white rounded-xl border border-gray-200 p-6">
      <div class="mb-6">
        <h2 class="text-xl font-bold text-gray-900">Nueva Solicitud de Porte</h2>
        <p class="text-sm text-gray-500 mt-1">
          Describe tu carga y nuestro sistema con IA calculará las dimensiones y vehículo necesario.
        </p>
      </div>

      <!-- Success message -->
      <div v-if="successMessage" class="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg">
        <div class="flex items-center gap-2">
          <i class="pi pi-check-circle text-green-600"></i>
          <p class="text-sm text-green-700 font-medium">{{ successMessage }}</p>
        </div>
        <div class="mt-3">
          <router-link to="/portal/mis-portes" class="text-sm text-green-700 hover:underline font-medium">
            Ver mis portes →
          </router-link>
        </div>
      </div>

      <!-- Error message -->
      <div v-if="portesStore.error" class="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-700">{{ portesStore.error }}</p>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-5">
        <!-- Origen / Destino -->
        <div class="grid md:grid-cols-2 gap-4">
          <div>
            <label for="origen" class="block text-sm font-medium text-gray-700 mb-1">
              Origen <span class="text-red-500">*</span>
            </label>
            <InputText
              id="origen"
              v-model="form.origen"
              placeholder="Ciudad o dirección de recogida"
              class="w-full"
              :disabled="portesStore.submitting"
              required
            />
          </div>
          <div>
            <label for="destino" class="block text-sm font-medium text-gray-700 mb-1">
              Destino <span class="text-red-500">*</span>
            </label>
            <InputText
              id="destino"
              v-model="form.destino"
              placeholder="Ciudad o dirección de entrega"
              class="w-full"
              :disabled="portesStore.submitting"
              required
            />
          </div>
        </div>

        <!-- Descripción de carga -->
        <div>
          <label for="descripcion" class="block text-sm font-medium text-gray-700 mb-1">
            Descripción de la Carga <span class="text-red-500">*</span>
          </label>
          <Textarea
            id="descripcion"
            v-model="form.descripcionCliente"
            placeholder="Ej: 10 palés de productos electrónicos, cada uno de 1.2m x 0.8m x 1.5m, peso aprox 200kg cada uno. Requiere furgoneta cerrada."
            class="w-full"
            rows="4"
            :disabled="portesStore.submitting"
            required
          />
          <p class="text-xs text-gray-400 mt-1">
            <i class="pi pi-sparkles text-primary-400"></i>
            Nuestra IA analizará la descripción para calcular dimensiones, peso y tipo de vehículo necesario.
          </p>
        </div>

        <!-- Fecha de recogida -->
        <div>
          <label for="fechaRecogida" class="block text-sm font-medium text-gray-700 mb-1">
            Fecha Deseada de Recogida
          </label>
          <DatePicker
            id="fechaRecogida"
            v-model="fechaRecogida"
            dateFormat="dd/mm/yy"
            :minDate="minDate"
            placeholder="Selecciona una fecha"
            class="w-full"
            :disabled="portesStore.submitting"
            showIcon
          />
        </div>

        <!-- Submit -->
        <div class="pt-2">
          <Button
            type="submit"
            label="Enviar Solicitud"
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
import { usePortesStore } from '@/stores/portes'
import type { SolicitudPorteRequest } from '@/stores/portes'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'

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
  d.setDate(d.getDate() + 1) // tomorrow at earliest
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
    successMessage.value = '¡Solicitud enviada correctamente! Estamos procesando tu carga.'

    // Reset form
    form.origen = ''
    form.destino = ''
    form.descripcionCliente = ''
    fechaRecogida.value = null
  } catch {
    // Error is shown via portesStore.error
  }
}
</script>
