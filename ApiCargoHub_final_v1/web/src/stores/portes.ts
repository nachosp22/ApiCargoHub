import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/services/api'


export type EstadoPorte =
  | 'PENDIENTE'
  | 'SOLICITUD'
  | 'ASIGNADO'
  | 'EN_TRANSITO'
  | 'ENTREGADO'
  | 'CANCELADO'
  | 'FACTURADO'

export interface Porte {
  id: number
  origen: string
  destino: string
  ciudadOrigen?: string
  ciudadDestino?: string
  distanciaKm?: number
  precio?: number
  descripcionCliente?: string
  pesoTotalKg?: number
  volumenTotalM3?: number
  largoMaxPaquete?: number
  anchoMaxPaquete?: number
  altoMaxPaquete?: number
  tipoVehiculoRequerido?: string
  revisionManual?: boolean
  motivoRevision?: string
  estado: EstadoPorte
  fechaCreacion?: string
  fechaRecogida?: string
  fechaEntrega?: string
  conductor?: { id: number; nombre: string; apellidos?: string } | null
}

export interface SolicitudPorteRequest {
  origen: string
  destino: string
  ciudadOrigen?: string
  ciudadDestino?: string
  latitudOrigen?: number
  longitudOrigen?: number
  latitudDestino?: number
  longitudDestino?: number
  descripcionCliente: string
  fechaRecogida?: string
}

export interface PorteTracking {
  driverLat: number | null
  driverLng: number | null
  lastUpdate: string | null
  speedKph: number | null
  headingDeg: number | null
  originLat: number | null
  originLng: number | null
  originName: string
  destinationLat: number | null
  destinationLng: number | null
  destinationName: string
  etaMinutes: number | null
  etaConfidence: string | null
  status: EstadoPorte
  driverName: string | null
  vehicleInfo: string | null
}


