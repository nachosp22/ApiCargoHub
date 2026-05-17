import { ref } from 'vue'

const PHOTON_SUPPORTED_LANGS = new Set(['default', 'de', 'en', 'fr'])

function resolvePhotonLang(): string {
  if (typeof navigator === 'undefined') return 'default'

  const raw = navigator.language?.toLowerCase() ?? 'default'
  const baseLang = raw.split('-')[0]
  return PHOTON_SUPPORTED_LANGS.has(baseLang) ? baseLang : 'default'
}

export interface AddressSuggestion {
  display: string
  city: string
  fullAddress: string
  lat: number
  lon: number
}

export interface PhotonProperties {
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

interface PhotonFeature {
  geometry: { coordinates: [number, number] }
  properties: PhotonProperties
}

function inferHouseNumberFromName(name?: string, street?: string): string {
  if (!name || !street) return ''

  const trimmedStreet = street.trim()
  const trimmedName = name.trim()
  if (!trimmedStreet || !trimmedName) return ''

  const escapedStreet = trimmedStreet.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const suffixMatch = trimmedName.match(new RegExp(`^${escapedStreet}\\s+([^,]+)`, 'i'))

  if (suffixMatch?.[1] && /\d/.test(suffixMatch[1])) {
    return suffixMatch[1].trim()
  }

  return ''
}

function normalizeForMatch(value?: string): string {
  return (value ?? '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function inferHouseNumberFromQuery(query?: string, street?: string, name?: string): string {
  if (!query) return ''

  const numberMatch = query.match(/\b\d{1,4}[a-zA-Z]?\b/)
  if (!numberMatch) return ''

  const normalizedQuery = normalizeForMatch(query)
  const reference = normalizeForMatch(street || name)
  if (!normalizedQuery || !reference) return ''

  const referenceTokens = reference
    .split(' ')
    .filter((token) => token.length >= 4)

  const hasStreetHint = (referenceTokens.length > 0 ? referenceTokens : reference.split(' '))
    .some((token) => token.length >= 3 && normalizedQuery.includes(token))

  return hasStreetHint ? numberMatch[0] : ''
}

export function formatFeatureMain(p: PhotonProperties, typedQuery?: string): string {
  const street = p.street?.trim() ?? ''
  const primary = street || p.name?.trim() || ''
  const houseNumber =
    p.housenumber?.trim() ||
    inferHouseNumberFromName(p.name, p.street) ||
    inferHouseNumberFromQuery(typedQuery, p.street, p.name)

  if (primary && houseNumber) {
    if (primary.toLowerCase().includes(houseNumber.toLowerCase())) {
      return primary
    }
    return `${primary} ${houseNumber}`
  }

  return primary
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
        const params = new URLSearchParams({
          q,
          lang: resolvePhotonLang(),
          limit: '5',
        })
        const url = `https://photon.komoot.io/api/?${params.toString()}`
        const res = await fetch(url)

        if (!res.ok) {
          suggestions.value = []
          return
        }

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

    const streetLine = formatFeatureMain(p, query.value)
    const localityLine = [p.postcode, city].filter(Boolean).join(' ').trim()
    const primaryLine = streetLine || p.name || ''
    const fullAddress = [primaryLine, localityLine, p.state, p.country]
      .filter((part) => !!part && part.trim().length > 0)
      .join(', ')
    const [lon, lat] = feature.geometry.coordinates

    const addr: AddressSuggestion = {
      display: fullAddress,
      city,
      fullAddress: fullAddress || (p.name ?? ''),
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
