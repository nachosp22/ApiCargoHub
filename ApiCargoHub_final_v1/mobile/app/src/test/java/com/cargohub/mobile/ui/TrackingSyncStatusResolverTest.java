package com.cargohub.mobile.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TrackingSyncStatusResolverTest {

    private static final long NOW_MS = 1_000_000L;

    @Test
    public void resolve_returnsPausedWhenTrackingIsNotPlaying() {
        TrackingSyncStatusResolver.SyncStatus status = TrackingSyncStatusResolver.resolve(
                false,
                true,
                false,
                NOW_MS - 5_000L,
                NOW_MS - 2_000L,
                NOW_MS
        );

        assertEquals(TrackingSyncStatusResolver.SyncStatus.PAUSED, status);
    }

    @Test
    public void resolve_prioritizesBlockedNotificationsOverNetworkFailureAndConfirmation() {
        TrackingSyncStatusResolver.SyncStatus status = TrackingSyncStatusResolver.resolve(
                true,
                true,
                false,
                NOW_MS - 5_000L,
                NOW_MS - 2_000L,
                NOW_MS
        );

        assertEquals(TrackingSyncStatusResolver.SyncStatus.BLOCKED_NOTIFICATIONS, status);
    }

    @Test
    public void resolve_prioritizesNoNetworkOverRecentFailureAndConfirmation() {
        TrackingSyncStatusResolver.SyncStatus status = TrackingSyncStatusResolver.resolve(
                true,
                false,
                false,
                NOW_MS - 5_000L,
                NOW_MS - 2_000L,
                NOW_MS
        );

        assertEquals(TrackingSyncStatusResolver.SyncStatus.DEGRADED_NO_NETWORK, status);
    }

    @Test
    public void resolve_returnsConfirmedRetryingWhenFailureIsRecentAndNotOverriddenByAck() {
        TrackingSyncStatusResolver.SyncStatus status = TrackingSyncStatusResolver.resolve(
                true,
                false,
                true,
                NOW_MS - 30_000L,
                0L,
                NOW_MS
        );

        assertEquals(TrackingSyncStatusResolver.SyncStatus.CONFIRMED_RETRYING, status);
    }

    @Test
    public void resolve_returnsConfirmedRetryingAtExactRecentFailureThreshold() {
        TrackingSyncStatusResolver.SyncStatus status = TrackingSyncStatusResolver.resolve(
                true,
                false,
                true,
                NOW_MS - 90_000L,
                0L,
                NOW_MS
        );

        assertEquals(TrackingSyncStatusResolver.SyncStatus.CONFIRMED_RETRYING, status);
    }

    @Test
    public void resolve_returnsConfirmedAfterThresholdWhenAckExists() {
        TrackingSyncStatusResolver.SyncStatus status = TrackingSyncStatusResolver.resolve(
                true,
                false,
                true,
                NOW_MS - 90_001L,
                NOW_MS - 120_000L,
                NOW_MS
        );

        assertEquals(TrackingSyncStatusResolver.SyncStatus.CONFIRMED, status);
    }

    @Test
    public void resolve_returnsSendingUnconfirmedAfterThresholdWithoutAck() {
        TrackingSyncStatusResolver.SyncStatus status = TrackingSyncStatusResolver.resolve(
                true,
                false,
                true,
                NOW_MS - 90_001L,
                0L,
                NOW_MS
        );

        assertEquals(TrackingSyncStatusResolver.SyncStatus.SENDING_UNCONFIRMED, status);
    }

    @Test
    public void resolve_returnsConfirmedWhenAckExistsWithoutRecentUnresolvedFailure() {
        TrackingSyncStatusResolver.SyncStatus status = TrackingSyncStatusResolver.resolve(
                true,
                false,
                true,
                NOW_MS - 120_000L,
                NOW_MS - 20_000L,
                NOW_MS
        );

        assertEquals(TrackingSyncStatusResolver.SyncStatus.CONFIRMED, status);
    }

    @Test
    public void resolve_returnsSendingUnconfirmedWhenNoAckAndNoRecentFailure() {
        TrackingSyncStatusResolver.SyncStatus status = TrackingSyncStatusResolver.resolve(
                true,
                false,
                true,
                0L,
                0L,
                NOW_MS
        );

        assertEquals(TrackingSyncStatusResolver.SyncStatus.SENDING_UNCONFIRMED, status);
    }

    @Test
    public void hasRecentSyncFailure_returnsTrueAtExactThreshold() {
        boolean isRecentFailure = TrackingSyncStatusResolver.hasRecentSyncFailure(
                NOW_MS - 90_000L,
                0L,
                NOW_MS
        );

        assertTrue(isRecentFailure);
    }

    @Test
    public void hasRecentSyncFailure_returnsFalseAfterThresholdByOneMs() {
        boolean isRecentFailure = TrackingSyncStatusResolver.hasRecentSyncFailure(
                NOW_MS - 90_001L,
                0L,
                NOW_MS
        );

        assertFalse(isRecentFailure);
    }
}
