package com.cargohub.mobile.tracking;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.TrackingRepository;
import com.cargohub.mobile.data.model.DriverLocationUpdateRequest;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TrackingCoordinator {

    public interface Listener {
        void onTrackingStateChanged(boolean running, @NonNull String message);

        void onLocationSynced(@NonNull String recordedAt);

        void onError(@NonNull String message);
    }

    /**
     * Extended listener that also receives raw location updates for map rendering.
     */
    public interface LocationAwareListener extends Listener {
        void onLocationUpdated(@NonNull Location location);
    }

    private static final long MIN_TIME_MS = 2_000L;
    private static final float MIN_DISTANCE_METERS = 0f;

    private static final long HIGH_FREQ_TIME_MS = 2_000L;
    private static final float HIGH_FREQ_DISTANCE_METERS = 0f;

    private final Context appContext;
    private final TrackingRepository trackingRepository;
    private final LocationManager locationManager;

    private Listener listener;
    private boolean running;
    private boolean highFrequencyMode;
    private boolean backendSyncEnabled = true;
    private Long activeConductorId;
    private Long activePorteId;
    private Long activeSessionId;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (listener instanceof LocationAwareListener) {
                ((LocationAwareListener) listener).onLocationUpdated(location);
            }
            syncLocation(location);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            notifyError("Activa la ubicacion para sincronizar tu tracking.");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public TrackingCoordinator(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.trackingRepository = new TrackingRepository();
        this.locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isTripTrackable(@Nullable Porte porte) {
        if (porte == null || porte.getEstadoPorte() == null) {
            return false;
        }
        EstadoPorte estado = porte.getEstadoPorte();
        return estado == EstadoPorte.ASIGNADO || estado == EstadoPorte.EN_TRANSITO;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Enable high-frequency location updates (5s / 10m) for map tracking.
     * Must be called before {@link #start(Porte, Listener)}.
     */
    public void setHighFrequencyMode(boolean enabled) {
        this.highFrequencyMode = enabled;
    }

    public void setBackendSyncEnabled(boolean enabled) {
        this.backendSyncEnabled = enabled;
    }

    public void setActiveSessionId(@Nullable Long sessionId) {
        this.activeSessionId = sessionId;
    }

    public void start(@NonNull Porte porte, @NonNull Listener listener) {
        this.listener = listener;
        if (!SessionManager.hasActiveSession()) {
            stopInternal("Tu sesion ya no esta activa. El tracking se detuvo.");
            return;
        }
        if (!hasLocationPermission()) {
            stopInternal("Sin permiso de ubicacion. Tu viaje sigue disponible sin tracking.");
            return;
        }
        if (!isTripTrackable(porte) || porte.getId() == null) {
            stopInternal("Necesitas un porte activo para enviar ubicacion.");
            return;
        }
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            stopInternal("No encontramos tu conductor activo.");
            return;
        }

        activeConductorId = conductorId;
        activePorteId = porte.getId();
        running = true;
        notifyState(true, "Tracking listo. Esperando posicion GPS para sincronizar.");

        long timeMs = highFrequencyMode ? HIGH_FREQ_TIME_MS : MIN_TIME_MS;
        float distanceM = highFrequencyMode ? HIGH_FREQ_DISTANCE_METERS : MIN_DISTANCE_METERS;

        try {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, timeMs, distanceM, locationListener);
            }
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timeMs, distanceM, locationListener);
            }
            Location lastKnown = getLastKnownLocation();
            if (lastKnown != null) {
                locationListener.onLocationChanged(lastKnown);
            }
        } catch (Exception ex) {
            stopInternal("No pudimos iniciar el tracking en este dispositivo.");
        }
    }

    public void stop() {
        stopInternal("Tracking detenido.");
    }

    public void stopForIdleSession() {
        if (running) {
            stopInternal("No hay un viaje activo. El tracking se detuvo.");
        }
    }

    @Nullable
    public Location getLastKnownLocation() {
        try {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc != null) return loc;
            }
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) return loc;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void syncLocation(@NonNull Location location) {
        if (!running || activeConductorId == null || activePorteId == null) {
            return;
        }
        if (!SessionManager.hasActiveSession()) {
            stopInternal("La sesion expiro mientras se enviaba el tracking.");
            return;
        }

        String recordedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                Instant.ofEpochMilli(location.getTime()).atOffset(ZoneOffset.UTC)
        );
        Double speedKph = location.hasSpeed() ? location.getSpeed() * 3.6d : null;
        Integer headingDeg = location.hasBearing() ? Math.round(location.getBearing()) : null;
        if (!backendSyncEnabled) {
            if (listener != null) {
                listener.onLocationSynced(recordedAt);
            }
            return;
        }
        DriverLocationUpdateRequest request = new DriverLocationUpdateRequest(
                location.getLatitude(),
                location.getLongitude(),
                recordedAt,
                speedKph,
                headingDeg,
                activeSessionId
        );
        trackingRepository.upsertLocation(activeConductorId, request, new com.cargohub.mobile.data.RepositoryCallback<Void>() {
            @Override
            public void onResult(@NonNull RepositoryResult<Void> result) {
                if (!result.isSuccessful()) {
                    if (result.isUnauthorized()) {
                        stopInternal("La sesion expiro durante la sincronizacion.");
                    } else {
                        notifyError(result.getMessage());
                    }
                    return;
                }
                if (listener != null) {
                    listener.onLocationSynced(recordedAt);
                }
            }
        });
    }

    private void stopInternal(@NonNull String message) {
        running = false;
        activeConductorId = null;
        activePorteId = null;
        activeSessionId = null;
        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception ignored) {
        }
        notifyState(false, message);
    }

    private void notifyState(boolean enabled, @NonNull String message) {
        if (listener != null) {
            listener.onTrackingStateChanged(enabled, message);
        }
    }

    private void notifyError(@NonNull String message) {
        if (listener != null) {
            listener.onError(message);
        }
    }
}
