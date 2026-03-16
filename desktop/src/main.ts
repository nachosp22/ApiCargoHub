import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import Tooltip from 'primevue/tooltip'
import Aura from '@primevue/themes/aura'
import 'primeicons/primeicons.css'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'

import './assets/main.css'

const app = createApp(App)

// Install Pinia FIRST (before router, which uses stores in guards)
const pinia = createPinia()
app.use(pinia)

// Restore auth state from localStorage before router runs guards
const authStore = useAuthStore()
authStore.loadFromStorage()

// Install Router (guards can now safely use auth store)
app.use(router)

// Install PrimeVue with Aura theme
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      prefix: 'p',
      darkModeSelector: '.dark-mode',
      cssLayer: false,
    },
  },
})

// PrimeVue services and directives
app.use(ToastService)
app.directive('tooltip', Tooltip)

app.mount('#app')
