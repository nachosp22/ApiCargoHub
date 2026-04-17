import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'cargohub-theme'

const currentMode = ref<ThemeMode>('system')
const isDark = ref(false)

let initialized = false

function getSystemPreference(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function applyTheme(dark: boolean): void {
  isDark.value = dark
  const root = document.documentElement
  if (dark) {
    root.classList.add('dark', 'dark-mode')
  } else {
    root.classList.remove('dark', 'dark-mode')
  }
}

function resolveAndApply(): void {
  const dark = currentMode.value === 'system'
    ? getSystemPreference()
    : currentMode.value === 'dark'
  applyTheme(dark)
}

export function useTheme() {
  if (!initialized) {
    initialized = true

    // Load saved preference
    const saved = localStorage.getItem(STORAGE_KEY) as ThemeMode | null
    currentMode.value = saved && ['light', 'dark', 'system'].includes(saved) ? saved : 'system'

    // Watch for changes
    watch(currentMode, (mode) => {
      localStorage.setItem(STORAGE_KEY, mode)
      resolveAndApply()
    })

    // Listen for system preference changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (currentMode.value === 'system') {
        resolveAndApply()
      }
    })

    // Apply immediately
    resolveAndApply()
  }

  function toggleTheme(): void {
    if (isDark.value) {
      currentMode.value = 'light'
    } else {
      currentMode.value = 'dark'
    }
  }

  function setTheme(mode: ThemeMode): void {
    currentMode.value = mode
  }

  return {
    currentMode,
    isDark,
    toggleTheme,
    setTheme,
  }
}
