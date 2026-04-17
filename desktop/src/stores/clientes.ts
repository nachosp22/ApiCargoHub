import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/services/api'

// --- TypeScript Interfaces ---

export interface Cliente {
  id: number
  nombreEmpresa: string
  cif: string
  emailContacto: string
  telefono: string
  direccion: string
  ciudad: string
  codigoPostal: string
  pais: string
}

export interface CreateClienteRequest {
  nombreEmpresa: string
  cif: string
  emailContacto: string
  telefono?: string
  direccion?: string
  ciudad?: string
  codigoPostal?: string
  pais?: string
}

export interface UpdateClienteRequest {
  nombreEmpresa?: string
  cif?: string
  emailContacto?: string
  telefono?: string
  direccion?: string
  ciudad?: string
  codigoPostal?: string
  pais?: string
}

export interface ClientePorte {
  id: number
  origen: string
  destino: string
  estado: string
  fechaRecogida?: string
  precio?: number
}

// --- Mock Data ---

const MOCK_CLIENTES: Cliente[] = [
  {
    id: 1, nombreEmpresa: 'Logistics Express S.L.', cif: 'B12345678',
    emailContacto: 'info@logisticsexpress.es', telefono: '911234567',
    direccion: 'Calle Gran Vía 28', ciudad: 'Madrid', codigoPostal: '28013', pais: 'España',
  },
  {
    id: 2, nombreEmpresa: 'Trans Ibérica S.A.', cif: 'A87654321',
    emailContacto: 'contacto@transiberica.es', telefono: '932345678',
    direccion: 'Avda. Diagonal 450', ciudad: 'Barcelona', codigoPostal: '08006', pais: 'España',
  },
  {
    id: 3, nombreEmpresa: 'Envíos Rápidos SL', cif: 'B11223344',
    emailContacto: 'admin@enviosrapidos.es', telefono: '961234567',
    direccion: 'Calle Colón 12', ciudad: 'Valencia', codigoPostal: '46004', pais: 'España',
  },
  {
    id: 4, nombreEmpresa: 'Cargo Sur S.A.', cif: 'A55667788',
    emailContacto: 'ops@cargosur.es', telefono: '954112233',
    direccion: 'Avda. de la Constitución 5', ciudad: 'Sevilla', codigoPostal: '41001', pais: 'España',
  },
  {
    id: 5, nombreEmpresa: 'Norte Transportes SL', cif: 'B99887766',
    emailContacto: 'info@nortetransportes.es', telefono: '944556677',
    direccion: 'Gran Vía 55', ciudad: 'Bilbao', codigoPostal: '48011', pais: 'España',
  },
]

const MOCK_PORTES: Record<number, ClientePorte[]> = {
  1: [
    { id: 1001, origen: 'Madrid', destino: 'Barcelona', estado: 'EN_TRANSITO', fechaRecogida: '2026-03-15T10:00:00', precio: 850 },
    { id: 1004, origen: 'Málaga', destino: 'Granada', estado: 'ASIGNADO', fechaRecogida: '2026-03-16T09:00:00', precio: 280 },
    { id: 1010, origen: 'Toledo', destino: 'Ciudad Real', estado: 'ASIGNADO', fechaRecogida: '2026-03-17T09:00:00', precio: 300 },
  ],
  2: [
    { id: 1002, origen: 'Valencia', destino: 'Sevilla', estado: 'ENTREGADO', fechaRecogida: '2026-03-12T07:00:00', precio: 720 },
    { id: 1008, origen: 'Santander', destino: 'Oviedo', estado: 'ENTREGADO', fechaRecogida: '2026-03-09T08:00:00', precio: 350 },
  ],
  3: [
    { id: 1003, origen: 'Bilbao', destino: 'Zaragoza', estado: 'PENDIENTE', fechaRecogida: '2026-03-18T06:00:00', precio: 480 },
    { id: 1009, origen: 'Pamplona', destino: 'San Sebastián', estado: 'PENDIENTE', fechaRecogida: '2026-03-19T07:00:00', precio: 240 },
  ],
  4: [],
  5: [],
}

// --- Store ---

