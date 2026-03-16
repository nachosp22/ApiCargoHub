import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/services/api'

// --- TypeScript Interfaces ---

export interface DashboardKpis {
  portesActivos: number
  conductores: number
  incidenciasAbiertas: number
  vehiculosDisponibles: number
}

export interface Porte {
  id: number
  origen: string
  destino: string
  conductor: string
  estado: string
  fecha: string
}

export interface KpiTrend {
  value: string
  positive: boolean
}

// --- Mock Data ---

const MOCK_KPIS: DashboardKpis = {
  portesActivos: 47,
  conductores: 23,
  incidenciasAbiertas: 8,
  vehiculosDisponibles: 15,
}

const MOCK_CHART_DATA: number[] = [42, 38, 55, 48, 62, 58, 71, 65, 73, 68, 52, 45]

const MOCK_PORTES: Porte[] = [
  { id: 1001, origen: 'Madrid', destino: 'Barcelona', conductor: 'Juan Pérez', estado: 'EN_RUTA', fecha: '2026-03-15' },
  { id: 1002, origen: 'Valencia', destino: 'Sevilla', conductor: 'María López', estado: 'COMPLETADO', fecha: '2026-03-14' },
  { id: 1003, origen: 'Bilbao', destino: 'Zaragoza', conductor: 'Carlos Ruiz', estado: 'PENDIENTE', fecha: '2026-03-16' },
  { id: 1004, origen: 'Málaga', destino: 'Granada', conductor: 'Ana García', estado: 'ENTREGADO', fecha: '2026-03-13' },
  { id: 1005, origen: 'Alicante', destino: 'Murcia', conductor: 'Pedro Martín', estado: 'PROGRAMADO', fecha: '2026-03-17' },
  { id: 1006, origen: 'Valladolid', destino: 'Salamanca', conductor: 'Laura Sánchez', estado: 'EN_RUTA', fecha: '2026-03-15' },
  { id: 1007, origen: 'Córdoba', destino: 'Jaén', conductor: 'Miguel Torres', estado: 'CANCELADO', fecha: '2026-03-12' },
  { id: 1008, origen: 'Santander', destino: 'Oviedo', conductor: 'Elena Díaz', estado: 'COMPLETADO', fecha: '2026-03-14' },
  { id: 1009, origen: 'Pamplona', destino: 'San Sebastián', conductor: 'Diego Navarro', estado: 'PENDIENTE', fecha: '2026-03-16' },
  { id: 1010, origen: 'Toledo', destino: 'Ciudad Real', conductor: 'Sofía Romero', estado: 'PROGRAMADO', fecha: '2026-03-18' },
  { id: 1011, origen: 'Cáceres', destino: 'Badajoz', conductor: 'Juan Pérez', estado: 'EN_RUTA', fecha: '2026-03-15' },
  { id: 1012, origen: 'Tarragona', destino: 'Lleida', conductor: 'María López', estado: 'ENTREGADO', fecha: '2026-03-13' },
]

const MOCK_TRENDS: Record<string, KpiTrend> = {
  portesActivos: { value: '+12%', positive: true },
  conductores: { value: '+2', positive: true },
  incidenciasAbiertas: { value: '-3', positive: true },
  vehiculosDisponibles: { value: '+5%', positive: true },
}

// --- Store ---

