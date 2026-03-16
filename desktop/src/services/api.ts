import axios from 'axios'
import type { InternalAxiosRequestConfig, AxiosError } from 'axios'
import {
  AUTH_TOKEN_STORAGE_KEY,
  LEGACY_TOKEN_STORAGE_KEYS,
} from '@/constants/auth'

/**
 * Axios instance pre-configured for the CargoHub API.
 *
 * - Base URL: http://localhost:8080/api
 * - Request interceptor: attaches Bearer token from auth store
 * - Response interceptor: on 401, clears auth and redirects to /login
 *
 * NOTE: We import the auth store lazily inside interceptors to avoid
 * the "no active Pinia" error (store used at module level).
 */
export const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// --- Request Interceptor: attach Bearer token ---
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Read token directly from localStorage to avoid Pinia timing issues
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
      // Lazy import to avoid "no active Pinia" at module level
      const { useAuthStore } = await import('@/stores/auth')
      const authStore = useAuthStore()

      authStore.logout()

      // Redirect to login (lazy import router to avoid circular deps)
      const { default: router } = await import('@/router')
      if (router.currentRoute.value.path !== '/login') {
        await router.push('/login')
      }
    }

    return Promise.reject(error)
  }
)
