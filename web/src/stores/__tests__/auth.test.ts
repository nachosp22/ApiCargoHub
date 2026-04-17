import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { AUTH_TOKEN_STORAGE_KEY, AUTH_USER_STORAGE_KEY } from '@/constants/auth'

// Mock the api module
vi.mock('@/services/api', () => ({
  api: {
    post: vi.fn(),
    get: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}))

import { api } from '@/services/api'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value }),
    removeItem: vi.fn((key: string) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
  }
})()

Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock })

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorageMock.clear()
  })

  describe('login', () => {
    it('sends form-urlencoded request and stores token', async () => {
      const mockResponse = {
        data: {
          accessToken: 'jwt-token-123',
          tokenType: 'Bearer',
          id: 1,
          email: 'test@test.com',
          rol: 'CLIENTE',
          nombre: 'Test User',
          clienteId: 5,
        },
      }

      vi.mocked(api.post).mockResolvedValueOnce(mockResponse)

      const store = useAuthStore()
      await store.login('test@test.com', 'password123')

      // Verify form-urlencoded call
      expect(api.post).toHaveBeenCalledWith(
        '/auth/login',
        expect.any(URLSearchParams),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } },
      )

      // Verify URLSearchParams content
      const params = vi.mocked(api.post).mock.calls[0][1] as URLSearchParams
      expect(params.get('email')).toBe('test@test.com')
      expect(params.get('password')).toBe('password123')

      // Verify state
      expect(store.token).toBe('jwt-token-123')
      expect(store.isAuthenticated).toBe(true)
      expect(store.user?.email).toBe('test@test.com')
      expect(store.user?.role).toBe('CLIENTE')
      expect(store.clienteId).toBe(5)

      // Verify persistence
      expect(localStorageMock.setItem).toHaveBeenCalledWith(AUTH_TOKEN_STORAGE_KEY, 'jwt-token-123')
    })

    it('throws AuthLoginError on invalid credentials (401)', async () => {
      const axiosError = {
        isAxiosError: true,
        response: { status: 401, data: 'Bad credentials' },
      }
      // Make axios.isAxiosError return true
      const axios = await import('axios')
      vi.spyOn(axios.default, 'isAxiosError').mockReturnValue(true)
      vi.mocked(api.post).mockRejectedValueOnce(axiosError)

      const store = useAuthStore()

      await expect(store.login('bad@test.com', 'wrong')).rejects.toThrow()
      expect(store.isAuthenticated).toBe(false)
    })

    it('throws AuthLoginError when response has no token', async () => {
      vi.mocked(api.post).mockResolvedValueOnce({ data: { email: 'test@test.com' } })

      const store = useAuthStore()

      await expect(store.login('test@test.com', 'pass')).rejects.toThrow('token')
    })
  })

  describe('register', () => {
    it('sends JSON body with CLIENTE role and auto-logs in', async () => {
      // Register call
      vi.mocked(api.post).mockResolvedValueOnce({ data: {} })
      // Login call (auto-login after register)
      vi.mocked(api.post).mockResolvedValueOnce({
        data: {
          accessToken: 'new-token',
          email: 'new@test.com',
          nombre: 'New User',
          rol: 'CLIENTE',
          clienteId: 10,
        },
      })

      const store = useAuthStore()
      await store.register({
        email: 'new@test.com',
        password: 'pass123',
        nombreEmpresa: 'New User Corp',
        cif: 'B12345678',
      })

      // Verify register call (JSON)
      expect(api.post).toHaveBeenCalledWith('/auth/register', {
        email: 'new@test.com',
        password: 'pass123',
        nombreEmpresa: 'New User Corp',
        cif: 'B12345678',
      })

      // After auto-login
      expect(store.isAuthenticated).toBe(true)
      expect(store.token).toBe('new-token')
    })
  })

  describe('logout', () => {
    it('clears state and localStorage', async () => {
      // Set up authenticated state
      vi.mocked(api.post).mockResolvedValueOnce({
        data: { accessToken: 'tok', email: 'x@x.com', nombre: 'X', rol: 'CLIENTE' },
      })

      const store = useAuthStore()
      await store.login('x@x.com', 'pass')
      expect(store.isAuthenticated).toBe(true)

      store.logout()

      expect(store.token).toBeNull()
      expect(store.user).toBeNull()
      expect(store.isAuthenticated).toBe(false)
      expect(localStorageMock.removeItem).toHaveBeenCalledWith(AUTH_TOKEN_STORAGE_KEY)
      expect(localStorageMock.removeItem).toHaveBeenCalledWith(AUTH_USER_STORAGE_KEY)
    })
  })

  describe('loadFromStorage', () => {
    it('restores token and user from localStorage', () => {
      const storedUser = { id: 1, nombre: 'Test', email: 'test@test.com', role: 'CLIENTE', clienteId: 5 }
      localStorageMock.getItem.mockImplementation((key: string): string => {
        if (key === AUTH_TOKEN_STORAGE_KEY) return 'stored-token'
        if (key === AUTH_USER_STORAGE_KEY) return JSON.stringify(storedUser)
        return ''
      })

      const store = useAuthStore()
      store.loadFromStorage()

      expect(store.token).toBe('stored-token')
      expect(store.user?.email).toBe('test@test.com')
      expect(store.isAuthenticated).toBe(true)
    })

    it('clears user when no token exists', () => {
      localStorageMock.getItem.mockImplementation((key: string): string => {
        if (key === AUTH_USER_STORAGE_KEY) return JSON.stringify({ nombre: 'X', email: 'x@x.com', role: 'CLIENTE' })
        return ''
      })

      const store = useAuthStore()
      store.loadFromStorage()

      expect(store.token).toBeNull()
      expect(store.user).toBeNull()
    })
  })
})
