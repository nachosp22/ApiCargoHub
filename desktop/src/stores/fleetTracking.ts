import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getFleetSnapshot,
  type DriverLocationPoint,
  type FleetSnapshotResponse,
} from '@/services/api'

type ConnectionState = 'ONLINE' | 'DEGRADED' | 'OFFLINE'

const DEFAULT_POLLING_MS = 10_000
const MAX_STALE_KEEP_MS = 60_000

export const useFleetTrackingStore = defineStore('fleetTracking', () => {
  const driversById = ref(new Map<string, DriverLocationPoint>())
  const lastSnapshotAt = ref<string | null>(null)
  const lastSuccessAt = ref<number | null>(null)
  const connectionState = ref<ConnectionState>('OFFLINE')
  const pollingMs = ref(DEFAULT_POLLING_MS)
  const degradedReason = ref<string | null>(null)
  const loading = ref(false)

  let intervalId: number | null = null
  let retryBackoffMs = DEFAULT_POLLING_MS

  const drivers = computed(() => Array.from(driversById.value.values()))
  const hasDrivers = computed(() => drivers.value.length > 0)

  async function fetchSnapshot(): Promise<void> {
    if (!loading.value) loading.value = true

    try {
      const snapshot = await getFleetSnapshot()
      applySnapshot(snapshot)
      connectionState.value = snapshot.meta.degraded ? 'DEGRADED' : 'ONLINE'
      degradedReason.value = snapshot.meta.degraded
        ? snapshot.meta.degradedReason ?? 'degraded'
        : null
      retryBackoffMs = pollingMs.value
    } catch {
      const now = Date.now()
      const canKeep =
        lastSuccessAt.value !== null && now - lastSuccessAt.value <= MAX_STALE_KEEP_MS

      if (canKeep) {
        connectionState.value = 'DEGRADED'
        degradedReason.value = 'using_last_snapshot'
      } else {
        driversById.value = new Map()
        connectionState.value = 'OFFLINE'
        degradedReason.value = 'snapshot_unavailable'
      }

      retryBackoffMs = Math.min(retryBackoffMs * 2, 60_000)
      restartPolling(retryBackoffMs)
    } finally {
      loading.value = false
    }
  }

  function applySnapshot(snapshot: FleetSnapshotResponse): void {
    const nextMap = new Map(driversById.value)

    for (const point of snapshot.drivers) {
      const previous = nextMap.get(point.driverId)
      if (!previous || previous.recordedAt !== point.recordedAt) {
        nextMap.set(point.driverId, point)
      }
    }

    const nextIds = new Set(snapshot.drivers.map((d) => d.driverId))
    for (const id of nextMap.keys()) {
      if (!nextIds.has(id)) {
        nextMap.delete(id)
      }
    }

    driversById.value = nextMap
    lastSnapshotAt.value = snapshot.snapshotAt
    lastSuccessAt.value = Date.now()

    const suggestedMs = Math.max(
      1_000,
      (snapshot.meta.pollingSuggestedSec || DEFAULT_POLLING_MS / 1000) * 1000
    )
    if (suggestedMs !== pollingMs.value) {
      pollingMs.value = suggestedMs
      restartPolling(suggestedMs)
    }
  }

  function startPolling(): void {
    stopPolling()
    void fetchSnapshot()
    intervalId = window.setInterval(() => {
      void fetchSnapshot()
    }, pollingMs.value)
  }

  function restartPolling(ms: number): void {
    if (intervalId !== null) {
      clearInterval(intervalId)
    }
    intervalId = window.setInterval(() => {
      void fetchSnapshot()
    }, ms)
  }

  function stopPolling(): void {
    if (intervalId !== null) {
      clearInterval(intervalId)
      intervalId = null
    }
  }

  return {
    drivers,
    hasDrivers,
    lastSnapshotAt,
    connectionState,
    degradedReason,
    loading,
    pollingMs,
    fetchSnapshot,
    startPolling,
    stopPolling,
  }
})
