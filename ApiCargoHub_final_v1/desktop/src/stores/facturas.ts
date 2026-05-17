import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/services/api'

// --- TypeScript Interfaces ---

export interface FacturaPorte {
  id: number
  origen: string
  destino: string
  fechaRecogida?: string | null
  fechaEntrega?: string | null
  descripcionCliente?: string | null
  pesoTotalKg?: number | null
  volumenTotalM3?: number | null
  conductor?: { id: number; nombre: string; apellidos?: string } | null
  cliente?: { id: number; nombreEmpresa: string; cif?: string; direccionFiscal?: string } | null
}

export interface Factura {
  id: number
  numeroSerie: string
  baseImponible: number
  iva: number
  ivaPercent: number
  importeTotal: number
  fechaEmision: string
  pagada: boolean
  fechaPago: string | null
  formaPago: string | null
  condicionesPago: string | null
  observaciones: string | null
  porte: FacturaPorte | null
}

type DataSource = 'api' | 'mock'

const MOCK_FACTURAS: Factura[] = [
  {
    id: 9001,
    numeroSerie: 'MOCK-2026-0001',
    baseImponible: 1000,
    iva: 210,
    ivaPercent: 21,
    importeTotal: 1210,
    fechaEmision: '2026-01-15T10:00:00.000Z',
    pagada: false,
    fechaPago: null,
    formaPago: 'TRANSFERENCIA',
    condicionesPago: '30 días',
    observaciones: 'Factura mock para fallback UI',
    porte: {
      id: 1,
      origen: 'Madrid',
      destino: 'Barcelona',
      fechaRecogida: null,
      fechaEntrega: null,
      descripcionCliente: null,
      pesoTotalKg: null,
      volumenTotalM3: null,
      conductor: null,
      cliente: null,
    },
  },
  {
    id: 9002,
    numeroSerie: 'MOCK-2026-0002',
    baseImponible: 480,
    iva: 100.8,
    ivaPercent: 21,
    importeTotal: 580.8,
    fechaEmision: '2026-01-28T09:30:00.000Z',
    pagada: true,
    fechaPago: '2026-02-14T12:00:00.000Z',
    formaPago: 'TRANSFERENCIA',
    condicionesPago: '15 días',
    observaciones: 'Demo QA: factura pagada con pago anticipado',
    porte: {
      id: 2,
      origen: 'Valencia',
      destino: 'Sevilla',
      fechaRecogida: '2026-01-25T08:00:00.000Z',
      fechaEntrega: '2026-01-26T17:45:00.000Z',
      descripcionCliente: 'Transporte paletizado',
      pesoTotalKg: 1320,
      volumenTotalM3: 9.4,
      conductor: { id: 201, nombre: 'Conductor Mock 1', apellidos: 'Demo' },
      cliente: { id: 301, nombreEmpresa: 'Cliente Demo Levante S.L.', cif: 'MOCK-VAL-0001' },
    },
  },
  {
    id: 9003,
    numeroSerie: 'MOCK-2026-0003',
    baseImponible: 2200,
    iva: 462,
    ivaPercent: 21,
    importeTotal: 2662,
    fechaEmision: '2026-02-10T14:15:00.000Z',
    pagada: false,
    fechaPago: null,
    formaPago: 'TRANSFERENCIA',
    condicionesPago: '30 días',
    observaciones: 'Demo QA: factura pendiente de alto importe',
    porte: {
      id: 7,
      origen: 'Bilbao',
      destino: 'Málaga',
      fechaRecogida: '2026-02-08T06:30:00.000Z',
      fechaEntrega: '2026-02-09T21:10:00.000Z',
      descripcionCliente: 'Mercancía industrial con manipulación especial',
      pesoTotalKg: 6200,
      volumenTotalM3: 27.8,
      conductor: { id: 202, nombre: 'Conductor Mock 2', apellidos: 'QA' },
      cliente: { id: 302, nombreEmpresa: 'Cliente Demo Norte Logistics', cif: 'MOCK-BIL-0002' },
    },
  },
  {
    id: 9004,
    numeroSerie: 'MOCK-2026-0004',
    baseImponible: 760,
    iva: 159.6,
    ivaPercent: 21,
    importeTotal: 919.6,
    fechaEmision: '2026-03-02T11:20:00.000Z',
    pagada: true,
    fechaPago: '2026-03-05T10:10:00.000Z',
    formaPago: 'TRANSFERENCIA',
    condicionesPago: '7 días',
    observaciones: 'Demo QA: mismo porte con segunda factura (ajuste)',
    porte: {
      id: 7,
      origen: 'Bilbao',
      destino: 'Málaga',
      fechaRecogida: '2026-02-08T06:30:00.000Z',
      fechaEntrega: '2026-02-09T21:10:00.000Z',
      descripcionCliente: 'Ajuste por servicios adicionales de descarga',
      pesoTotalKg: 6200,
      volumenTotalM3: 27.8,
      conductor: { id: 202, nombre: 'Conductor Mock 2', apellidos: 'QA' },
      cliente: { id: 302, nombreEmpresa: 'Cliente Demo Norte Logistics', cif: 'MOCK-BIL-0002' },
    },
  },
  {
    id: 9005,
    numeroSerie: 'MOCK-2026-0005',
    baseImponible: 150,
    iva: 31.5,
    ivaPercent: 21,
    importeTotal: 181.5,
    fechaEmision: '2026-03-18T16:50:00.000Z',
    pagada: false,
    fechaPago: null,
    formaPago: 'TRANSFERENCIA',
    condicionesPago: 'Contado',
    observaciones: 'Demo QA: importe bajo para contraste visual',
    porte: {
      id: 15,
      origen: 'Zaragoza',
      destino: 'Pamplona',
      fechaRecogida: '2026-03-18T05:45:00.000Z',
      fechaEntrega: '2026-03-18T13:30:00.000Z',
      descripcionCliente: 'Paquetería ligera consolidada',
      pesoTotalKg: 210,
      volumenTotalM3: 1.2,
      conductor: { id: 203, nombre: 'Conductor Mock 3', apellidos: 'Demo' },
      cliente: { id: 303, nombreEmpresa: 'Cliente Demo Aragón Distribución', cif: 'MOCK-ZAZ-0003' },
    },
  },
]

