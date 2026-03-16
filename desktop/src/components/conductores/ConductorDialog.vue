<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import type { Conductor, CreateConductorRequest } from '@/stores/conductores'

interface Props {
  visible: boolean
  conductor?: Conductor | null
  saving?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  conductor: null,
  saving: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save', data: CreateConductorRequest): void
}>()

const isEditing = computed(() => !!props.conductor)
const dialogTitle = computed(() =>
  isEditing.value ? `Editar Conductor #${props.conductor?.id}` : 'Nuevo Conductor'
)

// --- Form State ---
const form = ref({
  nombre: '',
  apellidos: '',
  email: '',
  telefono: '',
  dni: '',
  password: '',
  ciudadBase: '',
})

const submitted = ref(false)

// Validation
const errors = computed(() => ({
  nombre: submitted.value && !form.value.nombre.trim(),
  apellidos: submitted.value && !form.value.apellidos.trim(),
  email: submitted.value && !form.value.email.trim(),
  password: submitted.value && !isEditing.value && !form.value.password.trim(),
}))

const isValid = computed(
  () =>
    form.value.nombre.trim() &&
    form.value.apellidos.trim() &&
    form.value.email.trim() &&
    (isEditing.value || form.value.password.trim())
)

// Reset form when dialog opens
watch(
  () => props.visible,
  (val) => {
    if (val) {
      submitted.value = false
      if (props.conductor) {
        form.value = {
          nombre: props.conductor.nombre,
          apellidos: props.conductor.apellidos,
          email: props.conductor.email,
          telefono: props.conductor.telefono ?? '',
          dni: props.conductor.dni ?? '',
          password: '',
          ciudadBase: props.conductor.ciudadBase ?? '',
        }
      } else {
        form.value = {
          nombre: '',
          apellidos: '',
          email: '',
          telefono: '',
          dni: '',
          password: '',
          ciudadBase: '',
        }
      }
    }
  }
)

function onSubmit(): void {
  submitted.value = true
  if (!isValid.value) return

  const data: CreateConductorRequest = {
    nombre: form.value.nombre.trim(),
    apellidos: form.value.apellidos.trim(),
    email: form.value.email.trim(),
    telefono: form.value.telefono.trim() || undefined,
    dni: form.value.dni.trim() || undefined,
    ciudadBase: form.value.ciudadBase.trim() || undefined,
  }

  if (!isEditing.value && form.value.password.trim()) {
    data.password = form.value.password.trim()
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
    :style="{ width: '550px' }"
    @update:visible="onClose"
  >
    <div class="space-y-5 pt-2">
      <!-- Nombre / Apellidos -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Nombre <span class="text-red-500">*</span>
          </label>
          <InputText
            v-model="form.nombre"
            placeholder="Nombre"
            class="w-full"
            :invalid="errors.nombre"
          />
          <small v-if="errors.nombre" class="text-red-500 text-xs mt-1">Campo requerido</small>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Apellidos <span class="text-red-500">*</span>
          </label>
          <InputText
            v-model="form.apellidos"
            placeholder="Apellidos"
            class="w-full"
            :invalid="errors.apellidos"
          />
          <small v-if="errors.apellidos" class="text-red-500 text-xs mt-1">Campo requerido</small>
        </div>
      </div>

      <!-- Email -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Email <span class="text-red-500">*</span>
        </label>
        <InputText
          v-model="form.email"
          placeholder="correo@ejemplo.com"
          class="w-full"
          :invalid="errors.email"
          :disabled="isEditing"
        />
        <small v-if="errors.email" class="text-red-500 text-xs mt-1">Campo requerido</small>
        <small v-if="isEditing" class="text-gray-400 text-xs mt-1">El email no se puede modificar</small>
      </div>

      <!-- Teléfono / DNI -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
          <InputText
            v-model="form.telefono"
            placeholder="612345678"
            class="w-full"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">DNI / Licencia</label>
          <InputText
            v-model="form.dni"
            placeholder="12345678A"
            class="w-full"
          />
        </div>
      </div>

      <!-- Ciudad Base -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Ciudad Base</label>
        <InputText
          v-model="form.ciudadBase"
          placeholder="Madrid"
          class="w-full"
        />
      </div>

      <!-- Password (solo en creación) -->
      <div v-if="!isEditing">
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Contraseña <span class="text-red-500">*</span>
        </label>
        <Password
          v-model="form.password"
          placeholder="Contraseña"
          class="w-full"
          :invalid="errors.password"
          toggleMask
          :feedback="false"
          :pt="{ pcInputText: { root: { class: 'w-full' } } }"
        />
        <small v-if="errors.password" class="text-red-500 text-xs mt-1">Campo requerido</small>
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
          :label="isEditing ? 'Guardar Cambios' : 'Crear Conductor'"
          icon="pi pi-check"
          @click="onSubmit"
          :loading="saving"
        />
      </div>
    </template>
  </Dialog>
</template>
