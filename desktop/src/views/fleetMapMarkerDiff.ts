import type { DriverLocationPoint } from '@/services/api'

export interface MarkerDiffResult {
  toRemove: string[]
  toUpsert: DriverLocationPoint[]
}

export function computeMarkerDiff(
  incomingDrivers: DriverLocationPoint[],
  previousRecordedAtByDriverId: Map<string, string>
): MarkerDiffResult {
  const incomingIds = new Set(incomingDrivers.map((driver) => driver.driverId))
  const toRemove: string[] = []

  for (const existingId of previousRecordedAtByDriverId.keys()) {
    if (!incomingIds.has(existingId)) {
      toRemove.push(existingId)
    }
  }

  const toUpsert: DriverLocationPoint[] = []
  for (const driver of incomingDrivers) {
    const previousRecordedAt = previousRecordedAtByDriverId.get(driver.driverId)
    if (previousRecordedAt !== driver.recordedAt) {
      toUpsert.push(driver)
    }
  }

  return { toRemove, toUpsert }
}
