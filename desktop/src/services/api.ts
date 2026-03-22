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

export type DriverState = 'ONLINE' | 'STALE' | 'OFFLINE'
export type EtaMethod = 'ROUTE_PROVIDER' | 'HAVERSINE_FALLBACK'
export type EtaConfidence = 'LOW' | 'MEDIUM'

export interface DriverLocationPoint {
  driverId: string
  lat: number
  lon: number
  recordedAt: string
  speedKph?: number
  headingDeg?: number
  state: DriverState
}

export interface FleetSnapshotResponse {
  snapshotAt: string
  drivers: DriverLocationPoint[]
  meta: {
    pollingSuggestedSec: number
    degraded: boolean
    degradedReason?: string
  }
}

export interface DriverLocationUpsertRequest {
  lat: number
  lon: number
  recordedAt?: string
  speedKph?: number
  headingDeg?: number
}

export interface EtaEstimateResponse {
  etaMinutes: number
  method: EtaMethod
  estimatedAt: string
  confidence: EtaConfidence
}

export async function getFleetSnapshot(): Promise<FleetSnapshotResponse> {
  const response = await api.get<FleetSnapshotResponse>('/v1/fleet/snapshot')
  return response.data
}

export async function postDriverLocation(
  driverId: number,
  payload: DriverLocationUpsertRequest
): Promise<void> {
  await api.post(`/v1/tracking/drivers/${driverId}/locations`, payload)
}

export async function getEtaEstimate(
  driverId: number,
  jobId: number
): Promise<EtaEstimateResponse> {
  const response = await api.get<EtaEstimateResponse>('/v1/eta/estimate', {
    params: { driverId, jobId },
  })
  return response.data
}

export const getResumenPortes = (anio?: number, mes?: number) =>
  api.get('/portes/resumen', { params: { anio, mes } })

export const getIncidenciasPendientes = () =>
  api.get('/incidencias/contador')

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
