import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api, getResumenPortes, getIncidenciasPendientes } from '@/services/api'

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
  fechaRecogida?: string
  revisionManual?: boolean
  motivoRevision?: string
}

export interface KpiTrend {
  value: string
  positive: boolean
}

export interface MonthlyChartPoint {
  label: string
  count: number
}

// --- Store ---

export const useDashboardStore = defineStore('dashboard', () => {
  // --- State ---
  const kpis = ref<DashboardKpis>({ portesActivos: 0, conductores: 0, incidenciasAbiertas: 0, vehiculosDisponibles: 0 })
  const chartData = ref<number[]>([])
  const chartLabels = ref<string[]>([])
  const recentPortes = ref<Porte[]>([])
  const allPortes = ref<Porte[]>([])
  const trends = ref<Record<string, KpiTrend>>({})
  const loading = ref(false)
  const usingMockData = ref(false)
  const resumen = ref<{ portesMes: number; portesActivos: number; portesManana: number } | null>(null)
  const resumenAnterior = ref<{ portesMes: number; portesActivos: number; portesManana: number } | null>(null)
  const incidenciasPendientes = ref<number>(0)
  const revisionesPendientes = ref<number>(0)

  // --- Getters ---
  const totalPortes = computed(() => recentPortes.value.length)

  // --- Actions ---

  /**
   * Fetch all dashboard data from API endpoints.
   * Computes KPIs, chart data, and trends from real data.
   */
  async function fetchDashboardData(): Promise<void> {
    loading.value = true
    usingMockData.value = false

    try {
      // Fire all requests in parallel
      const [portesRes, conductoresRes, incidenciasRes, vehiculosRes, revisionesRes] = await Promise.allSettled([
        api.get('/portes'),
        api.get('/conductores'),
        api.get('/incidencias'),
        api.get('/vehiculos'),
        api.get('/portes/pendientes-revision'),
      ])

      // Compute KPIs from API responses
      const portesData = portesRes.status === 'fulfilled' ? portesRes.value.data : null
      const conductoresData = conductoresRes.status === 'fulfilled' ? conductoresRes.value.data : null
      const incidenciasData = incidenciasRes.status === 'fulfilled' ? incidenciasRes.value.data : null
      const vehiculosData = vehiculosRes.status === 'fulfilled' ? vehiculosRes.value.data : null

      const hasAnyData = portesData || conductoresData || incidenciasData || vehiculosData

      if (hasAnyData) {
        const portesList = extractArray(portesData)
        const conductoresList = extractArray(conductoresData)
        const incidenciasList = extractArray(incidenciasData)
        const vehiculosList = extractArray(vehiculosData)

        kpis.value = {
          portesActivos: portesList.filter((p: Record<string, unknown>) =>
            ['PENDIENTE', 'ASIGNADO', 'EN_TRANSITO'].includes(String(p.estado ?? ''))
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
          const mappedPortes = portesList.map((p: Record<string, unknown>) => ({
            id: Number(p.id ?? 0),
            origen: String(p.origen ?? p.ciudadOrigen ?? '—'),
            destino: String(p.destino ?? p.ciudadDestino ?? '—'),
            conductor: extractConductorName(p),
            estado: String(p.estado ?? 'PENDIENTE'),
            fecha: formatDateFromApi(p.fecha ?? p.fechaCreacion ?? p.fechaSalida),
            fechaRecogida: formatDateFromApi(p.fechaRecogida ?? p.fechaSalida),
            revisionManual: p.revisionManual === true,
            motivoRevision: p.motivoRevision ? String(p.motivoRevision) : undefined,
          }))

          allPortes.value = mappedPortes
          recentPortes.value = mappedPortes.slice(0, 20)
        } else {
          allPortes.value = []
        }

        revisionesPendientes.value = revisionesRes.status === 'fulfilled'
          ? extractArray(revisionesRes.value.data).length
          : portesList.filter((p: Record<string, unknown>) =>
            p.revisionManual === true || (String(p.estado ?? '') === 'PENDIENTE' && !p.conductor && !!p.motivoRevision)
          ).length

        // --- Chart: group portes by month (last 12 months) ---
        computeChartFromPortes(portesList)
      } else {
        usingMockData.value = true
        kpis.value = { portesActivos: 0, conductores: 0, incidenciasAbiertas: 0, vehiculosDisponibles: 0 }
        recentPortes.value = []
        allPortes.value = []
        chartData.value = []
        chartLabels.value = []
        revisionesPendientes.value = 0
      }
    } catch {
      usingMockData.value = true
      kpis.value = { portesActivos: 0, conductores: 0, incidenciasAbiertas: 0, vehiculosDisponibles: 0 }
      recentPortes.value = []
      allPortes.value = []
      chartData.value = []
      chartLabels.value = []
      revisionesPendientes.value = 0
    } finally {
      loading.value = false
    }
  }

  /**
   * Compute chart data by grouping portes by month for the last 12 months.
   */
  function computeChartFromPortes(portesList: Record<string, unknown>[]): void {
    const now = new Date()
    const MONTH_NAMES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']

    // Build last 12 months as YYYY-MM keys
    const months: { key: string; label: string }[] = []
    for (let i = 11; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
      const label = `${MONTH_NAMES[d.getMonth()]} ${String(d.getFullYear()).slice(2)}`
      months.push({ key, label })
    }

    // Count portes per month
    const countByMonth: Record<string, number> = {}
    for (const m of months) countByMonth[m.key] = 0

    for (const p of portesList) {
      const dateStr = String(p.fecha ?? p.fechaCreacion ?? p.fechaSalida ?? '')
      if (!dateStr) continue
      const monthKey = dateStr.slice(0, 7) // YYYY-MM
      if (monthKey in countByMonth) {
        countByMonth[monthKey]++
      }
    }

    chartLabels.value = months.map((m) => m.label)
    chartData.value = months.map((m) => countByMonth[m.key])
  }

  /**
   * Fetch resumen for current and previous month to calculate trends.
   */
  async function fetchResumen(): Promise<void> {
    try {
      const now = new Date()
      const currentYear = now.getFullYear()
      const currentMonth = now.getMonth() + 1
      const prevDate = new Date(currentYear, currentMonth - 2, 1)
      const prevYear = prevDate.getFullYear()
      const prevMonth = prevDate.getMonth() + 1

      const [currentRes, prevRes] = await Promise.allSettled([
        getResumenPortes(currentYear, currentMonth),
        getResumenPortes(prevYear, prevMonth),
      ])

      resumen.value = currentRes.status === 'fulfilled' ? currentRes.value.data : null
      resumenAnterior.value = prevRes.status === 'fulfilled' ? prevRes.value.data : null

      // Calculate trends from real data
      computeTrends()
    } catch {
      resumen.value = null
      resumenAnterior.value = null
      trends.value = {}
    }
  }

  /**
   * Compute KPI trends comparing current vs previous period.
   */
  function computeTrends(): void {
    const current = resumen.value
    const previous = resumenAnterior.value

    if (!current || !previous) {
      trends.value = {}
      return
    }

    const result: Record<string, KpiTrend> = {}

    // Portes del mes trend
    if (previous.portesMes > 0) {
      const pct = ((current.portesMes - previous.portesMes) / previous.portesMes * 100).toFixed(0)
      const val = Number(pct)
      result['portesMes'] = {
        value: `${val >= 0 ? '+' : ''}${pct}%`,
        positive: val >= 0,
      }
    }

    // Portes activos trend
    if (previous.portesActivos > 0) {
      const diff = current.portesActivos - previous.portesActivos
      result['portesActivos'] = {
        value: `${diff >= 0 ? '+' : ''}${diff}`,
        positive: diff >= 0,
      }
    }

    trends.value = result
  }

  async function fetchIncidenciasPendientes(): Promise<void> {
    try {
      const res = await getIncidenciasPendientes()
      incidenciasPendientes.value = res.data.pendientes ?? 0
    } catch {
      incidenciasPendientes.value = 0
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
    if (/^\d{4}-\d{2}-\d{2}$/.test(str)) return str
    if (str.includes('T')) return str.split('T')[0]
    return str
  }

  return {
    // State
    kpis,
    chartData,
    chartLabels,
    recentPortes,
    trends,
    loading,
    usingMockData,
    resumen,
    resumenAnterior,
    incidenciasPendientes,
    revisionesPendientes,
    allPortes,
    // Getters
    totalPortes,
    // Actions
    fetchDashboardData,
    fetchResumen,
    fetchIncidenciasPendientes,
  }
})
