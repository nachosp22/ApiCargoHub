import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useFleetTrackingStore } from './fleetTracking'
import type { FleetSnapshotResponse } from '@/services/api'

const getFleetSnapshotMock = vi.fn<() => Promise<FleetSnapshotResponse>>()

vi.mock('@/services/api', async () => {
  const actual = await vi.importActual<typeof import('@/services/api')>('@/services/api')
  return {
    ...actual,
    getFleetSnapshot: () => getFleetSnapshotMock(),
  }
})

function snapshot(overrides?: Partial<FleetSnapshotResponse>): FleetSnapshotResponse {
  return {
    snapshotAt: '2026-03-16T10:00:00Z',
    drivers: [],
    meta: {
      pollingSuggestedSec: 10,
      degraded: false,
    },
    ...overrides,
  }
}

describe('fleetTracking store', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-16T10:00:00Z'))
    setActivePinia(createPinia())
    getFleetSnapshotMock.mockReset()
  })

  it('keeps last snapshot on timeout and retries with degraded state', async () => {
    const store = useFleetTrackingStore()

    getFleetSnapshotMock.mockResolvedValueOnce(
      snapshot({
        drivers: [
          {
            driverId: '1',
            lat: 40.416,
            lon: -3.703,
            recordedAt: '2026-03-16T10:00:00Z',
            state: 'ONLINE',
          },
        ],
      })
    )
    await store.fetchSnapshot()

    getFleetSnapshotMock.mockRejectedValueOnce(new Error('timeout'))
    await store.fetchSnapshot()

    expect(store.drivers).toHaveLength(1)
    expect(store.connectionState).toBe('DEGRADED')
    expect(store.degradedReason).toBe('using_last_snapshot')

    store.startPolling()
    expect(getFleetSnapshotMock).toHaveBeenCalled()
  })

  it('returns empty fleet without error', async () => {
    const store = useFleetTrackingStore()
    getFleetSnapshotMock.mockResolvedValueOnce(snapshot({ drivers: [] }))

    await store.fetchSnapshot()

    expect(store.hasDrivers).toBe(false)
    expect(store.drivers).toEqual([])
    expect(store.connectionState).toBe('ONLINE')
  })
})
