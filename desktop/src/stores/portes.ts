import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/services/api'


export interface Conductor {
  id: number
  nombre: string
  apellidos?: string
  telefono?: string
  ciudadBase?: string
  disponible?: boolean
}

export interface Vehiculo {
  id: number
  matricula: string
  marca?: string
  modelo?: string
  tipo?: string
  estado?: string
  capacidadCargaKg?: number
}

export interface Cliente {
  id: number
  nombreEmpresa: string
  cif?: string
  emailContacto?: string
  telefono?: string
}

export type EstadoPorte =
  | 'PENDIENTE'
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
  latitudOrigen?: number
  longitudOrigen?: number
  latitudDestino?: number
  longitudDestino?: number
  distanciaKm?: number
  precio?: number
  ajustePrecio?: number
  motivoAjuste?: string
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
  conductor?: Conductor | null
  cliente?: Cliente | null
}

export interface ConductorCandidato {
  id: number
  nombre: string
  apellidos?: string
  telefono?: string
  ciudadBase?: string
  vehiculoInfo?: string
  score: number
}

export interface DimensionesRequest {
  pesoTotalKg?: number
  volumenTotalM3?: number
  largoMaxPaquete?: number
  anchoMaxPaquete?: number
  altoMaxPaquete?: number
  tipoVehiculoRequerido?: string
}

export interface CreatePorteRequest {
  clienteId: number
  origen: string
  destino: string
  ciudadOrigen?: string
  ciudadDestino?: string
  latitudOrigen?: number
  longitudOrigen?: number
  latitudDestino?: number
  longitudDestino?: number
  descripcionCliente?: string
  fechaRecogida?: string
  fechaEntrega?: string
}

export interface UpdatePorteRequest {
  origen?: string
  destino?: string
  descripcionCliente?: string
  fechaRecogida?: string
  fechaEntrega?: string
  estado?: EstadoPorte
}


const MOCK_CONDUCTORES: Conductor[] = [
  { id: 1, nombre: 'Juan', apellidos: 'Pérez', telefono: '612345678', ciudadBase: 'Madrid', disponible: true },
  { id: 2, nombre: 'María', apellidos: 'López', telefono: '623456789', ciudadBase: 'Barcelona', disponible: true },
  { id: 3, nombre: 'Carlos', apellidos: 'Ruiz', telefono: '634567890', ciudadBase: 'Valencia', disponible: true },
  { id: 4, nombre: 'Ana', apellidos: 'García', telefono: '645678901', ciudadBase: 'Sevilla', disponible: true },
  { id: 5, nombre: 'Pedro', apellidos: 'Martín', telefono: '656789012', ciudadBase: 'Bilbao', disponible: false },
]

const MOCK_VEHICULOS: Vehiculo[] = [
  { id: 1, matricula: '1234ABC', marca: 'Mercedes', modelo: 'Actros', tipo: 'TRAILER', estado: 'DISPONIBLE', capacidadCargaKg: 25000 },
  { id: 2, matricula: '5678DEF', marca: 'Volvo', modelo: 'FH16', tipo: 'TRAILER', estado: 'DISPONIBLE', capacidadCargaKg: 24000 },
  { id: 3, matricula: '9012GHI', marca: 'Iveco', modelo: 'Daily', tipo: 'FURGONETA', estado: 'DISPONIBLE', capacidadCargaKg: 3500 },
  { id: 4, matricula: '3456JKL', marca: 'MAN', modelo: 'TGX', tipo: 'RIGIDO', estado: 'DISPONIBLE', capacidadCargaKg: 18000 },
  { id: 5, matricula: '7890MNO', marca: 'Scania', modelo: 'R500', tipo: 'ESPECIAL', estado: 'DISPONIBLE', capacidadCargaKg: 30000 },
]

