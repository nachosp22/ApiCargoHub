import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { RouteLocationRaw } from 'vue-router'
import { useConductoresStore } from '@/stores/conductores'
import { usePortesStore } from '@/stores/portes'
import { useClientesStore } from '@/stores/clientes'
import { useVehiculosStore } from '@/stores/vehiculos'
import { useIncidenciasStore } from '@/stores/incidencias'
import { useFacturasStore } from '@/stores/facturas'

// --- Types ---

export type EntityType = 'accion' | 'conductor' | 'porte' | 'cliente' | 'vehiculo' | 'incidencia' | 'factura'

export interface SearchResult {
  id: number
  type: EntityType
  title: string
  subtitle: string
  route: RouteLocationRaw
  icon: string
  /** 0 = exact match, 1 = partial — used for sorting */
  _score: number
}

// --- Helpers ---

const MAX_PER_CATEGORY = 4
const MAX_TOTAL = 20
const fleetRealtimeEnabled = import.meta.env.VITE_FEATURE_FLEET_REALTIME === 'true'

function normalize(str: string): string {
  return str
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
}

function matches(field: string | null | undefined, query: string): boolean {
  if (!field) return false
  return normalize(field).includes(normalize(query))
}

function isExact(field: string | null | undefined, query: string): boolean {
  if (!field) return false
  return normalize(field).startsWith(normalize(query))
}

function score(fields: (string | null | undefined)[], query: string): number | null {
  const hit = fields.some((f) => matches(f, query))
  if (!hit) return null
  const exact = fields.some((f) => isExact(f, query))
  return exact ? 0 : 1
}

// --- Store ---

