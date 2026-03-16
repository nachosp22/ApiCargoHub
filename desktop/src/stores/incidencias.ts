import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/services/api'

// --- TypeScript Interfaces (matching API DTOs) ---

export type EstadoIncidencia = 'ABIERTA' | 'EN_REVISION' | 'RESUELTA' | 'DESESTIMADA'
export type SeveridadIncidencia = 'BAJA' | 'MEDIA' | 'ALTA'
export type PrioridadIncidencia = 'BAJA' | 'MEDIA' | 'ALTA'

export interface Incidencia {
  id: number
  porteId: number
  titulo: string
  descripcion: string
  estado: EstadoIncidencia
  severidad: SeveridadIncidencia
  prioridad: PrioridadIncidencia
  fechaReporte: string
  fechaLimiteSla: string | null
  resolucion: string | null
  fechaResolucion: string | null
  adminId: number | null
}

export interface IncidenciaEvento {
  id: number
  incidenciaId: number
  actorId: number | null
  estadoAnterior: EstadoIncidencia | null
  estadoNuevo: EstadoIncidencia
  fecha: string
  accion: string
  comentario: string | null
}

export interface CrearIncidenciaRequest {
  titulo: string
  descripcion: string
  severidad?: SeveridadIncidencia
  prioridad?: PrioridadIncidencia
}

export interface ResolverIncidenciaRequest {
  resolucion?: string
  estadoFinal: EstadoIncidencia
}

// --- Mock Data ---

const MOCK_INCIDENCIAS: Incidencia[] = [
  {
    id: 1,
    porteId: 1001,
    titulo: 'Retraso en entrega por condiciones meteorológicas',
    descripcion: 'La entrega se ha retrasado 4 horas debido a fuertes lluvias en la zona de Barcelona.',
    estado: 'ABIERTA',
    severidad: 'ALTA',
    prioridad: 'ALTA',
    fechaReporte: '2026-03-14T10:30:00',
    fechaLimiteSla: '2026-03-15T10:30:00',
    resolucion: null,
    fechaResolucion: null,
    adminId: null,
  },
  {
    id: 2,
    porteId: 1002,
    titulo: 'Daño parcial en mercancía alimentaria',
    descripcion: 'Se detectó daño en 2 de los 10 palés durante la descarga en destino.',
    estado: 'EN_REVISION',
    severidad: 'MEDIA',
    prioridad: 'ALTA',
    fechaReporte: '2026-03-12T16:45:00',
    fechaLimiteSla: '2026-03-13T16:45:00',
    resolucion: null,
    fechaResolucion: null,
    adminId: null,
  },
  {
    id: 3,
    porteId: 1003,
    titulo: 'Documentación incompleta del conductor',
    descripcion: 'El conductor no presentó el albarán firmado en origen.',
    estado: 'RESUELTA',
    severidad: 'BAJA',
    prioridad: 'BAJA',
    fechaReporte: '2026-03-10T08:00:00',
    fechaLimiteSla: '2026-03-15T08:00:00',
    resolucion: 'Se contactó al conductor y reenvió la documentación firmada digitalmente.',
    fechaResolucion: '2026-03-11T14:00:00',
    adminId: 1,
  },
  {
    id: 4,
    porteId: 1004,
    titulo: 'Error en dirección de entrega',
    descripcion: 'El sistema mostraba una dirección incorrecta para el punto de entrega en Granada.',
    estado: 'DESESTIMADA',
    severidad: 'BAJA',
    prioridad: 'MEDIA',
    fechaReporte: '2026-03-13T09:15:00',
    fechaLimiteSla: '2026-03-16T09:15:00',
    resolucion: 'Verificada la dirección, era correcta. Error del reportante.',
    fechaResolucion: '2026-03-13T11:00:00',
    adminId: 1,
  },
  {
    id: 5,
    porteId: 1006,
    titulo: 'Vehículo averiado en ruta',
    descripcion: 'El camión sufrió un fallo mecánico en la A-62 cerca de Tordesillas. Necesita asistencia.',
    estado: 'ABIERTA',
    severidad: 'ALTA',
    prioridad: 'ALTA',
    fechaReporte: '2026-03-15T07:30:00',
    fechaLimiteSla: '2026-03-16T07:30:00',
    resolucion: null,
    fechaResolucion: null,
    adminId: null,
  },
  {
    id: 6,
    porteId: 1001,
    titulo: 'Cliente solicita cambio de hora de entrega',
    descripcion: 'El destinatario solicita cambiar la entrega de las 18:00 a las 20:00.',
    estado: 'EN_REVISION',
    severidad: 'BAJA',
    prioridad: 'MEDIA',
    fechaReporte: '2026-03-15T11:00:00',
    fechaLimiteSla: '2026-03-18T11:00:00',
    resolucion: null,
    fechaResolucion: null,
    adminId: null,
  },
  {
    id: 7,
    porteId: 1008,
    titulo: 'Falta de refrigeración en transporte lácteo',
    descripcion: 'Se detectó que la temperatura del compartimento de carga subió a 12°C durante 2 horas.',
    estado: 'RESUELTA',
    severidad: 'ALTA',
    prioridad: 'ALTA',
    fechaReporte: '2026-03-09T15:00:00',
    fechaLimiteSla: '2026-03-10T15:00:00',
    resolucion: 'Se descargó la mercancía en punto intermedio con cámara frigorífica. Se reasignó vehículo.',
    fechaResolucion: '2026-03-09T20:00:00',
    adminId: 1,
  },
  {
    id: 8,
    porteId: 1011,
    titulo: 'Peaje no incluido en tarifa',
    descripcion: 'El conductor pagó un peaje de 15€ que no estaba incluido en la tarifa acordada con el cliente.',
    estado: 'ABIERTA',
    severidad: 'BAJA',
    prioridad: 'BAJA',
    fechaReporte: '2026-03-15T14:00:00',
    fechaLimiteSla: '2026-03-20T14:00:00',
    resolucion: null,
    fechaResolucion: null,
    adminId: null,
  },
]

