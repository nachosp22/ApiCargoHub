<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import { uploadProfilePhoto, deleteProfilePhoto, getProfilePhoto } from '@/services/api'

const authStore = useAuthStore()
const toast = useToast()

const visible = defineModel<boolean>('visible', { default: false })
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const previewUrl = ref<string | null>(null)
const activeTab = ref<'profile' | 'admin' | 'admins'>('profile')

const editNombre = ref('')
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const savingProfile = ref(false)
const changingPassword = ref(false)

const adminEmail = ref('')
const adminNombre = ref('')
const adminPassword = ref('')
const creatingAdmin = ref(false)
const deletingAdminId = ref<number | null>(null)
const togglingAdminId = ref<number | null>(null)
const adminsLoaded = ref(false)

const fotoUrl = computed(() => previewUrl.value ?? authStore.user?.fotoUrl ?? null)
const isSuperAdmin = computed(() => authStore.user?.role === 'SUPERADMIN')

const userInitials = computed(() => {
  const name = authStore.user?.nombre
  if (!name) return '?'
  return name
    .split(' ')
    .filter(Boolean)
    .map((w) => w[0].toUpperCase())
    .slice(0, 2)
    .join('')
})

onMounted(async () => {
  try {
    const response = await getProfilePhoto()
    if (response.status === 200 && response.data?.url) {
      authStore.setFotoUrl(response.data.url)
    }
  } catch {
  }
})

function resetForms() {
  editNombre.value = authStore.user?.nombre ?? ''
  currentPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  adminEmail.value = ''
  adminNombre.value = ''
  adminPassword.value = ''
  activeTab.value = 'profile'
}

function triggerFileInput() {
  fileInput.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) return
  if (file.size > 2 * 1024 * 1024) return

  uploading.value = true
  const temporaryObjectUrl = URL.createObjectURL(file)
  previewUrl.value = temporaryObjectUrl
  try {
    const base64 = await fileToBase64(file)
    const response = await uploadProfilePhoto(base64)
    if (response.data?.url) {
      authStore.setFotoUrl(response.data.url)
      previewUrl.value = null
    }
  } catch {
    previewUrl.value = null
  } finally {
    URL.revokeObjectURL(temporaryObjectUrl)
    uploading.value = false
    if (input) input.value = ''
  }
}

async function handleDeletePhoto() {
  uploading.value = true
  try {
    await deleteProfilePhoto()
    authStore.setFotoUrl(null)
    previewUrl.value = null
  } catch {
  } finally {
    uploading.value = false
  }
}

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      const base64 = result.split(',')[1]
      resolve(base64)
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function handleImageError() {
  previewUrl.value = null
  authStore.setFotoUrl(null)
}

async function handleSaveName() {
  if (!editNombre.value.trim()) return
  savingProfile.value = true
  const result = await authStore.updateProfile({ nombre: editNombre.value.trim() })
  savingProfile.value = false
  if (result.success) {
    toast.add({ severity: 'success', summary: 'Nombre actualizado', life: 3000 })
  } else {
    toast.add({ severity: 'error', summary: 'Error', detail: result.error ?? 'No se pudo actualizar', life: 5000 })
  }
}

async function handleChangePassword() {
  if (!currentPassword.value) {
    toast.add({ severity: 'warn', summary: 'Ingresá tu contraseña actual', life: 3000 })
    return
  }
  if (!newPassword.value || newPassword.value.length < 4) {
    toast.add({ severity: 'warn', summary: 'La contraseña nueva debe tener al menos 4 caracteres', life: 3000 })
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toast.add({ severity: 'warn', summary: 'Las contraseñas no coinciden', life: 3000 })
    return
  }
  changingPassword.value = true
  const result = await authStore.updateProfile({
    currentPassword: currentPassword.value,
    newPassword: newPassword.value,
  })
  changingPassword.value = false
  if (result.success) {
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    toast.add({ severity: 'success', summary: 'Contraseña actualizada', life: 3000 })
  } else {
    toast.add({ severity: 'error', summary: 'Error', detail: result.error ?? 'No se pudo cambiar la contraseña', life: 5000 })
  }
}

async function handleCreateAdmin() {
  if (!adminEmail.value.trim()) {
    toast.add({ severity: 'warn', summary: 'El email es obligatorio', life: 3000 })
    return
  }
  if (!adminNombre.value.trim()) {
    toast.add({ severity: 'warn', summary: 'El nombre es obligatorio', life: 3000 })
    return
  }
  if (!adminPassword.value || adminPassword.value.length < 4) {
    toast.add({ severity: 'warn', summary: 'La contraseña debe tener al menos 4 caracteres', life: 3000 })
    return
  }
  creatingAdmin.value = true
  const result = await authStore.createAdmin({
    email: adminEmail.value.trim(),
    nombre: adminNombre.value.trim(),
    password: adminPassword.value,
  })
  creatingAdmin.value = false
  if (result.success) {
    adminEmail.value = ''
    adminNombre.value = ''
    adminPassword.value = ''
    toast.add({ severity: 'success', summary: 'Administrador creado', detail: 'El nuevo admin ya puede iniciar sesión.', life: 4000 })
  } else {
    toast.add({ severity: 'error', summary: 'Error', detail: result.error ?? 'No se pudo crear el admin', life: 5000 })
  }
}