export const useGlobalSearchStore = defineStore('globalSearch', () => {
  const router = useRouter()

  const query = ref('')
  const results = ref<SearchResult[]>([])
  const loading = ref(false)
  const isOpen = ref(false)

  async function search(q: string): Promise<void> {
    query.value = q

    if (q.trim().length < 2) {
      results.value = []
      isOpen.value = false
      return
    }

    loading.value = true
    isOpen.value = true

    try {
      const found = await Promise.all([
        Promise.resolve(searchAcciones(q)),
        searchConductores(q),
        searchPortes(q),
        searchClientes(q),
        searchVehiculos(q),
        searchIncidencias(q),
        searchFacturas(q),
      ])

      // Flatten, sort within each category, limit, then global limit
      const all: SearchResult[] = []
      for (const group of found) {
        const sorted = group.sort((a, b) => a._score - b._score).slice(0, MAX_PER_CATEGORY)
        all.push(...sorted)
      }

      results.value = all.slice(0, MAX_TOTAL)
    } finally {
      loading.value = false
    }
  }

  function clearSearch(): void {
    query.value = ''
    results.value = []
    isOpen.value = false
  }

  function navigateTo(result: SearchResult): void {
    clearSearch()
    router.push(result.route)
  }

  function searchAcciones(q: string): SearchResult[] {
    const actions: SearchResult[] = [
      {
        id: 1,
        type: 'accion',
        title: 'Ir a Dashboard',
        subtitle: 'Resumen operativo',
        route: '/dashboard',
        icon: 'pi pi-home',
        _score: 1,
      },
      {
        id: 2,
        type: 'accion',
        title: 'Ir a Incidencias',
        subtitle: 'Gestión y resolución de incidencias',
        route: '/incidencias',
        icon: 'pi pi-exclamation-triangle',
        _score: 1,
      },
      {
        id: 3,
        type: 'accion',
        title: 'Ir a Revisión de portes',
        subtitle: 'Pendientes de revisión manual',
        route: '/revision-portes',
        icon: 'pi pi-eye',
        _score: 1,
      },
      {
        id: 4,
        type: 'accion',
        title: 'Ir a Portes',
        subtitle: 'Operación completa de portes',
        route: '/portes',
        icon: 'pi pi-box',
        _score: 1,
      },
      {
        id: 5,
        type: 'accion',
        title: 'Ir a Conductores',
        subtitle: 'Disponibilidad y estado de conductores',
        route: '/conductores',
        icon: 'pi pi-users',
        _score: 1,
      },
      {
        id: 6,
        type: 'accion',
        title: 'Ir a Vehículos',
        subtitle: 'Flota y disponibilidad',
        route: '/vehiculos',
        icon: 'pi pi-car',
        _score: 1,
      },
      {
        id: 7,
        type: 'accion',
        title: 'Ir a Clientes',
        subtitle: 'Cartera de clientes',
        route: '/clientes',
        icon: 'pi pi-building',
        _score: 1,
      },
    ]

    if (fleetRealtimeEnabled) {
      actions.push({
        id: 8,
        type: 'accion',
        title: 'Ir a Mapa de flota',
        subtitle: 'Tracking en tiempo real',
        route: '/fleet-map',
        icon: 'pi pi-map',
        _score: 1,
      })
    }

    return actions.reduce<SearchResult[]>((acc, action) => {
      const s = score([action.title, action.subtitle], q)
      if (s !== null) {
        acc.push({ ...action, _score: s })
      }
      return acc
    }, [])
  }

  // --- Per-entity search helpers ---

  async function searchConductores(q: string): Promise<SearchResult[]> {
    const store = useConductoresStore()
    if (store.conductores.length === 0) await store.fetchConductores()

    return store.conductores.reduce<SearchResult[]>((acc, c) => {
      const s = score([c.nombre, c.apellidos, c.email, c.dni], q)
      if (s !== null) {
        acc.push({
          id: c.id,
          type: 'conductor',
          title: `${c.nombre} ${c.apellidos}`,
          subtitle: c.email,
          route: { path: '/conductores', query: { conductorId: String(c.id) } },
          icon: 'pi pi-user',
          _score: s,
        })
      }
      return acc
    }, [])
  }

  async function searchPortes(q: string): Promise<SearchResult[]> {
    const store = usePortesStore()
    if (store.portes.length === 0) await store.fetchPortes()

    return store.portes.reduce<SearchResult[]>((acc, p) => {
      const s = score([String(p.id), p.origen, p.destino, p.descripcionCliente ?? null], q)
      if (s !== null) {
        acc.push({
          id: p.id,
          type: 'porte',
          title: `#${p.id} ${p.origen} → ${p.destino}`,
          subtitle: `${p.estado}${p.fechaCreacion ? ' · ' + p.fechaCreacion.split('T')[0] : ''}`,
          route: { path: '/portes', query: { porteId: String(p.id) } },
          icon: 'pi pi-box',
          _score: s,
        })
      }
      return acc
    }, [])
  }

  async function searchClientes(q: string): Promise<SearchResult[]> {
    const store = useClientesStore()
    if (store.clientes.length === 0) await store.fetchClientes()

    return store.clientes.reduce<SearchResult[]>((acc, c) => {
      const s = score([c.nombreEmpresa, c.cif, c.emailContacto], q)
      if (s !== null) {
        acc.push({
          id: c.id,
          type: 'cliente',
          title: c.nombreEmpresa,
          subtitle: c.cif,
          route: { path: '/clientes', query: { clienteId: String(c.id) } },
          icon: 'pi pi-building',
          _score: s,
        })
      }
      return acc
    }, [])
  }

  async function searchVehiculos(q: string): Promise<SearchResult[]> {
    const store = useVehiculosStore()
    if (store.vehiculos.length === 0) await store.fetchVehiculos()

    return store.vehiculos.reduce<SearchResult[]>((acc, v) => {
      const s = score([v.matricula, v.marca, v.modelo], q)
      if (s !== null) {
        acc.push({
          id: v.id,
          type: 'vehiculo',
          title: v.matricula,
          subtitle: `${v.marca} ${v.modelo}`,
          route: { path: '/vehiculos', query: { vehiculoId: String(v.id) } },
          icon: 'pi pi-car',
          _score: s,
        })
      }
      return acc
    }, [])
  }

  async function searchIncidencias(q: string): Promise<SearchResult[]> {
    const store = useIncidenciasStore()
    if (store.incidencias.length === 0) await store.fetchIncidencias()

    return store.incidencias.reduce<SearchResult[]>((acc, i) => {
      const s = score([String(i.id), i.titulo, i.descripcion, i.severidad], q)
      if (s !== null) {
        acc.push({
          id: i.id,
          type: 'incidencia',
          title: `#${i.id} ${i.titulo}`,
          subtitle: i.estado,
          route: { path: '/incidencias', query: { incidenciaId: String(i.id) } },
          icon: 'pi pi-exclamation-triangle',
          _score: s,
        })
      }
      return acc
    }, [])
  }

  async function searchFacturas(q: string): Promise<SearchResult[]> {
    const store = useFacturasStore()
    if (store.facturas.length === 0) await store.fetchFacturas()

    return store.facturas.reduce<SearchResult[]>((acc, f) => {
      const s = score([f.numeroSerie, String(f.id)], q)
      if (s !== null) {
        acc.push({
          id: f.id,
          type: 'factura',
          title: f.numeroSerie,
          subtitle: `${f.importeTotal.toFixed(2)} € · ${f.pagada ? 'Pagada' : 'Pendiente'}`,
          route: { path: '/facturas', query: { facturaId: String(f.id) } },
          icon: 'pi pi-file-edit',
          _score: s,
        })
      }
      return acc
    }, [])
  }

  return {
    query,
    results,
    loading,
    isOpen,
    search,
    clearSearch,
    navigateTo,
  }
})
