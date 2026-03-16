<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'
import Button from 'primevue/button'
import type { Vehiculo, CreateVehiculoRequest, TipoVehiculo } from '@/stores/vehiculos'

interface ConductorOption {
  id: number
  nombre: string
  apellidos: string
}

interface Props {
  visible: boolean
  vehiculo?: Vehiculo | null
  saving?: boolean
  conductores?: ConductorOption[]
}

const props = withDefaults(defineProps<Props>(), {
  vehiculo: null,
  saving: false,
  conductores: () => [],
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save', data: CreateVehiculoRequest): void
}>()

const isEditing = computed(() => !!props.vehiculo)
const dialogTitle = computed(() =>
  isEditing.value ? `Editar Vehículo #${props.vehiculo?.id}` : 'Nuevo Vehículo'
)

const tipoOptions = [
  { label: 'Furgoneta', value: 'FURGONETA' as TipoVehiculo },
  { label: 'Rígido', value: 'RIGIDO' as TipoVehiculo },
  { label: 'Tráiler', value: 'TRAILER' as TipoVehiculo },
  { label: 'Especial', value: 'ESPECIAL' as TipoVehiculo },
]

const conductorOptions = computed(() => {
  return [
    { label: 'Sin asignar', value: null as number | null },
    ...props.conductores.map((c) => ({
      label: `${c.nombre} ${c.apellidos}`,
      value: c.id as number | null,
    })),
  ]
})

// --- Form State ---
const form = ref({
  matricula: '',
  marca: '',
  modelo: '',
  tipo: 'FURGONETA' as TipoVehiculo,
  capacidadCargaKg: null as number | null,
  largoUtilMm: null as number | null,
  anchoUtilMm: null as number | null,
  altoUtilMm: null as number | null,
  trampillaElevadora: false,
  conductorId: null as number | null,
})

const submitted = ref(false)

// Validation
const errors = computed(() => ({
  matricula: submitted.value && !form.value.matricula.trim(),
  marca: submitted.value && !form.value.marca.trim(),
  modelo: submitted.value && !form.value.modelo.trim(),
}))

const isValid = computed(
  () =>
    form.value.matricula.trim() &&
    form.value.marca.trim() &&
    form.value.modelo.trim()
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
          conductorId: props.vehiculo.conductor?.id ?? null,
        }
      } else {
        form.value = {
          matricula: '',
          marca: '',
          modelo: '',
          tipo: 'FURGONETA',
          capacidadCargaKg: null,
          largoUtilMm: null,
          anchoUtilMm: null,
          altoUtilMm: null,
          trampillaElevadora: false,
          conductorId: null,
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
    tipo: form.value.tipo,
    capacidadCargaKg: form.value.capacidadCargaKg,
    largoUtilMm: form.value.largoUtilMm,
    anchoUtilMm: form.value.anchoUtilMm,
    altoUtilMm: form.value.altoUtilMm,
    trampillaElevadora: form.value.trampillaElevadora,
    conductor: form.value.conductorId ? { id: form.value.conductorId } : null,
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
          class="w-full font-mono uppercase"
          :invalid="errors.matricula"
        />
        <small v-if="errors.matricula" class="text-red-500 text-xs mt-1">Campo requerido</small>
      </div>

      <!-- Marca / Modelo -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Marca <span class="text-red-500">*</span>
          </label>
          <InputText
            v-model="form.marca"
            placeholder="Mercedes-Benz"
            class="w-full"
            :invalid="errors.marca"
          />
          <small v-if="errors.marca" class="text-red-500 text-xs mt-1">Campo requerido</small>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Modelo <span class="text-red-500">*</span>
          </label>
          <InputText
            v-model="form.modelo"
            placeholder="Atego 1224"
            class="w-full"
            :invalid="errors.modelo"
          />
          <small v-if="errors.modelo" class="text-red-500 text-xs mt-1">Campo requerido</small>
        </div>
      </div>

      <!-- Tipo / Capacidad -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Tipo</label>
          <Select
            v-model="form.tipo"
            :options="tipoOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Capacidad de Carga (kg)</label>
          <InputNumber
            v-model="form.capacidadCargaKg"
            placeholder="6000"
            class="w-full"
            :min="0"
            :max="50000"
            suffix=" kg"
          />
        </div>
      </div>

      <!-- Dimensiones -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">Dimensiones útiles (mm)</label>
        <div class="grid grid-cols-3 gap-4">
          <div>
            <InputNumber
              v-model="form.largoUtilMm"
              placeholder="Largo"
              class="w-full"
              :min="0"
              suffix=" mm"
            />
            <small class="text-gray-400 text-xs mt-0.5">Largo</small>
          </div>
          <div>
            <InputNumber
              v-model="form.anchoUtilMm"
              placeholder="Ancho"
              class="w-full"
              :min="0"
              suffix=" mm"
            />
            <small class="text-gray-400 text-xs mt-0.5">Ancho</small>
          </div>
          <div>
            <InputNumber
              v-model="form.altoUtilMm"
              placeholder="Alto"
              class="w-full"
              :min="0"
              suffix=" mm"
            />
            <small class="text-gray-400 text-xs mt-0.5">Alto</small>
          </div>
        </div>
      </div>

      <!-- Conductor Asignado -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Conductor Asignado</label>
        <Select
          v-model="form.conductorId"
          :options="conductorOptions"
          optionLabel="label"
          optionValue="value"
          class="w-full"
          placeholder="Seleccionar conductor..."
        />
      </div>

      <!-- Trampilla -->
      <div class="flex items-center gap-3">
        <Checkbox
          v-model="form.trampillaElevadora"
          :binary="true"
          inputId="trampilla"
        />
        <label for="trampilla" class="text-sm text-gray-700 cursor-pointer">
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
