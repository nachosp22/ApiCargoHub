<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useGlobalSearchStore, type EntityType } from '@/stores/globalSearch'

// --- Store ---
const store = useGlobalSearchStore()

// --- Refs ---
const inputRef = ref<HTMLInputElement | null>(null)
const dropdownRef = ref<HTMLDivElement | null>(null)
const activeIndex = ref(-1)

// --- Debounce ---
let debounceTimer: ReturnType<typeof setTimeout> | null = null

function onInput(e: Event) {
  const val = (e.target as HTMLInputElement).value
  activeIndex.value = -1

  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    store.search(val)
  }, 300)
}

// --- Computed: grouped results ---
const CATEGORY_LABELS: Record<EntityType, string> = {
  accion: 'Accesos rápidos',
  conductor: 'Conductores',
  porte: 'Portes',
  cliente: 'Clientes',
  vehiculo: 'Vehículos',
  incidencia: 'Incidencias',
  factura: 'Facturas',
}

const CATEGORY_ORDER: EntityType[] = ['accion', 'incidencia', 'porte', 'conductor', 'vehiculo', 'cliente', 'factura']

interface GroupedResult {
  type: EntityType
  label: string
  items: typeof store.results
}

const groupedResults = computed<GroupedResult[]>(() => {
  const groups: GroupedResult[] = []
  for (const type of CATEGORY_ORDER) {
    const items = store.results.filter((r) => r.type === type)
    if (items.length > 0) {
      groups.push({ type, label: CATEGORY_LABELS[type], items })
    }
  }
  return groups
})

const flatResults = computed(() => store.results)

// --- Keyboard navigation ---
function onKeyDown(e: KeyboardEvent) {
  if (!store.isOpen) return

  const total = flatResults.value.length
  if (total === 0 && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
    e.preventDefault()
    activeIndex.value = -1
    return
  }

  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % total
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + total) % total
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (total === 0) return
    if (activeIndex.value >= 0 && activeIndex.value < total) {
      store.navigateTo(flatResults.value[activeIndex.value])
    } else {
      store.navigateTo(flatResults.value[0])
    }
  } else if (e.key === 'Escape') {
    store.clearSearch()
    inputRef.value?.blur()
  }
}

// --- Click outside ---
function onClickOutside(e: MouseEvent) {
  const target = e.target as Node
  if (
    inputRef.value &&
    !inputRef.value.contains(target) &&
    dropdownRef.value &&
    !dropdownRef.value.contains(target)
  ) {
    store.clearSearch()
  }
}

// --- Global Ctrl+K shortcut ---
function onGlobalKeyDown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    nextTick(() => {
      inputRef.value?.focus()
    })
  }
}

// --- Expose focus to parent ---
function focusInput() {
  inputRef.value?.focus()
}

defineExpose({ focusInput })