export const usePortesStore = defineStore('portes', () => {
  const portes = ref<Porte[]>([])
  const loading = ref(false)
  const submitting = ref(false)
  const error = ref<string | null>(null)
  const tracking = ref<PorteTracking | null>(null)
  const trackingLoading = ref(false)
  let trackingInterval: ReturnType<typeof setInterval> | null = null

  const portesActivos = computed(() =>
    portes.value.filter((p) =>
      ['PENDIENTE', 'SOLICITUD', 'ASIGNADO', 'EN_TRANSITO'].includes(p.estado)
    )
  )

  const portesCompletados = computed(() =>
    portes.value.filter((p) => p.estado === 'ENTREGADO' || p.estado === 'FACTURADO')
  )


  async function fetchOwn(clienteId: number): Promise<void> {
    loading.value = true
    error.value = null

    try {
      const response = await api.get(`/portes/cliente/${clienteId}`)
      const data = extractArray(response.data)
      portes.value = data.map(mapPorteFromApi)
    } catch (err) {
      error.value = 'No se pudieron cargar los portes.'
      portes.value = []
    } finally {
      loading.value = false
    }
  }

  async function createSolicitud(request: SolicitudPorteRequest): Promise<Porte> {
    submitting.value = true
    error.value = null

    try {
      const response = await api.post('/portes/solicitud', request)
      const newPorte = mapPorteFromApi(response.data)
      portes.value.unshift(newPorte)
      return newPorte
    } catch (err) {
      error.value = 'No se pudo crear la solicitud de porte.'
      throw err
    } finally {
      submitting.value = false
    }
  }

  async function downloadAlbaran(porteId: number): Promise<void> {
    try {
      const response = await api.get(`/portes/${porteId}/albaran/pdf`, {
        responseType: 'blob',
      })

      const contentType = String(response.headers['content-type'] ?? '').toLowerCase()
      if (!contentType.includes('application/pdf')) {
        const errorText = await response.data.text()
        throw new Error(errorText || 'No se pudo descargar el albarán PDF')
      }

      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url

      const disposition = response.headers['content-disposition']
      const filenameMatch = disposition?.match(/filename[^;=\n]*=(.*)/i)
      link.download = filenameMatch
        ? filenameMatch[1].replace(/["']/g, '').trim()
        : `albaran-porte-${porteId}.pdf`

      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: Blob } }).response
        if (response?.data instanceof Blob) {
          const text = await response.data.text()
          throw new Error(text || 'No se pudo descargar el albarán PDF')
        }
      }

      if (err instanceof Error && err.message.trim()) {
        throw err
      }

      throw new Error('No se pudo descargar el albarán PDF')
    }
  }


  function extractArray(data: unknown): Record<string, unknown>[] {
    if (Array.isArray(data)) return data as Record<string, unknown>[]
    if (data && typeof data === 'object') {
      const obj = data as Record<string, unknown>
      if (Array.isArray(obj.content)) return obj.content as Record<string, unknown>[]
      if (Array.isArray(obj.data)) return obj.data as Record<string, unknown>[]
    }
    return []
  }

  function mapPorteFromApi(raw: unknown): Porte {
    const p = raw as Record<string, unknown>
    return {
      id: Number(p.id ?? 0),
      origen: String(p.origen ?? '—'),
      destino: String(p.destino ?? '—'),
      ciudadOrigen: p.ciudadOrigen ? String(p.ciudadOrigen) : undefined,
      ciudadDestino: p.ciudadDestino ? String(p.ciudadDestino) : undefined,
      distanciaKm: p.distanciaKm != null ? Number(p.distanciaKm) : undefined,
      precio: p.precio != null ? Number(p.precio) : undefined,
      descripcionCliente: p.descripcionCliente ? String(p.descripcionCliente) : undefined,
      pesoTotalKg: p.pesoTotalKg != null ? Number(p.pesoTotalKg) : undefined,
      volumenTotalM3: p.volumenTotalM3 != null ? Number(p.volumenTotalM3) : undefined,
      largoMaxPaquete: p.largoMaxPaquete != null ? Number(p.largoMaxPaquete) : undefined,
      anchoMaxPaquete: p.anchoMaxPaquete != null ? Number(p.anchoMaxPaquete) : undefined,
      altoMaxPaquete: p.altoMaxPaquete != null ? Number(p.altoMaxPaquete) : undefined,
      tipoVehiculoRequerido: p.tipoVehiculoRequerido ? String(p.tipoVehiculoRequerido) : undefined,
      revisionManual: p.revisionManual === true,
      motivoRevision: p.motivoRevision ? String(p.motivoRevision) : undefined,
      estado: (String(p.estado ?? 'PENDIENTE')) as EstadoPorte,
      fechaCreacion: p.fechaCreacion ? String(p.fechaCreacion) : undefined,
      fechaRecogida: p.fechaRecogida ? String(p.fechaRecogida) : undefined,
      fechaEntrega: p.fechaEntrega ? String(p.fechaEntrega) : undefined,
      conductor: mapConductor(p.conductor),
    }
  }

  function mapConductor(raw: unknown): Porte['conductor'] {
    if (!raw || typeof raw !== 'object') return null
    const c = raw as Record<string, unknown>
    return {
      id: Number(c.id ?? 0),
      nombre: String(c.nombre ?? ''),
      apellidos: c.apellidos ? String(c.apellidos) : undefined,
    }
  }

  async function fetchTracking(porteId: number): Promise<PorteTracking | null> {
    trackingLoading.value = true
    try {
      const response = await api.get(`/portes/${porteId}/tracking`)
      const d = response.data as Record<string, unknown>
      tracking.value = {
        driverLat: d.driverLat != null ? Number(d.driverLat) : null,
        driverLng: d.driverLng != null ? Number(d.driverLng) : null,
        lastUpdate: d.lastUpdate ? String(d.lastUpdate) : null,
        speedKph: d.speedKph != null ? Number(d.speedKph) : null,
        headingDeg: d.headingDeg != null ? Number(d.headingDeg) : null,
        originLat: d.originLat != null ? Number(d.originLat) : null,
        originLng: d.originLng != null ? Number(d.originLng) : null,
        originName: String(d.originName ?? ''),
        destinationLat: d.destinationLat != null ? Number(d.destinationLat) : null,
        destinationLng: d.destinationLng != null ? Number(d.destinationLng) : null,
        destinationName: String(d.destinationName ?? ''),
        etaMinutes: d.etaMinutes != null ? Number(d.etaMinutes) : null,
        etaConfidence: d.etaConfidence ? String(d.etaConfidence) : null,
        status: String(d.status ?? 'PENDIENTE') as EstadoPorte,
        driverName: d.driverName ? String(d.driverName) : null,
        vehicleInfo: d.vehicleInfo ? String(d.vehicleInfo) : null,
      }
      return tracking.value
    } catch {
      tracking.value = null
      return null
    } finally {
      trackingLoading.value = false
    }
  }

  function startTrackingPolling(porteId: number, intervalMs = 10000) {
    stopTrackingPolling()
    fetchTracking(porteId)
    trackingInterval = setInterval(() => fetchTracking(porteId), intervalMs)
  }

  function stopTrackingPolling() {
    if (trackingInterval) {
      clearInterval(trackingInterval)
      trackingInterval = null
    }
    tracking.value = null
  }

  return {
    portes,
    loading,
    submitting,
    error,
    tracking,
    trackingLoading,
    portesActivos,
    portesCompletados,
    fetchOwn,
    createSolicitud,
    downloadAlbaran,
    fetchTracking,
    startTrackingPolling,
    stopTrackingPolling,
  }
})
