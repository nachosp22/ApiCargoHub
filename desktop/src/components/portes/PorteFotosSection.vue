<script setup lang="ts">
import { ref, watch } from 'vue'
import { api } from '@/services/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

interface FotoCarga {
  id: number
  porteId: number
  tipo: 'CARGA' | 'DESCARGA' | 'DANO'
  fotoBase64: string
  descripcion?: string
  fechaCaptura?: string
}

const props = defineProps<{
  porteId: number | null
}>()

const fotos = ref<FotoCarga[]>([])
const loading = ref(false)
const error = ref('')

// Full image dialog
const showFullImage = ref(false)
const selectedFoto = ref<FotoCarga | null>(null)

const tipoLabels: Record<string, string> = {
  CARGA: 'Carga',
  DESCARGA: 'Descarga',
  DANO: 'Daño',
}

const tipoColors: Record<string, string> = {
  CARGA: 'bg-blue-100 text-blue-700',
  DESCARGA: 'bg-green-100 text-green-700',
  DANO: 'bg-red-100 text-red-700',
}

watch(
  () => props.porteId,
  async (id) => {
    if (!id) {
      fotos.value = []
      return
    }
    await fetchFotos(id)
  },
  { immediate: true }
)

async function fetchFotos(porteId: number): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get(`/portes/${porteId}/fotos`)
    fotos.value = Array.isArray(response.data) ? response.data : []
  } catch {
    error.value = 'No se pudieron cargar las fotos'
    fotos.value = []
  } finally {
    loading.value = false
  }
}

function openFullImage(foto: FotoCarga): void {
  selectedFoto.value = foto
  showFullImage.value = true
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '—'
  try {
    return new Date(dateStr).toLocaleString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateStr
  }
}
</script>

<template>
  <div>
    <h4 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
      📷 Fotos de Carga
    </h4>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-6">
      <i class="pi pi-spin pi-spinner text-2xl text-gray-400"></i>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="text-sm text-red-500 py-2">
      {{ error }}
      <Button label="Reintentar" text size="small" @click="fetchFotos(porteId!)" class="ml-2" />
    </div>

    <!-- Empty -->
    <div v-else-if="fotos.length === 0" class="text-sm text-gray-400 py-4 text-center">
      No hay fotos para este porte
    </div>

    <!-- Photo Grid -->
    <div v-else class="grid grid-cols-3 gap-3">
      <div
        v-for="foto in fotos"
        :key="foto.id"
        class="group relative bg-gray-50 rounded-xl overflow-hidden cursor-pointer border border-gray-200 hover:border-blue-300 transition-colors"
        @click="openFullImage(foto)"
      >
        <img
          :src="'data:image/jpeg;base64,' + foto.fotoBase64"
          :alt="foto.descripcion || 'Foto de carga'"
          class="w-full h-24 object-cover"
        />
        <div class="p-2">
          <span
            class="text-xs font-semibold px-2 py-0.5 rounded-full"
            :class="tipoColors[foto.tipo] || 'bg-gray-100 text-gray-600'"
          >
            {{ tipoLabels[foto.tipo] || foto.tipo }}
          </span>
          <p v-if="foto.descripcion" class="text-xs text-gray-500 mt-1 truncate">
            {{ foto.descripcion }}
          </p>
        </div>
      </div>
    </div>

    <!-- Full Image Dialog -->
    <Dialog
      v-model:visible="showFullImage"
      :header="selectedFoto ? (tipoLabels[selectedFoto.tipo] || selectedFoto.tipo) : 'Foto'"
      :modal="true"
      :closable="true"
      :style="{ width: '600px' }"
    >
      <div v-if="selectedFoto" class="space-y-3">
        <img
          :src="'data:image/jpeg;base64,' + selectedFoto.fotoBase64"
          :alt="selectedFoto.descripcion || 'Foto'"
          class="w-full rounded-lg"
        />
        <div class="flex items-center gap-3">
          <span
            class="text-xs font-semibold px-2 py-1 rounded-full"
            :class="tipoColors[selectedFoto.tipo] || 'bg-gray-100 text-gray-600'"
          >
            {{ tipoLabels[selectedFoto.tipo] || selectedFoto.tipo }}
          </span>
          <span class="text-xs text-gray-400">{{ formatDate(selectedFoto.fechaCaptura) }}</span>
        </div>
        <p v-if="selectedFoto.descripcion" class="text-sm text-gray-700">
          {{ selectedFoto.descripcion }}
        </p>
      </div>
    </Dialog>
  </div>
</template>
