package com.cargohub.mobile.ui;

class TrackingSyncStatusResolver {

    enum SyncStatus {
        PAUSED,
        BLOCKED_NOTIFICATIONS,
        DEGRADED_NO_NETWORK,
        CONFIRMED_RETRYING,
        CONFIRMED,
        SENDING_UNCONFIRMED
    }

    private TrackingSyncStatusResolver() {
    }

    static SyncStatus resolve(
            boolean isPlaying,
            boolean isSyncBlockedByNotificationPermission,
            boolean isNetworkOnline,
            long lastSyncFailureAtMs,
            long lastConfirmedSyncAtMs,
            long nowMs
    ) {
        if (!isPlaying) {
            return SyncStatus.PAUSED;
        }
        if (isSyncBlockedByNotificationPermission) {
            return SyncStatus.BLOCKED_NOTIFICATIONS;
        }
        if (!isNetworkOnline) {
            return SyncStatus.DEGRADED_NO_NETWORK;
        }
        if (hasRecentSyncFailure(lastSyncFailureAtMs, lastConfirmedSyncAtMs, nowMs)) {
            return SyncStatus.CONFIRMED_RETRYING;
        }
        if (lastConfirmedSyncAtMs > 0L) {
            return SyncStatus.CONFIRMED;
        }
        return SyncStatus.SENDING_UNCONFIRMED;
    }

    static boolean hasRecentSyncFailure(long lastSyncFailureAtMs, long lastConfirmedSyncAtMs, long nowMs) {
        if (lastSyncFailureAtMs <= 0L) {
            return false;
        }
        if (lastConfirmedSyncAtMs > 0L && lastConfirmedSyncAtMs >= lastSyncFailureAtMs) {
            return false;
        }
        return (nowMs - lastSyncFailureAtMs) <= 90_000L;
    }
}
