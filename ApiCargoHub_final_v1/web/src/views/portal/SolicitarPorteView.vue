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
      <div v-if="localError || portesStore.error" class="mb-6 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
        <p class="text-sm text-red-700 dark:text-red-400">{{ localError || portesStore.error }}</p>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-5">
        <!-- Origen / Destino -->
        <div class="grid md:grid-cols-2 gap-4">
          <AddressAutocomplete
            v-model="form.origen"
            :label="`${t('portal.solicitar.origin')} *`"
            :placeholder="t('portal.solicitar.originPlaceholder')"
            @select="onOrigenSelect"
          />
          <AddressAutocomplete
            v-model="form.destino"
            :label="`${t('portal.solicitar.destination')} *`"
            :placeholder="t('portal.solicitar.destinationPlaceholder')"
            @select="onDestinoSelect"
          />
        </div>
        <p v-if="showOriginValidation" class="text-xs text-red-600 -mt-2">
          {{ t('portal.solicitar.originSelectionRequired') }}
        </p>
        <p v-if="showDestinationValidation" class="text-xs text-red-600 -mt-2">
          {{ t('portal.solicitar.destinationSelectionRequired') }}
        </p>

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
            showTime
            hourFormat="24"
          />
          <p v-if="showPickupDateValidation" class="text-xs text-red-600 mt-1">
            {{ t('portal.solicitar.pickupDateRequired') }}
          </p>
        </div>

        <!-- Submit -->
        <div class="pt-2">
          <Button
            type="submit"
            :label="t('portal.solicitar.submit')"
            icon="pi pi-send"
            :loading="portesStore.submitting"
            :disabled="!canSubmit"
            class="w-full"
            severity="primary"
          />
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePortesStore } from '@/stores/portes'
import type { SolicitudPorteRequest } from '@/stores/portes'
import AddressAutocomplete from '@/components/AddressAutocomplete.vue'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'

const { t } = useI18n()
const portesStore = usePortesStore()

interface SelectedAddress {
  city: string
  fullAddress: string
  lat: number
  lon: number
}

const form = reactive({
  origen: '',
  destino: '',
  descripcionCliente: '',
  ciudadOrigen: '',
  ciudadDestino: '',
  latitudOrigen: undefined as number | undefined,
  longitudOrigen: undefined as number | undefined,
  latitudDestino: undefined as number | undefined,
  longitudDestino: undefined as number | undefined,
})

const fechaRecogida = ref<Date | null>(null)
const successMessage = ref('')
const localError = ref('')
const hasTriedSubmit = ref(false)
const selectedOrigen = ref<SelectedAddress | null>(null)
const selectedDestino = ref<SelectedAddress | null>(null)

const minDate = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  d.setHours(0, 0, 0, 0)
  return d
})

const isOrigenValido = computed(() => hasAddressText(form.origen))
const isDestinoValido = computed(() => hasAddressText(form.destino))

const canSubmit = computed(() => {
  return (
    !portesStore.submitting &&
    form.descripcionCliente.trim().length > 0 &&
    isOrigenValido.value &&
    isDestinoValido.value &&
    !!fechaRecogida.value
  )
})

const showOriginValidation = computed(() => hasTriedSubmit.value && !isOrigenValido.value)
const showDestinationValidation = computed(() => hasTriedSubmit.value && !isDestinoValido.value)
const showPickupDateValidation = computed(() => hasTriedSubmit.value && !fechaRecogida.value)

watch(
  () => form.origen,
  (value) => {
    localError.value = ''
    if (!selectedOrigen.value) return
    if (value === selectedOrigen.value.fullAddress) return

    selectedOrigen.value = null
    form.ciudadOrigen = ''
    form.latitudOrigen = undefined
    form.longitudOrigen = undefined
  }
)

watch(
  () => form.destino,
  (value) => {
    localError.value = ''
    if (!selectedDestino.value) return
    if (value === selectedDestino.value.fullAddress) return

    selectedDestino.value = null
    form.ciudadDestino = ''
    form.latitudDestino = undefined
    form.longitudDestino = undefined
  }
)

function onOrigenSelect(addr: SelectedAddress): void {
  localError.value = ''
  selectedOrigen.value = addr
  form.origen = addr.fullAddress
  form.ciudadOrigen = addr.city
  form.latitudOrigen = addr.lat || undefined
  form.longitudOrigen = addr.lon || undefined
}

function onDestinoSelect(addr: SelectedAddress): void {
  localError.value = ''
  selectedDestino.value = addr
  form.destino = addr.fullAddress
  form.ciudadDestino = addr.city
  form.latitudDestino = addr.lat || undefined
  form.longitudDestino = addr.lon || undefined
}

async function handleSubmit() {
  successMessage.value = ''
  localError.value = ''
  hasTriedSubmit.value = true

  if (!isOrigenValido.value || !isDestinoValido.value) {
    localError.value = t('portal.solicitar.invalidLocationsError')
    return
  }

  if (!fechaRecogida.value) {
    localError.value = t('portal.solicitar.pickupDateRequired')
    return
  }

  const request: SolicitudPorteRequest = {
    origen: form.origen,
    destino: form.destino,
    ciudadOrigen: form.ciudadOrigen || form.origen,
    ciudadDestino: form.ciudadDestino || form.destino,
    latitudOrigen: form.latitudOrigen,
    longitudOrigen: form.longitudOrigen,
    latitudDestino: form.latitudDestino,
    longitudDestino: form.longitudDestino,
    descripcionCliente: form.descripcionCliente,
    fechaRecogida: formatLocalDateTime(fechaRecogida.value),
  }

  try {
    const newPorte = await portesStore.createSolicitud(request)
    successMessage.value = newPorte.revisionManual
      ? t('portal.portes.manualReview')
      : t('portal.portes.pendingDriverAwaitingAcceptance')

    form.origen = ''
    form.destino = ''
    form.descripcionCliente = ''
    form.ciudadOrigen = ''
    form.ciudadDestino = ''
    form.latitudOrigen = undefined
    form.longitudOrigen = undefined
    form.latitudDestino = undefined
    form.longitudDestino = undefined
    fechaRecogida.value = null
    selectedOrigen.value = null
    selectedDestino.value = null
    hasTriedSubmit.value = false
  } catch {
    // Error is shown via portesStore.error
  }
}

function hasAddressText(value: string): boolean {
  return value.trim().length > 0
}

function formatLocalDateTime(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}
</script>