export const useClientesStore = defineStore('clientes', () => {
  // --- State ---
  const clientes = ref<Cliente[]>([])
  const selectedCliente = ref<Cliente | null>(null)
  const clientePortes = ref<ClientePorte[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const loadingPortes = ref(false)
  const usingMockData = ref(false)

  // --- Getters ---
  const totalClientes = computed(() => clientes.value.length)

  // --- Actions ---

  /**
   * Fetch all clientes from the API. Falls back to mock data on error.
   */
  async function fetchClientes(): Promise<void> {
    loading.value = true
    usingMockData.value = false

    try {
      const response = await api.get('/clientes')
      const data = extractArray(response.data)
      clientes.value = data.map(mapClienteFromApi)
    } catch {
      // API unavailable — use mock data
      usingMockData.value = true
      clientes.value = [...MOCK_CLIENTES]
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch a single cliente by ID.
   */
  async function fetchCliente(id: number): Promise<Cliente | null> {
    loading.value = true
    try {
      const response = await api.get(`/clientes/${id}`)
      const cliente = mapClienteFromApi(response.data)
      selectedCliente.value = cliente
      return cliente
    } catch {
      // Try from local list
      const found = clientes.value.find((c) => c.id === id)
      if (found) {
        selectedCliente.value = found
        return found
      }
      // Try mock
      const mock = MOCK_CLIENTES.find((c) => c.id === id)
      if (mock) {
        selectedCliente.value = mock
        return mock
      }
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * Create a new cliente.
   */
  async function createCliente(request: CreateClienteRequest): Promise<Cliente> {
    saving.value = true
    try {
      const response = await api.post('/clientes', request)
      const newCliente = mapClienteFromApi(response.data)
      clientes.value.unshift(newCliente)
      return newCliente
    } catch (error) {
      if (usingMockData.value) {
        const mockCliente: Cliente = {
          id: Math.max(0, ...clientes.value.map((c) => c.id)) + 1,
          nombreEmpresa: request.nombreEmpresa,
          cif: request.cif,
          emailContacto: request.emailContacto,
          telefono: request.telefono ?? '',
          direccion: request.direccion ?? '',
          ciudad: request.ciudad ?? '',
          codigoPostal: request.codigoPostal ?? '',
          pais: request.pais ?? 'España',
        }
        clientes.value.unshift(mockCliente)
        return mockCliente
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Update an existing cliente.
   */
  async function updateCliente(id: number, request: UpdateClienteRequest): Promise<Cliente> {
    saving.value = true
    try {
      const response = await api.put(`/clientes/${id}`, request)
      const updated = mapClienteFromApi(response.data)
      const idx = clientes.value.findIndex((c) => c.id === id)
      if (idx !== -1) clientes.value[idx] = updated
      if (selectedCliente.value?.id === id) selectedCliente.value = updated
      return updated
    } catch (error) {
      if (usingMockData.value) {
        const idx = clientes.value.findIndex((c) => c.id === id)
        if (idx !== -1) {
          const current = clientes.value[idx]
          const updated: Cliente = {
            ...current,
            nombreEmpresa: request.nombreEmpresa ?? current.nombreEmpresa,
            cif: request.cif ?? current.cif,
            emailContacto: request.emailContacto ?? current.emailContacto,
            telefono: request.telefono ?? current.telefono,
            direccion: request.direccion ?? current.direccion,
            ciudad: request.ciudad ?? current.ciudad,
            codigoPostal: request.codigoPostal ?? current.codigoPostal,
            pais: request.pais ?? current.pais,
          }
          clientes.value[idx] = updated
          if (selectedCliente.value?.id === id) selectedCliente.value = updated
          return updated
        }
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Delete a cliente.
   */
  async function deleteCliente(id: number): Promise<void> {
    saving.value = true
    try {
      await api.delete(`/clientes/${id}`)
      clientes.value = clientes.value.filter((c) => c.id !== id)
      if (selectedCliente.value?.id === id) selectedCliente.value = null
    } catch (error) {
      if (usingMockData.value) {
        clientes.value = clientes.value.filter((c) => c.id !== id)
        if (selectedCliente.value?.id === id) selectedCliente.value = null
        return
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * Fetch portes for a specific cliente.
   */
  async function fetchClientePortes(clienteId: number): Promise<void> {
    loadingPortes.value = true
    try {
      const response = await api.get(`/clientes/${clienteId}/portes`)
      const data = extractArray(response.data)
      clientePortes.value = data.map(mapClientePorteFromApi)
    } catch {
      // Fallback to mock
      clientePortes.value = MOCK_PORTES[clienteId] ?? []
    } finally {
      loadingPortes.value = false
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

  function mapClienteFromApi(raw: unknown): Cliente {
    const c = raw as Record<string, unknown>
    return {
      id: Number(c.id ?? 0),
      nombreEmpresa: String(c.nombreEmpresa ?? c.nombre ?? ''),
      cif: String(c.cif ?? ''),
      emailContacto: String(c.emailContacto ?? c.email ?? ''),
      telefono: String(c.telefono ?? ''),
      direccion: String(c.direccion ?? ''),
      ciudad: String(c.ciudad ?? ''),
      codigoPostal: String(c.codigoPostal ?? ''),
      pais: String(c.pais ?? ''),
    }
  }

  function mapClientePorteFromApi(raw: unknown): ClientePorte {
    const p = raw as Record<string, unknown>
    return {
      id: Number(p.id ?? 0),
      origen: String(p.origen ?? '—'),
      destino: String(p.destino ?? '—'),
      estado: String(p.estado ?? 'PENDIENTE'),
      fechaRecogida: p.fechaRecogida ? String(p.fechaRecogida) : undefined,
      precio: p.precio != null ? Number(p.precio) : undefined,
    }
  }

  // Return ALL state, getters, and actions
  return {
    // State
    clientes,
    selectedCliente,
    clientePortes,
    loading,
    saving,
    loadingPortes,
    usingMockData,
    // Getters
    totalClientes,
    // Actions
    fetchClientes,
    fetchCliente,
    createCliente,
    updateCliente,
    deleteCliente,
    fetchClientePortes,
  }
})
