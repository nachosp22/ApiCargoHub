import type { DriverLocationPoint } from '@/services/api'

export interface MarkerDiffResult {
  toRemove: string[]
  toUpsert: DriverLocationPoint[]
}

export function computeMarkerDiff(
  incomingDrivers: DriverLocationPoint[],
  previousRenderedSignatureByDriverId: Map<string, string>
): MarkerDiffResult {
  const incomingIds = new Set(incomingDrivers.map((driver) => driver.driverId))
  const toRemove: string[] = []

  for (const existingId of previousRenderedSignatureByDriverId.keys()) {
    if (!incomingIds.has(existingId)) {
      toRemove.push(existingId)
    }
  }

  const toUpsert: DriverLocationPoint[] = []
  for (const driver of incomingDrivers) {
    const previousSignature = previousRenderedSignatureByDriverId.get(driver.driverId)
    const currentSignature = getDriverRenderSignature(driver)
    if (previousSignature !== currentSignature) {
      toUpsert.push(driver)
    }
  }

  return { toRemove, toUpsert }
}

export function getDriverRenderSignature(driver: DriverLocationPoint): string {
  return [
    driver.recordedAt,
    driver.state,
    driver.driverName ?? '',
    driver.driverLastName ?? '',
    driver.activePorteId ?? '',
    driver.activePorteDestination ?? '',
    driver.activePorteStatus ?? '',
  ].join('|')
}
