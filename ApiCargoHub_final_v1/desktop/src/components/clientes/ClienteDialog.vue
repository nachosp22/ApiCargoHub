<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import type { Cliente, CreateClienteRequest } from '@/stores/clientes'

interface Props {
  visible: boolean
  cliente?: Cliente | null
  saving?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  cliente: null,
  saving: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save', data: CreateClienteRequest): void
}>()

const isEditing = computed(() => !!props.cliente)
const dialogTitle = computed(() =>
  isEditing.value ? `Editar Cliente #${props.cliente?.id}` : 'Nuevo Cliente'
)

// --- Form State ---
const form = ref({
  nombreEmpresa: '',
  cif: '',
  emailContacto: '',
  telefono: '',
  direccion: '',
  ciudad: '',
  codigoPostal: '',
  pais: '',
})

const submitted = ref(false)

// Validation
const errors = computed(() => ({
  nombreEmpresa: submitted.value && !form.value.nombreEmpresa.trim(),
  cif: submitted.value && !form.value.cif.trim(),
  emailContacto: submitted.value && !form.value.emailContacto.trim(),
}))

const isValid = computed(
  () =>
    form.value.nombreEmpresa.trim() &&
    form.value.cif.trim() &&
    form.value.emailContacto.trim()
)

// Reset form when dialog opens
watch(
  () => props.visible,
  (val) => {
    if (val) {
      submitted.value = false
      if (props.cliente) {
        form.value = {
          nombreEmpresa: props.cliente.nombreEmpresa,
          cif: props.cliente.cif,
          emailContacto: props.cliente.emailContacto,
          telefono: props.cliente.telefono ?? '',
          direccion: props.cliente.direccion ?? '',
          ciudad: props.cliente.ciudad ?? '',
          codigoPostal: props.cliente.codigoPostal ?? '',
          pais: props.cliente.pais ?? '',
        }
      } else {
        form.value = {
          nombreEmpresa: '',
          cif: '',
          emailContacto: '',
          telefono: '',
          direccion: '',
          ciudad: '',
          codigoPostal: '',
          pais: 'España',
        }
      }
    }
  }
)

function onSubmit(): void {
  submitted.value = true
  if (!isValid.value) return

  const data: CreateClienteRequest = {
    nombreEmpresa: form.value.nombreEmpresa.trim(),
    cif: form.value.cif.trim(),
    emailContacto: form.value.emailContacto.trim(),
    telefono: form.value.telefono.trim() || undefined,
    direccion: form.value.direccion.trim() || undefined,
    ciudad: form.value.ciudad.trim() || undefined,
    codigoPostal: form.value.codigoPostal.trim() || undefined,
    pais: form.value.pais.trim() || undefined,
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
      <!-- Nombre Empresa -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
          Nombre / Razón Social <span class="text-red-500">*</span>
        </label>
        <InputText
          v-model="form.nombreEmpresa"
          placeholder="Nombre de la empresa"
          class="w-full"
          :invalid="errors.nombreEmpresa"
        />
        <small v-if="errors.nombreEmpresa" class="text-red-500 text-xs mt-1">Campo requerido</small>
      </div>

      <!-- CIF / Email -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            CIF/NIF <span class="text-red-500">*</span>
          </label>
          <InputText
            v-model="form.cif"
            placeholder="B12345678"
            class="w-full"
            :invalid="errors.cif"
          />
          <small v-if="errors.cif" class="text-red-500 text-xs mt-1">Campo requerido</small>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Email <span class="text-red-500">*</span>
          </label>
          <InputText
            v-model="form.emailContacto"
            placeholder="contacto@empresa.es"
            class="w-full"
            :invalid="errors.emailContacto"
          />
          <small v-if="errors.emailContacto" class="text-red-500 text-xs mt-1">Campo requerido</small>
        </div>
      </div>

      <!-- Teléfono -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Teléfono</label>
        <InputText
          v-model="form.telefono"
          placeholder="911234567"
          class="w-full"
        />
      </div>

      <!-- Dirección -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Dirección</label>
        <InputText
          v-model="form.direccion"
          placeholder="Calle, número, piso..."
          class="w-full"
        />
      </div>

      <!-- Ciudad / CP / País -->
      <div class="grid grid-cols-3 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Ciudad</label>
          <InputText
            v-model="form.ciudad"
            placeholder="Madrid"
            class="w-full"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Código Postal</label>
          <InputText
            v-model="form.codigoPostal"
            placeholder="28001"
            class="w-full"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">País</label>
          <InputText
            v-model="form.pais"
            placeholder="España"
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
          :label="isEditing ? 'Guardar Cambios' : 'Crear Cliente'"
          icon="pi pi-check"
          @click="onSubmit"
          :loading="saving"
        />
      </div>
    </template>
  </Dialog>
</template>
