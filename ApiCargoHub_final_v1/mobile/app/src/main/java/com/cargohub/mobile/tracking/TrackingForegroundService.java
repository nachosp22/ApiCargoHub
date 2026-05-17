package com.cargohub.mobile.tracking;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;

import java.util.List;

public class TrackingForegroundService extends Service {

    public static final String ACTION_START = "com.cargohub.mobile.tracking.action.START";
    public static final String ACTION_STOP = "com.cargohub.mobile.tracking.action.STOP";
    public static final String ACTION_SYNC_STATUS = "com.cargohub.mobile.tracking.action.SYNC_STATUS";
    public static final String ACTION_LOCATION_UPDATE = "com.cargohub.mobile.tracking.action.LOCATION_UPDATE";
    public static final String EXTRA_SESSION_ID = "extra_tracking_session_id";
    public static final String EXTRA_SYNC_RECORDED_AT = "extra_sync_recorded_at";
    public static final String EXTRA_SYNC_SUCCESS = "extra_sync_success";
    public static final String EXTRA_LOCATION_LAT = "extra_location_lat";
    public static final String EXTRA_LOCATION_LON = "extra_location_lon";
    public static final String EXTRA_LOCATION_SPEED = "extra_location_speed";
    public static final String EXTRA_LOCATION_BEARING = "extra_location_bearing";
    public static final String EXTRA_LOCATION_ACCURACY = "extra_location_accuracy";
    public static final String EXTRA_LOCATION_TIME = "extra_location_time";

    private static final String CHANNEL_ID = "tracking_foreground_channel";
    private static final int NOTIFICATION_ID = 10101;
    private static final String PREFS_TRACKING = "tracking_prefs";
    private static final String PREF_SESSION_ID = "tracking_session_id";

    private final PorteRepository porteRepository = new PorteRepository();
    private TrackingCoordinator trackingCoordinator;
    private Long activeSessionId;
    private boolean isStopping;
    @Nullable
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        trackingCoordinator = new TrackingCoordinator(getApplicationContext());
        trackingCoordinator.setHighFrequencyMode(true);
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopTrackingAndSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && intent.hasExtra(EXTRA_SESSION_ID)) {
            activeSessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L);
            if (activeSessionId != null && activeSessionId <= 0) {
                activeSessionId = null;
            } else {
                persistSessionId(activeSessionId);
            }
        }
        if (activeSessionId == null) {
            activeSessionId = readPersistedSessionId();
        }

        if (trackingCoordinator != null && trackingCoordinator.isRunning()) {
            // Service already running: propagate late-arriving sessionId without restarting tracking.
            applyTrackingSession();
            return START_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        startTrackingIfPossible();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (!isStopping && trackingCoordinator != null) {
            trackingCoordinator.stop();
        }
        releaseWakeLock();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startTrackingIfPossible() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            stopTrackingAndSelf();
            return;
        }
        if (!trackingCoordinator.hasLocationPermission()) {
            stopTrackingAndSelf();
            return;
        }

        porteRepository.getAssignedTrips(conductorId, this::handleTripsResult);
    }

    private void handleTripsResult(@NonNull RepositoryResult<List<Porte>> result) {
        if (!result.isSuccessful() || result.getData() == null) {
            stopTrackingAndSelf();
            return;
        }

        Porte activeTrip = resolveActiveTrip(result.getData());
        if (activeTrip == null) {
            stopTrackingAndSelf();
            return;
        }

        applyTrackingSession();
        trackingCoordinator.start(activeTrip, new TrackingCoordinator.LocationAwareListener() {
            @Override
            public void onLocationUpdated(@NonNull Location location) {
                publishLocationUpdate(location);
            }

            @Override
            public void onTrackingStateChanged(boolean running, @NonNull String message) {
                if (!running) {
                    stopTrackingAndSelf();
                }
            }

            @Override
            public void onLocationSynced(@NonNull String recordedAt) {
                publishSyncStatus(recordedAt, true);
            }

            @Override
            public void onError(@NonNull String message) {
                // Keep service alive on transient failures; coordinator handles auth stop.
                publishSyncStatus(null, false);
            }
        });
    }

    private void publishSyncStatus(@Nullable String recordedAt, boolean success) {
        Intent statusIntent = new Intent(ACTION_SYNC_STATUS);
        statusIntent.setPackage(getPackageName());
        statusIntent.putExtra(EXTRA_SYNC_SUCCESS, success);
        if (recordedAt != null) {
            statusIntent.putExtra(EXTRA_SYNC_RECORDED_AT, recordedAt);
        }
        sendBroadcast(statusIntent);
    }

    private void publishLocationUpdate(@NonNull Location location) {
        Intent intent = new Intent(ACTION_LOCATION_UPDATE);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_LOCATION_LAT, location.getLatitude());
        intent.putExtra(EXTRA_LOCATION_LON, location.getLongitude());
        if (location.hasSpeed()) {
            intent.putExtra(EXTRA_LOCATION_SPEED, location.getSpeed());
        }
        if (location.hasBearing()) {
            intent.putExtra(EXTRA_LOCATION_BEARING, location.getBearing());
        }
        if (location.hasAccuracy()) {
            intent.putExtra(EXTRA_LOCATION_ACCURACY, location.getAccuracy());
        }
        intent.putExtra(EXTRA_LOCATION_TIME, location.getTime());
        sendBroadcast(intent);
    }

    @Nullable
    private Porte resolveActiveTrip(@NonNull List<Porte> trips) {
        for (Porte trip : trips) {
            if (trip.getEstadoPorte() == EstadoPorte.EN_TRANSITO || trip.getEstadoPorte() == EstadoPorte.ASIGNADO) {
                return trip;
            }
        }
        return null;
    }

    private void stopTrackingAndSelf() {
        if (isStopping) {
            return;
        }
        isStopping = true;
        if (trackingCoordinator != null) {
            trackingCoordinator.stop();
        }
        activeSessionId = null;
        clearPersistedSessionId();
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    private void applyTrackingSession() {
        if (trackingCoordinator != null) {
            trackingCoordinator.setBackendSyncEnabled(true);
            trackingCoordinator.setActiveSessionId(activeSessionId);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cargohub_logo)
                .setContentTitle(getString(R.string.tracking_notification_title))
                .setContentText(getString(R.string.tracking_notification_text))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = ContextCompat.getSystemService(this, NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tracking_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.tracking_notification_channel_desc));
        manager.createNotificationChannel(channel);
    }

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ApiCargoHub::TrackingWakeLock");
                wakeLock.acquire(10 * 60 * 1000L);
            }
        } catch (Exception ignored) {
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {
        }
        wakeLock = null;
    }

    private void persistSessionId(@Nullable Long sessionId) {
        getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_SESSION_ID, sessionId != null ? sessionId : -1L)
                .apply();
    }

    @Nullable
    private Long readPersistedSessionId() {
        long value = getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE)
                .getLong(PREF_SESSION_ID, -1L);
        return value > 0 ? value : null;
    }

    private void clearPersistedSessionId() {
        getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_SESSION_ID)
                .apply();
    }
}
