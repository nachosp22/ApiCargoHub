<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Button from 'primevue/button'
import type { Vehiculo, CreateVehiculoRequest, TipoVehiculo } from '@/stores/vehiculos'

interface Props {
  visible: boolean
  vehiculo?: Vehiculo | null
  saving?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  vehiculo: null,
  saving: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save', data: CreateVehiculoRequest): void
}>()

const isEditing = computed(() => !!props.vehiculo)
const dialogTitle = computed(() =>
  isEditing.value ? `Editar Vehículo — ${props.vehiculo?.matricula}` : 'Nuevo Vehículo'
)

// --- Form State ---
const form = ref({
  matricula: '',
  marca: '',
  modelo: '',
  tipo: '' as TipoVehiculo | '',
  capacidadCargaKg: null as number | null,
  largoUtilMm: null as number | null,
  anchoUtilMm: null as number | null,
  altoUtilMm: null as number | null,
})

const submitted = ref(false)

// Tipo options
const tipoOptions = [
  { label: 'Furgoneta', value: 'FURGONETA' },
  { label: 'Rígido', value: 'RIGIDO' },
  { label: 'Tráiler', value: 'TRAILER' },
  { label: 'Especial', value: 'ESPECIAL' },
]

// Validation
const errors = computed(() => ({
  matricula: submitted.value && !form.value.matricula.trim(),
  tipo: submitted.value && !form.value.tipo,
}))

const isValid = computed(
  () => form.value.matricula.trim() && form.value.tipo
)

// Reset form when dialog opens
watch(
  () => props.visible,
  (val) => {
    if (val) {
      submitted.value = false
      if (props.vehiculo) {
        form.value = {
          matricula: props.vehiculo.matricula,
          marca: props.vehiculo.marca,
          modelo: props.vehiculo.modelo,
          tipo: props.vehiculo.tipo,
          capacidadCargaKg: props.vehiculo.capacidadCargaKg,
          largoUtilMm: props.vehiculo.largoUtilMm,
          anchoUtilMm: props.vehiculo.anchoUtilMm,
          altoUtilMm: props.vehiculo.altoUtilMm,
        }
      } else {
        form.value = {
          matricula: '',
          marca: '',
          modelo: '',
          tipo: '',
          capacidadCargaKg: null,
          largoUtilMm: null,
          anchoUtilMm: null,
          altoUtilMm: null,
        }
      }
    }
  }
)

function onSubmit(): void {
  submitted.value = true
  if (!isValid.value) return

  const data: CreateVehiculoRequest = {
    matricula: form.value.matricula.trim().toUpperCase(),
    marca: form.value.marca.trim(),
    modelo: form.value.modelo.trim(),
    tipo: form.value.tipo as TipoVehiculo,
    capacidadCargaKg: form.value.capacidadCargaKg,
    largoUtilMm: form.value.largoUtilMm,
    anchoUtilMm: form.value.anchoUtilMm,
    altoUtilMm: form.value.altoUtilMm,
  }

  emit('save', data)
}

function onClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    :header="dialogTitle"
    :modal="true"
    :closable="true"
    :style="{ width: '640px' }"
    :breakpoints="{ '640px': '90vw' }"
    @update:visible="onClose"
  >
    <div class="space-y-5 pt-2">
      <!-- Matrícula -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Matrícula <span class="text-red-500">*</span>
        </label>
        <InputText
          v-model="form.matricula"
          placeholder="1234ABC"
          class="w-full font-mono"
          :invalid="errors.matricula"
        />
        <small v-if="errors.matricula" class="text-red-500 text-xs mt-1">Campo requerido</small>
      </div>

      <!-- Marca / Modelo -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Marca</label>
          <InputText
            v-model="form.marca"
            placeholder="Mercedes-Benz"
            class="w-full"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Modelo</label>
          <InputText
            v-model="form.modelo"
            placeholder="Actros 1845"
            class="w-full"
          />
        </div>
      </div>

      <!-- Tipo -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Tipo <span class="text-red-500">*</span>
        </label>
        <Select
          v-model="form.tipo"
          :options="tipoOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Seleccionar tipo"
          class="w-full"
          :invalid="errors.tipo"
        />
        <small v-if="errors.tipo" class="text-red-500 text-xs mt-1">Campo requerido</small>
      </div>

      <!-- Capacidad -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Capacidad de carga (kg)</label>
        <InputNumber
          v-model="form.capacidadCargaKg"
          placeholder="25000"
          class="w-full"
          :min="0"
          suffix=" kg"
        />
      </div>

      <!-- Dimensiones (mm) -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Dimensiones (mm)</label>
        <div class="grid grid-cols-3 gap-2">
          <InputNumber
            v-model="form.largoUtilMm"
            placeholder="Largo"
            :min="0"
          />
          <InputNumber
            v-model="form.anchoUtilMm"
            placeholder="Ancho"
            :min="0"
          />
          <InputNumber
            v-model="form.altoUtilMm"
            placeholder="Alto"
            :min="0"
          />
        </div>
      </div>

    </div>

    <!-- Footer -->
    <template #footer>
      <div class="flex items-center justify-end gap-3 pt-2">
        <Button
          label="Cancelar"
          severity="secondary"
          text
          @click="onClose"
          :disabled="saving"
        />
        <Button
          :label="isEditing ? 'Guardar Cambios' : 'Crear Vehículo'"
          icon="pi pi-check"
          @click="onSubmit"
          :loading="saving"
        />
      </div>
    </template>
  </Dialog>
</template>
