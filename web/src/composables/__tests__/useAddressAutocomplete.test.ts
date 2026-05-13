import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatFeatureMain, useAddressAutocomplete } from '@/composables/useAddressAutocomplete'

const feature = {
  geometry: { coordinates: [-3.7038, 40.4168] as [number, number] },
  properties: {
    city: 'Madrid',
    street: 'Gran Via',
    housenumber: '1',
    postcode: '28013',
    country: 'Spain',
  },
}

const featureWithNumberInNameOnly = {
  geometry: { coordinates: [-5.6615, 43.5322] as [number, number] },
  properties: {
    name: 'Calle Brasil 10',
    city: 'Gijon',
    street: 'Calle Brasil',
    postcode: '33213',
    country: 'Spain',
  },
}

const featureWithoutNumber = {
  geometry: { coordinates: [-5.6615, 43.5322] as [number, number] },
  properties: {
    name: 'Calle Brasil',
    city: 'Gijon',
    postcode: '33213',
    country: 'Spain',
  },
}

async function flushAsync(): Promise<void> {
  await Promise.resolve()
  await Promise.resolve()
}

describe('useAddressAutocomplete', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('uses default language for unsupported browser locales', async () => {
    vi.spyOn(window.navigator, 'language', 'get').mockReturnValue('es-AR')
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ features: [feature] }),
    } as Response)

    const { search, suggestions } = useAddressAutocomplete()

    await search('madrid')
    vi.advanceTimersByTime(300)
    await flushAsync()

    expect(fetch).toHaveBeenCalledTimes(1)
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('lang=default')
    )
    expect(suggestions.value).toHaveLength(1)
  })

  it('clears suggestions when provider responds with non-ok status', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      json: async () => ({ features: [feature] }),
    } as Response)

    const { search, suggestions } = useAddressAutocomplete()
    suggestions.value = [feature]

    await search('madrid')
    vi.advanceTimersByTime(300)
    await flushAsync()

    expect(suggestions.value).toEqual([])
  })

  it('uses number from name when housenumber field is missing', () => {
    expect(formatFeatureMain(featureWithNumberInNameOnly.properties)).toBe('Calle Brasil 10')
  })

  it('keeps selected full address with inferred house number', () => {
    const { select } = useAddressAutocomplete()

    const selected = select(featureWithNumberInNameOnly)

    expect(selected.fullAddress).toContain('Calle Brasil 10')
  })

  it('uses number from typed query when provider omits housenumber', () => {
    expect(formatFeatureMain(featureWithoutNumber.properties, 'calle brasil 10 gijon')).toBe('Calle Brasil 10')
  })

  it('keeps selected full address with number inferred from typed query', () => {
    const { select, query } = useAddressAutocomplete()
    query.value = 'calle brasil 10 gijon'

    const selected = select(featureWithoutNumber)

    expect(selected.fullAddress).toContain('Calle Brasil 10')
  })
})
