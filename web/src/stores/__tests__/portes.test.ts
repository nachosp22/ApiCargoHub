import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePortesStore } from '@/stores/portes'

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

describe('Portes Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('fetchOwn', () => {
    it('fetches portes for a given clienteId', async () => {
      const mockPortes = [
        { id: 1, origen: 'Madrid', destino: 'Barcelona', estado: 'PENDIENTE' },
        { id: 2, origen: 'Sevilla', destino: 'Valencia', estado: 'ENTREGADO' },
      ]

      vi.mocked(api.get).mockResolvedValueOnce({ data: mockPortes })

      const store = usePortesStore()
      await store.fetchOwn(5)

      expect(api.get).toHaveBeenCalledWith('/portes/cliente/5')
      expect(store.portes).toHaveLength(2)
      expect(store.portes[0].origen).toBe('Madrid')
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('sets error on failure', async () => {
      vi.mocked(api.get).mockRejectedValueOnce(new Error('Network error'))

      const store = usePortesStore()
      await store.fetchOwn(5)

      expect(store.error).toBe('No se pudieron cargar los portes.')
      expect(store.portes).toHaveLength(0)
      expect(store.loading).toBe(false)
    })

    it('handles paginated response with content wrapper', async () => {
      vi.mocked(api.get).mockResolvedValueOnce({
        data: { content: [{ id: 1, origen: 'A', destino: 'B', estado: 'SOLICITUD' }] },
      })

      const store = usePortesStore()
      await store.fetchOwn(5)

      expect(store.portes).toHaveLength(1)
    })
  })

  describe('createSolicitud', () => {
    it('creates a solicitud and prepends to list', async () => {
      const newPorte = { id: 99, origen: 'Madrid', destino: 'Zaragoza', estado: 'SOLICITUD' }
      vi.mocked(api.post).mockResolvedValueOnce({ data: newPorte })

      const store = usePortesStore()
      const result = await store.createSolicitud({
        origen: 'Madrid',
        destino: 'Zaragoza',
        descripcionCliente: '5 cajas pesadas',
      })

      expect(api.post).toHaveBeenCalledWith('/portes/solicitud', {
        origen: 'Madrid',
        destino: 'Zaragoza',
        descripcionCliente: '5 cajas pesadas',
      })

      expect(result.id).toBe(99)
      expect(store.portes[0].id).toBe(99)
      expect(store.submitting).toBe(false)
    })

    it('sets error and rethrows on failure', async () => {
      vi.mocked(api.post).mockRejectedValueOnce(new Error('Server error'))

      const store = usePortesStore()

      await expect(store.createSolicitud({
        origen: 'A',
        destino: 'B',
        descripcionCliente: 'test',
      })).rejects.toThrow()

      expect(store.error).toBe('No se pudo crear la solicitud de porte.')
      expect(store.submitting).toBe(false)
    })
  })

  describe('computed getters', () => {
    it('portesActivos filters active states', async () => {
      vi.mocked(api.get).mockResolvedValueOnce({
        data: [
          { id: 1, origen: 'A', destino: 'B', estado: 'PENDIENTE' },
          { id: 2, origen: 'C', destino: 'D', estado: 'ENTREGADO' },
          { id: 3, origen: 'E', destino: 'F', estado: 'EN_TRANSITO' },
        ],
      })

      const store = usePortesStore()
      await store.fetchOwn(1)

      expect(store.portesActivos).toHaveLength(2)
      expect(store.portesCompletados).toHaveLength(1)
    })
  })
})
