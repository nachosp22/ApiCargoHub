import { describe, expect, it } from 'vitest'
import type { DriverLocationPoint } from '@/services/api'
import { computeMarkerDiff, getDriverRenderSignature } from './fleetMapMarkerDiff'

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

  it('detects incremental upsert when recordedAt changes', () => {
    const driver1 = driver({ driverId: '1', recordedAt: '2026-03-16T10:00:00Z' })
    const driver2Before = driver({ driverId: '2', recordedAt: '2026-03-16T10:00:00Z' })
    const driver2After = driver({ driverId: '2', recordedAt: '2026-03-16T10:00:10Z' })
    const previous = new Map<string, string>([
      ['1', getDriverRenderSignature(driver1)],
      ['2', getDriverRenderSignature(driver2Before)],
    ])

    const result = computeMarkerDiff([
      driver1,
      driver2After,
    ], previous)

    expect(result.toRemove).toEqual([])
    expect(result.toUpsert).toHaveLength(1)
    expect(result.toUpsert[0].driverId).toBe('2')
  })

  it('detects incremental upsert when reporting state changes', () => {
    const driver1Before = driver({ driverId: '1', recordedAt: '2026-03-16T10:00:00Z', state: 'ONLINE' })
    const previous = new Map<string, string>([
      ['1', getDriverRenderSignature(driver1Before)],
    ])

    const result = computeMarkerDiff([
      driver({ driverId: '1', recordedAt: '2026-03-16T10:00:00Z', state: 'OFFLINE' }),
    ], previous)

    expect(result.toRemove).toEqual([])
    expect(result.toUpsert).toHaveLength(1)
    expect(result.toUpsert[0].driverId).toBe('1')
  })

  it('detects removed markers when driver disappears', () => {
    const driver1 = driver({ driverId: '1' })
    const driver2 = driver({ driverId: '2' })
    const previous = new Map<string, string>([
      ['1', getDriverRenderSignature(driver1)],
      ['2', getDriverRenderSignature(driver2)],
    ])

    const result = computeMarkerDiff([driver1], previous)

    expect(result.toRemove).toEqual(['2'])
    expect(result.toUpsert).toHaveLength(0)
  })
})
