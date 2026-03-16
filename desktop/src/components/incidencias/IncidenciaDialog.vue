<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import type { CrearIncidenciaRequest, SeveridadIncidencia, PrioridadIncidencia, PorteRef } from '@/stores/incidencias'

interface Props {
  visible: boolean
  porteOptions: PorteRef[]
  saving?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  saving: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save', porteId: number, data: CrearIncidenciaRequest): void
}>()

// --- Form State ---
const form = ref({
  porteId: null as number | null,
  titulo: '',
  descripcion: '',
  severidad: 'MEDIA' as SeveridadIncidencia,
  prioridad: 'MEDIA' as PrioridadIncidencia,
})

const submitted = ref(false)

// Validation
const errors = computed(() => ({
  porteId: submitted.value && !form.value.porteId,
  titulo: submitted.value && !form.value.titulo.trim(),
  descripcion: submitted.value && !form.value.descripcion.trim(),
}))

const isValid = computed(
  () => !!form.value.porteId && form.value.titulo.trim() && form.value.descripcion.trim()
)

const severidadOptions = [
  { label: 'Alta', value: 'ALTA' },
  { label: 'Media', value: 'MEDIA' },
  { label: 'Baja', value: 'BAJA' },
]

const prioridadOptions = [
  { label: 'Alta', value: 'ALTA' },
  { label: 'Media', value: 'MEDIA' },
  { label: 'Baja', value: 'BAJA' },
]

const porteDropdownOptions = computed(() =>
  props.porteOptions.map((p) => ({
    label: p.label,
    value: p.id,
  }))
)

// Reset form when dialog opens
watch(
  () => props.visible,
  (val) => {
    if (val) {
      submitted.value = false
      form.value = {
        porteId: null,
        titulo: '',
        descripcion: '',
        severidad: 'MEDIA',
        prioridad: 'MEDIA',
      }
    }
  }
)

function onSubmit(): void {
  submitted.value = true
  if (!isValid.value) return

  const data: CrearIncidenciaRequest = {
    titulo: form.value.titulo.trim(),
    descripcion: form.value.descripcion.trim(),
    severidad: form.value.severidad,
    prioridad: form.value.prioridad,
  }

  emit('save', form.value.porteId!, data)
}

function onClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    header="Nueva Incidencia"
    :modal="true"
    :closable="true"
    :style="{ width: '600px' }"
    @update:visible="onClose"
  >
    <div class="space-y-5 pt-2">
      <!-- Porte -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Porte <span class="text-red-500">*</span>
        </label>
        <Select
          v-model="form.porteId"
          :options="porteDropdownOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Seleccionar porte"
          class="w-full"
          :invalid="errors.porteId"
          filter
          filterPlaceholder="Buscar porte..."
        />
        <small v-if="errors.porteId" class="text-red-500 text-xs mt-1">Campo requerido</small>
      </div>

      <!-- Título -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Título <span class="text-red-500">*</span>
        </label>
        <InputText
          v-model="form.titulo"
          placeholder="Título breve de la incidencia"
          class="w-full"
          :invalid="errors.titulo"
          maxlength="150"
        />
        <small v-if="errors.titulo" class="text-red-500 text-xs mt-1">Campo requerido</small>
      </div>

      <!-- Descripción -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Descripción <span class="text-red-500">*</span>
        </label>
        <Textarea
          v-model="form.descripcion"
          rows="4"
          placeholder="Describe la incidencia con detalle..."
          class="w-full"
          :invalid="errors.descripcion"
          autoResize
          maxlength="4000"
        />
        <small v-if="errors.descripcion" class="text-red-500 text-xs mt-1">Campo requerido</small>
      </div>

      <!-- Severidad / Prioridad -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Severidad</label>
          <Select
            v-model="form.severidad"
            :options="severidadOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Prioridad</label>
          <Select
            v-model="form.prioridad"
            :options="prioridadOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
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
          label="Crear Incidencia"
          icon="pi pi-check"
          @click="onSubmit"
          :loading="saving"
        />
      </div>
    </template>
  </Dialog>
</template>
