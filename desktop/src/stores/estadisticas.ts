import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/services/api'

// --- TypeScript Interfaces ---

export interface TopConductor {
  nombre: string
  portes: number
  rating: number
}

export interface TopCliente {
  nombreEmpresa: string
  totalFacturado: number
  portes: number
}

export interface PorteMensual {
  mes: string
  cantidad: number
  ingresos: number
}

export interface PorteEstado {
  estado: string
  cantidad: number
}

export interface EstadisticasGlobales {
  totalPortes: number
  portesEsteMes: number
  portesTendencia: number
  totalIngresos: number
  ingresosEsteMes: number
  ingresosTendencia: number
  totalConductoresActivos: number
  totalClientes: number
  portesCompletados: number
  portesPendientes: number
  portesEnTransito: number
  facturasEmitidas: number
  facturasPagadas: number
  facturasPendientes: number
  topConductores: TopConductor[]
  topClientes: TopCliente[]
  portesPorMes: PorteMensual[]
  portesPorEstado: PorteEstado[]
}

// --- Store ---

export const useEstadisticasStore = defineStore('estadisticas', () => {
  const data = ref<EstadisticasGlobales | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchEstadisticas(): Promise<void> {
    loading.value = true
    error.value = null

    try {
      const res = await api.get<EstadisticasGlobales>('/estadisticas/globales')
      data.value = res.data
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al cargar estadísticas'
      error.value = msg
      data.value = null
    } finally {
      loading.value = false
    }
  }

  return {
    data,
    loading,
    error,
    fetchEstadisticas,
  }
})
