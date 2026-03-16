import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/services/api'

// --- TypeScript Interfaces ---

export type EstadoVehiculo = 'DISPONIBLE' | 'EN_MANTENIMIENTO' | 'BAJA'
export type TipoVehiculo = 'FURGONETA' | 'RIGIDO' | 'TRAILER' | 'ESPECIAL'

export interface ConductorResumen {
  id: number
  nombre: string
  apellidos: string
}

export interface Vehiculo {
  id: number
  matricula: string
  marca: string
  modelo: string
  tipo: TipoVehiculo
  estado: EstadoVehiculo
  capacidadCargaKg: number | null
  largoUtilMm: number | null
  anchoUtilMm: number | null
  altoUtilMm: number | null
  volumenM3: number | null
  trampillaElevadora: boolean
  conductor: ConductorResumen | null
}

export interface CreateVehiculoRequest {
  matricula: string
  marca: string
  modelo: string
  tipo: TipoVehiculo
  capacidadCargaKg?: number | null
  largoUtilMm?: number | null
  anchoUtilMm?: number | null
  altoUtilMm?: number | null
  trampillaElevadora?: boolean
  conductor?: { id: number } | null
}

export interface UpdateVehiculoRequest {
  matricula?: string
  marca?: string
  modelo?: string
  tipo?: TipoVehiculo
  capacidadCargaKg?: number | null
  largoUtilMm?: number | null
  anchoUtilMm?: number | null
  altoUtilMm?: number | null
  trampillaElevadora?: boolean
  conductor?: { id: number } | null
}

// --- Mock Data ---

const MOCK_VEHICULOS: Vehiculo[] = [
  {
    id: 1, matricula: '1234ABC', marca: 'Iveco', modelo: 'Daily 35S14',
    tipo: 'FURGONETA', estado: 'DISPONIBLE', capacidadCargaKg: 1500,
    largoUtilMm: 4200, anchoUtilMm: 1800, altoUtilMm: 1900, volumenM3: 14.36,
    trampillaElevadora: false,
    conductor: { id: 1, nombre: 'Juan', apellidos: 'Pérez García' },
  },
  {
    id: 2, matricula: '5678DEF', marca: 'Mercedes-Benz', modelo: 'Atego 1224',
    tipo: 'RIGIDO', estado: 'DISPONIBLE', capacidadCargaKg: 6000,
    largoUtilMm: 7200, anchoUtilMm: 2400, altoUtilMm: 2500, volumenM3: 43.2,
    trampillaElevadora: true,
    conductor: { id: 2, nombre: 'María', apellidos: 'López Fernández' },
  },
  {
    id: 3, matricula: '9012GHI', marca: 'Volvo', modelo: 'FH 500',
    tipo: 'TRAILER', estado: 'DISPONIBLE', capacidadCargaKg: 24000,
    largoUtilMm: 13600, anchoUtilMm: 2480, altoUtilMm: 2700, volumenM3: 91.07,
    trampillaElevadora: true,
    conductor: { id: 3, nombre: 'Carlos', apellidos: 'Ruiz Martínez' },
  },
  {
    id: 4, matricula: '3456JKL', marca: 'Renault', modelo: 'Master L3H2',
    tipo: 'FURGONETA', estado: 'EN_MANTENIMIENTO', capacidadCargaKg: 1200,
    largoUtilMm: 3700, anchoUtilMm: 1765, altoUtilMm: 1880, volumenM3: 12.28,
    trampillaElevadora: false,
    conductor: { id: 6, nombre: 'Laura', apellidos: 'Hernández Romero' },
  },
  {
    id: 5, matricula: '7890MNO', marca: 'MAN', modelo: 'TGX 18.510',
    tipo: 'TRAILER', estado: 'DISPONIBLE', capacidadCargaKg: 25000,
    largoUtilMm: 13600, anchoUtilMm: 2480, altoUtilMm: 2700, volumenM3: 91.07,
    trampillaElevadora: true,
    conductor: { id: 7, nombre: 'Diego', apellidos: 'Navarro Torres' },
  },
  {
    id: 6, matricula: '2345PQR', marca: 'DAF', modelo: 'XF 480',
    tipo: 'TRAILER', estado: 'BAJA', capacidadCargaKg: 24000,
    largoUtilMm: 13600, anchoUtilMm: 2480, altoUtilMm: 2700, volumenM3: 91.07,
    trampillaElevadora: false,
    conductor: null,
  },
  {
    id: 7, matricula: '6789STU', marca: 'Iveco', modelo: 'Eurocargo 120E25',
    tipo: 'RIGIDO', estado: 'DISPONIBLE', capacidadCargaKg: 7500,
    largoUtilMm: 8000, anchoUtilMm: 2400, altoUtilMm: 2500, volumenM3: 48.0,
    trampillaElevadora: true,
    conductor: { id: 8, nombre: 'Sofía', apellidos: 'Jiménez Moreno' },
  },
  {
    id: 8, matricula: '0123VWX', marca: 'Peugeot', modelo: 'Boxer L4H3',
    tipo: 'FURGONETA', estado: 'DISPONIBLE', capacidadCargaKg: 1400,
    largoUtilMm: 4070, anchoUtilMm: 1870, altoUtilMm: 2172, volumenM3: 16.53,
    trampillaElevadora: false,
    conductor: { id: 10, nombre: 'Elena', apellidos: 'Morales Vega' },
  },
  {
    id: 9, matricula: '4567YZA', marca: 'Scania', modelo: 'R 450',
    tipo: 'ESPECIAL', estado: 'EN_MANTENIMIENTO', capacidadCargaKg: 20000,
    largoUtilMm: 12000, anchoUtilMm: 2500, altoUtilMm: 2800, volumenM3: 84.0,
    trampillaElevadora: true,
    conductor: { id: 11, nombre: 'Raúl', apellidos: 'Castillo Prieto' },
  },
  {
    id: 10, matricula: '8901BCD', marca: 'Ford', modelo: 'Transit L3H2',
    tipo: 'FURGONETA', estado: 'DISPONIBLE', capacidadCargaKg: 1100,
    largoUtilMm: 3494, anchoUtilMm: 1784, altoUtilMm: 1886, volumenM3: 11.76,
    trampillaElevadora: false,
    conductor: null,
  },
]

