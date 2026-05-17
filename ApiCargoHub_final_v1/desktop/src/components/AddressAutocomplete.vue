<template>
  <div class="relative">
    <label v-if="label" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
      {{ label }}
    </label>

    <div class="relative">
      <!-- Input styled like PrimeVue InputText -->
      <input
        ref="inputRef"
        type="text"
        :value="query"
        :placeholder="placeholder"
        autocomplete="off"
        class="w-full px-3 py-2 pr-9 text-sm border rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 border-gray-300 dark:border-gray-600 placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition"
        @input="onInput"
        @keydown.down.prevent="moveDown"
        @keydown.up.prevent="moveUp"
        @keydown.enter.prevent="confirmSelection"
        @keydown.escape="closeSuggestions"
        @blur="onBlur"
        @focus="onFocus"
      />

      <!-- Loading spinner -->
      <span v-if="loading" class="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none">
        <i class="pi pi-spin pi-spinner text-gray-400 text-xs"></i>
      </span>

      <!-- Clear button -->
      <button
        v-else-if="query"
        type="button"
        class="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
        @mousedown.prevent="onClear"
      >
        <i class="pi pi-times text-xs"></i>
      </button>
    </div>

    <!-- Suggestions dropdown -->
    <ul
      v-if="showDropdown && suggestions.length > 0"
      class="absolute z-50 w-full mt-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-md shadow-xl overflow-hidden"
    >
      <li
        v-for="(feature, index) in suggestions"
        :key="index"
        class="px-4 py-2.5 cursor-pointer text-sm transition-colors"
        :class="[
          index === activeIndex
            ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
            : 'hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-800 dark:text-gray-100'
        ]"
        @mousedown.prevent="selectFeature(feature)"
      >
        <p class="font-semibold leading-tight">{{ getSuggestionMain(feature) }}</p>
        <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{{ getSuggestionSub(feature) }}</p>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAddressAutocomplete } from '@/composables/useAddressAutocomplete'

interface Props {
  modelValue?: string
  placeholder?: string
  label?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: 'Buscar dirección...',
  label: '',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'select', address: { city: string; fullAddress: string; lat: number; lon: number }): void
}>()

const { query, suggestions, loading, search, select, clear } = useAddressAutocomplete()

const inputRef = ref<HTMLInputElement | null>(null)
const activeIndex = ref(-1)
const showDropdown = ref(false)

watch(
  () => props.modelValue,
  (val) => {
    if (val !== query.value) {
      query.value = val ?? ''
    }
  },
  { immediate: true }
)

function onInput(e: Event): void {
  const val = (e.target as HTMLInputElement).value
  activeIndex.value = -1
  showDropdown.value = true
  search(val)
  emit('update:modelValue', val)
}

function onFocus(): void {
  if (suggestions.value.length > 0) showDropdown.value = true
}

function onBlur(): void {
  setTimeout(() => {
    showDropdown.value = false
  }, 150)
}

function closeSuggestions(): void {
  showDropdown.value = false
  activeIndex.value = -1
}

function moveDown(): void {
  if (!showDropdown.value) return
  activeIndex.value = Math.min(activeIndex.value + 1, suggestions.value.length - 1)
}

function moveUp(): void {
  if (!showDropdown.value) return
  activeIndex.value = Math.max(activeIndex.value - 1, -1)
}

function confirmSelection(): void {
  if (activeIndex.value >= 0 && suggestions.value[activeIndex.value]) {
    selectFeature(suggestions.value[activeIndex.value])
  }
}

function selectFeature(feature: (typeof suggestions.value)[0]): void {
  const addr = select(feature)
  showDropdown.value = false
  activeIndex.value = -1
  emit('update:modelValue', addr.fullAddress)
  emit('select', {
    city: addr.city,
    fullAddress: addr.fullAddress,
    lat: addr.lat,
    lon: addr.lon,
  })
}

function onClear(): void {
  clear()
  showDropdown.value = false
  emit('update:modelValue', '')
  emit('select', { city: '', fullAddress: '', lat: 0, lon: 0 })
}

function getSuggestionMain(feature: (typeof suggestions.value)[0]): string {
  const p = feature.properties
  const parts: string[] = []
  if (p.street) parts.push(p.street)
  if (p.housenumber) parts.push(p.housenumber)
  return parts.length > 0 ? parts.join(' ') : (p.name ?? '')
}

function getSuggestionSub(feature: (typeof suggestions.value)[0]): string {
  const p = feature.properties
  const city = p.city ?? p.town ?? p.village ?? p.county ?? ''
  const parts: string[] = []
  if (p.postcode) parts.push(p.postcode)
  if (city) parts.push(city)
  if (p.state) parts.push(p.state)
  if (p.country) parts.push(p.country)
  return parts.join(', ')
}
</script>