async function loadAdmins() {
  if (adminsLoaded.value) return
  adminsLoaded.value = true
  await authStore.fetchAdmins()
}

async function handleToggleAdmin(id: number) {
  togglingAdminId.value = id
  const activo = await authStore.toggleAdminActive(id)
  togglingAdminId.value = null
  toast.add({
    severity: activo ? 'success' : 'warn',
    summary: activo ? 'Acceso activado' : 'Acceso bloqueado',
    life: 3000,
  })
}

async function handleDeleteAdmin(id: number, nombre: string) {
  deletingAdminId.value = id
  const ok = await authStore.deleteAdmin(id)
  deletingAdminId.value = null
  if (ok) {
    toast.add({ severity: 'success', summary: 'Admin eliminado', detail: `${nombre} fue eliminado.`, life: 3000 })
  } else {
    toast.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar el admin', life: 5000 })
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm"
      @click.self="visible = false"
    >
      <div
        class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden"
        @vue:mounted="resetForms"
      >
        <div class="flex items-center justify-between px-6 pt-6 pb-2">
          <h2 class="text-lg font-semibold text-gray-800 dark:text-white">Mi perfil</h2>
          <button
            type="button"
            class="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            @click="visible = false"
          >
            <i class="pi pi-times text-sm"></i>
          </button>
        </div>

        <div v-if="isSuperAdmin" class="flex border-b border-gray-200 dark:border-gray-700 px-6">
          <button
            class="px-4 py-2.5 text-sm font-medium border-b-2 transition-colors"
            :class="activeTab === 'profile'
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'"
            @click="activeTab = 'profile'"
          >
            <i class="pi pi-user mr-1.5 text-xs" /> Perfil
          </button>
          <button
            class="px-4 py-2.5 text-sm font-medium border-b-2 transition-colors"
            :class="activeTab === 'admin'
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'"
            @click="activeTab = 'admin'"
          >
            <i class="pi pi-user-plus mr-1.5 text-xs" /> Crear Admin
          </button>
          <button
            class="px-4 py-2.5 text-sm font-medium border-b-2 transition-colors"
            :class="activeTab === 'admins'
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'"
            @click="activeTab = 'admins'; loadAdmins()"
          >
            <i class="pi pi-users mr-1.5 text-xs" /> Administradores
            <span
              v-if="authStore.admins.length"
              class="ml-1.5 inline-flex items-center justify-center min-w-[1.125rem] h-[1.125rem] px-1 text-[10px] font-bold rounded-full bg-primary text-white"
            >
              {{ authStore.admins.length }}
            </span>
          </button>
        </div>

        <div v-if="activeTab === 'profile'" class="px-6 pb-6 pt-4 flex flex-col items-center gap-5">
          <div class="relative group cursor-pointer" @click="triggerFileInput">
            <div
              class="w-24 h-24 rounded-full overflow-hidden border-2 border-gray-200 dark:border-gray-600 flex items-center justify-center bg-primary/10"
            >
              <img
                v-if="fotoUrl"
                :src="fotoUrl"
                alt="Avatar"
                class="w-full h-full object-cover"
                @error="handleImageError"
              />
              <span v-else class="text-3xl font-bold text-primary">
                {{ userInitials }}
              </span>
            </div>
            <div
              class="absolute inset-0 rounded-full bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
            >
              <i class="pi pi-camera text-white text-xl"></i>
            </div>
            <div
              v-if="uploading"
              class="absolute inset-0 rounded-full bg-black/60 flex items-center justify-center"
            >
              <i class="pi pi-spin pi-spinner text-white text-xl"></i>
            </div>
          </div>

          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            class="hidden"
            @change="onFileSelected"
          />

          <button
            v-if="fotoUrl"
            type="button"
            class="text-sm text-red-500 hover:text-red-600 dark:text-red-400 dark:hover:text-red-300 transition-colors"
            :disabled="uploading"
            @click="handleDeletePhoto"
          >
            Eliminar foto
          </button>

          <div class="text-center w-full">
            <p class="text-lg font-semibold text-gray-800 dark:text-white">
              {{ authStore.user?.nombre ?? 'Usuario' }}
            </p>
            <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
              {{ authStore.user?.email ?? '' }}
            </p>
            <span
              v-if="authStore.user?.role"
              class="inline-block mt-2 px-3 py-1 text-xs font-medium rounded-full bg-primary/10 text-primary"
            >
              {{ authStore.user.role }}
            </span>
          </div>

          <div class="w-full space-y-3 pt-2 border-t border-gray-100 dark:border-gray-700">
            <h4 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">Editar nombre</h4>
            <div class="flex gap-2">
              <input
                v-model="editNombre"
                type="text"
                placeholder="Tu nombre"
                class="flex-1 px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none"
              />
              <button
                type="button"
                class="px-4 py-2 text-sm font-medium bg-primary text-white rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50 shrink-0"
                :disabled="savingProfile || !editNombre.trim()"
                @click="handleSaveName"
              >
                <i v-if="savingProfile" class="pi pi-spin pi-spinner text-xs" />
                <span v-else>Guardar</span>
              </button>
            </div>
          </div>

          <div class="w-full space-y-3 pt-2 border-t border-gray-100 dark:border-gray-700">
            <h4 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">Cambiar contraseña</h4>
            <input
              v-model="currentPassword"
              type="password"
              placeholder="Contraseña actual"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none"
            />
            <input
              v-model="newPassword"
              type="password"
              placeholder="Nueva contraseña"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none"
            />
            <input
              v-model="confirmPassword"
              type="password"
              placeholder="Confirmar nueva contraseña"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none"
            />
            <button
              type="button"
              class="w-full py-2 text-sm font-medium bg-gray-800 dark:bg-gray-200 text-white dark:text-gray-800 rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50"
              :disabled="changingPassword"
              @click="handleChangePassword"
            >
              <i v-if="changingPassword" class="pi pi-spin pi-spinner mr-2 text-xs" />
              Actualizar contraseña
            </button>
          </div>
        </div>

        <!-- ====== ADMIN CREATION TAB ====== -->
        <div v-if="activeTab === 'admin'" class="px-6 pb-6 pt-4 space-y-4">
          <div class="bg-amber-50 dark:bg-amber-900/30 border border-amber-200 dark:border-amber-700 rounded-lg p-3 text-sm text-amber-800 dark:text-amber-200">
            <i class="pi pi-info-circle mr-1.5" />
            Como SUPERADMIN podés crear cuentas de administrador para tu equipo.
          </div>

          <div class="space-y-3">
            <div>
              <label class="block text-xs font-medium text-gray-600 dark:text-gray-300 mb-1">Nombre completo</label>
              <input
                v-model="adminNombre"
                type="text"
                placeholder="Ej: María García"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 dark:text-gray-300 mb-1">Email</label>
              <input
                v-model="adminEmail"
                type="email"
                placeholder="admin@cargohub.es"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 dark:text-gray-300 mb-1">Contraseña</label>
              <input
                v-model="adminPassword"
                type="password"
                placeholder="Mínimo 4 caracteres"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none"
              />
            </div>
          </div>

          <button
            type="button"
            class="w-full py-2.5 text-sm font-medium bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors disabled:opacity-50"
            :disabled="creatingAdmin"
            @click="handleCreateAdmin"
          >
            <i v-if="creatingAdmin" class="pi pi-spin pi-spinner mr-2 text-xs" />
            <i v-else class="pi pi-user-plus mr-2 text-xs" />
            Crear administrador
          </button>
        </div>

        <!-- ====== ADMINS LIST TAB ====== -->
        <div v-if="activeTab === 'admins'" class="px-6 pb-6 pt-4 space-y-3">
          <div v-if="authStore.adminsLoading" class="flex items-center justify-center py-8">
            <i class="pi pi-spin pi-spinner text-primary text-xl" />
          </div>

          <div v-else-if="!authStore.admins.length" class="text-center py-8 text-sm text-gray-500 dark:text-gray-400">
            No hay administradores registrados.
          </div>

          <ul v-else class="space-y-2">
            <li
              v-for="admin in authStore.admins"
              :key="admin.id"
              class="flex items-center justify-between gap-3 p-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900"
            >
              <div class="min-w-0">
                <p class="text-sm font-semibold text-gray-800 dark:text-gray-100 truncate">
                  {{ admin.nombre || 'Sin nombre' }}
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-400 truncate">{{ admin.email }}</p>
              </div>
              <div class="flex items-center gap-2 shrink-0">
                <span
                  class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium"
                  :class="admin.activo
                    ? 'bg-emerald-50 text-emerald-700'
                    : 'bg-red-50 text-red-700'"
                >
                  {{ admin.activo ? 'Activo' : 'Bloqueado' }}
                </span>
                <button
                  type="button"
                  class="p-1.5 text-xs rounded-lg transition-colors"
                  :class="admin.activo
                    ? 'text-amber-600 hover:bg-amber-50 dark:hover:bg-amber-900/30'
                    : 'text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-900/30'"
                  :disabled="togglingAdminId === admin.id"
                  :title="admin.activo ? 'Bloquear acceso' : 'Activar acceso'"
                  @click="handleToggleAdmin(admin.id)"
                >
                  <i
                    v-if="togglingAdminId === admin.id"
                    class="pi pi-spin pi-spinner text-xs"
                  />
                  <i v-else class="pi text-xs" :class="admin.activo ? 'pi-lock' : 'pi-lock-open'" />
                </button>
                <button
                  type="button"
                  class="p-1.5 text-xs text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors"
                  :disabled="deletingAdminId === admin.id"
                  :title="'Eliminar ' + (admin.nombre || 'admin')"
                  @click="handleDeleteAdmin(admin.id, admin.nombre || admin.email)"
                >
                  <i
                    v-if="deletingAdminId === admin.id"
                    class="pi pi-spin pi-spinner text-xs"
                  />
                  <i v-else class="pi pi-trash text-xs" />
                </button>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </Teleport>
</template>
