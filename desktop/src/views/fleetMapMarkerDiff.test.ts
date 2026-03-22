import { describe, expect, it } from 'vitest'
import type { DriverLocationPoint } from '@/services/api'
import { computeMarkerDiff } from './fleetMapMarkerDiff'

function driver(overrides: Partial<DriverLocationPoint>): DriverLocationPoint {
  return {
    driverId: '1',
    lat: 40.416,
    lon: -3.703,
    recordedAt: '2026-03-16T10:00:00Z',
    state: 'ONLINE',
    ...overrides,
  }
}

describe('computeMarkerDiff', () => {
  it('returns empty upserts/removals for empty fleet', () => {
    const previous = new Map<string, string>()

    const result = computeMarkerDiff([], previous)

    expect(result.toRemove).toEqual([])
    expect(result.toUpsert).toEqual([])
  })

  it('detects incremental upsert only for changed recordedAt', () => {
    const previous = new Map<string, string>([
      ['1', '2026-03-16T10:00:00Z'],
      ['2', '2026-03-16T10:00:00Z'],
    ])

    const result = computeMarkerDiff([
      driver({ driverId: '1', recordedAt: '2026-03-16T10:00:00Z' }),
      driver({ driverId: '2', recordedAt: '2026-03-16T10:00:10Z' }),
    ], previous)

    expect(result.toRemove).toEqual([])
    expect(result.toUpsert).toHaveLength(1)
    expect(result.toUpsert[0].driverId).toBe('2')
  })

  it('detects removed markers when driver disappears', () => {
    const previous = new Map<string, string>([
      ['1', '2026-03-16T10:00:00Z'],
      ['2', '2026-03-16T10:00:00Z'],
    ])

    const result = computeMarkerDiff([driver({ driverId: '1' })], previous)

    expect(result.toRemove).toEqual(['2'])
    expect(result.toUpsert).toHaveLength(0)
  })
})
