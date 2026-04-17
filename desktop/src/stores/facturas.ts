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

// --- Store ---

export const useFacturasStore = defineStore('facturas', () => {
  // --- State ---
  const facturas = ref<Factura[]>([])
  const selectedFactura = ref<Factura | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const usingMockData = ref(false)

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

    try {
      const response = await api.get('/facturas')
      const data = extractArray(response.data)
      facturas.value = data.map(mapFacturaFromApi)
    } catch (err) {
      error.value = 'Error al cargar las facturas'
      facturas.value = []
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

    try {
      const response = await api.get(`/facturas/porte/${porteId}`)
      const data = extractArray(response.data)
      return data.map(mapFacturaFromApi)
    } catch {
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
    error,
    usingMockData,
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
  }
})
