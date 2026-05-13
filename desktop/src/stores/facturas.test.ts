import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useFacturasStore } from './facturas'

const getMock = vi.fn()

vi.mock('@/services/api', () => ({
  api: {
    get: (...args: unknown[]) => getMock(...args),
  },
}))

function facturaPayload(overrides?: Record<string, unknown>): Record<string, unknown> {
  return {
    id: 101,
    numeroSerie: 'F-101',
    baseImponible: 100,
    iva: 21,
    ivaPercent: 21,
    importeTotal: 121,
    fechaEmision: '2026-01-10T10:00:00Z',
    pagada: false,
    fechaPago: null,
    formaPago: null,
    condicionesPago: null,
    observaciones: null,
    porte: {
      id: 7,
      origen: 'Madrid',
      destino: 'Valencia',
    },
    ...overrides,
  }
}

describe('facturas store - fetchFacturasByPorte', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    getMock.mockReset()
  })

  it('normaliza array directo', async () => {
    const store = useFacturasStore()
    getMock.mockResolvedValueOnce({ data: [facturaPayload({ id: 1 })] })

    const result = await store.fetchFacturasByPorte(7)

    expect(result).toHaveLength(1)
    expect(result[0].id).toBe(1)
    expect(store.usingMockData).toBe(false)
    expect(store.dataSource).toBe('api')
    expect(store.warning).toBeNull()
  })

  it('normaliza objeto singular', async () => {
    const store = useFacturasStore()
    getMock.mockResolvedValueOnce({ data: facturaPayload({ id: 2 }) })

    const result = await store.fetchFacturasByPorte(7)

    expect(result).toHaveLength(1)
    expect(result[0].id).toBe(2)
  })

  it('normaliza content[]', async () => {
    const store = useFacturasStore()
    getMock.mockResolvedValueOnce({ data: { content: [facturaPayload({ id: 3 })] } })

    const result = await store.fetchFacturasByPorte(7)

    expect(result).toHaveLength(1)
    expect(result[0].id).toBe(3)
  })

  it('normaliza data[]', async () => {
    const store = useFacturasStore()
    getMock.mockResolvedValueOnce({ data: { data: [facturaPayload({ id: 4 })] } })

    const result = await store.fetchFacturasByPorte(7)

    expect(result).toHaveLength(1)
    expect(result[0].id).toBe(4)
  })

  it('retorna [] para payload inválido o vacío', async () => {
    const store = useFacturasStore()
    getMock.mockResolvedValueOnce({ data: { foo: 'bar' } })

    const result = await store.fetchFacturasByPorte(7)

    expect(result).toEqual([])
    expect(store.error).toBeNull()
  })

  it('activa degradación/mock cuando falla API y hay fallback', async () => {
    const store = useFacturasStore()
    store.facturas = [
      {
        id: 999,
        numeroSerie: 'CACHE-999',
        baseImponible: 50,
        iva: 10.5,
        ivaPercent: 21,
        importeTotal: 60.5,
        fechaEmision: '2026-02-01T10:00:00Z',
        pagada: false,
        fechaPago: null,
        formaPago: null,
        condicionesPago: null,
        observaciones: null,
        porte: {
          id: 777,
          origen: 'A',
          destino: 'B',
          fechaRecogida: null,
          fechaEntrega: null,
          descripcionCliente: null,
          pesoTotalKg: null,
          volumenTotalM3: null,
          conductor: null,
          cliente: null,
        },
      },
    ]

    getMock.mockRejectedValueOnce(new Error('api down'))

    const result = await store.fetchFacturasByPorte(777)

    expect(result).toHaveLength(1)
    expect(store.usingMockData).toBe(true)
    expect(store.dataSource).toBe('mock')
    expect(store.warning).toContain('mock')
    expect(store.error).toContain('API')
  })

  it('mantiene estado de error real sin fallback', async () => {
    const store = useFacturasStore()
    getMock.mockRejectedValueOnce(new Error('api down'))

    const result = await store.fetchFacturasByPorte(123456)

    expect(result).toEqual([])
    expect(store.usingMockData).toBe(false)
    expect(store.dataSource).toBe('api')
    expect(store.warning).toBeNull()
    expect(store.error).toBe('No se pudieron cargar las facturas del porte')
  })

  it('usa fallback mock por porte cuando API falla y no hay cache local', async () => {
    const store = useFacturasStore()
    getMock.mockRejectedValueOnce(new Error('api down'))

    const result = await store.fetchFacturasByPorte(7)

    expect(result.length).toBeGreaterThan(1)
    expect(result.every((f) => f.porte?.id === 7)).toBe(true)
    expect(store.usingMockData).toBe(true)
    expect(store.dataSource).toBe('mock')
    expect(store.warning).toContain('mock')
    expect(store.error).toContain('API')
  })
})
