import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/services/api'

export interface Valoracion {
  id: number
  puntuacion: number
  comentario?: string
  fechaCreacion: string
  conductor?: { id: number; nombre: string; apellidos?: string }
}

export const useValoracionesStore = defineStore('valoraciones', () => {
  const loading = ref(false)
  const submitting = ref(false)
  const error = ref<string | null>(null)

  // Cache: porteId -> Valoracion | null | undefined (undefined = not fetched)
  const valoracionesByPorte = ref<Record<number, Valoracion | null>>({})

  async function fetchMiValoracion(porteId: number): Promise<Valoracion | null> {
    try {
      const response = await api.get(`/valoraciones/porte/${porteId}/mi-valoracion`)
      if (response.status === 204) {
        valoracionesByPorte.value[porteId] = null
        return null
      }
      const v = mapValoracion(response.data)
      valoracionesByPorte.value[porteId] = v
      return v
    } catch {
      valoracionesByPorte.value[porteId] = null
      return null
    }
  }

  async function crearValoracion(
    porteId: number,
    puntuacion: number,
    comentario?: string
  ): Promise<Valoracion> {
    submitting.value = true
    error.value = null
    try {
      const response = await api.post('/valoraciones', {
        porteId,
        puntuacion,
        comentario: comentario || null,
      })
      const v = mapValoracion(response.data)
      valoracionesByPorte.value[porteId] = v
      return v
    } catch (err: unknown) {
      const message =
        err && typeof err === 'object' && 'response' in err
          ? String((err as { response: { data: unknown } }).response?.data ?? 'Error al enviar valoración')
          : 'Error al enviar valoración'
      error.value = message
      throw err
    } finally {
      submitting.value = false
    }
  }

  function mapValoracion(raw: unknown): Valoracion {
    const d = raw as Record<string, unknown>
    return {
      id: Number(d.id ?? 0),
      puntuacion: Number(d.puntuacion ?? 0),
      comentario: d.comentario ? String(d.comentario) : undefined,
      fechaCreacion: String(d.fechaCreacion ?? ''),
      conductor: d.conductor ? mapConductor(d.conductor) : undefined,
    }
  }

  function mapConductor(raw: unknown): Valoracion['conductor'] {
    if (!raw || typeof raw !== 'object') return undefined
    const c = raw as Record<string, unknown>
    return {
      id: Number(c.id ?? 0),
      nombre: String(c.nombre ?? ''),
      apellidos: c.apellidos ? String(c.apellidos) : undefined,
    }
  }

  function getValoracion(porteId: number): Valoracion | null | undefined {
    return valoracionesByPorte.value[porteId]
  }

  return {
    loading,
    submitting,
    error,
    valoracionesByPorte,
    fetchMiValoracion,
    crearValoracion,
    getValoracion,
  }
})