// --- Store ---

export const useVehiculosStore = defineStore('vehiculos', () => {
  // --- State ---
  const vehiculos = ref<Vehiculo[]>([])
  const selectedVehiculo = ref<Vehiculo | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const usingMockData = ref(false)

  // --- Getters ---
  const totalVehiculos = computed(() => vehiculos.value.length)

  const vehiculosByEstado = computed(() => {
    const counts: Record<string, number> = {}
    for (const v of vehiculos.value) {
      counts[v.estado] = (counts[v.estado] || 0) + 1
    }
    return counts
  })

  const vehiculosByTipo = computed(() => {
    const counts: Record<string, number> = {}
    for (const v of vehiculos.value) {
      counts[v.tipo] = (counts[v.tipo] || 0) + 1
    }
    return counts
  })

  const disponibles = computed(() =>
    vehiculos.value.filter((v) => v.estado === 'DISPONIBLE')
  )

  const enMantenimiento = computed(() =>
    vehiculos.value.filter((v) => v.estado === 'EN_MANTENIMIENTO')
  )

  const enBaja = computed(() =>
    vehiculos.value.filter((v) => v.estado === 'BAJA')
  )

  // --- Actions ---

  /**
   * Fetch all vehiculos from the API. Falls back to mock data on error.
   */
  async function fetchVehiculos(): Promise<void> {
    loading.value = true
    usingMockData.value = false

    try {
      const response = await api.get('/vehiculos')
      const data = extractArray(response.data)
      vehiculos.value = data.map(mapVehiculoFromApi)
    } catch {
      // API unavailable — use mock data
      usingMockData.value = true
      vehiculos.value = [...MOCK_VEHICULOS]
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch a single vehiculo by ID.
   */
  async function fetchVehiculoById(id: number): Promise<Vehiculo | null> {
    loading.value = true
    try {
      const response = await api.get(`/vehiculos/${id}`)
      const vehiculo = mapVehiculoFromApi(response.data)
      selectedVehiculo.value = vehiculo
      return vehiculo
    } catch {
      // Try from local list
      const found = vehiculos.value.find((v) => v.id === id)
      if (found) {
        selectedVehiculo.value = found
        return found
      }
      // Try mock
      const mock = MOCK_VEHICULOS.find((v) => v.id === id)
      if (mock) {
        selectedVehiculo.value = mock
        return mock
      }
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Create a new vehiculo.
   */
  async function createVehiculo(request: CreateVehiculoRequest): Promise<Vehiculo> {
    saving.value = true
    try {
      const response = await api.post('/vehiculos', request)
      const newVehiculo = mapVehiculoFromApi(response.data)
      vehiculos.value.unshift(newVehiculo)
      return newVehiculo
    } catch (error) {
      // If API is down, create a mock vehiculo for UX continuity
      if (usingMockData.value) {
        const mockVehiculo: Vehiculo = {
          id: Math.max(0, ...vehiculos.value.map((v) => v.id)) + 1,
          matricula: request.matricula.toUpperCase(),
          marca: request.marca,
          modelo: request.modelo,
          tipo: request.tipo,
          estado: 'DISPONIBLE',
          capacidadCargaKg: request.capacidadCargaKg ?? null,
          largoUtilMm: request.largoUtilMm ?? null,
          anchoUtilMm: request.anchoUtilMm ?? null,
          altoUtilMm: request.altoUtilMm ?? null,
          volumenM3: null,
          trampillaElevadora: request.trampillaElevadora ?? false,
          conductor: null,
        }
        vehiculos.value.unshift(mockVehiculo)
        return mockVehiculo
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Update an existing vehiculo (uses POST since API only has POST for save).
   */
  async function updateVehiculo(id: number, request: UpdateVehiculoRequest): Promise<Vehiculo> {
    saving.value = true
    try {
      // API uses POST /vehiculos for both create and update (entity has id)
      const payload = { id, ...request }
      const response = await api.post('/vehiculos', payload)
      const updated = mapVehiculoFromApi(response.data)
      const idx = vehiculos.value.findIndex((v) => v.id === id)
      if (idx !== -1) vehiculos.value[idx] = updated
      if (selectedVehiculo.value?.id === id) selectedVehiculo.value = updated
      return updated
    } catch (error) {
      // If using mock data, update locally
      if (usingMockData.value) {
        const idx = vehiculos.value.findIndex((v) => v.id === id)
        if (idx !== -1) {
          const current = vehiculos.value[idx]
          const updated: Vehiculo = {
            ...current,
            matricula: request.matricula ?? current.matricula,
            marca: request.marca ?? current.marca,
            modelo: request.modelo ?? current.modelo,
            tipo: request.tipo ?? current.tipo,
            capacidadCargaKg: request.capacidadCargaKg !== undefined ? request.capacidadCargaKg : current.capacidadCargaKg,
            largoUtilMm: request.largoUtilMm !== undefined ? request.largoUtilMm : current.largoUtilMm,
            anchoUtilMm: request.anchoUtilMm !== undefined ? request.anchoUtilMm : current.anchoUtilMm,
            altoUtilMm: request.altoUtilMm !== undefined ? request.altoUtilMm : current.altoUtilMm,
            trampillaElevadora: request.trampillaElevadora !== undefined ? request.trampillaElevadora : current.trampillaElevadora,
          }
          vehiculos.value[idx] = updated
          if (selectedVehiculo.value?.id === id) selectedVehiculo.value = updated
          return updated
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Toggle vehiculo active/baja status (soft-delete via API darDeBaja / reactivar).
   */
  async function toggleEstado(id: number): Promise<Vehiculo> {
    saving.value = true
    try {
      const vehiculo = vehiculos.value.find((v) => v.id === id)
      if (!vehiculo) throw new Error('Vehículo no encontrado')

      if (vehiculo.estado === 'BAJA') {
        // Reactivar
        await api.put(`/vehiculos/${id}/reactivar`)
        const idx = vehiculos.value.findIndex((v) => v.id === id)
        if (idx !== -1) {
          vehiculos.value[idx] = { ...vehiculos.value[idx], estado: 'DISPONIBLE' }
          return vehiculos.value[idx]
        }
      } else {
        // Dar de baja
        await api.delete(`/vehiculos/${id}`)
        const idx = vehiculos.value.findIndex((v) => v.id === id)
        if (idx !== -1) {
          vehiculos.value[idx] = { ...vehiculos.value[idx], estado: 'BAJA' }
          return vehiculos.value[idx]
        }
      }
      throw new Error('No se pudo actualizar el estado')
    } catch (error) {
      if (usingMockData.value) {
        const idx = vehiculos.value.findIndex((v) => v.id === id)
        if (idx !== -1) {
          const current = vehiculos.value[idx]
          const newEstado: EstadoVehiculo = current.estado === 'BAJA' ? 'DISPONIBLE' : 'BAJA'
          vehiculos.value[idx] = { ...current, estado: newEstado }
          return vehiculos.value[idx]
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Delete a vehiculo (ADMIN only — uses darDeBaja endpoint).
   */
  async function deleteVehiculo(id: number): Promise<void> {
    saving.value = true
    try {
      await api.delete(`/vehiculos/${id}`)
      // Mark as BAJA instead of removing from list (soft delete)
      const idx = vehiculos.value.findIndex((v) => v.id === id)
      if (idx !== -1) {
        vehiculos.value[idx] = { ...vehiculos.value[idx], estado: 'BAJA' }
      }
      if (selectedVehiculo.value?.id === id) {
        selectedVehiculo.value = vehiculos.value[idx] ?? null
      }
    } catch (error) {
      if (usingMockData.value) {
        const idx = vehiculos.value.findIndex((v) => v.id === id)
        if (idx !== -1) {
          vehiculos.value[idx] = { ...vehiculos.value[idx], estado: 'BAJA' }
        }
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

  function mapVehiculoFromApi(raw: unknown): Vehiculo {
    const v = raw as Record<string, unknown>

    // Map conductor (may be nested object or null)
    let conductor: ConductorResumen | null = null
    if (v.conductor && typeof v.conductor === 'object') {
      const c = v.conductor as Record<string, unknown>
      conductor = {
        id: Number(c.id ?? 0),
        nombre: String(c.nombre ?? ''),
        apellidos: String(c.apellidos ?? ''),
      }
    }

    return {
      id: Number(v.id ?? 0),
      matricula: String(v.matricula ?? ''),
      marca: String(v.marca ?? ''),
      modelo: String(v.modelo ?? ''),
      tipo: (String(v.tipo ?? 'FURGONETA') as TipoVehiculo),
      estado: (String(v.estado ?? 'DISPONIBLE') as EstadoVehiculo),
      capacidadCargaKg: v.capacidadCargaKg != null ? Number(v.capacidadCargaKg) : null,
      largoUtilMm: v.largoUtilMm != null ? Number(v.largoUtilMm) : null,
      anchoUtilMm: v.anchoUtilMm != null ? Number(v.anchoUtilMm) : null,
      altoUtilMm: v.altoUtilMm != null ? Number(v.altoUtilMm) : null,
      volumenM3: v.volumenM3 != null ? Number(v.volumenM3) : null,
      trampillaElevadora: v.trampillaElevadora === true,
      conductor,
    }
  }

  // Return ALL state, getters, and actions
  return {
    // State
    vehiculos,
    selectedVehiculo,
    loading,
    saving,
    usingMockData,
    // Getters
    totalVehiculos,
    vehiculosByEstado,
    vehiculosByTipo,
    disponibles,
    enMantenimiento,
    enBaja,
    // Actions
    fetchVehiculos,
    fetchVehiculoById,
    createVehiculo,
    updateVehiculo,
    toggleEstado,
    deleteVehiculo,
  }
})