// --- Store ---

export const useFacturasStore = defineStore('facturas', () => {
  // --- State ---
  const facturas = ref<Factura[]>([])
  const selectedFactura = ref<Factura | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const error = ref<string | null>(null)
  const usingMockData = ref(false)
  const dataSource = ref<DataSource>('api')
  const warning = ref<string | null>(null)

  // --- Getters ---
  const totalFacturado = computed(() =>
    facturas.value.reduce((sum, f) => sum + f.importeTotal, 0),
  )

  const totalPagado = computed(() =>
    facturas.value.filter((f) => f.pagada).reduce((sum, f) => sum + f.importeTotal, 0),
  )

  const totalPendiente = computed(() =>
    facturas.value.filter((f) => !f.pagada).reduce((sum, f) => sum + f.importeTotal, 0),
  )

  const totalFacturas = computed(() => facturas.value.length)

  const facturasPagadas = computed(() => facturas.value.filter((f) => f.pagada).length)

  const facturasPendientes = computed(() => facturas.value.filter((f) => !f.pagada).length)

  // --- Actions ---

  async function fetchFacturas(): Promise<void> {
    loading.value = true
    error.value = null
    warning.value = null
    usingMockData.value = false
    dataSource.value = 'api'

    try {
      const response = await api.get('/facturas')
      const data = extractArray(response.data)
      facturas.value = data.map(mapFacturaFromApi)
    } catch (err) {
      usingMockData.value = true
      dataSource.value = 'mock'
      warning.value = 'Mostrando facturas mock porque la API no respondió'
      error.value = 'Error al cargar las facturas'
      facturas.value = [...MOCK_FACTURAS]
    } finally {
      loading.value = false
    }
  }

  async function fetchFactura(id: number): Promise<Factura | null> {
    loading.value = true
    error.value = null

    try {
      const response = await api.get(`/facturas/${id}`)
      const factura = mapFacturaFromApi(response.data)
      selectedFactura.value = factura
      return factura
    } catch {
      error.value = 'No se pudo cargar la factura solicitada'
      const found = facturas.value.find((f) => f.id === id)
      if (found) {
        selectedFactura.value = found
        return found
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchFacturasByPorte(porteId: number): Promise<Factura[]> {
    loading.value = true
    error.value = null
    warning.value = null
    usingMockData.value = false
    dataSource.value = 'api'

    try {
      const response = await api.get(`/facturas/porte/${porteId}`)
      return extractFacturasByPorte(response.data)
    } catch {
      const fallback = facturas.value.filter((f) => f.porte?.id === porteId)
      const mockFallback = MOCK_FACTURAS.filter((f) => f.porte?.id === porteId)
      const degraded = fallback.length > 0 ? fallback : mockFallback

      if (degraded.length > 0) {
        usingMockData.value = true
        dataSource.value = 'mock'
        warning.value = 'Mostrando facturas mock por indisponibilidad de API'
        error.value = 'No se pudieron cargar las facturas del porte desde la API'
        return degraded
      }

      usingMockData.value = false
      dataSource.value = 'api'
      warning.value = null
      error.value = 'No se pudieron cargar las facturas del porte'
      return []
    } finally {
      loading.value = false
    }
  }

  async function downloadPdf(facturaId: number): Promise<void> {
    try {
      const response = await api.get(`/facturas/${facturaId}/pdf`, {
        responseType: 'blob',
      })
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url

      // Try to get filename from content-disposition header
      const disposition = response.headers['content-disposition']
      let filename = `factura-${facturaId}.pdf`
      if (disposition) {
        const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
        if (match?.[1]) {
          filename = match[1].replace(/['"]/g, '')
        }
      }

      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch {
      error.value = 'Error al descargar el PDF'
    }
  }

  async function pagarFactura(facturaId: number, formaPago?: string): Promise<Factura | null> {
    saving.value = true
    error.value = null
    try {
      const body: Record<string, string> = {}
      if (formaPago) body.formaPago = formaPago
      const response = await api.patch(`/facturas/${facturaId}/pagar`, body)
      const pagada = mapFacturaFromApi(response.data)
      // Update in local list
      const idx = facturas.value.findIndex((f) => f.id === facturaId)
      if (idx !== -1) facturas.value[idx] = pagada
      return pagada
    } catch (e: unknown) {
      const message =
        e instanceof Error ? e.message : 'Error al marcar la factura como pagada'
      error.value = message
      return null
    } finally {
      saving.value = false
    }
  }

  // --- Helpers ---

  function extractArray(data: unknown): Record<string, unknown>[] {
    if (Array.isArray(data)) return data as Record<string, unknown>[]
    if (data && typeof data === 'object') {
      const obj = data as Record<string, unknown>
      if (Array.isArray(obj.content)) return obj.content as Record<string, unknown>[]
      if (Array.isArray(obj.data)) return obj.data as Record<string, unknown>[]
    }
    return []
  }

  function extractFacturasByPorte(data: unknown): Factura[] {
    if (Array.isArray(data)) return data.map(mapFacturaFromApi)
    if (data && typeof data === 'object') {
      const obj = data as Record<string, unknown>
      if (Array.isArray(obj.content)) return (obj.content as unknown[]).map(mapFacturaFromApi)
      if (Array.isArray(obj.data)) return (obj.data as unknown[]).map(mapFacturaFromApi)
      if (obj.id != null) return [mapFacturaFromApi(obj)]
    }
    return []
  }

  function mapPorteFromApi(raw: unknown): FacturaPorte | null {
    if (!raw || typeof raw !== 'object') return null
    const p = raw as Record<string, unknown>
    return {
      id: Number(p.id ?? 0),
      origen: String(p.origen ?? '—'),
      destino: String(p.destino ?? '—'),
      fechaRecogida: p.fechaRecogida ? String(p.fechaRecogida) : null,
      fechaEntrega: p.fechaEntrega ? String(p.fechaEntrega) : null,
      descripcionCliente: p.descripcionCliente ? String(p.descripcionCliente) : null,
      pesoTotalKg: p.pesoTotalKg != null ? Number(p.pesoTotalKg) : null,
      volumenTotalM3: p.volumenTotalM3 != null ? Number(p.volumenTotalM3) : null,
      conductor: p.conductor && typeof p.conductor === 'object'
        ? {
            id: Number((p.conductor as Record<string, unknown>).id ?? 0),
            nombre: String((p.conductor as Record<string, unknown>).nombre ?? ''),
            apellidos: (p.conductor as Record<string, unknown>).apellidos
              ? String((p.conductor as Record<string, unknown>).apellidos)
              : undefined,
          }
        : null,
      cliente: p.cliente && typeof p.cliente === 'object'
        ? {
            id: Number((p.cliente as Record<string, unknown>).id ?? 0),
            nombreEmpresa: String(
              (p.cliente as Record<string, unknown>).nombreEmpresa ??
              (p.cliente as Record<string, unknown>).nombre ?? '',
            ),
            cif: (p.cliente as Record<string, unknown>).cif
              ? String((p.cliente as Record<string, unknown>).cif)
              : undefined,
            direccionFiscal: (p.cliente as Record<string, unknown>).direccionFiscal
              ? String((p.cliente as Record<string, unknown>).direccionFiscal)
              : undefined,
          }
        : null,
    }
  }

  function mapFacturaFromApi(raw: unknown): Factura {
    const f = raw as Record<string, unknown>
    return {
      id: Number(f.id ?? 0),
      numeroSerie: String(f.numeroSerie ?? ''),
      baseImponible: Number(f.baseImponible ?? 0),
      iva: Number(f.iva ?? 0),
      ivaPercent: Number(f.ivaPercent ?? 21),
      importeTotal: Number(f.importeTotal ?? 0),
      fechaEmision: f.fechaEmision ? String(f.fechaEmision) : new Date().toISOString(),
      pagada: f.pagada === true,
      fechaPago: f.fechaPago ? String(f.fechaPago) : null,
      formaPago: f.formaPago ? String(f.formaPago) : null,
      condicionesPago: f.condicionesPago ? String(f.condicionesPago) : null,
      observaciones: f.observaciones ? String(f.observaciones) : null,
      porte: mapPorteFromApi(f.porte),
    }
  }

  return {
    // State
    facturas,
    selectedFactura,
    loading,
    saving,
    error,
    usingMockData,
    dataSource,
    warning,
    // Getters
    totalFacturado,
    totalPagado,
    totalPendiente,
    totalFacturas,
    facturasPagadas,
    facturasPendientes,
    // Actions
    fetchFacturas,
    fetchFactura,
    fetchFacturasByPorte,
    downloadPdf,
    pagarFactura,
  }
})
