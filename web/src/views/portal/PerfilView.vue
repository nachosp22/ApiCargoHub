<template>
  <div class="max-w-3xl">
    <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-6">Mi Perfil</h2>

    <!-- Profile Section -->
    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-6">
      <div class="flex items-center justify-between mb-6">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Datos de Empresa</h3>
        <Button
          v-if="!editing"
          label="Editar"
          icon="pi pi-pencil"
          severity="secondary"
          size="small"
          @click="editing = true"
        />
      </div>

      <div v-if="loadingProfile" class="flex justify-center py-8">
        <i class="pi pi-spin pi-spinner text-2xl text-gray-400"></i>
      </div>

      <form v-else @submit.prevent="saveProfile" class="space-y-4">
        <div class="grid md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nombre de Empresa</label>
            <InputText v-model="form.nombreEmpresa" class="w-full" :disabled="!editing" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">CIF/NIF</label>
            <InputText :modelValue="form.cif" class="w-full" disabled />
            <p class="text-xs text-gray-400 dark:text-gray-500 mt-1">No editable</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Direccion Fiscal</label>
            <InputText v-model="form.direccionFiscal" class="w-full" :disabled="!editing" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Telefono</label>
            <InputText v-model="form.telefono" class="w-full" :disabled="!editing" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email de Contacto</label>
            <InputText v-model="form.emailContacto" class="w-full" :disabled="!editing" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Sector</label>
            <Dropdown
              v-model="form.sector"
              :options="sectores"
              placeholder="Seleccionar sector"
              class="w-full"
              :disabled="!editing"
            />
          </div>
        </div>

        <div v-if="editing" class="flex gap-3 pt-2">
          <Button type="submit" label="Guardar" icon="pi pi-check" :loading="saving" />
          <Button label="Cancelar" severity="secondary" @click="cancelEdit" />
        </div>
      </form>
    </div>

    <!-- Change Password Section -->
    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-6">Cambiar Contrasena</h3>

      <form @submit.prevent="handleChangePassword" class="space-y-4 max-w-md">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Contrasena Actual</label>
          <InputText v-model="pwForm.currentPassword" type="password" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nueva Contrasena</label>
          <InputText v-model="pwForm.newPassword" type="password" class="w-full" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Confirmar Nueva Contrasena</label>
          <InputText v-model="pwForm.confirmPassword" type="password" class="w-full" />
        </div>
        <Button
          type="submit"
          label="Cambiar Contraseña"
          icon="pi pi-lock"
          :loading="changingPassword"
        />
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'

const authStore = useAuthStore()
const toast = useToast()

const editing = ref(false)
const loadingProfile = ref(true)
const saving = ref(false)
const changingPassword = ref(false)

const sectores = [
  'Alimentación',
  'Automoción',
  'Construcción',
  'Electrónica',
  'Farmacéutico',
  'Industrial',
  'Logística',
  'Retail',
  'Textil',
  'Otro',
]

const form = ref({
  nombreEmpresa: '',
  cif: '',
  direccionFiscal: '',
  telefono: '',
  emailContacto: '',
  sector: '',
})

const pwForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

onMounted(async () => {
  await loadProfile()
})

async function loadProfile() {
  loadingProfile.value = true
  try {
    const clienteId = authStore.clienteId
    if (!clienteId) return
    const data = await authStore.fetchProfile(clienteId) as Record<string, string>
    form.value = {
      nombreEmpresa: data.nombreEmpresa ?? '',
      cif: data.cif ?? '',
      direccionFiscal: data.direccionFiscal ?? '',
      telefono: data.telefono ?? '',
      emailContacto: data.emailContacto ?? '',
      sector: data.sector ?? '',
    }
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'No se pudo cargar el perfil', life: 3000 })
  } finally {
    loadingProfile.value = false
  }
}

function cancelEdit() {
  editing.value = false
  loadProfile()
}

async function saveProfile() {
  saving.value = true
  try {
    const clienteId = authStore.clienteId
    if (!clienteId) return
    await authStore.updateProfile(clienteId, {
      nombreEmpresa: form.value.nombreEmpresa,
      direccionFiscal: form.value.direccionFiscal,
      telefono: form.value.telefono,
      emailContacto: form.value.emailContacto,
      sector: form.value.sector,
    })
    editing.value = false
    toast.add({ severity: 'success', summary: 'Perfil actualizado', detail: 'Los datos se guardaron correctamente', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'No se pudo guardar el perfil', life: 3000 })
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  if (pwForm.value.newPassword !== pwForm.value.confirmPassword) {
    toast.add({ severity: 'warn', summary: 'Error', detail: 'Las contraseñas no coinciden', life: 3000 })
    return
  }
  if (pwForm.value.newPassword.length < 6) {
    toast.add({ severity: 'warn', summary: 'Error', detail: 'La contraseña debe tener al menos 6 caracteres', life: 3000 })
    return
  }

  changingPassword.value = true
  try {
    const clienteId = authStore.clienteId
    if (!clienteId) return
    await authStore.changePassword(clienteId, pwForm.value.currentPassword, pwForm.value.newPassword)
    pwForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    toast.add({ severity: 'success', summary: 'Contraseña cambiada', detail: 'Tu contraseña se actualizó correctamente', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'No se pudo cambiar la contraseña. Verifica tu contraseña actual.', life: 3000 })
  } finally {
    changingPassword.value = false
  }
}
</script>