const MOCK_CLIENTES: Cliente[] = [
  { id: 1, nombreEmpresa: 'Logistics Express S.L.', cif: 'B12345678', emailContacto: 'info@logisticsexpress.es', telefono: '911234567' },
  { id: 2, nombreEmpresa: 'Trans Ibérica S.A.', cif: 'A87654321', emailContacto: 'contacto@transiberica.es', telefono: '932345678' },
  { id: 3, nombreEmpresa: 'Envíos Rápidos SL', cif: 'B11223344', emailContacto: 'admin@enviosrapidos.es', telefono: '961234567' },
]

const MOCK_PORTES: Porte[] = [
  {
    id: 1001, origen: 'Madrid', destino: 'Barcelona', estado: 'EN_TRANSITO',
    fechaCreacion: '2026-03-10T08:00:00', fechaRecogida: '2026-03-15T10:00:00', fechaEntrega: '2026-03-15T18:00:00',
    precio: 850, distanciaKm: 621, descripcionCliente: 'Palés de electrónica',
    conductor: MOCK_CONDUCTORES[0], cliente: MOCK_CLIENTES[0],
  },
  {
    id: 1002, origen: 'Valencia', destino: 'Sevilla', estado: 'ENTREGADO',
    fechaCreacion: '2026-03-08T09:30:00', fechaRecogida: '2026-03-12T07:00:00', fechaEntrega: '2026-03-12T16:00:00',
    precio: 720, distanciaKm: 654, descripcionCliente: 'Carga general alimentaria',
    conductor: MOCK_CONDUCTORES[1], cliente: MOCK_CLIENTES[1],
  },
  {
    id: 1003, origen: 'Bilbao', destino: 'Zaragoza', estado: 'PENDIENTE',
    fechaCreacion: '2026-03-14T11:00:00', fechaRecogida: '2026-03-18T06:00:00',
    precio: 480, distanciaKm: 305, descripcionCliente: 'Materiales de construcción',
    conductor: null, cliente: MOCK_CLIENTES[2],
  },
  {
    id: 1004, origen: 'Málaga', destino: 'Granada', estado: 'ASIGNADO',
    fechaCreacion: '2026-03-13T14:00:00', fechaRecogida: '2026-03-16T09:00:00', fechaEntrega: '2026-03-16T13:00:00',
    precio: 280, distanciaKm: 126, descripcionCliente: 'Mobiliario de oficina',
    conductor: MOCK_CONDUCTORES[3], cliente: MOCK_CLIENTES[0],
  },
  {
    id: 1005, origen: 'Alicante', destino: 'Murcia', estado: 'CANCELADO',
    fechaCreacion: '2026-03-09T10:00:00', fechaRecogida: '2026-03-11T08:00:00',
    precio: 180, distanciaKm: 82, descripcionCliente: 'Paquetería variada',
    conductor: null, cliente: MOCK_CLIENTES[1],
  },
  {
    id: 1006, origen: 'Valladolid', destino: 'Salamanca', estado: 'EN_TRANSITO',
    fechaCreacion: '2026-03-14T07:00:00', fechaRecogida: '2026-03-15T06:30:00', fechaEntrega: '2026-03-15T10:00:00',
    precio: 220, distanciaKm: 115, descripcionCliente: 'Documentos y correspondencia urgente',
    conductor: MOCK_CONDUCTORES[2], cliente: MOCK_CLIENTES[2],
  },
  {
    id: 1007, origen: 'Córdoba', destino: 'Jaén', estado: 'FACTURADO',
    fechaCreacion: '2026-03-05T12:00:00', fechaRecogida: '2026-03-07T07:00:00', fechaEntrega: '2026-03-07T11:00:00',
    precio: 190, distanciaKm: 107, descripcionCliente: 'Aceite de oliva a granel',
    conductor: MOCK_CONDUCTORES[4], cliente: MOCK_CLIENTES[0],
  },
  {
    id: 1008, origen: 'Santander', destino: 'Oviedo', estado: 'ENTREGADO',
    fechaCreacion: '2026-03-06T08:30:00', fechaRecogida: '2026-03-09T08:00:00', fechaEntrega: '2026-03-09T14:00:00',
    precio: 350, distanciaKm: 203, descripcionCliente: 'Productos lácteos',
    conductor: MOCK_CONDUCTORES[1], cliente: MOCK_CLIENTES[1],
  },
  {
    id: 1009, origen: 'Pamplona', destino: 'San Sebastián', estado: 'PENDIENTE',
    fechaCreacion: '2026-03-15T16:00:00', fechaRecogida: '2026-03-19T07:00:00',
    precio: 240, distanciaKm: 79, descripcionCliente: 'Vinos y espirituosos',
    conductor: null, cliente: MOCK_CLIENTES[2],
  },
  {
    id: 1010, origen: 'Toledo', destino: 'Ciudad Real', estado: 'ASIGNADO',
    fechaCreacion: '2026-03-14T10:00:00', fechaRecogida: '2026-03-17T09:00:00', fechaEntrega: '2026-03-17T13:00:00',
    precio: 300, distanciaKm: 119, descripcionCliente: 'Recambios industriales',
    conductor: MOCK_CONDUCTORES[0], cliente: MOCK_CLIENTES[0],
  },
  {
    id: 1011, origen: 'Cáceres', destino: 'Badajoz', estado: 'EN_TRANSITO',
    fechaCreacion: '2026-03-13T09:00:00', fechaRecogida: '2026-03-15T06:00:00', fechaEntrega: '2026-03-15T10:30:00',
    precio: 260, distanciaKm: 90, descripcionCliente: 'Maquinaria agrícola',
    conductor: MOCK_CONDUCTORES[3], cliente: MOCK_CLIENTES[1],
  },
  {
    id: 1012, origen: 'Tarragona', destino: 'Lleida', estado: 'FACTURADO',
    fechaCreacion: '2026-03-04T11:00:00', fechaRecogida: '2026-03-06T07:00:00', fechaEntrega: '2026-03-06T12:00:00',
    precio: 310, distanciaKm: 98, descripcionCliente: 'Textiles y confección',
    conductor: MOCK_CONDUCTORES[2], cliente: MOCK_CLIENTES[2],
  },
]


