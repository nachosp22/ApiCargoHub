import { ref } from 'vue'

export interface AddressSuggestion {
  display: string
  city: string
  fullAddress: string
  lat: number
  lon: number
}

interface PhotonFeature {
  geometry: { coordinates: [number, number] }
  properties: {
    name?: string
    city?: string
    town?: string
    village?: string
    county?: string
    state?: string
    country?: string
    postcode?: string
    street?: string
    housenumber?: string
  }
}

export function useAddressAutocomplete() {
  const query = ref('')
  const suggestions = ref<PhotonFeature[]>([])
  const loading = ref(false)
  const selectedAddress = ref<AddressSuggestion | null>(null)

  let debounceTimer: ReturnType<typeof setTimeout> | null = null

  async function search(q: string): Promise<void> {
    query.value = q

    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }

    if (!q || q.trim().length < 3) {
      suggestions.value = []
      return
    }

    debounceTimer = setTimeout(async () => {
      loading.value = true
      try {
        const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(q)}&lang=es&limit=5`
        const res = await fetch(url)
        const data = await res.json()
        suggestions.value = (data.features ?? []) as PhotonFeature[]
      } catch {
        suggestions.value = []
      } finally {
        loading.value = false
      }
    }, 300)
  }

  function select(feature: PhotonFeature): AddressSuggestion {
    const p = feature.properties
    const city = p.city ?? p.town ?? p.village ?? p.county ?? p.state ?? ''

    const parts: string[] = []
    if (p.street) parts.push(p.street)
    if (p.housenumber) parts.push(p.housenumber)
    if (p.postcode) parts.push(p.postcode)
    if (city) parts.push(city)

    const fullAddress = parts.length > 0 ? parts.join(', ') : (p.name ?? '')
    const [lon, lat] = feature.geometry.coordinates

    const addr: AddressSuggestion = {
      display: fullAddress,
      city,
      fullAddress,
      lat,
      lon,
    }

    selectedAddress.value = addr
    query.value = fullAddress
    suggestions.value = []

    return addr
  }

  function clear(): void {
    query.value = ''
    suggestions.value = []
    loading.value = false
    selectedAddress.value = null
  }

  return {
    query,
    suggestions,
    loading,
    selectedAddress,
    search,
    select,
    clear,
  }
}
