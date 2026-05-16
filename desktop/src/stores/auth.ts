import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import { api } from '@/services/api'
import {
  AUTH_TOKEN_STORAGE_KEY,
  AUTH_USER_STORAGE_KEY,
  LEGACY_TOKEN_STORAGE_KEYS,
} from '@/constants/auth'

export interface User {
  id?: number
  nombre: string
  email: string
  role: string
  fotoUrl?: string | null
}

export interface AdminUser {
  id: number
  email: string
  nombre: string
  activo: boolean
}

interface LoginResponse {
  accessToken?: string
  token?: string
  tokenType?: string
  expiresIn?: number
  expiresAt?: string
  id?: number
  email?: string
  role?: string
  rol?: string
  nombre?: string
  nombreEmpresa?: string
  conductorId?: number
  clienteId?: number
}

export type LoginErrorKind =
  | 'network'
  | 'invalid_credentials'
  | 'server'
  | 'unknown'

export class AuthLoginError extends Error {
  kind: LoginErrorKind
  status?: number

  constructor(kind: LoginErrorKind, message: string, status?: number) {
    super(message)
    this.name = 'AuthLoginError'
    this.kind = kind
    this.status = status
    }
  }

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)
  const admins = ref<AdminUser[]>([])
  const adminsLoading = ref(false)

  const isAuthenticated = computed(() => !!token.value)

  function toRoleString(value: unknown): string {
    if (typeof value !== 'string') {
      return ''
    }

    return value
  }

  function toNullableString(value: unknown): string | null {
    return typeof value === 'string' && value.trim().length > 0 ? value : null
  }

  function normalizeUser(data: LoginResponse, fallbackEmail: string): User {
    const resolvedName =
      toNullableString(data.nombre) ??
      toNullableString(data.nombreEmpresa) ??
      toNullableString(fallbackEmail) ??
      'Usuario'

    const resolvedEmail = toNullableString(data.email) ?? fallbackEmail
    const resolvedRole = toNullableString(data.role) ?? toNullableString(data.rol) ?? ''

    return {
      id: data.id,
      nombre: resolvedName,
      email: resolvedEmail,
      role: toRoleString(resolvedRole),
      fotoUrl: null,
    }
  }

  function normalizeStoredUser(raw: unknown): User | null {
    if (!raw || typeof raw !== 'object') {
      return null
    }

    const candidate = raw as Record<string, unknown>
    const nombre = toNullableString(candidate.nombre)
    const email = toNullableString(candidate.email)
    const role = toNullableString(candidate.role) ?? toNullableString(candidate.rol) ?? ''
    const fotoUrl = toNullableString(candidate.fotoUrl)
    const maybeId = typeof candidate.id === 'number' ? candidate.id : undefined

    if (!nombre || !email) {
      return null
    }

    return {
      id: maybeId,
      nombre,
      email,
      role: toRoleString(role),
      fotoUrl,
    }
  }

  function resolveStoredToken(): string | null {
    const currentKeyToken = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
    if (currentKeyToken) {
      return currentKeyToken
    }

    for (const legacyKey of LEGACY_TOKEN_STORAGE_KEYS) {
      const legacyToken = localStorage.getItem(legacyKey)
      if (legacyToken) {
        localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, legacyToken)
        localStorage.removeItem(legacyKey)
        return legacyToken
      }
    }

    return null
  }

  async function login(email: string, password: string): Promise<void> {
    try {
      const formData = new URLSearchParams({ email, password })

      const response = await api.post<LoginResponse>('/auth/login', formData, {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
      })

      const data = response.data
      const resolvedToken = data.accessToken ?? data.token

      if (!resolvedToken) {
        throw new AuthLoginError('server', 'La respuesta no incluye token de acceso.')
      }

      const normalizedUser = normalizeUser(data, email)

      token.value = resolvedToken
      user.value = normalizedUser

      localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, resolvedToken)
      localStorage.setItem(AUTH_USER_STORAGE_KEY, JSON.stringify(normalizedUser))
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        if (!error.response) {
          throw new AuthLoginError('network', 'No se pudo conectar con el servidor.')
        }

        const status = error.response.status

        if (status === 400 || status === 401) {
          throw new AuthLoginError(
            'invalid_credentials',
            'Credenciales inválidas.',
            status
          )
        }

        throw new AuthLoginError(
          'server',
          'El servidor devolvió un error al iniciar sesión.',
          status
        )
      }

      throw new AuthLoginError('unknown', 'Ocurrió un error inesperado al iniciar sesión.')
    }
  }

  function logout(): void {
    token.value = null
    user.value = null
    localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
    localStorage.removeItem(AUTH_USER_STORAGE_KEY)

    for (const legacyKey of LEGACY_TOKEN_STORAGE_KEYS) {
      localStorage.removeItem(legacyKey)
    }
  }

  function loadFromStorage(): void {
    const storedToken = resolveStoredToken()
    const storedUser = localStorage.getItem(AUTH_USER_STORAGE_KEY)

    token.value = storedToken
    user.value = null

    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser) as unknown
        user.value = normalizeStoredUser(parsed)
      } catch {
        user.value = null
      }
    }

    if (!token.value && user.value) {
      user.value = null
      localStorage.removeItem(AUTH_USER_STORAGE_KEY)
    }
  }

  function setFotoUrl(url: string | null): void {
    if (user.value) {
      user.value = { ...user.value, fotoUrl: url }
      localStorage.setItem(AUTH_USER_STORAGE_KEY, JSON.stringify(user.value))
    }
  }

  async function updateProfile(data: {
    nombre?: string
    currentPassword?: string
    newPassword?: string
  }): Promise<{ success: boolean; error?: string }> {
    try {
      const response = await api.patch('/usuarios/me', data)
      if (user.value && response.data?.nombre) {
        user.value = { ...user.value, nombre: response.data.nombre }
        localStorage.setItem(AUTH_USER_STORAGE_KEY, JSON.stringify(user.value))
      }
      return { success: true }
    } catch (e: unknown) {
      const msg =
        e instanceof Error ? e.message : 'Error al actualizar el perfil'
      if (axios.isAxiosError(e) && e.response?.data?.error) {
        return { success: false, error: String(e.response.data.error) }
      }
      return { success: false, error: msg }
    }
  }

  async function createAdmin(data: {
    email: string
    nombre: string
    password: string
  }): Promise<{ success: boolean; error?: string }> {
    try {
      await api.post('/usuarios/admin', data)
      return { success: true }
    } catch (e: unknown) {
      const msg =
        e instanceof Error ? e.message : 'Error al crear administrador'
      if (axios.isAxiosError(e) && e.response?.data?.error) {
        return { success: false, error: String(e.response.data.error) }
      }
      return { success: false, error: msg }
    }
  }

  async function fetchAdmins(): Promise<void> {
    adminsLoading.value = true
    try {
      const response = await api.get<AdminUser[]>('/usuarios/admins')
      admins.value = Array.isArray(response.data) ? response.data : []
    } catch {
      admins.value = []
    } finally {
      adminsLoading.value = false
    }
  }

  async function toggleAdminActive(id: number): Promise<boolean> {
    try {
      const response = await api.patch(`/usuarios/${id}/toggle-active`)
      const activo = response.data?.activo === true
      const idx = admins.value.findIndex((a) => a.id === id)
      if (idx !== -1) admins.value[idx] = { ...admins.value[idx], activo }
      return activo
    } catch {
      return false
    }
  }

  async function deleteAdmin(id: number): Promise<boolean> {
    try {
      await api.delete(`/usuarios/${id}`)
      admins.value = admins.value.filter((a) => a.id !== id)
      return true
    } catch {
      return false
    }
  }

  return {
    token,
    user,
    admins,
    adminsLoading,
    isAuthenticated,
    login,
    logout,
    loadFromStorage,
    setFotoUrl,
    updateProfile,
    createAdmin,
    fetchAdmins,
    toggleAdminActive,
    deleteAdmin,
  }
})