export const usePortesStore = defineStore('portes', () => {
  type DataSource = 'api' | 'mock'
  const portes = ref<Porte[]>([])
  const selectedPorte = ref<Porte | null>(null)
  const conductores = ref<Conductor[]>([])
  const vehiculos = ref<Vehiculo[]>([])
  const clientes = ref<Cliente[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const usingMockData = ref(false)
  const dataSource = ref<DataSource>('api')
  const warning = ref<string | null>(null)
  const error = ref<string | null>(null)
  const pendientesRevision = ref<Porte[]>([])
  const conductorCandidatos = ref<ConductorCandidato[]>([])
  const loadingCandidatos = ref(false)

  const totalPortes = computed(() => portes.value.length)
  const portesByEstado = computed(() => {
    const counts: Record<string, number> = {}
    for (const p of portes.value) {
      counts[p.estado] = (counts[p.estado] || 0) + 1
    }
    return counts
  })


  /**
   * Fetch all portes from the API. Falls back to mock data on error.
   */
  async function fetchPortes(): Promise<void> {
    loading.value = true
    usingMockData.value = false
    dataSource.value = 'api'
    warning.value = null
    error.value = null

    try {
      const response = await api.get('/portes')
      const data = extractArray(response.data)
      portes.value = data.map(mapPorteFromApi)
    } catch {
      usingMockData.value = true
      dataSource.value = 'mock'
      warning.value = 'Mostrando portes mock porque la API no respondió'
      error.value = 'No se pudo obtener portes desde la API'
      portes.value = [...MOCK_PORTES]
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch a single porte by ID.
   */
  async function fetchPorteById(id: number): Promise<Porte | null> {
    loading.value = true
    try {
      const response = await api.get(`/portes/${id}`)
      const porte = mapPorteFromApi(response.data)
      const idx = portes.value.findIndex((p) => p.id === id)
      if (idx !== -1) {
        portes.value[idx] = porte
      } else {
        portes.value.unshift(porte)
      }
      selectedPorte.value = porte
      return porte
    } catch {
      const found = portes.value.find((p) => p.id === id)
      if (found) {
        selectedPorte.value = found
        return found
      }
      const mock = MOCK_PORTES.find((p) => p.id === id)
      if (mock) {
        selectedPorte.value = mock
        return mock
      }
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Create a new porte.
   */
  async function createPorte(request: CreatePorteRequest): Promise<Porte> {
    saving.value = true
    try {
      const response = await api.post('/portes', request)
      const newPorte = mapPorteFromApi(response.data)
      portes.value.unshift(newPorte)
      return newPorte
    } catch (error) {
      if (usingMockData.value) {
        const mockPorte: Porte = {
          id: Math.max(0, ...portes.value.map((p) => p.id)) + 1,
          origen: request.origen,
          destino: request.destino,
          descripcionCliente: request.descripcionCliente,
          estado: 'PENDIENTE',
          fechaCreacion: new Date().toISOString(),
          fechaRecogida: request.fechaRecogida ?? undefined,
          fechaEntrega: request.fechaEntrega ?? undefined,
          conductor: null,
          cliente: MOCK_CLIENTES.find((c) => c.id === request.clienteId) ?? null,
        }
        portes.value.unshift(mockPorte)
        return mockPorte
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Update an existing porte.
   */
  async function updatePorte(id: number, request: UpdatePorteRequest): Promise<Porte> {
    saving.value = true
    try {
      const response = await api.put(`/portes/${id}`, request)
      const updated = mapPorteFromApi(response.data)
      const idx = portes.value.findIndex((p) => p.id === id)
      if (idx !== -1) portes.value[idx] = updated
      if (selectedPorte.value?.id === id) selectedPorte.value = updated
      return updated
    } catch (error) {
      if (usingMockData.value) {
        const idx = portes.value.findIndex((p) => p.id === id)
        if (idx !== -1) {
          const current = portes.value[idx]
          const updated: Porte = {
            ...current,
            origen: request.origen ?? current.origen,
            destino: request.destino ?? current.destino,
            descripcionCliente: request.descripcionCliente ?? current.descripcionCliente,
            fechaRecogida: request.fechaRecogida ?? current.fechaRecogida,
            fechaEntrega: request.fechaEntrega ?? current.fechaEntrega,
            estado: request.estado ?? current.estado,
          }
          portes.value[idx] = updated
          if (selectedPorte.value?.id === id) selectedPorte.value = updated
          return updated
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Change porte status via the dedicated endpoint.
   */
  async function changeEstado(porteId: number, nuevoEstado: EstadoPorte): Promise<Porte> {
    saving.value = true
    try {
      const response = await api.put(`/portes/${porteId}/estado`, null, {
        params: { nuevo: nuevoEstado },
      })
      const updated = mapPorteFromApi(response.data)
      const idx = portes.value.findIndex((p) => p.id === porteId)
      if (idx !== -1) portes.value[idx] = updated
      if (selectedPorte.value?.id === porteId) selectedPorte.value = updated
      return updated
    } catch (error) {
      if (usingMockData.value) {
        const idx = portes.value.findIndex((p) => p.id === porteId)
        if (idx !== -1) {
          portes.value[idx] = { ...portes.value[idx], estado: nuevoEstado }
          return portes.value[idx]
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Adjust the price of a porte.
   */
  async function ajustarPrecio(id: number, data: { precioAjustado: number; motivo: string }): Promise<Porte> {
    saving.value = true
    try {
      const motivo = data.motivo.trim()
      if (!motivo) {
        throw new Error('Debes indicar un motivo para el ajuste.')
      }

      const current = portes.value.find((p) => p.id === id)
      const precioBase = current?.precio ?? 0
      const precioAjustado = Number(data.precioAjustado)
      const ajuste = Number((precioAjustado - Number(precioBase)).toFixed(2))

      if (!Number.isFinite(precioAjustado) || !Number.isFinite(ajuste)) {
        throw new Error('El precio ingresado no es válido.')
      }

      const response = await api.post(`/portes/${id}/ajuste`, null, {
        params: {
          cantidad: ajuste.toString(),
          concepto: motivo,
        },
      })
      const updated = mapPorteFromApi(response.data)
      const idx = portes.value.findIndex((p) => p.id === id)
      if (idx !== -1) portes.value[idx] = updated
      if (selectedPorte.value?.id === id) selectedPorte.value = updated
      return updated
    } catch (error) {
      if (usingMockData.value) {
        const idx = portes.value.findIndex((p) => p.id === id)
        if (idx !== -1) {
          const precioBase = Number(portes.value[idx].precio ?? 0)
          const ajuste = Number(data.precioAjustado) - precioBase
          const motivo = data.motivo.trim()
          portes.value[idx] = {
            ...portes.value[idx],
            ajustePrecio: ajuste,
            motivoAjuste: motivo,
          }
          if (selectedPorte.value?.id === id) selectedPorte.value = portes.value[idx]
          return portes.value[idx]
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Mark a porte as invoiced (facturado).
   */
  async function facturarPorte(id: number): Promise<void> {
    saving.value = true
    try {
      await api.post(`/portes/${id}/facturar`)
      const refreshed = await fetchPorteById(id)
      if (!refreshed) {
        await fetchPortes()
      }
    } catch (error) {
      if (usingMockData.value) {
        const idx = portes.value.findIndex((p) => p.id === id)
        if (idx !== -1) {
          portes.value[idx] = { ...portes.value[idx], estado: 'FACTURADO' }
          if (selectedPorte.value?.id === id) selectedPorte.value = portes.value[idx]
          return
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Delete a porte (ADMIN only).
   */
  async function deletePorte(id: number): Promise<void> {
    saving.value = true
    try {
      await api.delete(`/portes/${id}`)
      portes.value = portes.value.filter((p) => p.id !== id)
      if (selectedPorte.value?.id === id) selectedPorte.value = null
    } catch (error) {
      if (usingMockData.value) {
        portes.value = portes.value.filter((p) => p.id !== id)
        if (selectedPorte.value?.id === id) selectedPorte.value = null
        return
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Fetch conductores list for dropdowns.
   */
  async function fetchConductores(): Promise<void> {
    try {
      const response = await api.get('/conductores')
      conductores.value = extractArray(response.data).map((c: Record<string, unknown>) => ({
        id: Number(c.id ?? 0),
        nombre: String(c.nombre ?? ''),
        apellidos: c.apellidos ? String(c.apellidos) : undefined,
        telefono: c.telefono ? String(c.telefono) : undefined,
        ciudadBase: c.ciudadBase ? String(c.ciudadBase) : undefined,
        disponible: c.disponible !== false,
      }))
    } catch {
      conductores.value = [...MOCK_CONDUCTORES]
    }
  }

  /**
   * Fetch vehiculos list for dropdowns.
   */
  async function fetchVehiculos(): Promise<void> {
    try {
      const response = await api.get('/vehiculos')
      vehiculos.value = extractArray(response.data).map((v: Record<string, unknown>) => ({
        id: Number(v.id ?? 0),
        matricula: String(v.matricula ?? ''),
        marca: v.marca ? String(v.marca) : undefined,
        modelo: v.modelo ? String(v.modelo) : undefined,
        tipo: v.tipo ? String(v.tipo) : undefined,
        estado: v.estado ? String(v.estado) : undefined,
        capacidadCargaKg: v.capacidadCargaKg ? Number(v.capacidadCargaKg) : undefined,
      }))
    } catch {
      vehiculos.value = [...MOCK_VEHICULOS]
    }
  }

  /**
   * Fetch clientes list for dropdowns.
   */
  async function fetchClientes(): Promise<void> {
    try {
      const response = await api.get('/clientes')
      clientes.value = extractArray(response.data).map((c: Record<string, unknown>) => ({
        id: Number(c.id ?? 0),
        nombreEmpresa: String(c.nombreEmpresa ?? c.nombre ?? ''),
        cif: c.cif ? String(c.cif) : undefined,
        emailContacto: c.emailContacto ? String(c.emailContacto) : undefined,
        telefono: c.telefono ? String(c.telefono) : undefined,
      }))
    } catch {
      clientes.value = [...MOCK_CLIENTES]
    }
  }

  /**
   * Fetch all reference data (conductores, vehiculos, clientes) in parallel.
   */
  async function fetchReferenceData(): Promise<void> {
    await Promise.allSettled([fetchConductores(), fetchVehiculos(), fetchClientes()])
  }


  /**
   * Fetch portes pending manual review.
   */
  async function fetchPendientesRevision(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get('/portes/pendientes-revision')
      const data = extractArray(response.data)
      pendientesRevision.value = data.map(mapPorteFromApi)
    } catch {
      error.value = 'No se pudo cargar el porte solicitado'
      pendientesRevision.value = portes.value.filter(
        (p) => p.revisionManual
          || (
            p.estado === 'PENDIENTE'
            && !p.conductor
            && !!p.motivoRevision
            && p.motivoRevision !== 'La carga se ha analizado correctamente. Le asignaremos un conductor lo antes posible.'
          ),
      )
    } finally {
      loading.value = false
    }
  }

  /**
   * Update cargo dimensions on a porte (admin).
   */
  async function updateDimensiones(porteId: number, dimensiones: DimensionesRequest): Promise<Porte> {
    saving.value = true
    try {
      const response = await api.put(`/portes/${porteId}/dimensiones`, dimensiones)
      const updated = mapPorteFromApi(response.data)
      const idx = portes.value.findIndex((p) => p.id === porteId)
      if (idx !== -1) portes.value[idx] = updated
      const revIdx = pendientesRevision.value.findIndex((p) => p.id === porteId)
      if (revIdx !== -1) pendientesRevision.value[revIdx] = updated
      return updated
    } finally {
      saving.value = false
    }
  }

  /**
   * Search for matching conductor candidates for a porte.
   */
  async function buscarConductores(porteId: number): Promise<ConductorCandidato[]> {
    loadingCandidatos.value = true
    conductorCandidatos.value = []
    try {
      const response = await api.post(`/portes/${porteId}/buscar-conductores`)
      const data = Array.isArray(response.data) ? response.data : []
      conductorCandidatos.value = data.map((c: Record<string, unknown>) => ({
        id: Number(c.id ?? 0),
        nombre: String(c.nombre ?? ''),
        apellidos: c.apellidos ? String(c.apellidos) : undefined,
        telefono: c.telefono ? String(c.telefono) : undefined,
        ciudadBase: c.ciudadBase ? String(c.ciudadBase) : undefined,
        vehiculoInfo: c.vehiculoInfo ? String(c.vehiculoInfo) : undefined,
        score: Number(c.score ?? 0),
      }))
      return conductorCandidatos.value
    } catch {
      conductorCandidatos.value = []
      return []
    } finally {
      loadingCandidatos.value = false
    }
  }

  /**
   * Assign a conductor to a porte manually (admin).
   */
  async function asignarConductor(porteId: number, conductorId: number): Promise<Porte> {
    saving.value = true
    try {
      const response = await api.post(`/portes/${porteId}/asignar-conductor`, null, {
        params: { conductorId },
      })
      const updated = mapPorteFromApi(response.data)
      const idx = portes.value.findIndex((p) => p.id === porteId)
      if (idx !== -1) portes.value[idx] = updated
      pendientesRevision.value = pendientesRevision.value.filter((p) => p.id !== porteId)
      conductorCandidatos.value = []
      return updated
    } finally {
      saving.value = false
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
      let filename = `albaran-porte-${porteId}.pdf`
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
    } catch (err) {
      const message = err instanceof Error && err.message.trim()
        ? err.message
        : 'No se pudo descargar el albarán PDF'
      error.value = message
      throw new Error(message)
    }
  }

  /**
   * Retry strict automatic matching after admin corrections.
   */
  async function retryMatching(porteId: number): Promise<Porte> {
    saving.value = true
    try {
      const response = await api.post(`/portes/${porteId}/retry-matching`)
      const updated = mapPorteFromApi(response.data)
      const idx = portes.value.findIndex((p) => p.id === porteId)
      if (idx !== -1) portes.value[idx] = updated

      const revIdx = pendientesRevision.value.findIndex((p) => p.id === porteId)
      if (revIdx !== -1) {
        if (updated.revisionManual || (updated.estado === 'PENDIENTE' && !updated.conductor && !!updated.motivoRevision && updated.motivoRevision !== 'La carga se ha analizado correctamente. Le asignaremos un conductor lo antes posible.')) {
          pendientesRevision.value[revIdx] = updated
        } else {
          pendientesRevision.value = pendientesRevision.value.filter((p) => p.id !== porteId)
        }
      }
      return updated
    } finally {
      saving.value = false
    }
  }

  /**
   * Publica un porte revisado: limpia revisionManual, valida y ejecuta matching.
   */
  async function publicarPorteRevisado(porteId: number): Promise<Porte> {
    saving.value = true
    try {
      const response = await api.post(`/portes/${porteId}/publicar`)
      const updated = mapPorteFromApi(response.data)
      const idx = portes.value.findIndex((p) => p.id === porteId)
      if (idx !== -1) portes.value[idx] = updated

      // Quitar de pendientes revisión
      pendientesRevision.value = pendientesRevision.value.filter((p) => p.id !== porteId)
      return updated
    } finally {
      saving.value = false
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
      latitudOrigen: p.latitudOrigen != null ? Number(p.latitudOrigen) : undefined,
      longitudOrigen: p.longitudOrigen != null ? Number(p.longitudOrigen) : undefined,
      latitudDestino: p.latitudDestino != null ? Number(p.latitudDestino) : undefined,
      longitudDestino: p.longitudDestino != null ? Number(p.longitudDestino) : undefined,
      distanciaKm: p.distanciaKm != null ? Number(p.distanciaKm) : undefined,
      precio: p.precio != null ? Number(p.precio) : undefined,
      ajustePrecio: p.ajustePrecio != null ? Number(p.ajustePrecio) : undefined,
      motivoAjuste: p.motivoAjuste ? String(p.motivoAjuste) : undefined,
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
      cliente: mapCliente(p.cliente),
    }
  }

  function mapConductor(raw: unknown): Conductor | null {
    if (!raw || typeof raw !== 'object') return null
    const c = raw as Record<string, unknown>
    return {
      id: Number(c.id ?? 0),
      nombre: String(c.nombre ?? ''),
      apellidos: c.apellidos ? String(c.apellidos) : undefined,
      telefono: c.telefono ? String(c.telefono) : undefined,
      ciudadBase: c.ciudadBase ? String(c.ciudadBase) : undefined,
      disponible: c.disponible !== false,
    }
  }

  function mapCliente(raw: unknown): Cliente | null {
    if (!raw || typeof raw !== 'object') return null
    const c = raw as Record<string, unknown>
    return {
      id: Number(c.id ?? 0),
      nombreEmpresa: String(c.nombreEmpresa ?? c.nombre ?? ''),
      cif: c.cif ? String(c.cif) : undefined,
      emailContacto: c.emailContacto ? String(c.emailContacto) : undefined,
      telefono: c.telefono ? String(c.telefono) : undefined,
    }
  }

  return {
    portes,
    selectedPorte,
    conductores,
    vehiculos,
    clientes,
    loading,
    saving,
    usingMockData,
    dataSource,
    warning,
    error,
    pendientesRevision,
    conductorCandidatos,
    loadingCandidatos,
    totalPortes,
    portesByEstado,
    fetchPortes,
    fetchPorteById,
    createPorte,
    updatePorte,
    changeEstado,
    deletePorte,
    ajustarPrecio,
    facturarPorte,
    downloadAlbaran,
    fetchConductores,
    fetchVehiculos,
    fetchClientes,
    fetchReferenceData,
    fetchPendientesRevision,
    updateDimensiones,
    buscarConductores,
    asignarConductor,
    retryMatching,
    publicarPorteRevisado,
  }
})
