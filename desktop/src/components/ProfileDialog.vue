<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { uploadProfilePhoto, deleteProfilePhoto, getProfilePhoto } from '@/services/api'

const authStore = useAuthStore()

const visible = defineModel<boolean>('visible', { default: false })
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const fotoUrl = computed(() => authStore.user?.fotoUrl ?? null)

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
    // No photo or error — keep current state
  }
})

function triggerFileInput() {
  fileInput.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // Validate
  if (!file.type.startsWith('image/')) return
  if (file.size > 2 * 1024 * 1024) return // 2MB raw limit

  uploading.value = true
  try {
    const base64 = await fileToBase64(file)
    const response = await uploadProfilePhoto(base64)
    if (response.data?.url) {
      authStore.setFotoUrl(response.data.url)
    }
  } catch {
    // Error handled silently — could add toast
  } finally {
    uploading.value = false
    // Reset input so same file can be re-selected
    if (input) input.value = ''
  }
}

async function handleDeletePhoto() {
  uploading.value = true
  try {
    await deleteProfilePhoto()
    authStore.setFotoUrl(null)
  } catch {
    // Error handled silently
  } finally {
    uploading.value = false
  }
}

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      // Strip "data:image/...;base64," prefix
      const base64 = result.split(',')[1]
      resolve(base64)
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
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
      >
        <!-- Header -->
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

        <!-- Content -->
        <div class="px-6 pb-6 pt-4 flex flex-col items-center gap-5">
          <!-- Avatar -->
          <div class="relative group cursor-pointer" @click="triggerFileInput">
            <!-- Photo or initials -->
            <div
              class="w-24 h-24 rounded-full overflow-hidden border-2 border-gray-200 dark:border-gray-600 flex items-center justify-center bg-primary/10"
            >
              <img
                v-if="fotoUrl"
                :src="fotoUrl"
                alt="Avatar"
                class="w-full h-full object-cover"
              />
              <span v-else class="text-3xl font-bold text-primary">
                {{ userInitials }}
              </span>
            </div>

            <!-- Hover overlay -->
            <div
              class="absolute inset-0 rounded-full bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
            >
              <i class="pi pi-camera text-white text-xl"></i>
            </div>

            <!-- Loading spinner -->
            <div
              v-if="uploading"
              class="absolute inset-0 rounded-full bg-black/60 flex items-center justify-center"
            >
              <i class="pi pi-spin pi-spinner text-white text-xl"></i>
            </div>
          </div>

          <!-- Hidden file input -->
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            class="hidden"
            @change="onFileSelected"
          />

          <!-- Remove photo link -->
          <button
            v-if="fotoUrl"
            type="button"
            class="text-sm text-red-500 hover:text-red-600 dark:text-red-400 dark:hover:text-red-300 transition-colors"
            :disabled="uploading"
            @click="handleDeletePhoto"
          >
            Eliminar foto
          </button>

          <!-- User info -->
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
        </div>
      </div>
    </div>
  </Teleport>
</template>
