<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'
import type { Porte, CreatePorteRequest, EstadoPorte, Conductor, Vehiculo, Cliente } from '@/stores/portes'

interface Props {
  visible: boolean
  porte?: Porte | null
  conductores: Conductor[]
  vehiculos: Vehiculo[]
  clientes: Cliente[]
  saving?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  porte: null,
  saving: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save', data: CreatePorteRequest & { estado?: EstadoPorte }): void
}>()

const isEditing = computed(() => !!props.porte)
const dialogTitle = computed(() => (isEditing.value ? `Editar Porte #${props.porte?.id}` : 'Nuevo Porte'))

// --- Form State ---
const form = ref({
  origen: '',
  destino: '',
  clienteId: null as number | null,
  conductorId: null as number | null,
  vehiculoId: null as number | null,
  fechaRecogida: null as Date | null,
  fechaEntrega: null as Date | null,
  descripcionCliente: '',
  estado: 'PENDIENTE' as EstadoPorte,
})

const submitted = ref(false)

// Validation
const errors = computed(() => ({
  origen: submitted.value && !form.value.origen.trim(),
  destino: submitted.value && !form.value.destino.trim(),
  clienteId: submitted.value && !isEditing.value && !form.value.clienteId,
}))

const isValid = computed(
  () => form.value.origen.trim() && form.value.destino.trim() && (isEditing.value || form.value.clienteId)
)

// Estado options for edit mode
const estadoOptions = [
  { label: 'Pendiente', value: 'PENDIENTE' },
  { label: 'Asignado', value: 'ASIGNADO' },
  { label: 'En Tránsito', value: 'EN_TRANSITO' },
  { label: 'Entregado', value: 'ENTREGADO' },
  { label: 'Cancelado', value: 'CANCELADO' },
  { label: 'Facturado', value: 'FACTURADO' },
]

// Dropdown options
const conductorOptions = computed(() =>
  props.conductores.map((c) => ({
    label: `${c.nombre}${c.apellidos ? ' ' + c.apellidos : ''}`,
    value: c.id,
  }))
)

const vehiculoOptions = computed(() =>
  props.vehiculos.map((v) => ({
    label: `${v.matricula} — ${v.marca ?? ''} ${v.modelo ?? ''}`.trim(),
    value: v.id,
  }))
)

const clienteOptions = computed(() =>
  props.clientes.map((c) => ({
    label: c.nombreEmpresa,
    value: c.id,
  }))
)

// Reset form when dialog opens
watch(
  () => props.visible,
  (val) => {
    if (val) {
      submitted.value = false
      if (props.porte) {
        form.value = {
          origen: props.porte.origen,
          destino: props.porte.destino,
          clienteId: props.porte.cliente?.id ?? null,
          conductorId: props.porte.conductor?.id ?? null,
          vehiculoId: null,
          fechaRecogida: props.porte.fechaRecogida ? new Date(props.porte.fechaRecogida) : null,
          fechaEntrega: props.porte.fechaEntrega ? new Date(props.porte.fechaEntrega) : null,
          descripcionCliente: props.porte.descripcionCliente ?? '',
          estado: props.porte.estado,
        }
      } else {
        form.value = {
          origen: '',
          destino: '',
          clienteId: null,
          conductorId: null,
          vehiculoId: null,
          fechaRecogida: null,
          fechaEntrega: null,
          descripcionCliente: '',
          estado: 'PENDIENTE',
        }
      }
    }
  }
)

function onSubmit(): void {
  submitted.value = true
  if (!isValid.value) return

  const data: CreatePorteRequest & { estado?: EstadoPorte } = {
    origen: form.value.origen.trim(),
    destino: form.value.destino.trim(),
    clienteId: form.value.clienteId,
    descripcionCliente: form.value.descripcionCliente.trim() || undefined,
    fechaRecogida: form.value.fechaRecogida ? form.value.fechaRecogida.toISOString() : undefined,
    fechaEntrega: form.value.fechaEntrega ? form.value.fechaEntrega.toISOString() : undefined,
  }

  if (isEditing.value) {
    data.estado = form.value.estado
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
      <!-- Origen / Destino -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Origen <span class="text-red-500">*</span>
          </label>
          <InputText
            v-model="form.origen"
            placeholder="Ciudad de origen"
            class="w-full"
            :invalid="errors.origen"
          />
          <small v-if="errors.origen" class="text-red-500 text-xs mt-1">Campo requerido</small>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Destino <span class="text-red-500">*</span>
          </label>
          <InputText
            v-model="form.destino"
            placeholder="Ciudad de destino"
            class="w-full"
            :invalid="errors.destino"
          />
          <small v-if="errors.destino" class="text-red-500 text-xs mt-1">Campo requerido</small>
        </div>
      </div>

      <!-- Cliente -->
      <div v-if="!isEditing">
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Cliente <span class="text-red-500">*</span>
        </label>
        <Select
          v-model="form.clienteId"
          :options="clienteOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Seleccionar cliente"
          class="w-full"
          :invalid="errors.clienteId"
        />
        <small v-if="errors.clienteId" class="text-red-500 text-xs mt-1">Campo requerido</small>
      </div>

      <!-- Conductor / Vehículo -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Conductor</label>
          <Select
            v-model="form.conductorId"
            :options="conductorOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Seleccionar conductor"
            class="w-full"
            showClear
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Vehículo</label>
          <Select
            v-model="form.vehiculoId"
            :options="vehiculoOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Seleccionar vehículo"
            class="w-full"
            showClear
          />
        </div>
      </div>

      <!-- Fechas -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Fecha Recogida</label>
          <DatePicker
            v-model="form.fechaRecogida"
            showTime
            hourFormat="24"
            dateFormat="dd/mm/yy"
            placeholder="Seleccionar fecha"
            class="w-full"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Fecha Entrega</label>
          <DatePicker
            v-model="form.fechaEntrega"
            showTime
            hourFormat="24"
            dateFormat="dd/mm/yy"
            placeholder="Seleccionar fecha"
            class="w-full"
          />
        </div>
      </div>

      <!-- Descripción -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Descripción / Notas</label>
        <Textarea
          v-model="form.descripcionCliente"
          rows="3"
          placeholder="Descripción de la carga, instrucciones especiales..."
          class="w-full"
          autoResize
        />
      </div>

      <!-- Estado (solo en edición) -->
      <div v-if="isEditing">
        <label class="block text-sm font-medium text-gray-700 mb-1">Estado</label>
        <Select
          v-model="form.estado"
          :options="estadoOptions"
          optionLabel="label"
          optionValue="value"
          class="w-full"
        />
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
          :label="isEditing ? 'Guardar Cambios' : 'Crear Porte'"
          icon="pi pi-check"
          @click="onSubmit"
          :loading="saving"
        />
      </div>
    </template>
  </Dialog>
</template>
