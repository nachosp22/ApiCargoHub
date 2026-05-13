package com.cargohub.mobile.ui;

import android.Manifest;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageManager;
import android.util.Log;
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
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.TrackingRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.data.model.PorteTrackingResponse;
import com.cargohub.mobile.data.model.RecordTrackingPauseRequest;
import com.cargohub.mobile.data.model.StartTrackingSessionRequest;
import com.cargohub.mobile.data.model.TrackingSessionResponse;
import com.cargohub.mobile.data.model.UpdateTrackingSessionRequest;
import com.cargohub.mobile.network.ConnectivityObserver;
import com.cargohub.mobile.session.SessionManager;
import com.cargohub.mobile.tracking.MotivoPausa;
import com.cargohub.mobile.tracking.OsrmRouteService;
import com.cargohub.mobile.tracking.RouteInfo;
import com.cargohub.mobile.tracking.TrackingForegroundService;
import com.cargohub.mobile.tracking.TrackingPause;
import com.cargohub.mobile.ui.FirmaEntregaFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrackingStatusFragment extends Fragment {

    private static final String ARG_HINT_PORTE_ID = "hint_porte_id";
    private static final String ARG_DEST_LAT = "dest_lat";
    private static final String ARG_DEST_LON = "dest_lon";
    private static final String TAG = "TrackingStatusFragment";
    private static final String TRACKING_STATUS_ACTIVE = "ACTIVE";
    private static final String TRACKING_STATUS_PAUSED = "PAUSED";
    private static final String TRACKING_STATUS_ENDED = "ENDED";
    private static final String TRACKING_PHASE_TO_PICKUP = "TO_PICKUP";
    private static final String TRACKING_PHASE_TO_DROPOFF = "TO_DROPOFF";
    private static final String TRACKING_PHASE_IDLE = "IDLE";

    // Default driver location: Gijón, Asturias (used when no real GPS is available)
    private static final double DEFAULT_DRIVER_LAT = 43.5322;
    private static final double DEFAULT_DRIVER_LON = -5.6611;

    private static final ITileSource ROAD_FOCUSED_TILE_SOURCE = TileSourceFactory.MAPNIK;

    private final PorteRepository porteRepository = new PorteRepository();
    private final TrackingRepository trackingRepository = new TrackingRepository();
    private final OsrmRouteService osrmRouteService = new OsrmRouteService();
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private static final String PREFS_TRACKING = "tracking_prefs";
    private static final String PREF_SESSION_ID = "tracking_session_id";
    private static final String PREF_TIMER_PORTE_ID = "tracking_timer_porte_id";
    private static final String PREF_TIMER_START_MS = "tracking_timer_start_ms";
    private static final String PREF_TIMER_WORK_MS = "tracking_timer_work_ms";
    private static final String PREF_TIMER_LAST_PLAY_MS = "tracking_timer_last_play_ms";
    private static final String PREF_TIMER_IS_PLAYING = "tracking_timer_is_playing";
    private static final String PREF_TIMER_DISTANCE_M = "tracking_timer_distance_m";
    private static final long ROUTE_REFRESH_INTERVAL_MS = 45_000L;

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
    private TextView gpsStatusValue;
    private TextView syncStatusValue;
    private MaterialButton reportIncidentButton;
    private MaterialButton stopButton;

    // New views: play/pause, status chip, pause stats
    private FloatingActionButton playPauseFab;
    private FloatingActionButton recenterFab;
    private FloatingActionButton infoFab;
    private MaterialCardView statusChipCard;
    private TextView statusChipText;
    private MaterialCardView sessionMetaCard;
    private TextView sessionStatusValue;
    private TextView sessionPhaseValue;
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
    private Polyline routeOutline;

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
    @Nullable
    private Integer backendEtaMinutes;
    private long backendEtaReceivedAtMs;
    private Long activeTrackingSessionId;
    private long lastEtaRefreshTimeMs;
    private boolean isAutoFollowEnabled = true;
    private long suppressUserGestureUntilMs;
    private boolean isNetworkOnline = true;
    private boolean isSyncBlockedByNotificationPermission;
    private boolean syncStatusReceiverRegistered;
    private boolean locationUpdateReceiverRegistered;
    private long lastConfirmedSyncAtMs;
    private long lastSyncFailureAtMs;
    @Nullable
    private String trackingSessionStatusRaw;
    @Nullable
    private String trackingSessionPhaseRaw;
    private ConnectivityObserver connectivityObserver;
    private final Handler etaRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable etaRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && activeTrip != null && activeTrip.getId() != null) {
                refreshBackendEta();
            }
            etaRefreshHandler.postDelayed(this, 60_000L);
        }
    };
    private final Handler routeRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable routeRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && lastLocation != null && !Double.isNaN(destLat) && !Double.isNaN(destLon)) {
                fetchRoute(lastLocation.getLatitude(), lastLocation.getLongitude());
            }
            routeRefreshHandler.postDelayed(this, ROUTE_REFRESH_INTERVAL_MS);
        }
    };
    private final BroadcastReceiver syncStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TrackingForegroundService.ACTION_SYNC_STATUS.equals(intent.getAction())) {
                return;
            }
            boolean success = intent.getBooleanExtra(TrackingForegroundService.EXTRA_SYNC_SUCCESS, false);
            if (success) {
                String recordedAt = intent.getStringExtra(TrackingForegroundService.EXTRA_SYNC_RECORDED_AT);
                long syncedAtMs = parseRecordedAtMillis(recordedAt);
                lastConfirmedSyncAtMs = syncedAtMs > 0 ? syncedAtMs : System.currentTimeMillis();
                if (lastConfirmedSyncAtMs >= lastSyncFailureAtMs) {
                    lastSyncFailureAtMs = 0L;
                }
            } else {
                lastSyncFailureAtMs = System.currentTimeMillis();
            }
            if (isAdded()) {
                updateNavigationStatusUI();
            }
        }
    };

    private final BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TrackingForegroundService.ACTION_LOCATION_UPDATE.equals(intent.getAction())) {
                return;
            }
            double lat = intent.getDoubleExtra(TrackingForegroundService.EXTRA_LOCATION_LAT, Double.NaN);
            double lon = intent.getDoubleExtra(TrackingForegroundService.EXTRA_LOCATION_LON, Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lon) || !isAdded()) {
                return;
            }
            Location location = new Location("");
            location.setLatitude(lat);
            location.setLongitude(lon);
            if (intent.hasExtra(TrackingForegroundService.EXTRA_LOCATION_SPEED)) {
                location.setSpeed(intent.getFloatExtra(TrackingForegroundService.EXTRA_LOCATION_SPEED, 0f));
            }
            if (intent.hasExtra(TrackingForegroundService.EXTRA_LOCATION_BEARING)) {
                location.setBearing(intent.getFloatExtra(TrackingForegroundService.EXTRA_LOCATION_BEARING, 0f));
            }
            if (intent.hasExtra(TrackingForegroundService.EXTRA_LOCATION_ACCURACY)) {
                location.setAccuracy(intent.getFloatExtra(TrackingForegroundService.EXTRA_LOCATION_ACCURACY, 0f));
            }
            if (intent.hasExtra(TrackingForegroundService.EXTRA_LOCATION_TIME)) {
                location.setTime(intent.getLongExtra(TrackingForegroundService.EXTRA_LOCATION_TIME, System.currentTimeMillis()));
            } else {
                location.setTime(System.currentTimeMillis());
            }
            if (isPlaying) {
                handleNewLocation(location);
            }
        }
    };

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::handlePermissionResult
    );

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    startTrackingAfterPermissionChecks();
                } else if (isAdded()) {
                    handleForegroundTrackingBlockedByNotificationPermission();
                    showSnackbar(R.string.tracking_notification_permission_denied, Snackbar.LENGTH_LONG);
                }
            }
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

        if (activeTrackingSessionId == null) {
            activeTrackingSessionId = readPersistedSessionId();
        }

        // Setup overlay
        setupOverlay = view.findViewById(R.id.trackingSetupOverlay);
        loadingProgress = view.findViewById(R.id.trackingLoadingProgress);
        statusText = view.findViewById(R.id.trackingStatusText);
        permissionText = view.findViewById(R.id.trackingPermissionText);
        permissionButton = view.findViewById(R.id.trackingPermissionButton);
        toggleTrackingButton = view.findViewById(R.id.trackingToggleButton);

        // Map
        mapView = view.findViewById(R.id.trackingMapView);
        mapView.setTileSource(ROAD_FOCUSED_TILE_SOURCE);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);
        mapController.setCenter(new GeoPoint(DEFAULT_DRIVER_LAT, DEFAULT_DRIVER_LON));

        // Stats card
        statsCard = view.findViewById(R.id.trackingStatsCard);
        etaValue = view.findViewById(R.id.trackingEtaValue);
        distanceRemainingValue = view.findViewById(R.id.trackingDistanceRemainingValue);
        timeWorkedValue = view.findViewById(R.id.trackingTimeWorkedValue);
        distanceCoveredValue = view.findViewById(R.id.trackingDistanceCoveredValue);
        speedValue = view.findViewById(R.id.trackingSpeedValue);
        gpsStatusValue = view.findViewById(R.id.trackingGpsStatusValue);
        syncStatusValue = view.findViewById(R.id.trackingSyncStatusValue);
        reportIncidentButton = view.findViewById(R.id.trackingReportIncidentButton);
        stopButton = view.findViewById(R.id.trackingStopButton);

        // Play/pause FAB
        playPauseFab = view.findViewById(R.id.trackingPlayPauseFab);
        recenterFab = view.findViewById(R.id.trackingRecenterFab);
        infoFab = view.findViewById(R.id.trackingInfoFab);

        // Status chip
        statusChipCard = view.findViewById(R.id.trackingStatusChipCard);
        statusChipText = view.findViewById(R.id.trackingStatusChipText);
        sessionMetaCard = view.findViewById(R.id.trackingSessionMetaCard);
        sessionStatusValue = view.findViewById(R.id.trackingSessionStatusValue);
        sessionPhaseValue = view.findViewById(R.id.trackingSessionPhaseValue);

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
        recenterFab.setOnClickListener(v -> onRecenterTapped());
        infoFab.setOnClickListener(v -> showActiveTripDetails());
        reportIncidentButton.setOnClickListener(v -> openNewIncidentFromTracking());
        stopButton.setOnClickListener(v -> showFinalizeConfirmation());
        drivingModeFab.setOnClickListener(v -> {
            // Unified navigation mode: toggle intentionally disabled.
        });
        drivingPlayPauseFab.setOnClickListener(v -> onPlayPauseTapped());
        drivingExitFab.setOnClickListener(v -> exitDrivingMode());

        // Show setup overlay initially
        setupOverlay.setVisibility(View.VISIBLE);
        statsCard.setVisibility(View.GONE);
        playPauseFab.setVisibility(View.GONE);
        statusChipCard.setVisibility(View.GONE);
        sessionMetaCard.setVisibility(View.GONE);
        recenterFab.setVisibility(View.GONE);
        reportIncidentButton.setVisibility(View.GONE);

        connectivityObserver = ConnectivityObserver.getInstance(requireContext());
        isNetworkOnline = connectivityObserver.isOnline();
        setupMapInteractionListener();
        updateNavigationStatusUI();

        refreshTrackingState();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerSyncStatusReceiver();
        registerLocationUpdateReceiver();
        if (connectivityObserver != null) {
            connectivityObserver.setListener(this::onConnectivityChanged);
            connectivityObserver.start();
            isNetworkOnline = connectivityObserver.isOnline();
            updateNavigationStatusUI();
        }
        if (mapView != null) {
            mapView.onResume();
        }
        if (isPlaying) {
            ensureTrackingSessionForResume();
        }
        etaRefreshHandler.post(etaRefreshRunnable);
        routeRefreshHandler.post(routeRefreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        persistTimerState();
        unregisterSyncStatusReceiver();
        unregisterLocationUpdateReceiver();
        if (connectivityObserver != null) {
            connectivityObserver.setListener(null);
            connectivityObserver.stop();
        }
        if (mapView != null) {
            mapView.onPause();
        }
        etaRefreshHandler.removeCallbacks(etaRefreshRunnable);
        routeRefreshHandler.removeCallbacks(routeRefreshRunnable);
    }

    @Override
    public void onStop() {
        super.onStop();
        persistTimerState();
        timerHandler.removeCallbacksAndMessages(null);
        if (!SessionManager.hasActiveSession()) {
            stopTrackingForegroundService();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        exitDrivingMode();
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
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        RecyclerView list = sheet.findViewById(R.id.pauseReasonList);
        EditText noteInput = sheet.findViewById(R.id.pauseNoteInput);
        MaterialButton cancelButton = sheet.findViewById(R.id.pauseReasonCancelButton);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> dialog.dismiss());
        }

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
        updateTrackingSession(TRACKING_STATUS_PAUSED);
        stopTrackingForegroundService();

        // Record pause locally
        TrackingPause pause = new TrackingPause(motivo, nota, now);
        pauses.add(pause);

        // Persist pause to backend
        persistPauseToBackend(motivo.name(), nota, now, 0L);

        persistTimerState();
        updatePlayPauseUI();
    }

    private void resumeTracking() {
        // Close active pause if any
        long now = System.currentTimeMillis();
        TrackingPause activePause = getActivePause();
        if (activePause != null) {
            activePause.cerrar(now);
            // Persist pause end to backend
            persistPauseEndToBackend(now);
        }

        ensureTrackingSessionForResume(() -> {
            if (activeTrip != null) {
                startTrackingAfterPermissionChecks();
            }
        });
    }

    private void persistPauseToBackend(@NonNull String motivo, @Nullable String nota, long startedAtMs, long endedAtMs) {
        if (activeTrackingSessionId == null || activeTrackingSessionId <= 0) {
            return;
        }
        RecordTrackingPauseRequest request = endedAtMs > 0
                ? new RecordTrackingPauseRequest(motivo, nota, startedAtMs, endedAtMs)
                : new RecordTrackingPauseRequest(motivo, nota, startedAtMs);
        trackingRepository.recordPause(activeTrackingSessionId, request, result -> {
            if (!result.isSuccessful()) {
                Log.w(TAG, "Failed to persist pause to backend: " + result.getMessage());
            }
        });
    }

    private void persistPauseEndToBackend(long endedAtMs) {
        if (activeTrackingSessionId == null || activeTrackingSessionId <= 0) {
            return;
        }
        RecordTrackingPauseRequest request = new RecordTrackingPauseRequest(
                "", null, endedAtMs, endedAtMs);
        trackingRepository.recordPause(activeTrackingSessionId, request, result -> {
            if (!result.isSuccessful()) {
                Log.w(TAG, "Failed to persist pause end to backend: " + result.getMessage());
            }
        });
    }

    private void startTrackingAfterPermissionChecks() {
        if (!isAdded() || activeTrip == null) {
            return;
        }
        if (hasPersistedTimerForDifferentPorte()) {
            showSnackbar(R.string.tracking_active_trip_already_running, Snackbar.LENGTH_LONG);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            handleForegroundTrackingBlockedByNotificationPermission();
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        isSyncBlockedByNotificationPermission = false;
        isPlaying = true;
        lastPlayResumeMs = System.currentTimeMillis();
        persistTimerState();
        startTrackingForegroundServiceInternal();
        updatePlayPauseUI();
    }

    private void handleForegroundTrackingBlockedByNotificationPermission() {
        isSyncBlockedByNotificationPermission = true;
        isPlaying = false;
        lastPlayResumeMs = 0;
        statusText.setText(R.string.tracking_notification_permission_required);
        setupOverlay.setVisibility(View.VISIBLE);
        updateNavigationStatusUI();
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
            playPauseFab.setContentDescription(getString(R.string.tracking_pause_action_description));
            drivingPlayPauseFab.setContentDescription(getString(R.string.tracking_pause_action_description));
            playPauseFab.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ch_success)));
            statusChipCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ch_success));
            statusChipText.setText(R.string.tracking_status_on_route);
        } else {
            playPauseFab.setImageResource(android.R.drawable.ic_media_play);
            playPauseFab.setContentDescription(getString(R.string.tracking_resume_action_description));
            drivingPlayPauseFab.setContentDescription(getString(R.string.tracking_resume_action_description));
            playPauseFab.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ch_warning)));
            statusChipCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ch_warning));
            TrackingPause active = getActivePause();
            if (active != null) {
                String label = getString(active.getMotivo().getLabelResId());
                statusChipText.setText(getString(R.string.tracking_status_paused, label));
            } else {
                statusChipText.setText(R.string.tracking_status_paused_generic);
            }
        }
        // Sync driving mode overlay if active
        if (isDrivingMode) {
            updateDrivingOverlayUI();
        }
        updateNavigationStatusUI();
        updateTrackingSessionMetaUi();
    }

    private void onConnectivityChanged(boolean online) {
        isNetworkOnline = online;
        if (isAdded()) {
            updateNavigationStatusUI();
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
            clearTrackingSessionMeta();
            stopTrackingForegroundService();
            return;
        }
        loadingProgress.setVisibility(View.VISIBLE);
        porteRepository.getAssignedTrips(conductorId, this::handleTripsResult);
    }

    private void handleTripsResult(@NonNull RepositoryResult<List<Porte>> result) {
        if (!isAdded()) return;
        loadingProgress.setVisibility(View.GONE);

        if (!result.isSuccessful() || result.getData() == null) {
            showSnackbar(result.getMessage(), R.string.generic_api_error_short, Snackbar.LENGTH_LONG);
            return;
        }

        activeTrip = resolveActiveTrip(result.getData());

        Long persistedTimerPorteId = readPersistedTimerPorteId();
        if (persistedTimerPorteId != null && hintedPorteId != null && !persistedTimerPorteId.equals(hintedPorteId)) {
            showSnackbar(R.string.tracking_active_trip_already_running, Snackbar.LENGTH_LONG);
        }

        // Resolve destination coordinates from trip if not passed via args
        if (Double.isNaN(destLat) && activeTrip != null && activeTrip.hasDestinationCoordinates()) {
            destLat = activeTrip.getDestinoLat();
            destLon = activeTrip.getDestinoLon();
        }

        if (activeTrip == null) {
            statusText.setText(R.string.tracking_status_idle);
            clearTrackingSessionMeta();
            setupOverlay.setVisibility(View.VISIBLE);
            stopTrackingForegroundService();
        } else {
            renderPermissionState();
            if (hasLocationPermission()) {
                // Show map view and wait for user to press play (no auto-start)
                showMapView();
            } else {
                setupOverlay.setVisibility(View.VISIBLE);
                statusText.setText(R.string.tracking_status_ready);
            }
        }
        toggleTrackingButton.setEnabled(activeTrip != null);
        updateReportIncidentCtaVisibility();
        updateFinalizePorteCtaVisibility();
    }

    @Nullable
    private Porte resolveActiveTrip(@NonNull List<Porte> trips) {
        Long persistedTimerPorteId = readPersistedTimerPorteId();
        Porte persisted = null;
        Porte hinted = null;
        Porte fallback = null;
        for (Porte trip : trips) {
            EstadoPorte state = trip.getEstadoPorte();
            boolean active = state == EstadoPorte.EN_TRANSITO || state == EstadoPorte.ASIGNADO;
            if (!active) continue;
            if (persistedTimerPorteId != null && trip.getId() != null && persistedTimerPorteId.equals(trip.getId())) {
                persisted = trip;
                break;
            }
            if (hintedPorteId != null && trip.getId() != null && hintedPorteId.equals(trip.getId())) {
                hinted = trip;
                break;
            }
            if (fallback == null) fallback = trip;
        }
        if (persisted != null) return persisted;
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
            showSnackbar(R.string.tracking_permission_denied_feedback, Snackbar.LENGTH_LONG);
        }
    }

    private void toggleTracking() {
        if (isPlaying) {
            updateTrackingSession(TRACKING_STATUS_PAUSED);
            stopTrackingForegroundService();
            showSetupOverlay(getString(R.string.tracking_status_stopped));
            return;
        }
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        if (activeTrip != null) {
            showMapView();
        }
    }

    private void showSnackbar(@StringRes int messageRes, int duration) {
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, messageRes, duration).show();
        }
    }

    private void showSnackbar(@Nullable String message, @StringRes int fallbackMessageRes, int duration) {
        String resolvedMessage = message != null ? message.trim() : "";
        if (resolvedMessage.isEmpty()) {
            showSnackbar(fallbackMessageRes, duration);
            return;
        }
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, resolvedMessage, duration).show();
        }
    }

    /**
     * First-time tracking initialization — sets up timers and initial state as PAUSED.
     * The user must press Play to begin.
     */
    private void initializeTrackingSession() {
        if (restoreTimerStateForActiveTrip()) {
            startTimerUpdates();
            if (isPlaying) {
                startTrackingForegroundServiceInternal();
            }
            updatePlayPauseUI();
            return;
        }

        trackingStartTimeMs = System.currentTimeMillis();
        totalDistanceMeters = 0;
        lastLocation = null;
        locationCount = 0;
        totalSpeedSum = 0;
        totalWorkTimeMs = 0;
        lastPlayResumeMs = 0;
        isPlaying = false;
        pauses.clear();

        if (activeTrackingSessionId == null) {
            createTrackingSession(TRACKING_STATUS_PAUSED);
        } else {
            updateTrackingSession(TRACKING_STATUS_PAUSED);
        }

        persistTimerState();
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
        }
        updateNavigationStatusUI();

        // Distance covered
        distanceCoveredValue.setText(String.format(Locale.US, "%.1f km", totalDistanceMeters / 1000.0));
        persistTimerState();
        updateRecenterUiState();

        // Update driver marker
        updateDriverMarker(lat, lon);

        // Rotate map by route bearing (preferred) or GPS bearing as fallback
        Float routeBearing = calculateRouteBearing(lat, lon);
        float bearing;
        if (routeBearing != null) {
            bearing = routeBearing;
        } else if (location.hasBearing()) {
            bearing = location.getBearing();
        } else {
            bearing = Float.NaN;
        }
        if (!Float.isNaN(bearing)) {
            float smoothed = smoothBearing(bearing);
            mapView.setMapOrientation(-smoothed);
            // Do NOT rotate the marker — the rotated map already orients it correctly.
            // The arrow icon points up; with map rotation it aligns with the road.
            if (driverMarker != null) {
                driverMarker.setRotation(0f);
            }
            // Always use forward-perspective centering when we have a bearing
            centerMapWithForwardPerspective(lat, lon, smoothed);
        } else {
            centerMapOnDriver(lat, lon);
        }

        // Destination marker & route
        if (!Double.isNaN(destLat)) {
            updateDestinationMarker();
            fetchRoute(lat, lon);
            // Only fit bounds when no valid bearing (e.g. stationary)
            if (Float.isNaN(bearing)) {
                fitMapBounds(lat, lon);
            }
        }
    }

    private void updateDriverMarker(double lat, double lon) {
        GeoPoint driverPoint = new GeoPoint(lat, lon);
        if (driverMarker == null) {
            driverMarker = new Marker(mapView);
            driverMarker.setTitle("Tu posición");
            applyDriverMarkerStyle();
            mapView.getOverlays().add(driverMarker);
        }
        applyDriverMarkerStyle();
        driverMarker.setPosition(driverPoint);
        if (!lastLocationHasBearing()) {
            driverMarker.setRotation(0f);
        }
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
        if (!isAutoFollowEnabled || isDrivingMode) return;
        if (Double.isNaN(destLat)) {
            animateMapTo(new GeoPoint(lat, lon));
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
            markProgrammaticCameraMove(1200L);
            mapView.zoomToBoundingBox(box, true, 100);
        } catch (Exception ignored) {
            animateMapTo(new GeoPoint(driverLat, driverLon));
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
                        if (!isAdded()) return;
                        drawStraightLineRoute(fromLat, fromLon, destLat, destLon);
                    }
                });
    }

    private void drawRoute(@NonNull RouteInfo route) {
        if (route.getGeometry() == null || route.getGeometry().isEmpty()) {
            if (routePolyline != null) {
                mapView.getOverlays().remove(routePolyline);
                routePolyline = null;
            }
            if (routeOutline != null) {
                mapView.getOverlays().remove(routeOutline);
                routeOutline = null;
            }
            mapView.invalidate();
            Log.w(TAG, "Route geometry is empty; route overlay not drawn");
            if (lastLocation != null) {
                drawStraightLineRoute(lastLocation.getLatitude(), lastLocation.getLongitude(), destLat, destLon);
            }
            return;
        }

        if (routePolyline != null) {
            mapView.getOverlays().remove(routePolyline);
        }
        if (routeOutline != null) {
            mapView.getOverlays().remove(routeOutline);
        }

        routeOutline = new Polyline();
        routeOutline.setPoints(route.getGeometry());
        routeOutline.getOutlinePaint().setColor(ContextCompat.getColor(requireContext(), R.color.white));
        routeOutline.getOutlinePaint().setStrokeWidth(18f);
        routeOutline.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        routeOutline.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);

        routePolyline = new Polyline();
        routePolyline.setPoints(route.getGeometry());
        routePolyline.getOutlinePaint().setColor(ContextCompat.getColor(requireContext(), R.color.ch_primary));
        routePolyline.getOutlinePaint().setStrokeWidth(12f);
        routePolyline.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        routePolyline.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
        mapView.getOverlays().add(0, routeOutline);
        mapView.getOverlays().add(1, routePolyline);
        mapView.invalidate();
    }

    private void updateEta(@NonNull RouteInfo route) {
        lastRouteInfo = route;
        renderEta();
        distanceRemainingValue.setText(route.getDistanceKmFormatted());
        lastEtaRefreshTimeMs = System.currentTimeMillis();
    }

    /**
     * Renders ETA display using only local OSRM route info.
     * Backend ETA is intentionally ignored because it uses a fixed 50 km/h
     * speed assumption, which overestimates duration by ~2x on highways.
     * OSRM provides real road-speed based duration.
     * Shows arrival time plus remaining duration: "09:18 ~ 5h 20min".
     * Skips UI update when text is already identical to avoid flicker.
     */
    private void renderEta() {
        if (lastRouteInfo == null) {
            return;
        }
        String text = lastRouteInfo.getArrivalTimeFormatted() + " " + formatRouteDuration(lastRouteInfo);
        // Guard: avoid redundant setText to prevent visual flicker / duplication artifacts
        CharSequence current = etaValue.getText();
        if (current != null && text.contentEquals(current)) {
            return;
        }
        etaValue.setText(text);
        if (isDrivingMode) {
            drivingEtaValue.setText(text);
        }
    }

    @NonNull
    private String formatMadridTime(long epochMillis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Madrid"));
        return sdf.format(new java.util.Date(epochMillis));
    }

    @NonNull
    private String formatDuration(int totalMinutes) {
        if (totalMinutes < 60) {
            return "~" + totalMinutes + " min";
        }
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return "~" + h + " h " + m + " min";
    }

    private void drawStraightLineRoute(double fromLat, double fromLon, double toLat, double toLon) {
        if (Double.isNaN(toLat) || Double.isNaN(toLon)) return;

        if (routePolyline != null) {
            mapView.getOverlays().remove(routePolyline);
        }
        if (routeOutline != null) {
            mapView.getOverlays().remove(routeOutline);
        }

        List<GeoPoint> straightLine = new ArrayList<>();
        straightLine.add(new GeoPoint(fromLat, fromLon));
        straightLine.add(new GeoPoint(toLat, toLon));

        routeOutline = new Polyline();
        routeOutline.setPoints(straightLine);
        routeOutline.getOutlinePaint().setColor(ContextCompat.getColor(requireContext(), R.color.white));
        routeOutline.getOutlinePaint().setStrokeWidth(18f);
        routeOutline.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        routeOutline.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);

        routePolyline = new Polyline();
        routePolyline.setPoints(straightLine);
        routePolyline.getOutlinePaint().setColor(ContextCompat.getColor(requireContext(), R.color.ch_primary));
        routePolyline.getOutlinePaint().setStrokeWidth(12f);
        routePolyline.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        routePolyline.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
        mapView.getOverlays().add(0, routeOutline);
        mapView.getOverlays().add(1, routePolyline);
        mapView.invalidate();

        // Compute straight-line distance as fallback for remaining km
        float[] results = new float[1];
        android.location.Location.distanceBetween(fromLat, fromLon, toLat, toLon, results);
        double distanceKm = results[0] / 1000.0;
        distanceRemainingValue.setText(String.format(Locale.US, "%.1f km", distanceKm));
    }

    private void refreshBackendEta() {
        if (activeTrip == null || activeTrip.getId() == null) {
            return;
        }
        porteRepository.getPorteTracking(activeTrip.getId(), result -> {
            if (!isAdded() || !result.isSuccessful() || result.getData() == null) {
                return;
            }
            PorteTrackingResponse tracking = result.getData();
            Integer etaMinutes = tracking.getEtaMinutes();
            // Ignore null, negative, or suspiciously low values (backend often
            // returns 0 before the driver has moved enough to compute a real ETA).
            if (etaMinutes == null || etaMinutes < 2) {
                return;
            }
            backendEtaMinutes = etaMinutes;
            backendEtaReceivedAtMs = System.currentTimeMillis();
            renderEta();
            if (isDrivingMode) {
                updateDrivingOverlayUI();
            }
        });
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
        updateNavigationStatusUI();
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
        if (hasPersistedTimerForDifferentPorte()) {
            showSnackbar(R.string.tracking_active_trip_already_running, Snackbar.LENGTH_LONG);
            return;
        }
        setupOverlay.setVisibility(View.GONE);
        statsCard.setVisibility(View.VISIBLE);
        mapView.setVisibility(View.VISIBLE);
        playPauseFab.setVisibility(View.VISIBLE);
        statusChipCard.setVisibility(View.VISIBLE);
        infoFab.setVisibility(View.VISIBLE);
        updateTrackingSessionMetaUi();
        drivingModeFab.setVisibility(View.GONE);
        recenterFab.setVisibility(View.GONE);
        isAutoFollowEnabled = true;

        if (!isDrivingMode) {
            enterDrivingMode();
        }

        updateReportIncidentCtaVisibility();
        updateFinalizePorteCtaVisibility();
        initializeTrackingSession();

        // Try real last known location first; fallback only if nothing is available
        if (lastLocation == null) {
            Location realLocation = getLastKnownRealLocation();
            if (realLocation != null) {
                lastLocation = realLocation;
                updateDriverMarker(lastLocation.getLatitude(), lastLocation.getLongitude());
                animateMapTo(new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));
            } else {
                updateDriverMarker(DEFAULT_DRIVER_LAT, DEFAULT_DRIVER_LON);
                animateMapTo(new GeoPoint(DEFAULT_DRIVER_LAT, DEFAULT_DRIVER_LON));
            }
        }

        // Draw route immediately if we have a destination
        if (!Double.isNaN(destLat) && !Double.isNaN(destLon)) {
            updateDestinationMarker();
            double routeFromLat = lastLocation != null ? lastLocation.getLatitude() : DEFAULT_DRIVER_LAT;
            double routeFromLon = lastLocation != null ? lastLocation.getLongitude() : DEFAULT_DRIVER_LON;
            fetchRoute(routeFromLat, routeFromLon);
        }
    }

    private void showSetupOverlay(@NonNull String message) {
        exitDrivingMode();
        setupOverlay.setVisibility(View.VISIBLE);
        statsCard.setVisibility(View.GONE);
        playPauseFab.setVisibility(View.GONE);
        statusChipCard.setVisibility(View.GONE);
        infoFab.setVisibility(View.GONE);
        sessionMetaCard.setVisibility(View.GONE);
        clearTrackingSessionMeta();
        drivingModeFab.setVisibility(View.GONE);
        recenterFab.setVisibility(View.GONE);
        if (reportIncidentButton != null) {
            reportIncidentButton.setVisibility(View.GONE);
        }
        statusText.setText(message);
        timerHandler.removeCallbacksAndMessages(null);
    }

    private void showActiveTripDetails() {
        if (activeTrip == null) {
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_porte_details, null);
        dialog.setContentView(sheet);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView origen = sheet.findViewById(R.id.bsPorteOrigen);
        TextView destino = sheet.findViewById(R.id.bsPorteDestino);
        TextView mercancia = sheet.findViewById(R.id.bsPorteMercancia);
        TextView cliente = sheet.findViewById(R.id.bsPorteCliente);
        TextView fechas = sheet.findViewById(R.id.bsPorteFechas);
        TextView precio = sheet.findViewById(R.id.bsPortePrecio);
        TextView estado = sheet.findViewById(R.id.bsPorteEstado);

        if (origen != null) {
            origen.setText(nvl(activeTrip.getOrigen(), "—"));
        }
        if (destino != null) {
            destino.setText(nvl(activeTrip.getDestino(), "—"));
        }
        if (mercancia != null) {
            mercancia.setText(nvl(activeTrip.getDescripcionMercancia(), "—"));
        }
        if (cliente != null) {
            cliente.setText(nvl(activeTrip.getDescripcionCliente(), "—"));
        }
        if (fechas != null) {
            fechas.setText(UiFormatters.formatPorteSchedule(activeTrip));
        }
        if (precio != null) {
            precio.setText(UiFormatters.formatPortePrice(activeTrip));
        }
        if (estado != null) {
            estado.setText(UiFormatters.formatPorteState(activeTrip));
        }

        dialog.show();
    }

    @NonNull
    private String nvl(@Nullable String value, @NonNull String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    private void updateReportIncidentCtaVisibility() {
        if (reportIncidentButton == null) {
            return;
        }
        reportIncidentButton.setVisibility(hasActiveTripWithId() ? View.VISIBLE : View.GONE);
    }

    private boolean hasActiveTripWithId() {
        return activeTrip != null && activeTrip.getId() != null;
    }

    private void openNewIncidentFromTracking() {
        if (!hasActiveTripWithId()) {
            updateReportIncidentCtaVisibility();
            return;
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, NuevaIncidenciaFragment.newInstance(activeTrip.getId()))
                .addToBackStack(null)
                .commit();
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
        if (activeTrip == null || activeTrip.getId() == null || activeTrip.getEstadoPorte() != EstadoPorte.EN_TRANSITO) {
            showSnackbar(R.string.generic_api_error_short, Snackbar.LENGTH_LONG);
            return;
        }
        stopButton.setEnabled(false);
        porteRepository.changeTripState(
                activeTrip.getId(),
                EstadoPorte.EN_TRANSITO,
                EstadoPorte.ENTREGADO,
                result -> {
                    if (!isAdded()) {
                        return;
                    }
                    stopButton.setEnabled(true);
                    if (!result.isSuccessful() || result.getData() == null) {
                        showSnackbar(result.getMessage(), R.string.generic_api_error_short, Snackbar.LENGTH_LONG);
                        return;
                    }
                    activeTrip = result.getData();
                    finishTrackingSessionAndShowSummary();
                }
        );
    }

    private void finishTrackingSessionAndShowSummary() {
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
        long now = System.currentTimeMillis();
        if (activePause != null) {
            activePause.cerrar(now);
            persistPauseEndToBackend(now);
        }

        stopTrackingForegroundService();
        updateTrackingSession(TRACKING_STATUS_ENDED);
        applyTrackingSessionMeta(TRACKING_STATUS_ENDED, resolveTrackingPhase());
        activeTrackingSessionId = null;
        clearPersistedSessionId();
        clearPersistedTimerState();
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
                .setPositiveButton("Firmar entrega", (d, w) -> openFirmaEntrega())
                .setNegativeButton("Volver", (d, w) -> navigateBack())
                .setCancelable(false)
                .show();
    }

    private void openFirmaEntrega() {
        if (activeTrip == null || activeTrip.getId() == null) {
            navigateBack();
            return;
        }
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, FirmaEntregaFragment.newInstance(activeTrip.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            showSetupOverlay(getString(R.string.tracking_status_stopped));
        }
    }

    private void renderPermissionState() {
        boolean granted = hasLocationPermission();
        permissionText.setText(granted
                ? getString(R.string.tracking_permission_granted)
                : getString(R.string.tracking_permission_missing));
        permissionButton.setVisibility(granted ? View.GONE : View.VISIBLE);
        toggleTrackingButton.setText(isPlaying
                ? R.string.tracking_stop_action
                : R.string.tracking_start_action);
    }

    // ── Driving mode ──────────────────────────────────────────────────────

    private void enterDrivingMode() {
        isDrivingMode = true;

        // Keep unified UI: map + bottom stats/actions.
        statsCard.setVisibility(View.VISIBLE);
        statusChipCard.setVisibility(View.VISIBLE);
        playPauseFab.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        updateFinalizePorteCtaVisibility();
        drivingModeFab.setVisibility(View.GONE);
        drivingOverlayTop.setVisibility(View.GONE);
        drivingOverlayBottom.setVisibility(View.GONE);

        // Keep screen on
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Switch driver marker to arrow
        updateDriverMarkerToDrivingMode();

        // Set map orientation for navigation-style follow
        mapView.getController().setZoom(17.0);
        recenterFab.setVisibility(View.GONE);

        // Update driving overlay data
        updateDrivingOverlayUI();
    }

    private void exitDrivingMode() {
        isDrivingMode = false;

        // Restore default map mode behavior while preserving card UI.
        statsCard.setVisibility(View.VISIBLE);
        statusChipCard.setVisibility(View.VISIBLE);
        playPauseFab.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        updateFinalizePorteCtaVisibility();
        drivingModeFab.setVisibility(View.GONE);

        // Ensure dedicated driving overlays remain hidden.
        drivingOverlayTop.setVisibility(View.GONE);
        drivingOverlayBottom.setVisibility(View.GONE);

        // Remove keep-screen-on
        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Reset map orientation
        mapView.setMapOrientation(0f);
        mapView.getController().setZoom(15.0);
        isAutoFollowEnabled = true;
        updateRecenterUiState();

        // Restore default driver marker
        if (driverMarker != null) {
            applyDriverMarkerStyle();
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
        applyDriverMarkerStyle();
    }

    private void updateFinalizePorteCtaVisibility() {
        if (stopButton == null) {
            return;
        }
        boolean canFinalizePorte = activeTrip != null && activeTrip.getEstadoPorte() == EstadoPorte.EN_TRANSITO;
        stopButton.setVisibility(canFinalizePorte ? View.VISIBLE : View.GONE);
        stopButton.setEnabled(canFinalizePorte);
    }

    private void applyDriverMarkerStyle() {
        if (driverMarker == null) return;
        Drawable arrowDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_driver_arrow);
        if (arrowDrawable != null) {
            driverMarker.setIcon(arrowDrawable);
            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        }
    }

    private boolean lastLocationHasBearing() {
        return lastLocation != null && lastLocation.hasBearing();
    }

    /**
     * Computes bearing from the closest point on the route to the next point,
     * so the map rotates following the road rather than raw GPS heading.
     */
    @Nullable
    private Float calculateRouteBearing(double lat, double lon) {
        if (lastRouteInfo == null || lastRouteInfo.getGeometry().size() < 2) {
            return null;
        }
        GeoPoint current = new GeoPoint(lat, lon);
        List<GeoPoint> geometry = lastRouteInfo.getGeometry();
        int closestIdx = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < geometry.size(); i++) {
            double d = current.distanceToAsDouble(geometry.get(i));
            if (d < minDist) {
                minDist = d;
                closestIdx = i;
            }
        }
        int nextIdx = Math.min(closestIdx + 1, geometry.size() - 1);
        if (nextIdx == closestIdx && closestIdx > 0) {
            nextIdx = closestIdx - 1;
        }
        GeoPoint from = geometry.get(closestIdx);
        GeoPoint to = geometry.get(nextIdx);
        float[] results = new float[2];
        android.location.Location.distanceBetween(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude(), results);
        return results[1];
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
     * Centers the map so the driver appears in the lower portion of the screen
     * with the road stretching ahead. Uses pixel projection so it works at any
     * map rotation and zoom level.
     */
    private void centerMapWithForwardPerspective(double lat, double lon, float bearing) {
        if (mapView.getHeight() <= 0) {
            animateMapTo(new GeoPoint(lat, lon));
            return;
        }
        try {
            Projection projection = mapView.getProjection();
            GeoPoint driverPoint = new GeoPoint(lat, lon);
            Point driverPixel = projection.toPixels(driverPoint, null);

            // Place driver at ~75% from top (25% from bottom) = 30% ahead offset
            int offsetPixels = (int) (mapView.getHeight() * 0.30);

            // Bearing 0 = North = -Y on screen. Move center "ahead" of driver.
            int aheadX = (int) (Math.sin(Math.toRadians(bearing)) * offsetPixels);
            int aheadY = (int) (-Math.cos(Math.toRadians(bearing)) * offsetPixels);

            Point centerPixel = new Point(driverPixel.x + aheadX, driverPixel.y + aheadY);
            GeoPoint centerGeo = (GeoPoint) projection.fromPixels(centerPixel.x, centerPixel.y);
            animateMapTo(centerGeo);
        } catch (Exception e) {
            animateMapTo(new GeoPoint(lat, lon));
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

        // ETA (renderEta handles backend vs OSRM preference) + distance
        renderEta();
        if (lastRouteInfo != null) {
            drivingDistanceValue.setText(lastRouteInfo.getDistanceKmFormatted());
        }

        // Status chip color based on play/pause
        if (isPlaying) {
            drivingStatusText.setText(R.string.driving_mode_on_route);
            drivingStatusText.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ch_success_chip_tint));
            drivingPlayPauseFab.setImageResource(android.R.drawable.ic_media_pause);
            drivingPlayPauseFab.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ch_success)));
        } else {
            drivingStatusText.setText(R.string.driving_mode_paused);
            drivingStatusText.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ch_warning_chip_tint));
            drivingPlayPauseFab.setImageResource(android.R.drawable.ic_media_play);
            drivingPlayPauseFab.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ch_warning)));
        }
    }

    private void setupMapInteractionListener() {
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                onUserMapInteractionDetected();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                onUserMapInteractionDetected();
                return false;
            }
        });
    }

    private void onUserMapInteractionDetected() {
        if (System.currentTimeMillis() < suppressUserGestureUntilMs) return;
        if (!isAutoFollowEnabled) return;
        isAutoFollowEnabled = false;
        updateRecenterUiState();
    }

    private void onRecenterTapped() {
        isAutoFollowEnabled = true;
        if (lastLocation != null) {
            float bearing = Float.NaN;
            Float routeBearing = calculateRouteBearing(lastLocation.getLatitude(), lastLocation.getLongitude());
            if (routeBearing != null) {
                bearing = routeBearing;
            } else if (lastLocation.hasBearing()) {
                bearing = lastLocation.getBearing();
            }
            if (!Float.isNaN(bearing)) {
                float smoothed = smoothBearing(bearing);
                mapView.setMapOrientation(-smoothed);
                if (driverMarker != null) {
                    driverMarker.setRotation(0f);
                }
                centerMapWithForwardPerspective(lastLocation.getLatitude(), lastLocation.getLongitude(), smoothed);
            } else {
                centerMapOnDriver(lastLocation.getLatitude(), lastLocation.getLongitude());
                if (!Double.isNaN(destLat)) {
                    fitMapBounds(lastLocation.getLatitude(), lastLocation.getLongitude());
                }
            }
        }
        updateRecenterUiState();
    }

    private void updateRecenterUiState() {
        if (recenterFab == null) return;
        boolean show = !isAutoFollowEnabled && lastLocation != null;
        recenterFab.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void markProgrammaticCameraMove(long durationMs) {
        suppressUserGestureUntilMs = System.currentTimeMillis() + durationMs;
    }

    private void animateMapTo(@NonNull GeoPoint point) {
        markProgrammaticCameraMove(1200L);
        mapView.getController().animateTo(point);
    }

    private void updateNavigationStatusUI() {
        if (!isAdded() || gpsStatusValue == null || syncStatusValue == null) return;
        if (lastLocation == null || isLocationStale(lastLocation)) {
            gpsStatusValue.setText(R.string.tracking_gps_searching);
            gpsStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_warning));
        } else if (lastLocation.hasAccuracy() && lastLocation.getAccuracy() > 40f) {
            gpsStatusValue.setText(R.string.tracking_gps_low_accuracy);
            gpsStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_warning));
        } else {
            gpsStatusValue.setText(R.string.tracking_gps_ready);
            gpsStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_success));
        }

        TrackingSyncStatusResolver.SyncStatus syncStatus = TrackingSyncStatusResolver.resolve(
                isPlaying,
                isSyncBlockedByNotificationPermission,
                isNetworkOnline,
                lastSyncFailureAtMs,
                lastConfirmedSyncAtMs,
                System.currentTimeMillis()
        );
        switch (syncStatus) {
            case PAUSED:
                syncStatusValue.setText(R.string.tracking_sync_paused);
                syncStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_on_surface_variant));
                break;
            case BLOCKED_NOTIFICATIONS:
                syncStatusValue.setText(R.string.tracking_sync_blocked_notifications);
                syncStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_warning));
                break;
            case DEGRADED_NO_NETWORK:
                syncStatusValue.setText(R.string.tracking_sync_degraded);
                syncStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_warning));
                break;
            case CONFIRMED_RETRYING:
                syncStatusValue.setText(R.string.tracking_sync_confirmed_retrying);
                syncStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_warning));
                break;
            case CONFIRMED:
                syncStatusValue.setText(getString(R.string.tracking_sync_confirmed_value,
                        formatSyncAge(lastConfirmedSyncAtMs)));
                syncStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_success));
                break;
            case SENDING_UNCONFIRMED:
            default:
                syncStatusValue.setText(R.string.tracking_sync_sending_unconfirmed);
                syncStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_on_surface_variant));
                break;
        }
    }

    @NonNull
    private String formatSyncAge(long syncedAtMs) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - syncedAtMs) / 1_000L);
        if (seconds < 60) {
            return getString(R.string.tracking_sync_seconds_ago, seconds);
        }
        long minutes = seconds / 60;
        return getString(R.string.tracking_sync_minutes_ago, minutes);
    }

    private long parseRecordedAtMillis(@Nullable String recordedAt) {
        if (recordedAt == null || recordedAt.trim().isEmpty()) {
            return 0L;
        }
        try {
            return OffsetDateTime.parse(recordedAt).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            try {
                return Instant.parse(recordedAt).toEpochMilli();
            } catch (Exception ignoredAgain) {
                return 0L;
            }
        }
    }

    private void registerSyncStatusReceiver() {
        if (!isAdded() || syncStatusReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(TrackingForegroundService.ACTION_SYNC_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(syncStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(syncStatusReceiver, filter);
        }
        syncStatusReceiverRegistered = true;
    }

    private void unregisterSyncStatusReceiver() {
        if (!isAdded() || !syncStatusReceiverRegistered) {
            return;
        }
        try {
            requireContext().unregisterReceiver(syncStatusReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        syncStatusReceiverRegistered = false;
    }

    private void registerLocationUpdateReceiver() {
        if (!isAdded() || locationUpdateReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(TrackingForegroundService.ACTION_LOCATION_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(locationUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(locationUpdateReceiver, filter);
        }
        locationUpdateReceiverRegistered = true;
    }

    private void unregisterLocationUpdateReceiver() {
        if (!isAdded() || !locationUpdateReceiverRegistered) {
            return;
        }
        try {
            requireContext().unregisterReceiver(locationUpdateReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        locationUpdateReceiverRegistered = false;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    private Location getLastKnownRealLocation() {
        try {
            LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return null;
            Location best = null;
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (gps != null) best = gps;
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (network != null && (best == null || network.getTime() > best.getTime())) {
                    best = network;
                }
            }
            return best;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void persistSessionId(@Nullable Long sessionId) {
        if (!isAdded()) return;
        requireContext().getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_SESSION_ID, sessionId != null ? sessionId : -1L)
                .apply();
    }

    @Nullable
    private Long readPersistedSessionId() {
        if (!isAdded()) return null;
        long value = requireContext().getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE)
                .getLong(PREF_SESSION_ID, -1L);
        return value > 0 ? value : null;
    }

    private void clearPersistedSessionId() {
        if (!isAdded()) return;
        requireContext().getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_SESSION_ID)
                .apply();
    }

    private void persistTimerState() {
        if (!isAdded() || activeTrip == null || activeTrip.getId() == null) return;
        requireContext().getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_TIMER_PORTE_ID, activeTrip.getId())
                .putLong(PREF_TIMER_START_MS, trackingStartTimeMs)
                .putLong(PREF_TIMER_WORK_MS, totalWorkTimeMs)
                .putLong(PREF_TIMER_LAST_PLAY_MS, lastPlayResumeMs)
                .putBoolean(PREF_TIMER_IS_PLAYING, isPlaying)
                .putFloat(PREF_TIMER_DISTANCE_M, (float) totalDistanceMeters)
                .apply();
    }

    @Nullable
    private Long readPersistedTimerPorteId() {
        if (!isAdded()) return null;
        long value = requireContext().getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE)
                .getLong(PREF_TIMER_PORTE_ID, -1L);
        return value > 0 ? value : null;
    }

    private boolean hasPersistedTimerForDifferentPorte() {
        if (activeTrip == null || activeTrip.getId() == null) return false;
        Long persistedPorteId = readPersistedTimerPorteId();
        return persistedPorteId != null && !persistedPorteId.equals(activeTrip.getId());
    }

    private boolean restoreTimerStateForActiveTrip() {
        if (!isAdded() || activeTrip == null || activeTrip.getId() == null) return false;
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE);
        long storedPorteId = prefs.getLong(PREF_TIMER_PORTE_ID, -1L);
        if (storedPorteId != activeTrip.getId()) {
            return false;
        }
        long storedStartMs = prefs.getLong(PREF_TIMER_START_MS, 0L);
        if (storedStartMs <= 0L) {
            return false;
        }
        trackingStartTimeMs = storedStartMs;
        totalWorkTimeMs = Math.max(0L, prefs.getLong(PREF_TIMER_WORK_MS, 0L));
        lastPlayResumeMs = Math.max(0L, prefs.getLong(PREF_TIMER_LAST_PLAY_MS, 0L));
        isPlaying = prefs.getBoolean(PREF_TIMER_IS_PLAYING, false);
        totalDistanceMeters = Math.max(0f, prefs.getFloat(PREF_TIMER_DISTANCE_M, 0f));
        locationCount = 0;
        totalSpeedSum = 0;
        pauses.clear();
        return true;
    }

    private void clearPersistedTimerState() {
        if (!isAdded()) return;
        requireContext().getSharedPreferences(PREFS_TRACKING, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_TIMER_PORTE_ID)
                .remove(PREF_TIMER_START_MS)
                .remove(PREF_TIMER_WORK_MS)
                .remove(PREF_TIMER_LAST_PLAY_MS)
                .remove(PREF_TIMER_IS_PLAYING)
                .remove(PREF_TIMER_DISTANCE_M)
                .apply();
    }

    private boolean isLocationStale(@NonNull Location location) {
        return (System.currentTimeMillis() - location.getTime()) > 30_000L;
    }

    private void startTrackingForegroundServiceInternal() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), TrackingForegroundService.class);
        intent.setAction(TrackingForegroundService.ACTION_START);
        if (activeTrackingSessionId != null) {
            intent.putExtra(TrackingForegroundService.EXTRA_SESSION_ID, activeTrackingSessionId);
        }
        ContextCompat.startForegroundService(requireContext(), intent);
    }

    private void stopTrackingForegroundService() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), TrackingForegroundService.class);
        intent.setAction(TrackingForegroundService.ACTION_STOP);
        requireContext().startService(intent);
    }

    private void ensureTrackingSessionForResume() {
        ensureTrackingSessionForResume(null);
    }

    private void ensureTrackingSessionForResume(@Nullable Runnable onReady) {
        if (activeTrackingSessionId == null) {
            createTrackingSession(TRACKING_STATUS_ACTIVE, onReady);
            return;
        }
        updateTrackingSession(TRACKING_STATUS_ACTIVE, onReady);
    }

    private void createTrackingSession(@NonNull String status) {
        createTrackingSession(status, null);
    }

    private void createTrackingSession(@NonNull String status, @Nullable Runnable onComplete) {
        Long driverId = SessionManager.resolveConductorId();
        Long porteId = activeTrip != null ? activeTrip.getId() : null;
        StartTrackingSessionRequest request = new StartTrackingSessionRequest(
                driverId,
                porteId,
                status,
                resolveTrackingPhase()
        );
        applyTrackingSessionMeta(status, request.getCurrentPhase());
        trackingRepository.startTrackingSession(request, result -> {
            if (!result.isSuccessful() || result.getData() == null || result.getData().getId() == null) {
                Log.w(TAG, "Tracking session start failed. Continuing without backend session.");
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }
            TrackingSessionResponse response = result.getData();
            applyTrackingSessionMetaFromResponse(response, status, request.getCurrentPhase());
            activeTrackingSessionId = response.getId();
            persistSessionId(activeTrackingSessionId);
            propagateSessionIdToTrackingPipelines();
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void updateTrackingSession(@NonNull String status) {
        updateTrackingSession(status, null);
    }

    private void updateTrackingSession(@NonNull String status, @Nullable Runnable onComplete) {
        Long sessionId = activeTrackingSessionId;
        if (sessionId == null || sessionId <= 0) {
            Log.w(TAG, "Tracking session update skipped: no active session id");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        UpdateTrackingSessionRequest request = new UpdateTrackingSessionRequest(status, resolveTrackingPhase());
        applyTrackingSessionMeta(status, request.getCurrentPhase());
        trackingRepository.updateTrackingSession(sessionId, request, result -> {
            if (!result.isSuccessful()) {
                Log.w(TAG, "Tracking session update failed. Degrading gracefully.");
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }
            TrackingSessionResponse response = result.getData();
            applyTrackingSessionMetaFromResponse(response, status, request.getCurrentPhase());
            if (TRACKING_STATUS_ENDED.equals(status)) {
                activeTrackingSessionId = null;
                clearPersistedSessionId();
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }
            if (response != null && response.getId() != null) {
                activeTrackingSessionId = response.getId();
                persistSessionId(activeTrackingSessionId);
                propagateSessionIdToTrackingPipelines();
            }
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    @NonNull
    private String formatRouteDuration(@NonNull RouteInfo route) {
        int totalMinutes = (int) Math.ceil(route.getDurationSeconds() / 60.0);
        if (totalMinutes < 60) {
            return "~ " + totalMinutes + "min";
        }
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return "~ " + hours + "h " + minutes + "min";
    }

    @NonNull
    private String resolveTrackingPhase() {
        EstadoPorte state = activeTrip != null ? activeTrip.getEstadoPorte() : null;
        if (state == null) {
            return TRACKING_PHASE_IDLE;
        }
        switch (state) {
            case EN_TRANSITO:
                return TRACKING_PHASE_TO_DROPOFF;
            case PENDIENTE:
            case ASIGNADO:
                return TRACKING_PHASE_TO_PICKUP;
            case ENTREGADO:
            case FACTURADO:
            default:
                return TRACKING_PHASE_IDLE;
        }
    }

    private void propagateSessionIdToTrackingPipelines() {
        if (!isPlaying || activeTrackingSessionId == null || !isAdded()) {
            return;
        }
        // If service was already started without sessionId, send ACTION_START again with the id.
        // Service handles this as an in-place session propagation.
        startTrackingForegroundServiceInternal();
    }

    private void applyTrackingSessionMetaFromResponse(@Nullable TrackingSessionResponse response,
                                                      @NonNull String fallbackStatus,
                                                      @Nullable String fallbackPhase) {
        if (response == null) {
            applyTrackingSessionMeta(fallbackStatus, fallbackPhase);
            return;
        }
        String resolvedStatus = response.getStatus() != null ? response.getStatus() : fallbackStatus;
        String resolvedPhase = response.getPhase() != null ? response.getPhase() : fallbackPhase;
        applyTrackingSessionMeta(resolvedStatus, resolvedPhase);
    }

    private void applyTrackingSessionMeta(@Nullable String rawStatus, @Nullable String rawPhase) {
        trackingSessionStatusRaw = rawStatus;
        trackingSessionPhaseRaw = rawPhase;
        updateTrackingSessionMetaUi();
    }

    private void clearTrackingSessionMeta() {
        trackingSessionStatusRaw = null;
        trackingSessionPhaseRaw = null;
        if (sessionMetaCard != null) {
            sessionMetaCard.setContentDescription(null);
            sessionMetaCard.setVisibility(View.GONE);
        }
    }

    private void updateTrackingSessionMetaUi() {
        if (sessionMetaCard != null) {
            sessionMetaCard.setVisibility(View.GONE);
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
