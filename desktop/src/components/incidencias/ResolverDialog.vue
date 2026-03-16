<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import Button from 'primevue/button'
import IncidenciaStatusBadge from './IncidenciaStatusBadge.vue'
import type { Incidencia, EstadoIncidencia, ResolverIncidenciaRequest } from '@/stores/incidencias'

interface Props {
  visible: boolean
  incidencia: Incidencia | null
  saving?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  incidencia: null,
  saving: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'resolve', id: number, data: ResolverIncidenciaRequest): void
}>()

// --- Form State ---
const form = ref({
  estadoFinal: '' as EstadoIncidencia | '',
  resolucion: '',
  comentario: '',
})

const submitted = ref(false)

// Valid transitions
const validTransitions: Record<string, EstadoIncidencia[]> = {
  ABIERTA: ['EN_REVISION', 'RESUELTA', 'DESESTIMADA'],
  EN_REVISION: ['RESUELTA', 'DESESTIMADA'],
}

const estadoLabels: Record<string, string> = {
  EN_REVISION: 'En Revisión',
  RESUELTA: 'Resuelta',
  DESESTIMADA: 'Desestimada',
}

const transitionOptions = computed(() => {
  if (!props.incidencia) return []
  const transitions = validTransitions[props.incidencia.estado] ?? []
  return transitions.map((t) => ({
    label: estadoLabels[t] ?? t,
    value: t,
  }))
})

const needsResolucion = computed(() => form.value.estadoFinal === 'RESUELTA')

// Validation
const errors = computed(() => ({
  estadoFinal: submitted.value && !form.value.estadoFinal,
  resolucion: submitted.value && needsResolucion.value && !form.value.resolucion.trim(),
}))

const isValid = computed(
  () => !!form.value.estadoFinal && (!needsResolucion.value || form.value.resolucion.trim())
)

// Reset form when dialog opens
watch(
  () => props.visible,
  (val) => {
    if (val) {
      submitted.value = false
      form.value = {
        estadoFinal: '',
        resolucion: '',
        comentario: '',
      }
    }
  }
)

function onSubmit(): void {
  submitted.value = true
  if (!isValid.value || !props.incidencia) return

  const data: ResolverIncidenciaRequest = {
    estadoFinal: form.value.estadoFinal as EstadoIncidencia,
    resolucion: form.value.resolucion.trim() || undefined,
  }

  emit('resolve', props.incidencia.id, data)
}

function onClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    header="Resolver Incidencia"
    :modal="true"
    :closable="true"
    :style="{ width: '550px' }"
    @update:visible="onClose"
  >
    <div v-if="incidencia" class="space-y-5 pt-2">
      <!-- Current info -->
      <div class="bg-gray-50 rounded-xl p-4">
        <div class="flex items-center justify-between">
          <div>
            <span class="text-sm text-gray-500">Incidencia</span>
            <p class="text-gray-800 font-semibold">#{{ incidencia.id }} — {{ incidencia.titulo }}</p>
          </div>
          <IncidenciaStatusBadge :estado="incidencia.estado" />
        </div>
      </div>

      <!-- Nuevo Estado -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Nuevo Estado <span class="text-red-500">*</span>
        </label>
        <Select
          v-model="form.estadoFinal"
          :options="transitionOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Seleccionar estado"
          class="w-full"
          :invalid="errors.estadoFinal"
        />
        <small v-if="errors.estadoFinal" class="text-red-500 text-xs mt-1">Campo requerido</small>
      </div>

      <!-- Resolución (required for RESUELTA) -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Resolución
          <span v-if="needsResolucion" class="text-red-500">*</span>
        </label>
        <Textarea
          v-model="form.resolucion"
          rows="4"
          placeholder="Describe cómo se resolvió la incidencia..."
          class="w-full"
          :invalid="errors.resolucion"
          autoResize
          maxlength="4000"
        />
        <small v-if="errors.resolucion" class="text-red-500 text-xs mt-1">
          Campo requerido para resolver la incidencia
        </small>
      </div>

      <!-- Comentario (optional) -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Comentario (opcional)</label>
        <Textarea
          v-model="form.comentario"
          rows="2"
          placeholder="Comentario adicional..."
          class="w-full"
          autoResize
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
          label="Confirmar"
          icon="pi pi-check"
          @click="onSubmit"
          :loading="saving"
        />
      </div>
    </template>
  </Dialog>
</template>