const MOCK_HISTORIAL: Record<number, IncidenciaEvento[]> = {
  1: [
    {
      id: 1, incidenciaId: 1, actorId: 3, estadoAnterior: null, estadoNuevo: 'ABIERTA',
      fecha: '2026-03-14T10:30:00', accion: 'CREACION', comentario: 'Incidencia reportada por conductor.',
    },
  ],
  2: [
    {
      id: 2, incidenciaId: 2, actorId: 3, estadoAnterior: null, estadoNuevo: 'ABIERTA',
      fecha: '2026-03-12T16:45:00', accion: 'CREACION', comentario: 'Daño detectado en descarga.',
    },
    {
      id: 3, incidenciaId: 2, actorId: 1, estadoAnterior: 'ABIERTA', estadoNuevo: 'EN_REVISION',
      fecha: '2026-03-12T17:30:00', accion: 'CAMBIO_ESTADO', comentario: 'Revisando fotos del daño.',
    },
  ],
  3: [
    {
      id: 4, incidenciaId: 3, actorId: 2, estadoAnterior: null, estadoNuevo: 'ABIERTA',
      fecha: '2026-03-10T08:00:00', accion: 'CREACION', comentario: null,
    },
    {
      id: 5, incidenciaId: 3, actorId: 1, estadoAnterior: 'ABIERTA', estadoNuevo: 'EN_REVISION',
      fecha: '2026-03-10T10:00:00', accion: 'CAMBIO_ESTADO', comentario: 'Contactando al conductor.',
    },
    {
      id: 6, incidenciaId: 3, actorId: 1, estadoAnterior: 'EN_REVISION', estadoNuevo: 'RESUELTA',
      fecha: '2026-03-11T14:00:00', accion: 'RESOLUCION', comentario: 'Documentación recibida.',
    },
  ],
  7: [
    {
      id: 7, incidenciaId: 7, actorId: 3, estadoAnterior: null, estadoNuevo: 'ABIERTA',
      fecha: '2026-03-09T15:00:00', accion: 'CREACION', comentario: 'Alerta de temperatura.',
    },
    {
      id: 8, incidenciaId: 7, actorId: 1, estadoAnterior: 'ABIERTA', estadoNuevo: 'RESUELTA',
      fecha: '2026-03-09T20:00:00', accion: 'RESOLUCION', comentario: 'Mercancía a salvo, vehículo reasignado.',
    },
  ],
}

// --- Porte type for dropdown ---
export interface PorteRef {
  id: number
  label: string
}

// --- Store ---

