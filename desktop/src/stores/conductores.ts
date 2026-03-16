import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/services/api'

// --- TypeScript Interfaces ---

export type EstadoConductor = 'ACTIVO' | 'INACTIVO' | 'SUSPENDIDO'

export interface Conductor {
  id: number
  nombre: string
  apellidos: string
  email: string
  telefono: string
  dni: string
  ciudadBase: string
  disponible: boolean
  estado: EstadoConductor
  rating: number
  numeroValoraciones: number
  latitudBase: number | null
  longitudBase: number | null
  radioAccionKm: number
  diasLaborables: string
  portesAsignados: number
}

export interface CreateConductorRequest {
  nombre: string
  apellidos: string
  email: string
  telefono?: string
  dni?: string
  password?: string
  ciudadBase?: string
}

export interface UpdateConductorRequest {
  nombre?: string
  apellidos?: string
  telefono?: string
  ciudadBase?: string
  diasLaborables?: string
  radioAccionKm?: number
}

// --- Mock Data ---

const MOCK_CONDUCTORES: Conductor[] = [
  {
    id: 1, nombre: 'Juan', apellidos: 'Pérez García', email: 'juan.perez@cargohub.es',
    telefono: '612345678', dni: '12345678A', ciudadBase: 'Madrid', disponible: true,
    estado: 'ACTIVO', rating: 4.5, numeroValoraciones: 28, latitudBase: 40.416, longitudBase: -3.703,
    radioAccionKm: 300, diasLaborables: '1,2,3,4,5', portesAsignados: 12,
  },
  {
    id: 2, nombre: 'María', apellidos: 'López Fernández', email: 'maria.lopez@cargohub.es',
    telefono: '623456789', dni: '23456789B', ciudadBase: 'Barcelona', disponible: true,
    estado: 'ACTIVO', rating: 4.8, numeroValoraciones: 35, latitudBase: 41.389, longitudBase: 2.159,
    radioAccionKm: 250, diasLaborables: '1,2,3,4,5', portesAsignados: 15,
  },
  {
    id: 3, nombre: 'Carlos', apellidos: 'Ruiz Martínez', email: 'carlos.ruiz@cargohub.es',
    telefono: '634567890', dni: '34567890C', ciudadBase: 'Valencia', disponible: true,
    estado: 'ACTIVO', rating: 4.2, numeroValoraciones: 19, latitudBase: 39.469, longitudBase: -0.376,
    radioAccionKm: 200, diasLaborables: '1,2,3,4,5,6', portesAsignados: 8,
  },
  {
    id: 4, nombre: 'Ana', apellidos: 'García Sánchez', email: 'ana.garcia@cargohub.es',
    telefono: '645678901', dni: '45678901D', ciudadBase: 'Sevilla', disponible: false,
    estado: 'SUSPENDIDO', rating: 3.9, numeroValoraciones: 14, latitudBase: 37.389, longitudBase: -5.984,
    radioAccionKm: 150, diasLaborables: '1,2,3,4,5', portesAsignados: 5,
  },
  {
    id: 5, nombre: 'Pedro', apellidos: 'Martín Díaz', email: 'pedro.martin@cargohub.es',
    telefono: '656789012', dni: '56789012E', ciudadBase: 'Bilbao', disponible: false,
    estado: 'INACTIVO', rating: 4.0, numeroValoraciones: 10, latitudBase: 43.263, longitudBase: -2.935,
    radioAccionKm: 200, diasLaborables: '1,2,3,4,5', portesAsignados: 0,
  },
  {
    id: 6, nombre: 'Laura', apellidos: 'Hernández Romero', email: 'laura.hernandez@cargohub.es',
    telefono: '667890123', dni: '67890123F', ciudadBase: 'Zaragoza', disponible: true,
    estado: 'ACTIVO', rating: 4.6, numeroValoraciones: 22, latitudBase: 41.649, longitudBase: -0.887,
    radioAccionKm: 350, diasLaborables: '1,2,3,4,5', portesAsignados: 10,
  },
  {
    id: 7, nombre: 'Diego', apellidos: 'Navarro Torres', email: 'diego.navarro@cargohub.es',
    telefono: '678901234', dni: '78901234G', ciudadBase: 'Málaga', disponible: true,
    estado: 'ACTIVO', rating: 4.3, numeroValoraciones: 17, latitudBase: 36.721, longitudBase: -4.421,
    radioAccionKm: 180, diasLaborables: '1,2,3,4,5,6', portesAsignados: 7,
  },
  {
    id: 8, nombre: 'Sofía', apellidos: 'Jiménez Moreno', email: 'sofia.jimenez@cargohub.es',
    telefono: '689012345', dni: '89012345H', ciudadBase: 'Alicante', disponible: true,
    estado: 'ACTIVO', rating: 4.7, numeroValoraciones: 31, latitudBase: 38.345, longitudBase: -0.481,
    radioAccionKm: 220, diasLaborables: '1,2,3,4,5', portesAsignados: 14,
  },
  {
    id: 9, nombre: 'Miguel', apellidos: 'Álvarez Gil', email: 'miguel.alvarez@cargohub.es',
    telefono: '690123456', dni: '90123456I', ciudadBase: 'Valladolid', disponible: false,
    estado: 'INACTIVO', rating: 3.8, numeroValoraciones: 8, latitudBase: 41.652, longitudBase: -4.724,
    radioAccionKm: 150, diasLaborables: '1,2,3,4,5', portesAsignados: 0,
  },
  {
    id: 10, nombre: 'Elena', apellidos: 'Morales Vega', email: 'elena.morales@cargohub.es',
    telefono: '601234567', dni: '01234567J', ciudadBase: 'Murcia', disponible: true,
    estado: 'ACTIVO', rating: 4.4, numeroValoraciones: 25, latitudBase: 37.984, longitudBase: -1.128,
    radioAccionKm: 200, diasLaborables: '1,2,3,4,5', portesAsignados: 9,
  },
  {
    id: 11, nombre: 'Raúl', apellidos: 'Castillo Prieto', email: 'raul.castillo@cargohub.es',
    telefono: '612098765', dni: '11234567K', ciudadBase: 'Santander', disponible: true,
    estado: 'ACTIVO', rating: 4.1, numeroValoraciones: 13, latitudBase: 43.462, longitudBase: -3.810,
    radioAccionKm: 280, diasLaborables: '1,2,3,4,5,6', portesAsignados: 6,
  },
  {
    id: 12, nombre: 'Lucía', apellidos: 'Domínguez Ortiz', email: 'lucia.dominguez@cargohub.es',
    telefono: '623098765', dni: '12234567L', ciudadBase: 'Córdoba', disponible: false,
    estado: 'SUSPENDIDO', rating: 3.5, numeroValoraciones: 6, latitudBase: 37.884, longitudBase: -4.779,
    radioAccionKm: 120, diasLaborables: '1,2,3,4,5', portesAsignados: 2,
  },
]

