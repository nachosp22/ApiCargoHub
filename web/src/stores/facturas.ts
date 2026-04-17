import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/services/api'

// --- Types ---

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
  porte: {
    id: number
    origen: string
    destino: string
  } | null
}

// --- Store ---

export const useFacturasStore = defineStore('facturas', () => {
  const facturas = ref<Factura[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // --- Getters ---
  const totalPendiente = computed(() =>
    facturas.value.filter((f) => !f.pagada).reduce((sum, f) => sum + f.importeTotal, 0)
  )

  const facturasPendientes = computed(() => facturas.value.filter((f) => !f.pagada).length)

  const totalFacturado = computed(() =>
    facturas.value.reduce((sum, f) => sum + f.importeTotal, 0)
  )

  // --- Actions ---

  async function fetchOwn(clienteId: number): Promise<void> {
    loading.value = true
    error.value = null

    try {
      const response = await api.get(`/facturas/cliente/${clienteId}`)
      const data = extractArray(response.data)
      facturas.value = data.map(mapFacturaFromApi)
    } catch (err) {
      error.value = 'No se pudieron cargar las facturas.'
      facturas.value = []
    } finally {
      loading.value = false
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
      porte: mapPorteFromApi(f.porte),
    }
  }

  function mapPorteFromApi(raw: unknown): Factura['porte'] {
    if (!raw || typeof raw !== 'object') return null
    const p = raw as Record<string, unknown>
    return {
      id: Number(p.id ?? 0),
      origen: String(p.origen ?? '—'),
      destino: String(p.destino ?? '—'),
    }
  }

  async function fetchMisFacturas(): Promise<void> {
    loading.value = true
    error.value = null

    try {
      const response = await api.get('/facturas/mis-facturas')
      const data = extractArray(response.data)
      facturas.value = data.map(mapFacturaFromApi)
    } catch (err) {
      error.value = 'No se pudieron cargar las facturas.'
      facturas.value = []
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

      // Try to extract filename from Content-Disposition header
      const disposition = response.headers['content-disposition']
      const filenameMatch = disposition?.match(/filename[^;=\n]*=(.*)/i)
      link.download = filenameMatch ? filenameMatch[1].replace(/["']/g, '').trim() : `factura-${facturaId}.pdf`

      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (err) {
      throw new Error('No se pudo descargar el PDF.')
    }
  }

  return {
    facturas,
    loading,
    error,
    totalPendiente,
    facturasPendientes,
    totalFacturado,
    fetchOwn,
    fetchMisFacturas,
    downloadPdf,
  }
})