// --- Lifecycle ---
onMounted(() => {
  document.addEventListener('mousedown', onClickOutside)
  document.addEventListener('keydown', onGlobalKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('mousedown', onClickOutside)
  document.removeEventListener('keydown', onGlobalKeyDown)
  if (debounceTimer) clearTimeout(debounceTimer)
})

// --- Helpers ---
function resultGlobalIndex(type: EntityType, localIndex: number): number {
  let base = 0
  for (const g of groupedResults.value) {
    if (g.type === type) return base + localIndex
    base += g.items.length
  }
  return -1
}

function isActive(type: EntityType, localIndex: number): boolean {
  return resultGlobalIndex(type, localIndex) === activeIndex.value
}

function onResultClick(type: EntityType, localIndex: number) {
  const idx = resultGlobalIndex(type, localIndex)
  if (idx >= 0) {
    store.navigateTo(flatResults.value[idx])
  }
}

const isMac = typeof navigator !== 'undefined' && /Mac/i.test(navigator.platform)
const shortcutHint = isMac ? '⌘K' : 'Ctrl+K'
</script>

<template>
  <div class="relative w-full max-w-md">
    <!-- Input -->
    <div class="relative">
      <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm pointer-events-none" />
      <input
        ref="inputRef"
        type="text"
        :value="store.query"
        placeholder="Buscar..."
        class="w-full pl-10 pr-20 py-2 bg-gray-100 border-0 rounded-lg text-sm text-gray-700 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-colors"
        autocomplete="off"
        @input="onInput"
        @keydown="onKeyDown"
        @focus="store.query.trim().length >= 2 && (store.isOpen = true)"
      />

      <!-- Shortcut hint -->
      <span
        v-if="!store.query"
        class="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-gray-400 bg-gray-200 rounded px-1.5 py-0.5 font-mono select-none pointer-events-none"
      >
        {{ shortcutHint }}
      </span>

      <!-- Loading spinner -->
      <i
        v-else-if="store.loading"
        class="pi pi-spinner pi-spin absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm"
      />

      <!-- Clear button -->
      <button
        v-else-if="store.query"
        type="button"
        class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
        @click="store.clearSearch(); inputRef?.focus()"
        aria-label="Limpiar búsqueda"
      >
        <i class="pi pi-times text-sm" />
      </button>
    </div>

    <!-- Dropdown -->
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0 translate-y-1 scale-95"
      enter-to-class="opacity-100 translate-y-0 scale-100"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100 translate-y-0 scale-100"
      leave-to-class="opacity-0 translate-y-1 scale-95"
    >
      <div
        v-if="store.isOpen"
        ref="dropdownRef"
        class="absolute top-full left-0 right-0 mt-2 bg-white rounded-xl shadow-xl border border-gray-100 z-50 overflow-hidden max-h-[420px] overflow-y-auto"
      >
        <!-- Results -->
        <template v-if="groupedResults.length > 0">
          <div
            v-for="group in groupedResults"
            :key="group.type"
          >
            <!-- Category header -->
            <div class="px-4 pt-3 pb-1">
              <span class="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                {{ group.label }}
              </span>
            </div>

            <!-- Items -->
            <button
              v-for="(result, localIdx) in group.items"
              :key="result.id"
              type="button"
              class="w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors"
              :class="isActive(group.type, localIdx)
                ? 'bg-primary/10 text-primary'
                : 'hover:bg-gray-50 text-gray-700'"
              @click="onResultClick(group.type, localIdx)"
              @mouseenter="activeIndex = resultGlobalIndex(group.type, localIdx)"
            >
              <!-- Icon -->
              <span
                class="flex-shrink-0 w-8 h-8 rounded-lg flex items-center justify-center"
                :class="isActive(group.type, localIdx) ? 'bg-primary/20' : 'bg-gray-100'"
              >
                <i :class="[result.icon, 'text-sm', isActive(group.type, localIdx) ? 'text-primary' : 'text-gray-500']" />
              </span>

              <!-- Text -->
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium truncate">{{ result.title }}</p>
                <p class="text-xs text-gray-400 truncate">{{ result.subtitle }}</p>
              </div>

              <!-- Type badge -->
              <span
                class="flex-shrink-0 text-xs px-2 py-0.5 rounded-full font-medium"
                :class="isActive(group.type, localIdx)
                  ? 'bg-primary/20 text-primary'
                  : 'bg-gray-100 text-gray-500'"
              >
                {{ CATEGORY_LABELS[result.type] }}
              </span>
            </button>
          </div>

          <!-- Footer hint -->
          <div class="border-t border-gray-100 px-4 py-2 flex items-center gap-3 text-xs text-gray-400">
            <span><kbd class="font-mono bg-gray-100 px-1 rounded">↑↓</kbd> navegar</span>
            <span><kbd class="font-mono bg-gray-100 px-1 rounded">↵</kbd> ir</span>
            <span><kbd class="font-mono bg-gray-100 px-1 rounded">Esc</kbd> cerrar</span>
          </div>
        </template>

        <!-- Empty state -->
        <div
          v-else-if="!store.loading"
          class="flex flex-col items-center justify-center py-10 text-gray-400"
        >
          <i class="pi pi-search text-2xl mb-2 opacity-40" />
          <p class="text-sm">Sin resultados para <span class="font-medium text-gray-600">"{{ store.query }}"</span></p>
          <p class="text-xs mt-1 opacity-70">Intentá con otro término</p>
        </div>

        <!-- Loading state -->
        <div
          v-else
          class="flex items-center justify-center py-8 text-gray-400"
        >
          <i class="pi pi-spinner pi-spin text-xl mr-2" />
          <span class="text-sm">Buscando...</span>
        </div>
      </div>
    </Transition>
  </div>
</template>