export const useDashboardStore = defineStore('dashboard', () => {
  // --- State ---
  const kpis = ref<DashboardKpis>(MOCK_KPIS)
  const chartData = ref<number[]>(MOCK_CHART_DATA)
  const recentPortes = ref<Porte[]>(MOCK_PORTES)
  const trends = ref<Record<string, KpiTrend>>(MOCK_TRENDS)
  const loading = ref(false)
  const usingMockData = ref(false)

  // --- Getters ---
  const totalPortes = computed(() => recentPortes.value.length)

  // --- Actions ---

  /**
   * Fetch all dashboard data from API endpoints.
   * Falls back to mock data on any error.
   */
  async function fetchDashboardData(): Promise<void> {
    loading.value = true
    usingMockData.value = false

    try {
      // Fire all requests in parallel
      const [portesRes, conductoresRes, incidenciasRes, vehiculosRes] = await Promise.allSettled([
        api.get('/portes'),
        api.get('/conductores'),
        api.get('/incidencias'),
        api.get('/vehiculos'),
      ])

      // Compute KPIs from API responses
      const portesData = portesRes.status === 'fulfilled' ? portesRes.value.data : null
      const conductoresData = conductoresRes.status === 'fulfilled' ? conductoresRes.value.data : null
      const incidenciasData = incidenciasRes.status === 'fulfilled' ? incidenciasRes.value.data : null
      const vehiculosData = vehiculosRes.status === 'fulfilled' ? vehiculosRes.value.data : null

      // If at least one endpoint returned data, compute from API
      const hasAnyData = portesData || conductoresData || incidenciasData || vehiculosData

      if (hasAnyData) {
        // Extract arrays (API may return directly or wrap in content/data)
        const portesList = extractArray(portesData)
        const conductoresList = extractArray(conductoresData)
        const incidenciasList = extractArray(incidenciasData)
        const vehiculosList = extractArray(vehiculosData)

        kpis.value = {
          portesActivos: portesList.filter((p: Record<string, unknown>) =>
            ['EN_RUTA', 'PENDIENTE', 'PROGRAMADO'].includes(String(p.estado ?? ''))
          ).length,
          conductores: conductoresList.length,
          incidenciasAbiertas: incidenciasList.filter((i: Record<string, unknown>) =>
            ['ABIERTA', 'EN_REVISION'].includes(String(i.estado ?? ''))
          ).length,
          vehiculosDisponibles: vehiculosList.filter((v: Record<string, unknown>) =>
            v.disponible === true || String(v.estado ?? '').toUpperCase() === 'DISPONIBLE'
          ).length || vehiculosList.length,
        }

        // Map recent portes for the table
        if (portesList.length > 0) {
          recentPortes.value = portesList.slice(0, 20).map((p: Record<string, unknown>) => ({
            id: Number(p.id ?? 0),
            origen: String(p.origen ?? p.ciudadOrigen ?? '—'),
            destino: String(p.destino ?? p.ciudadDestino ?? '—'),
            conductor: extractConductorName(p),
            estado: String(p.estado ?? 'PENDIENTE'),
            fecha: formatDateFromApi(p.fecha ?? p.fechaCreacion ?? p.fechaSalida),
          }))
        }
      } else {
        // All endpoints failed → use mock data
        usingMockData.value = true
        kpis.value = MOCK_KPIS
        recentPortes.value = MOCK_PORTES
      }
    } catch {
      // Unexpected error → use mock data
      usingMockData.value = true
      kpis.value = MOCK_KPIS
      recentPortes.value = MOCK_PORTES
    } finally {
      // Chart data is always mock (no stats endpoint)
      chartData.value = MOCK_CHART_DATA
      trends.value = MOCK_TRENDS
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

  function extractConductorName(porte: Record<string, unknown>): string {
    if (typeof porte.conductorNombre === 'string') return porte.conductorNombre
    if (porte.conductor && typeof porte.conductor === 'object') {
      const c = porte.conductor as Record<string, unknown>
      return String(c.nombre ?? c.name ?? '—')
    }
    return String(porte.conductor ?? '—')
  }

  function formatDateFromApi(value: unknown): string {
    if (!value) return '—'
    const str = String(value)
    // If already YYYY-MM-DD, return as is
    if (/^\d{4}-\d{2}-\d{2}$/.test(str)) return str
    // If ISO datetime, extract date part
    if (str.includes('T')) return str.split('T')[0]
    return str
  }

  // Return ALL state, getters, and actions
  return {
    // State
    kpis,
    chartData,
    recentPortes,
    trends,
    loading,
    usingMockData,
    // Getters
    totalPortes,
    // Actions
    fetchDashboardData,
  }
})
