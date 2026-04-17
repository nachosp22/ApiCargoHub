<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'
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
  trampillaElevadora: false,
})

const submitted = ref(false)

// Tipo options
const tipoOptions = [
  { label: 'Frigorífico', value: 'FRIGORIFICO' },
  { label: 'Lona', value: 'LONA' },
  { label: 'Plataforma', value: 'PLATAFORMA' },
  { label: 'Cisterna', value: 'CISTERNA' },
  { label: 'Portacontenedores', value: 'PORTACONTENEDORES' },
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
          trampillaElevadora: props.vehiculo.trampillaElevadora,
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
          trampillaElevadora: false,
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
    trampillaElevadora: form.value.trampillaElevadora,
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
    :style="{ width: '600px' }"
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
      <div class="grid grid-cols-2 gap-4">
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

      <!-- Dimensiones -->
      <div class="grid grid-cols-3 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Largo (mm)</label>
          <InputNumber
            v-model="form.largoUtilMm"
            placeholder="13600"
            class="w-full"
            :min="0"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Ancho (mm)</label>
          <InputNumber
            v-model="form.anchoUtilMm"
            placeholder="2480"
            class="w-full"
            :min="0"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Alto (mm)</label>
          <InputNumber
            v-model="form.altoUtilMm"
            placeholder="2700"
            class="w-full"
            :min="0"
          />
        </div>
      </div>

      <!-- Trampilla Elevadora -->
      <div class="flex items-center gap-3">
        <Checkbox
          v-model="form.trampillaElevadora"
          :binary="true"
          inputId="trampilla"
        />
        <label for="trampilla" class="text-sm font-medium text-gray-700 cursor-pointer">
          Trampilla elevadora
        </label>
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