// --- Store ---

export const useConductoresStore = defineStore('conductores', () => {
  // --- State ---
  const conductores = ref<Conductor[]>([])
  const selectedConductor = ref<Conductor | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const usingMockData = ref(false)

  // --- Getters ---
  const totalConductores = computed(() => conductores.value.length)

  const conductoresByEstado = computed(() => {
    const counts: Record<string, number> = {}
    for (const c of conductores.value) {
      counts[c.estado] = (counts[c.estado] || 0) + 1
    }
    return counts
  })

  const activos = computed(() =>
    conductores.value.filter((c) => c.estado === 'ACTIVO')
  )

  const disponibles = computed(() =>
    conductores.value.filter((c) => c.disponible && c.estado === 'ACTIVO')
  )

  // --- Actions ---

  /**
   * Fetch all conductores from the API. Falls back to mock data on error.
   */
  async function fetchConductores(): Promise<void> {
    loading.value = true
    usingMockData.value = false

    try {
      const response = await api.get('/conductores')
      const data = extractArray(response.data)
      conductores.value = data.map(mapConductorFromApi)
    } catch {
      // API unavailable — use mock data
      usingMockData.value = true
      conductores.value = [...MOCK_CONDUCTORES]
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch a single conductor by ID.
   */
  async function fetchConductorById(id: number): Promise<Conductor | null> {
    loading.value = true
    try {
      const response = await api.get(`/conductores/${id}`)
      const conductor = mapConductorFromApi(response.data)
      selectedConductor.value = conductor
      return conductor
    } catch {
      // Try from local list
      const found = conductores.value.find((c) => c.id === id)
      if (found) {
        selectedConductor.value = found
        return found
      }
      // Try mock
      const mock = MOCK_CONDUCTORES.find((c) => c.id === id)
      if (mock) {
        selectedConductor.value = mock
        return mock
      }
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Create a new conductor.
   */
  async function createConductor(request: CreateConductorRequest): Promise<Conductor> {
    saving.value = true
    try {
      const response = await api.post('/conductores', request)
      const newConductor = mapConductorFromApi(response.data)
      conductores.value.unshift(newConductor)
      return newConductor
    } catch (error) {
      // If API is down, create a mock conductor for UX continuity
      if (usingMockData.value) {
        const mockConductor: Conductor = {
          id: Math.max(0, ...conductores.value.map((c) => c.id)) + 1,
          nombre: request.nombre,
          apellidos: request.apellidos,
          email: request.email,
          telefono: request.telefono ?? '',
          dni: request.dni ?? '',
          ciudadBase: request.ciudadBase ?? '',
          disponible: true,
          estado: 'ACTIVO',
          rating: 4.0,
          numeroValoraciones: 0,
          latitudBase: null,
          longitudBase: null,
          radioAccionKm: 0,
          diasLaborables: '1,2,3,4,5',
          portesAsignados: 0,
        }
        conductores.value.unshift(mockConductor)
        return mockConductor
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Update an existing conductor.
   */
  async function updateConductor(id: number, request: UpdateConductorRequest): Promise<Conductor> {
    saving.value = true
    try {
      const response = await api.put(`/conductores/${id}`, request)
      const updated = mapConductorFromApi(response.data)
      const idx = conductores.value.findIndex((c) => c.id === id)
      if (idx !== -1) conductores.value[idx] = updated
      if (selectedConductor.value?.id === id) selectedConductor.value = updated
      return updated
    } catch (error) {
      // If using mock data, update locally
      if (usingMockData.value) {
        const idx = conductores.value.findIndex((c) => c.id === id)
        if (idx !== -1) {
          const current = conductores.value[idx]
          const updated: Conductor = {
            ...current,
            nombre: request.nombre ?? current.nombre,
            apellidos: request.apellidos ?? current.apellidos,
            telefono: request.telefono ?? current.telefono,
            ciudadBase: request.ciudadBase ?? current.ciudadBase,
            diasLaborables: request.diasLaborables ?? current.diasLaborables,
            radioAccionKm: request.radioAccionKm ?? current.radioAccionKm,
          }
          conductores.value[idx] = updated
          if (selectedConductor.value?.id === id) selectedConductor.value = updated
          return updated
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Toggle conductor active/inactive status (soft-delete style via API darDeBaja).
   */
  async function toggleEstado(id: number): Promise<Conductor> {
    saving.value = true
    try {
      const conductor = conductores.value.find((c) => c.id === id)
      if (!conductor) throw new Error('Conductor no encontrado')

      if (conductor.estado === 'ACTIVO') {
        // Dar de baja
        await api.delete(`/conductores/${id}`)
        const idx = conductores.value.findIndex((c) => c.id === id)
        if (idx !== -1) {
          conductores.value[idx] = { ...conductores.value[idx], estado: 'INACTIVO', disponible: false }
          return conductores.value[idx]
        }
      } else {
        // Reactivar — update disponible
        const response = await api.put(`/conductores/${id}`, { disponible: true })
        const updated = mapConductorFromApi(response.data)
        const idx = conductores.value.findIndex((c) => c.id === id)
        if (idx !== -1) {
          conductores.value[idx] = { ...updated, estado: 'ACTIVO' }
          return conductores.value[idx]
        }
      }
      throw new Error('No se pudo actualizar el estado')
    } catch (error) {
      if (usingMockData.value) {
        const idx = conductores.value.findIndex((c) => c.id === id)
        if (idx !== -1) {
          const current = conductores.value[idx]
          const newEstado: EstadoConductor = current.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
          conductores.value[idx] = {
            ...current,
            estado: newEstado,
            disponible: newEstado === 'ACTIVO',
          }
          return conductores.value[idx]
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Delete a conductor (ADMIN only).
   */
  async function deleteConductor(id: number): Promise<void> {
    saving.value = true
    try {
      await api.delete(`/conductores/${id}`)
      conductores.value = conductores.value.filter((c) => c.id !== id)
      if (selectedConductor.value?.id === id) selectedConductor.value = null
    } catch (error) {
      if (usingMockData.value) {
        conductores.value = conductores.value.filter((c) => c.id !== id)
        if (selectedConductor.value?.id === id) selectedConductor.value = null
        return
      }
      throw error
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

  function mapConductorFromApi(raw: unknown): Conductor {
    const c = raw as Record<string, unknown>

    // Determine estado from disponible + usuario.activo
    let estado: EstadoConductor = 'ACTIVO'
    const usuario = c.usuario as Record<string, unknown> | null | undefined
    if (usuario && usuario.activo === false) {
      estado = 'INACTIVO'
    } else if (c.disponible === false) {
      estado = 'SUSPENDIDO'
    }

    return {
      id: Number(c.id ?? 0),
      nombre: String(c.nombre ?? ''),
      apellidos: String(c.apellidos ?? ''),
      email: usuario ? String(usuario.email ?? '') : '',
      telefono: String(c.telefono ?? ''),
      dni: String(c.dni ?? ''),
      ciudadBase: String(c.ciudadBase ?? ''),
      disponible: c.disponible !== false,
      estado,
      rating: c.rating != null ? Number(c.rating) : 4.0,
      numeroValoraciones: c.numeroValoraciones != null ? Number(c.numeroValoraciones) : 0,
      latitudBase: c.latitudBase != null ? Number(c.latitudBase) : null,
      longitudBase: c.longitudBase != null ? Number(c.longitudBase) : null,
      radioAccionKm: c.radioAccionKm != null ? Number(c.radioAccionKm) : 0,
      diasLaborables: String(c.diasLaborables ?? '1,2,3,4,5'),
      portesAsignados: c.portesAsignados != null ? Number(c.portesAsignados) : 0,
    }
  }

  // Return ALL state, getters, and actions
  return {
    // State
    conductores,
    selectedConductor,
    loading,
    saving,
    usingMockData,
    // Getters
    totalConductores,
    conductoresByEstado,
    activos,
    disponibles,
    // Actions
    fetchConductores,
    fetchConductorById,
    createConductor,
    updateConductor,
    toggleEstado,
    deleteConductor,
  }
})