export const useIncidenciasStore = defineStore('incidencias', () => {
  // --- State ---
  const incidencias = ref<Incidencia[]>([])
  const selectedIncidencia = ref<Incidencia | null>(null)
  const historial = ref<IncidenciaEvento[]>([])
  const porteOptions = ref<PorteRef[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const loadingHistorial = ref(false)
  const usingMockData = ref(false)

  // --- Getters ---
  const totalIncidencias = computed(() => incidencias.value.length)

  const incidenciasByEstado = computed(() => {
    const counts: Record<string, number> = {}
    for (const i of incidencias.value) {
      counts[i.estado] = (counts[i.estado] || 0) + 1
    }
    return counts
  })

  const pendientes = computed(() =>
    incidencias.value.filter((i) => i.estado === 'ABIERTA' || i.estado === 'EN_REVISION')
  )

  const vencidas = computed(() =>
    incidencias.value.filter((i) => {
      if (i.estado === 'RESUELTA' || i.estado === 'DESESTIMADA') return false
      if (!i.fechaLimiteSla) return false
      return new Date(i.fechaLimiteSla) < new Date()
    })
  )

  // --- Actions ---

  async function fetchIncidencias(): Promise<void> {
    loading.value = true
    usingMockData.value = false

    try {
      const response = await api.get('/incidencias')
      const data = extractArray(response.data)
      incidencias.value = data.map(mapIncidenciaFromApi)
    } catch {
      usingMockData.value = true
      incidencias.value = [...MOCK_INCIDENCIAS]
    } finally {
      loading.value = false
    }
  }

  async function fetchVencidas(): Promise<void> {
    loading.value = true
    usingMockData.value = false

    try {
      const response = await api.get('/incidencias/vencidas-sla')
      const data = extractArray(response.data)
      incidencias.value = data.map(mapIncidenciaFromApi)
    } catch {
      // Fallback: filter mock data to show overdue
      usingMockData.value = true
      const now = new Date()
      incidencias.value = MOCK_INCIDENCIAS.filter((i) => {
        if (i.estado === 'RESUELTA' || i.estado === 'DESESTIMADA') return false
        if (!i.fechaLimiteSla) return false
        return new Date(i.fechaLimiteSla) < now
      })
    } finally {
      loading.value = false
    }
  }

  async function fetchIncidenciaById(id: number): Promise<Incidencia | null> {
    loading.value = true
    try {
      const response = await api.get(`/incidencias/${id}`)
      const inc = mapIncidenciaFromApi(response.data)
      selectedIncidencia.value = inc
      return inc
    } catch {
      const found = incidencias.value.find((i) => i.id === id)
      if (found) {
        selectedIncidencia.value = found
        return found
      }
      const mock = MOCK_INCIDENCIAS.find((i) => i.id === id)
      if (mock) {
        selectedIncidencia.value = mock
        return mock
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchHistorial(incidenciaId: number): Promise<void> {
    loadingHistorial.value = true
    historial.value = []
    try {
      const response = await api.get(`/incidencias/${incidenciaId}/historial`)
      const data = extractArray(response.data)
      historial.value = data.map(mapEventoFromApi)
    } catch {
      // Mock fallback
      historial.value = MOCK_HISTORIAL[incidenciaId] ?? []
    } finally {
      loadingHistorial.value = false
    }
  }

  async function crearIncidencia(porteId: number, request: CrearIncidenciaRequest): Promise<Incidencia> {
    saving.value = true
    try {
      const response = await api.post('/incidencias', request, {
        params: { porteId },
      })
      const created = mapIncidenciaFromApi(response.data)
      incidencias.value.unshift(created)
      return created
    } catch (error) {
      if (usingMockData.value) {
        const mockInc: Incidencia = {
          id: Math.max(0, ...incidencias.value.map((i) => i.id)) + 1,
          porteId,
          titulo: request.titulo,
          descripcion: request.descripcion,
          estado: 'ABIERTA',
          severidad: request.severidad ?? 'MEDIA',
          prioridad: request.prioridad ?? 'MEDIA',
          fechaReporte: new Date().toISOString(),
          fechaLimiteSla: new Date(Date.now() + 72 * 60 * 60 * 1000).toISOString(),
          resolucion: null,
          fechaResolucion: null,
          adminId: null,
        }
        incidencias.value.unshift(mockInc)
        return mockInc
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  async function resolverIncidencia(id: number, request: ResolverIncidenciaRequest): Promise<Incidencia> {
    saving.value = true
    try {
      const response = await api.put(`/incidencias/${id}/resolver`, request)
      const updated = mapIncidenciaFromApi(response.data)
      const idx = incidencias.value.findIndex((i) => i.id === id)
      if (idx !== -1) incidencias.value[idx] = updated
      if (selectedIncidencia.value?.id === id) selectedIncidencia.value = updated
      return updated
    } catch (error) {
      if (usingMockData.value) {
        const idx = incidencias.value.findIndex((i) => i.id === id)
        if (idx !== -1) {
          const current = incidencias.value[idx]
          const updated: Incidencia = {
            ...current,
            estado: request.estadoFinal,
            resolucion: request.resolucion ?? current.resolucion,
            fechaResolucion: new Date().toISOString(),
            adminId: 1,
          }
          incidencias.value[idx] = updated
          if (selectedIncidencia.value?.id === id) selectedIncidencia.value = updated
          return updated
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  async function fetchPorteOptions(): Promise<void> {
    try {
      const response = await api.get('/portes')
      const data = extractArray(response.data)
      porteOptions.value = data.map((p: Record<string, unknown>) => ({
        id: Number(p.id ?? 0),
        label: `#${p.id} — ${p.origen ?? '?'} → ${p.destino ?? '?'}`,
      }))
    } catch {
      // Mock fallback
      porteOptions.value = [
        { id: 1001, label: '#1001 — Madrid → Barcelona' },
        { id: 1002, label: '#1002 — Valencia → Sevilla' },
        { id: 1003, label: '#1003 — Bilbao → Zaragoza' },
        { id: 1004, label: '#1004 — Málaga → Granada' },
        { id: 1005, label: '#1005 — Alicante → Murcia' },
        { id: 1006, label: '#1006 — Valladolid → Salamanca' },
        { id: 1007, label: '#1007 — Córdoba → Jaén' },
        { id: 1008, label: '#1008 — Santander → Oviedo' },
      ]
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

  function mapIncidenciaFromApi(raw: unknown): Incidencia {
    const p = raw as Record<string, unknown>
    return {
      id: Number(p.id ?? 0),
      porteId: Number(p.porteId ?? 0),
      titulo: String(p.titulo ?? ''),
      descripcion: String(p.descripcion ?? ''),
      estado: (String(p.estado ?? 'ABIERTA')) as EstadoIncidencia,
      severidad: (String(p.severidad ?? 'MEDIA')) as SeveridadIncidencia,
      prioridad: (String(p.prioridad ?? 'MEDIA')) as PrioridadIncidencia,
      fechaReporte: p.fechaReporte ? String(p.fechaReporte) : new Date().toISOString(),
      fechaLimiteSla: p.fechaLimiteSla ? String(p.fechaLimiteSla) : null,
      resolucion: p.resolucion ? String(p.resolucion) : null,
      fechaResolucion: p.fechaResolucion ? String(p.fechaResolucion) : null,
      adminId: p.adminId != null ? Number(p.adminId) : null,
    }
  }

  function mapEventoFromApi(raw: unknown): IncidenciaEvento {
    const e = raw as Record<string, unknown>
    return {
      id: Number(e.id ?? 0),
      incidenciaId: Number(e.incidenciaId ?? 0),
      actorId: e.actorId != null ? Number(e.actorId) : null,
      estadoAnterior: e.estadoAnterior ? (String(e.estadoAnterior) as EstadoIncidencia) : null,
      estadoNuevo: (String(e.estadoNuevo ?? 'ABIERTA')) as EstadoIncidencia,
      fecha: e.fecha ? String(e.fecha) : new Date().toISOString(),
      accion: String(e.accion ?? ''),
      comentario: e.comentario ? String(e.comentario) : null,
    }
  }

  // Return ALL state, getters, and actions
  return {
    // State
    incidencias,
    selectedIncidencia,
    historial,
    porteOptions,
    loading,
    saving,
    loadingHistorial,
    usingMockData,
    // Getters
    totalIncidencias,
    incidenciasByEstado,
    pendientes,
    vencidas,
    // Actions
    fetchIncidencias,
    fetchVencidas,
    fetchIncidenciaById,
    fetchHistorial,
    crearIncidencia,
    resolverIncidencia,
    fetchPorteOptions,
  }
})
