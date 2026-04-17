package com.cargohub.mobile.ui;

import android.Manifest;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;
import com.cargohub.mobile.tracking.MotivoPausa;
import com.cargohub.mobile.tracking.OsrmRouteService;
import com.cargohub.mobile.tracking.RouteInfo;
import com.cargohub.mobile.tracking.TrackingCoordinator;
import com.cargohub.mobile.tracking.TrackingPause;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrackingStatusFragment extends Fragment {

    private static final String ARG_HINT_PORTE_ID = "hint_porte_id";
    private static final String ARG_DEST_LAT = "dest_lat";
    private static final String ARG_DEST_LON = "dest_lon";

    private final PorteRepository porteRepository = new PorteRepository();
    private TrackingCoordinator trackingCoordinator;
    private final OsrmRouteService osrmRouteService = new OsrmRouteService();
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    // Setup overlay views
    private View setupOverlay;
    private ProgressBar loadingProgress;
    private TextView statusText;
    private TextView permissionText;
    private MaterialButton permissionButton;
    private MaterialButton toggleTrackingButton;

    // Map views
    private MapView mapView;
    private MaterialCardView statsCard;
    private TextView etaValue;
    private TextView distanceRemainingValue;
    private TextView timeWorkedValue;
    private TextView distanceCoveredValue;
    private TextView speedValue;
    private TextView avgSpeedValue;
    private MaterialButton stopButton;

    // New views: play/pause, status chip, pause stats
    private FloatingActionButton playPauseFab;
    private MaterialCardView statusChipCard;
    private TextView statusChipText;
    private TextView timePausedValue;
    private TextView pauseCountValue;
    private TextView timeTotalValue;

    // Driving mode views
    private FloatingActionButton drivingModeFab;
    private FrameLayout drivingOverlayTop;
    private FrameLayout drivingOverlayBottom;
    private TextView drivingSpeedValue;
    private TextView drivingEtaValue;
    private TextView drivingStatusText;
    private TextView drivingDistanceValue;
    private FloatingActionButton drivingPlayPauseFab;
    private FloatingActionButton drivingExitFab;

    // Map overlays
    private Marker driverMarker;
    private Marker destinationMarker;
    private Polyline routePolyline;

    // State
    private Porte activeTrip;
    private Long hintedPorteId;
    private double destLat = Double.NaN;
    private double destLon = Double.NaN;

    // Stats tracking
    private long trackingStartTimeMs;
    private long totalWorkTimeMs;
    private long lastPlayResumeMs;
    private double totalDistanceMeters;
    private Location lastLocation;
    private int locationCount;
    private double totalSpeedSum;

    // Play/Pause state
    private boolean isPlaying;
    private final ArrayList<TrackingPause> pauses = new ArrayList<>();

    // Driving mode state
    private boolean isDrivingMode;
    private float lastBearing;
    private float currentSmoothedBearing;
    private RouteInfo lastRouteInfo;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::handlePermissionResult
    );

    public static TrackingStatusFragment newInstance(@Nullable Long porteId) {
        TrackingStatusFragment fragment = new TrackingStatusFragment();
        Bundle args = new Bundle();
        if (porteId != null) {
            args.putLong(ARG_HINT_PORTE_ID, porteId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static TrackingStatusFragment newInstance(@Nullable Long porteId,
                                                     double destLat, double destLon) {
        TrackingStatusFragment fragment = new TrackingStatusFragment();
        Bundle args = new Bundle();
        if (porteId != null) {
            args.putLong(ARG_HINT_PORTE_ID, porteId);
        }
        args.putDouble(ARG_DEST_LAT, destLat);
        args.putDouble(ARG_DEST_LON, destLon);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(
                requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_tracking_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_HINT_PORTE_ID)) {
                hintedPorteId = getArguments().getLong(ARG_HINT_PORTE_ID);
            }
            if (getArguments().containsKey(ARG_DEST_LAT)) {
                destLat = getArguments().getDouble(ARG_DEST_LAT);
                destLon = getArguments().getDouble(ARG_DEST_LON);
            }
        }

        trackingCoordinator = new TrackingCoordinator(requireContext());
        trackingCoordinator.setHighFrequencyMode(true);

        // Setup overlay
        setupOverlay = view.findViewById(R.id.trackingSetupOverlay);
        loadingProgress = view.findViewById(R.id.trackingLoadingProgress);
        statusText = view.findViewById(R.id.trackingStatusText);
        permissionText = view.findViewById(R.id.trackingPermissionText);
        permissionButton = view.findViewById(R.id.trackingPermissionButton);
        toggleTrackingButton = view.findViewById(R.id.trackingToggleButton);

        // Map
        mapView = view.findViewById(R.id.trackingMapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);

        // Stats card
        statsCard = view.findViewById(R.id.trackingStatsCard);
        etaValue = view.findViewById(R.id.trackingEtaValue);
        distanceRemainingValue = view.findViewById(R.id.trackingDistanceRemainingValue);
        timeWorkedValue = view.findViewById(R.id.trackingTimeWorkedValue);
        distanceCoveredValue = view.findViewById(R.id.trackingDistanceCoveredValue);
        speedValue = view.findViewById(R.id.trackingSpeedValue);
        avgSpeedValue = view.findViewById(R.id.trackingAvgSpeedValue);
        stopButton = view.findViewById(R.id.trackingStopButton);

        // Play/pause FAB
        playPauseFab = view.findViewById(R.id.trackingPlayPauseFab);

        // Status chip
        statusChipCard = view.findViewById(R.id.trackingStatusChipCard);
        statusChipText = view.findViewById(R.id.trackingStatusChipText);

        // Pause stats
        timePausedValue = view.findViewById(R.id.trackingTimePausedValue);
        pauseCountValue = view.findViewById(R.id.trackingPauseCountValue);
        timeTotalValue = view.findViewById(R.id.trackingTimeTotalValue);

        // Driving mode
        drivingModeFab = view.findViewById(R.id.drivingModeFab);
        drivingOverlayTop = view.findViewById(R.id.drivingOverlayTop);
        drivingOverlayBottom = view.findViewById(R.id.drivingOverlayBottom);
        drivingSpeedValue = view.findViewById(R.id.drivingSpeedValue);
        drivingEtaValue = view.findViewById(R.id.drivingEtaValue);
        drivingStatusText = view.findViewById(R.id.drivingStatusText);
        drivingDistanceValue = view.findViewById(R.id.drivingDistanceValue);
        drivingPlayPauseFab = view.findViewById(R.id.drivingPlayPauseFab);
        drivingExitFab = view.findViewById(R.id.drivingExitFab);

        permissionButton.setOnClickListener(v -> requestLocationPermission());
        toggleTrackingButton.setOnClickListener(v -> toggleTracking());
        playPauseFab.setOnClickListener(v -> onPlayPauseTapped());
        stopButton.setOnClickListener(v -> showFinalizeConfirmation());
        drivingModeFab.setOnClickListener(v -> toggleDrivingMode());
        drivingPlayPauseFab.setOnClickListener(v -> onPlayPauseTapped());
        drivingExitFab.setOnClickListener(v -> exitDrivingMode());

        // Show setup overlay initially
        setupOverlay.setVisibility(View.VISIBLE);
        statsCard.setVisibility(View.GONE);
        playPauseFab.setVisibility(View.GONE);
        statusChipCard.setVisibility(View.GONE);

        refreshTrackingState();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        timerHandler.removeCallbacksAndMessages(null);
        if (!SessionManager.hasActiveSession()) {
            trackingCoordinator.stopForIdleSession();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isDrivingMode) {
            exitDrivingMode();
        }
        if (mapView != null) {
            mapView.onDetach();
        }
    }

    // ── Play/Pause logic ────────────────────────────────────────────────

    private void onPlayPauseTapped() {
        if (isPlaying) {
            // User wants to PAUSE → show reason picker
            showPauseReasonPicker();
        } else {
            // User wants to RESUME (play)
            resumeTracking();
        }
    }

    private void showPauseReasonPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_pause_reason_bottom_sheet, null);
        dialog.setContentView(sheet);
        dialog.setCancelable(false);

        RecyclerView list = sheet.findViewById(R.id.pauseReasonList);
        EditText noteInput = sheet.findViewById(R.id.pauseNoteInput);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        MotivoPausa[] reasons = MotivoPausa.values();
        list.setAdapter(new RecyclerView.Adapter<PauseReasonVH>() {
            @NonNull
            @Override
            public PauseReasonVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_pause_reason, parent, false);
                return new PauseReasonVH(v);
            }

            @Override
            public void onBindViewHolder(@NonNull PauseReasonVH holder, int position) {
                MotivoPausa reason = reasons[position];
                holder.emoji.setText(reason.getEmoji());
                holder.label.setText(reason.getLabelResId());
                holder.itemView.setOnClickListener(v -> {
                    String note = noteInput != null ? noteInput.getText().toString().trim() : null;
                    if (note != null && note.isEmpty()) note = null;
                    executePause(reason, note);
                    dialog.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return reasons.length;
            }
        });

        dialog.show();
    }

    private void executePause(@NonNull MotivoPausa motivo, @Nullable String nota) {
        // Accumulate work time up to now
        long now = System.currentTimeMillis();
        if (lastPlayResumeMs > 0) {
            totalWorkTimeMs += (now - lastPlayResumeMs);
        }
        lastPlayResumeMs = 0;

        isPlaying = false;
        trackingCoordinator.stop();

        // Record pause
        TrackingPause pause = new TrackingPause(motivo, nota, now);
        pauses.add(pause);

        updatePlayPauseUI();
    }

    private void resumeTracking() {
        // Close active pause if any
        TrackingPause activePause = getActivePause();
        if (activePause != null) {
            activePause.cerrar(System.currentTimeMillis());
        }

        isPlaying = true;
        lastPlayResumeMs = System.currentTimeMillis();

        // Restart GPS via coordinator
        if (activeTrip != null) {
            startTrackingGps();
        }

        updatePlayPauseUI();
    }

    @Nullable
    private TrackingPause getActivePause() {
        if (pauses.isEmpty()) return null;
        TrackingPause last = pauses.get(pauses.size() - 1);
        return last.isActive() ? last : null;
    }

    private void updatePlayPauseUI() {
        if (isPlaying) {
            playPauseFab.setImageResource(android.R.drawable.ic_media_pause);
            playPauseFab.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ch_success, null)));
            statusChipCard.setCardBackgroundColor(getResources().getColor(R.color.ch_success, null));
            statusChipText.setText(R.string.tracking_status_on_route);
        } else {
            playPauseFab.setImageResource(android.R.drawable.ic_media_play);
            playPauseFab.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ch_warning, null)));
            statusChipCard.setCardBackgroundColor(getResources().getColor(R.color.ch_warning, null));
            TrackingPause active = getActivePause();
            if (active != null) {
                String label = getString(active.getMotivo().getLabelResId());
                statusChipText.setText(getString(R.string.tracking_status_paused, label));
            }
        }
        // Sync driving mode overlay if active
        if (isDrivingMode) {
            updateDrivingOverlayUI();
        }
    }

    // ── Pause stats ─────────────────────────────────────────────────────

    private long getTotalPausedMs() {
        long total = 0;
        for (TrackingPause p : pauses) {
            total += p.getDurationMs();
        }
        return total;
    }

    private long getCurrentWorkTimeMs() {
        long work = totalWorkTimeMs;
        if (isPlaying && lastPlayResumeMs > 0) {
            work += (System.currentTimeMillis() - lastPlayResumeMs);
        }
        return work;
    }

    private long getTotalElapsedMs() {
        if (trackingStartTimeMs == 0) return 0;
        return System.currentTimeMillis() - trackingStartTimeMs;
    }

    // ── Existing tracking lifecycle (adapted) ───────────────────────────

    private void refreshTrackingState() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            statusText.setText(R.string.tracking_status_signed_out);
            trackingCoordinator.stopForIdleSession();
            return;
        }
        loadingProgress.setVisibility(View.VISIBLE);
        porteRepository.getAssignedTrips(conductorId, this::handleTripsResult);
    }

    private void handleTripsResult(@NonNull RepositoryResult<List<Porte>> result) {
        if (!isAdded()) return;
        loadingProgress.setVisibility(View.GONE);

        if (!result.isSuccessful() || result.getData() == null) {
            Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
            return;
        }

        activeTrip = resolveActiveTrip(result.getData());

        // Resolve destination coordinates from trip if not passed via args
        if (Double.isNaN(destLat) && activeTrip != null && activeTrip.hasDestinationCoordinates()) {
            destLat = activeTrip.getDestinoLat();
            destLon = activeTrip.getDestinoLon();
        }

        if (activeTrip == null) {
            statusText.setText(R.string.tracking_status_idle);
            setupOverlay.setVisibility(View.VISIBLE);
            trackingCoordinator.stopForIdleSession();
        } else {
            renderPermissionState();
            if (trackingCoordinator.hasLocationPermission()) {
                // Show map view and wait for user to press play (no auto-start)
                showMapView();
            } else {
                setupOverlay.setVisibility(View.VISIBLE);
                statusText.setText(R.string.tracking_status_ready);
            }
        }
        toggleTrackingButton.setEnabled(activeTrip != null);
    }

    @Nullable
    private Porte resolveActiveTrip(@NonNull List<Porte> trips) {
        Porte hinted = null;
        Porte fallback = null;
        for (Porte trip : trips) {
            EstadoPorte state = trip.getEstadoPorte();
            boolean active = state == EstadoPorte.EN_TRANSITO || state == EstadoPorte.ASIGNADO;
            if (!active) continue;
            if (hintedPorteId != null && trip.getId() != null && hintedPorteId.equals(trip.getId())) {
                hinted = trip;
                break;
            }
            if (fallback == null) fallback = trip;
        }
        return hinted != null ? hinted : fallback;
    }

    private void requestLocationPermission() {
        permissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void handlePermissionResult(@NonNull Map<String, Boolean> result) {
        boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (granted && activeTrip != null) {
            showMapView();
        } else {
            statusText.setText(R.string.tracking_status_permission_denied);
            Snackbar.make(requireView(), R.string.tracking_permission_denied_feedback, Snackbar.LENGTH_LONG).show();
        }
    }

    private void toggleTracking() {
        if (trackingCoordinator.isRunning()) {
            trackingCoordinator.stop();
            showSetupOverlay(getString(R.string.tracking_status_stopped));
            return;
        }
        if (!trackingCoordinator.hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        if (activeTrip != null) {
            showMapView();
        }
    }

    /**
     * Starts GPS tracking via the coordinator. Called when user presses PLAY (resume).
     */
    private void startTrackingGps() {
        if (activeTrip == null) return;

        trackingCoordinator.start(activeTrip, new TrackingCoordinator.LocationAwareListener() {
            @Override
            public void onLocationUpdated(@NonNull Location location) {
                if (!isAdded()) return;
                if (isPlaying) {
                    handleNewLocation(location);
                }
            }

            @Override
            public void onTrackingStateChanged(boolean running, @NonNull String message) {
                if (!isAdded()) return;
                if (running) {
                    Location last = trackingCoordinator.getLastKnownLocation();
                    if (last != null) {
                        centerMapOnDriver(last.getLatitude(), last.getLongitude());
                    }
                }
            }

            @Override
            public void onLocationSynced(@NonNull String recordedAt) {
                // Backend sync confirmed
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /**
     * First-time tracking initialization — sets up timers and initial state as PAUSED.
     * The user must press Play to begin.
     */
    private void initializeTrackingSession() {
        if (trackingStartTimeMs == 0) {
            trackingStartTimeMs = System.currentTimeMillis();
        }
        totalDistanceMeters = 0;
        lastLocation = null;
        locationCount = 0;
        totalSpeedSum = 0;
        totalWorkTimeMs = 0;
        lastPlayResumeMs = 0;
        isPlaying = false;
        pauses.clear();

        startTimerUpdates();
        updatePlayPauseUI();
    }

    private void handleNewLocation(@NonNull Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        // Accumulate distance only when playing
        if (lastLocation != null) {
            totalDistanceMeters += lastLocation.distanceTo(location);
        }
        lastLocation = location;

        // Speed stats
        if (location.hasSpeed()) {
            double speedKph = location.getSpeed() * 3.6;
            locationCount++;
            totalSpeedSum += speedKph;
            speedValue.setText(String.format(Locale.US, "%.0f km/h", speedKph));
            double avgSpeed = totalSpeedSum / locationCount;
            avgSpeedValue.setText(String.format(Locale.US, "%.0f km/h", avgSpeed));
        }

        // Distance covered
        distanceCoveredValue.setText(String.format(Locale.US, "%.1f km", totalDistanceMeters / 1000.0));

        // Update driver marker
        updateDriverMarker(lat, lon);

        // Driving mode: rotate map by bearing, center with offset
        if (isDrivingMode) {
            if (location.hasBearing()) {
                float smoothed = smoothBearing(location.getBearing());
                mapView.setMapOrientation(-smoothed);
                if (driverMarker != null) {
                    driverMarker.setRotation(smoothed);
                }
            }
            centerMapOnDriverDrivingMode(lat, lon);
            updateDrivingOverlayUI();
        } else {
            centerMapOnDriver(lat, lon);
        }

        // Destination marker & route
        if (!Double.isNaN(destLat)) {
            updateDestinationMarker();
            fetchRoute(lat, lon);
            if (!isDrivingMode) {
                fitMapBounds(lat, lon);
            }
        }
    }

    private void updateDriverMarker(double lat, double lon) {
        GeoPoint driverPoint = new GeoPoint(lat, lon);
        if (driverMarker == null) {
            driverMarker = new Marker(mapView);
            driverMarker.setTitle("Tu posición");
            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(driverMarker);
        }
        driverMarker.setPosition(driverPoint);
        mapView.invalidate();
    }

    private void updateDestinationMarker() {
        if (destinationMarker != null) return;
        GeoPoint destPoint = new GeoPoint(destLat, destLon);
        destinationMarker = new Marker(mapView);
        destinationMarker.setPosition(destPoint);
        destinationMarker.setTitle("Destino");
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(destinationMarker);
        mapView.invalidate();
    }

    private void centerMapOnDriver(double lat, double lon) {
        if (Double.isNaN(destLat)) {
            mapView.getController().animateTo(new GeoPoint(lat, lon));
        }
    }

    private void fitMapBounds(double driverLat, double driverLon) {
        if (Double.isNaN(destLat)) return;
        try {
            double north = Math.max(driverLat, destLat);
            double south = Math.min(driverLat, destLat);
            double east = Math.max(driverLon, destLon);
            double west = Math.min(driverLon, destLon);
            double latPad = (north - south) * 0.2;
            double lonPad = (east - west) * 0.2;
            BoundingBox box = new BoundingBox(
                    north + latPad, east + lonPad,
                    south - latPad, west - lonPad);
            mapView.zoomToBoundingBox(box, true, 100);
        } catch (Exception ignored) {
            mapView.getController().animateTo(new GeoPoint(driverLat, driverLon));
        }
    }

    private void fetchRoute(double fromLat, double fromLon) {
        osrmRouteService.fetchRoute(fromLat, fromLon, destLat, destLon, false,
                new OsrmRouteService.Callback() {
                    @Override
                    public void onRouteReady(@NonNull RouteInfo route) {
                        if (!isAdded()) return;
                        drawRoute(route);
                        updateEta(route);
                    }

                    @Override
                    public void onRouteError(@NonNull String message) {
                        // Silent fail
                    }
                });
    }

    private void drawRoute(@NonNull RouteInfo route) {
        if (routePolyline != null) {
            mapView.getOverlays().remove(routePolyline);
        }
        routePolyline = new Polyline();
        routePolyline.setPoints(route.getGeometry());
        routePolyline.getOutlinePaint().setColor(Color.parseColor("#1E40AF"));
        routePolyline.getOutlinePaint().setStrokeWidth(8f);
        mapView.getOverlays().add(0, routePolyline);
        mapView.invalidate();
    }

    private void updateEta(@NonNull RouteInfo route) {
        lastRouteInfo = route;
        String arrival = route.getArrivalTimeFormatted();
        String eta = route.getEtaFormatted();
        etaValue.setText(String.format(Locale.US, "%s (%s)", arrival, eta));
        distanceRemainingValue.setText(route.getDistanceKmFormatted());
    }

    private void startTimerUpdates() {
        timerHandler.removeCallbacksAndMessages(null);
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                updateTimerDisplays();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateTimerDisplays() {
        // Work time: only when playing
        long workMs = getCurrentWorkTimeMs();
        timeWorkedValue.setText(formatDuration(workMs));

        // Total time since session start
        long totalMs = getTotalElapsedMs();
        timeTotalValue.setText(formatDuration(totalMs));

        // Paused time
        long pausedMs = getTotalPausedMs();
        timePausedValue.setText(formatDurationShort(pausedMs));

        // Pause count
        pauseCountValue.setText(String.valueOf(pauses.size()));
    }

    @NonNull
    private String formatDuration(long ms) {
        long seconds = (ms / 1000) % 60;
        long minutes = (ms / (1000 * 60)) % 60;
        long hours = ms / (1000 * 60 * 60);
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    @NonNull
    private String formatDurationShort(long ms) {
        long minutes = ms / (1000 * 60);
        if (minutes < 60) {
            return String.format(Locale.US, "%d min", minutes);
        }
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format(Locale.US, "%dh %02dm", hours, mins);
    }

    // ── View state management ───────────────────────────────────────────

    private void showMapView() {
        setupOverlay.setVisibility(View.GONE);
        statsCard.setVisibility(View.VISIBLE);
        mapView.setVisibility(View.VISIBLE);
        playPauseFab.setVisibility(View.VISIBLE);
        statusChipCard.setVisibility(View.VISIBLE);
        drivingModeFab.setVisibility(View.VISIBLE);

        initializeTrackingSession();
    }

    private void showSetupOverlay(@NonNull String message) {
        if (isDrivingMode) {
            exitDrivingMode();
        }
        setupOverlay.setVisibility(View.VISIBLE);
        statsCard.setVisibility(View.GONE);
        playPauseFab.setVisibility(View.GONE);
        statusChipCard.setVisibility(View.GONE);
        drivingModeFab.setVisibility(View.GONE);
        statusText.setText(message);
        timerHandler.removeCallbacksAndMessages(null);
    }

    // ── Finalize journey ────────────────────────────────────────────────

    private void showFinalizeConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.tracking_finalize_confirm_title)
                .setMessage(R.string.tracking_finalize_confirm_message)
                .setPositiveButton(R.string.tracking_finalize_confirm_yes, (d, w) -> finalizeJourney())
                .setNegativeButton(R.string.tracking_finalize_confirm_no, null)
                .show();
    }

    private void finalizeJourney() {
        // Exit driving mode if active
        if (isDrivingMode) {
            exitDrivingMode();
        }

        // If still playing, accumulate last work segment
        if (isPlaying && lastPlayResumeMs > 0) {
            totalWorkTimeMs += (System.currentTimeMillis() - lastPlayResumeMs);
            lastPlayResumeMs = 0;
        }
        isPlaying = false;

        // Close active pause
        TrackingPause activePause = getActivePause();
        if (activePause != null) {
            activePause.cerrar(System.currentTimeMillis());
        }

        trackingCoordinator.stop();
        timerHandler.removeCallbacksAndMessages(null);

        // Show summary
        String totalTime = formatDuration(getTotalElapsedMs());
        String workTime = formatDuration(totalWorkTimeMs);
        String pausedTime = formatDurationShort(getTotalPausedMs());
        String distance = String.format(Locale.US, "%.1f km", totalDistanceMeters / 1000.0);
        String avgSpeed = locationCount > 0
                ? String.format(Locale.US, "%.0f km/h", totalSpeedSum / locationCount)
                : "0 km/h";

        String body = getString(R.string.tracking_finalize_summary_body,
                totalTime, workTime, pausedTime, pauses.size(), distance, avgSpeed);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.tracking_finalize_summary_title)
                .setMessage(body)
                .setPositiveButton(android.R.string.ok, (d, w) -> navigateBack())
                .setCancelable(false)
                .show();
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            showSetupOverlay(getString(R.string.tracking_status_stopped));
        }
    }

    private void renderPermissionState() {
        boolean granted = trackingCoordinator.hasLocationPermission();
        permissionText.setText(granted
                ? getString(R.string.tracking_permission_granted)
                : getString(R.string.tracking_permission_missing));
        permissionButton.setVisibility(granted ? View.GONE : View.VISIBLE);
        toggleTrackingButton.setText(trackingCoordinator.isRunning()
                ? R.string.tracking_stop_action
                : R.string.tracking_start_action);
    }

    // ── Driving mode ──────────────────────────────────────────────────────

    private void toggleDrivingMode() {
        if (isDrivingMode) {
            exitDrivingMode();
        } else {
            enterDrivingMode();
        }
    }

    private void enterDrivingMode() {
        isDrivingMode = true;

        // Hide normal UI elements
        statsCard.setVisibility(View.GONE);
        statusChipCard.setVisibility(View.GONE);
        playPauseFab.setVisibility(View.GONE);
        stopButton.setVisibility(View.GONE);
        drivingModeFab.setVisibility(View.GONE);

        // Show driving overlays
        drivingOverlayTop.setVisibility(View.VISIBLE);
        drivingOverlayBottom.setVisibility(View.VISIBLE);

        // Immersive full-screen
        enterImmersiveMode();

        // Hide toolbar
        if (getActivity() instanceof AppCompatActivity) {
            androidx.appcompat.app.ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (ab != null) ab.hide();
        }

        // Keep screen on
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Switch driver marker to arrow
        updateDriverMarkerToDrivingMode();

        // Set map orientation for bearing-based rotation
        mapView.getController().setZoom(17.0);

        // Update driving overlay data
        updateDrivingOverlayUI();
    }

    private void exitDrivingMode() {
        isDrivingMode = false;

        // Show normal UI elements
        statsCard.setVisibility(View.VISIBLE);
        statusChipCard.setVisibility(View.VISIBLE);
        playPauseFab.setVisibility(View.VISIBLE);
        drivingModeFab.setVisibility(View.VISIBLE);

        // Hide driving overlays
        drivingOverlayTop.setVisibility(View.GONE);
        drivingOverlayBottom.setVisibility(View.GONE);

        // Exit immersive mode
        exitImmersiveMode();

        // Show toolbar
        if (getActivity() instanceof AppCompatActivity) {
            androidx.appcompat.app.ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (ab != null) ab.show();
        }

        // Remove keep-screen-on
        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Reset map orientation
        mapView.setMapOrientation(0f);
        mapView.getController().setZoom(15.0);

        // Restore default driver marker
        if (driverMarker != null) {
            driverMarker.setIcon(null); // default icon
            driverMarker.setRotation(0f);
        }

        // Re-fit bounds if we have destination
        if (lastLocation != null && !Double.isNaN(destLat)) {
            fitMapBounds(lastLocation.getLatitude(), lastLocation.getLongitude());
        }
    }

    @SuppressWarnings("deprecation")
    private void enterImmersiveMode() {
        if (getActivity() == null) return;
        Window window = getActivity().getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars()
                        | android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @SuppressWarnings("deprecation")
    private void exitImmersiveMode() {
        if (getActivity() == null) return;
        Window window = getActivity().getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.show(android.view.WindowInsets.Type.statusBars()
                        | android.view.WindowInsets.Type.navigationBars());
            }
        } else {
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void updateDriverMarkerToDrivingMode() {
        if (driverMarker == null) return;
        Drawable arrowDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_driver_arrow);
        if (arrowDrawable != null) {
            driverMarker.setIcon(arrowDrawable);
            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        }
    }

    /**
     * Smoothly interpolate between old and new bearing to avoid jumpy rotation.
     */
    private float smoothBearing(float newBearing) {
        float diff = newBearing - currentSmoothedBearing;
        // Normalize to [-180, 180]
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        // Lerp factor 0.3 for smooth transition
        currentSmoothedBearing += diff * 0.3f;
        // Normalize result
        while (currentSmoothedBearing < 0) currentSmoothedBearing += 360;
        while (currentSmoothedBearing >= 360) currentSmoothedBearing -= 360;
        return currentSmoothedBearing;
    }

    /**
     * Centers the driver position at ~70% from top for forward visibility.
     */
    private void centerMapOnDriverDrivingMode(double lat, double lon) {
        int mapHeight = mapView.getHeight();
        if (mapHeight <= 0) {
            mapView.getController().animateTo(new GeoPoint(lat, lon));
            return;
        }
        // Approximate: at current zoom, compute how many degrees correspond to 20% of screen
        // Use the projection to get the visible bounding box and derive offset
        try {
            org.osmdroid.util.BoundingBox bb = mapView.getBoundingBox();
            double latSpan = bb.getLatNorth() - bb.getLatSouth();
            double offsetDegrees = latSpan * 0.2;
            GeoPoint offsetCenter = new GeoPoint(lat + offsetDegrees, lon);
            mapView.getController().animateTo(offsetCenter);
        } catch (Exception e) {
            mapView.getController().animateTo(new GeoPoint(lat, lon));
        }
    }

    private void updateDrivingOverlayUI() {
        // Speed
        if (lastLocation != null && lastLocation.hasSpeed()) {
            int speedKph = (int) (lastLocation.getSpeed() * 3.6);
            drivingSpeedValue.setText(String.valueOf(speedKph));
        } else {
            drivingSpeedValue.setText("0");
        }

        // ETA + distance from last route info
        if (lastRouteInfo != null) {
            String arrival = lastRouteInfo.getArrivalTimeFormatted();
            String eta = lastRouteInfo.getEtaFormatted();
            drivingEtaValue.setText(String.format(Locale.US, "%s · %s", arrival, eta));
            drivingDistanceValue.setText(lastRouteInfo.getDistanceKmFormatted());
        }

        // Status chip color based on play/pause
        if (isPlaying) {
            drivingStatusText.setText(R.string.driving_mode_on_route);
            drivingStatusText.setBackgroundColor(Color.parseColor("#4016A34A")); // green tint
            drivingPlayPauseFab.setImageResource(android.R.drawable.ic_media_pause);
            drivingPlayPauseFab.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ch_success, null)));
        } else {
            drivingStatusText.setText(R.string.driving_mode_paused);
            drivingStatusText.setBackgroundColor(Color.parseColor("#40D97706")); // amber tint
            drivingPlayPauseFab.setImageResource(android.R.drawable.ic_media_play);
            drivingPlayPauseFab.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ch_warning, null)));
        }
    }

    // ── ViewHolder for pause reasons ────────────────────────────────────

    private static class PauseReasonVH extends RecyclerView.ViewHolder {
        final TextView emoji;
        final TextView label;

        PauseReasonVH(@NonNull View itemView) {
            super(itemView);
            emoji = itemView.findViewById(R.id.pauseReasonEmoji);
            label = itemView.findViewById(R.id.pauseReasonLabel);
        }
    }
}
