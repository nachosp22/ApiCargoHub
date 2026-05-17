import axios from 'axios'
import type { InternalAxiosRequestConfig, AxiosError } from 'axios'
import {
  AUTH_TOKEN_STORAGE_KEY,
  LEGACY_TOKEN_STORAGE_KEYS,
} from '@/constants/auth'

/**
 * Axios instance for the CargoHub API.
 *
 * - Base URL: /api (proxied by Vite dev server to localhost:8080)
 * - Request interceptor: attaches Bearer token from localStorage
 * - Response interceptor: on 401, clears auth and redirects to /login
 */
export const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// --- Request Interceptor: attach Bearer token ---
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token =
      localStorage.getItem(AUTH_TOKEN_STORAGE_KEY) ??
      LEGACY_TOKEN_STORAGE_KEYS.map((key) => localStorage.getItem(key)).find(
        (storedValue): storedValue is string => !!storedValue
      )

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// --- Response Interceptor: handle 401 Unauthorized ---
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      const { useAuthStore } = await import('@/stores/auth')
      const authStore = useAuthStore()

      authStore.logout()

      const { default: router } = await import('@/router')
      if (router.currentRoute.value.path !== '/login') {
        await router.push('/login')
      }
    }

    return Promise.reject(error)
  }
)
