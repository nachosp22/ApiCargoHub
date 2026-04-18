<template>
  <div class="mt-3 border-t border-gray-200 dark:border-gray-700 pt-3">
    <!-- Loading state -->
    <div v-if="loadingState" class="flex items-center gap-2 text-sm text-gray-400">
      <i class="pi pi-spin pi-spinner"></i>
      <span>Cargando valoración...</span>
    </div>

    <!-- Already rated: read-only display -->
    <div v-else-if="existingValoracion" class="space-y-1">
      <div class="flex items-center gap-1">
        <span class="text-sm text-gray-500 mr-1">Tu valoración:</span>
        <i
          v-for="star in 5"
          :key="star"
          class="pi text-lg"
          :class="star <= existingValoracion.puntuacion ? 'pi-star-fill text-yellow-400' : 'pi-star text-gray-300'"
        />
      </div>
      <p v-if="existingValoracion.comentario" class="text-sm text-gray-600 italic">
        "{{ existingValoracion.comentario }}"
      </p>
    </div>

    <!-- Rating form -->
    <div v-else class="space-y-2">
      <p class="text-sm font-medium text-gray-700 dark:text-gray-300">Valorar conductor</p>
      <div class="flex items-center gap-1">
        <button
          v-for="star in 5"
          :key="star"
          type="button"
          class="focus:outline-none transition-transform hover:scale-110"
          @click="selectedRating = star"
          @mouseenter="hoverRating = star"
          @mouseleave="hoverRating = 0"
        >
          <i
            class="pi text-2xl cursor-pointer"
            :class="star <= (hoverRating || selectedRating) ? 'pi-star-fill text-yellow-400' : 'pi-star text-gray-300'"
          />
        </button>
        <span v-if="selectedRating" class="ml-2 text-sm text-gray-500">{{ selectedRating }}/5</span>
      </div>
      <textarea
        v-model="comentario"
        :placeholder="t('portal.portes.ratingCommentPlaceholder', 'Comentario opcional (máx. 500 caracteres)')"
        maxlength="500"
        rows="2"
        class="w-full text-sm border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 resize-none focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700 dark:text-gray-200"
      />
      <div class="flex items-center gap-2">
        <Button
          :label="t('portal.portes.submitRating', 'Enviar valoración')"
          icon="pi pi-check"
          size="small"
          :disabled="selectedRating === 0 || submitting"
          :loading="submitting"
          @click="submitRating"
        />
        <span v-if="errorMsg" class="text-sm text-red-500">{{ errorMsg }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useValoracionesStore, type Valoracion } from '@/stores/valoraciones'
import Button from 'primevue/button'

const props = defineProps<{
  porteId: number
}>()

const { t } = useI18n()
const valoracionesStore = useValoracionesStore()

const loadingState = ref(true)
const existingValoracion = ref<Valoracion | null>(null)
const selectedRating = ref(0)
const hoverRating = ref(0)
const comentario = ref('')
const submitting = ref(false)
const errorMsg = ref<string | null>(null)

onMounted(async () => {
  try {
    // Check cache first
    const cached = valoracionesStore.getValoracion(props.porteId)
    if (cached !== undefined) {
      existingValoracion.value = cached
      loadingState.value = false
      return
    }
    const val = await valoracionesStore.fetchMiValoracion(props.porteId)
    existingValoracion.value = val
  } finally {
    loadingState.value = false
  }
})

async function submitRating() {
  if (selectedRating.value === 0) return
  submitting.value = true
  errorMsg.value = null
  try {
    const val = await valoracionesStore.crearValoracion(
      props.porteId,
      selectedRating.value,
      comentario.value || undefined
    )
    existingValoracion.value = val
  } catch {
    errorMsg.value = valoracionesStore.error ?? 'Error al enviar la valoración'
  } finally {
    submitting.value = false
  }
}
</script>
