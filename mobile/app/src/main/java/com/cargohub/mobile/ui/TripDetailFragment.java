package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.IncidenciaRepository;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.IncidenciaResponse;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class TripDetailFragment extends Fragment {

    private static final String ARG_PORTE_ID = "porte_id";

    private final PorteRepository porteRepository = new PorteRepository();
    private final IncidenciaRepository incidenciaRepository = new IncidenciaRepository();

    private long porteId;
    private Porte currentPorte;

    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout loadingContainer;
    private LinearLayout emptyContainer;
    private LinearLayout errorContainer;
    private LinearLayout contentContainer;
    private TextView titleText;
    private TextView stateText;
    private TextView scheduleText;
    private TextView cargoText;
    private TextView priceText;
    private TextView nextActionHintText;
    private TextView trackingHintText;
    private TextView incidentsEmptyText;
    private TextView emptyMessage;
    private TextView errorMessage;
    private RecyclerView incidentsRecyclerView;
    private IncidenciaAdapter incidenciaAdapter;
    private MaterialButton primaryActionButton;
    private MaterialButton secondaryActionButton;
    private MaterialButton reportIncidentButton;
    private MaterialButton openTrackingButton;

    public static TripDetailFragment newInstance(long porteId) {
        TripDetailFragment fragment = new TripDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PORTE_ID, porteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        porteId = requireArguments().getLong(ARG_PORTE_ID);
        swipeRefreshLayout = view.findViewById(R.id.tripDetailSwipeRefresh);
        loadingContainer = view.findViewById(R.id.tripDetailLoadingContainer);
        emptyContainer = view.findViewById(R.id.tripDetailEmptyContainer);
        errorContainer = view.findViewById(R.id.tripDetailErrorContainer);
        contentContainer = view.findViewById(R.id.tripDetailContentContainer);
        titleText = view.findViewById(R.id.tripDetailTitleText);
        stateText = view.findViewById(R.id.tripDetailStateText);
        scheduleText = view.findViewById(R.id.tripDetailScheduleText);
        cargoText = view.findViewById(R.id.tripDetailCargoText);
        priceText = view.findViewById(R.id.tripDetailPriceText);
        nextActionHintText = view.findViewById(R.id.tripDetailActionHintText);
        trackingHintText = view.findViewById(R.id.tripDetailTrackingHintText);
        incidentsEmptyText = view.findViewById(R.id.tripIncidentsEmptyText);
        emptyMessage = view.findViewById(R.id.tripDetailEmptyText);
        errorMessage = view.findViewById(R.id.tripDetailErrorMessage);
        incidentsRecyclerView = view.findViewById(R.id.tripIncidentsRecyclerView);
        primaryActionButton = view.findViewById(R.id.tripPrimaryActionButton);
        secondaryActionButton = view.findViewById(R.id.tripSecondaryActionButton);
        reportIncidentButton = view.findViewById(R.id.tripReportIncidentButton);
        openTrackingButton = view.findViewById(R.id.tripOpenTrackingButton);
        MaterialButton retryButton = view.findViewById(R.id.tripDetailRetryButton);

        incidenciaAdapter = new IncidenciaAdapter();
        incidenciaAdapter.setOnItemClickListener(this::openIncidenciaDetail);
        incidentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        incidentsRecyclerView.setAdapter(incidenciaAdapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.ch_accent_primary);
        swipeRefreshLayout.setOnRefreshListener(this::refreshAll);
        reportIncidentButton.setOnClickListener(v -> openNewIncident());
        openTrackingButton.setOnClickListener(v -> openTracking());
        retryButton.setOnClickListener(v -> refreshAll());
        refreshAll();
    }

    private void refreshAll() {
        showLoading();
        porteRepository.getPorteDetail(porteId, result -> {
            if (!isAdded()) {
                return;
            }
            swipeRefreshLayout.setRefreshing(false);
            if (!result.isSuccessful() || result.getData() == null) {
                if (!result.isSuccessful()) {
                    showError(result.getMessage());
                    return;
                }
                showEmpty(getString(R.string.trip_detail_state_empty_body));
                return;
            }
            currentPorte = result.getData();
            renderTrip(currentPorte);
            showContent();
            loadTripIncidents();
        });
    }

    private void renderTrip(@NonNull Porte porte) {
        titleText.setText(UiFormatters.formatPorteTitle(porte));
        stateText.setText(UiFormatters.formatPorteState(porte));
        scheduleText.setText(UiFormatters.formatPorteSchedule(porte));
        cargoText.setText(UiFormatters.formatPorteCargo(porte));
        priceText.setText(UiFormatters.formatPortePrice(porte));
        trackingHintText.setText(isTrackable(porte)
                ? getString(R.string.trip_tracking_ready_hint)
                : getString(R.string.trip_tracking_idle_hint));
        renderActions(porte);
    }

    private void renderActions(@NonNull Porte porte) {
        List<PorteRepository.PorteAction> actions = porteRepository.getAvailableActions(porte);
        if (actions.isEmpty()) {
            nextActionHintText.setText(R.string.trip_no_actions_hint);
            primaryActionButton.setVisibility(View.GONE);
            secondaryActionButton.setVisibility(View.GONE);
            return;
        }
        PorteRepository.PorteAction primaryAction = actions.get(0);
        nextActionHintText.setText(getActionHint(primaryAction));
        bindAction(primaryActionButton, primaryAction);
        primaryActionButton.setVisibility(View.VISIBLE);
        if (actions.size() > 1) {
            bindAction(secondaryActionButton, actions.get(1));
            secondaryActionButton.setVisibility(View.VISIBLE);
        } else {
            secondaryActionButton.setVisibility(View.GONE);
        }
    }

    private void bindAction(@NonNull MaterialButton button, @NonNull PorteRepository.PorteAction action) {
        button.setText(getActionLabel(action));
        button.setOnClickListener(v -> executeAction(action));
    }

    private void executeAction(@NonNull PorteRepository.PorteAction action) {
        if (currentPorte == null || currentPorte.getId() == null) {
            return;
        }
        if (action == PorteRepository.PorteAction.ACCEPT_OFFER) {
            Long conductorId = SessionManager.resolveConductorId();
            if (conductorId == null || conductorId <= 0) {
                Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
                return;
            }
            setActionsEnabled(false);
            porteRepository.acceptOffer(currentPorte.getId(), conductorId, this::handleTripMutationResult);
            return;
        }

        EstadoPorte currentState = currentPorte.getEstadoPorte();
        EstadoPorte nextState = action == PorteRepository.PorteAction.START_TRIP
                ? EstadoPorte.EN_TRANSITO
                : EstadoPorte.ENTREGADO;
        if (currentState == null) {
            return;
        }
        setActionsEnabled(false);
        porteRepository.changeTripState(currentPorte.getId(), currentState, nextState, this::handleTripMutationResult);
    }

    private void handleTripMutationResult(@NonNull RepositoryResult<Porte> result) {
        if (!isAdded()) {
            return;
        }
        setActionsEnabled(true);
        if (!result.isSuccessful() || result.getData() == null) {
            Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
            return;
        }
        currentPorte = result.getData();
        renderTrip(currentPorte);
        Snackbar.make(requireView(), R.string.trip_action_success, Snackbar.LENGTH_LONG).show();
    }

    private void loadTripIncidents() {
        if (currentPorte == null || currentPorte.getId() == null) {
            incidentsEmptyText.setVisibility(View.VISIBLE);
            incidentsRecyclerView.setVisibility(View.GONE);
            return;
        }
        incidenciaRepository.getPorPorte(currentPorte.getId(), new IncidenciaRepository.IncidenciasCallback() {
            @Override
            public void onSuccess(@NonNull List<IncidenciaResponse> incidencias) {
                if (!isAdded()) {
                    return;
                }
                if (incidencias.isEmpty()) {
                    incidentsEmptyText.setVisibility(View.VISIBLE);
                    incidentsRecyclerView.setVisibility(View.GONE);
                    return;
                }
                incidentsEmptyText.setVisibility(View.GONE);
                incidentsRecyclerView.setVisibility(View.VISIBLE);
                incidenciaAdapter.setIncidencias(limitIncidencias(incidencias));
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                incidentsEmptyText.setVisibility(View.VISIBLE);
                incidentsEmptyText.setText(message);
                incidentsRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    @NonNull
    private List<IncidenciaResponse> limitIncidencias(@NonNull List<IncidenciaResponse> source) {
        if (source.size() <= 3) {
            return source;
        }
        return new ArrayList<>(source.subList(0, 3));
    }

    private void openNewIncident() {
        if (currentPorte == null || currentPorte.getId() == null) {
            return;
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, NuevaIncidenciaFragment.newInstance(currentPorte.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void openIncidenciaDetail(@NonNull IncidenciaResponse incidencia) {
        if (incidencia.getId() == null) {
            return;
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, IncidenciaDetailFragment.newInstance(incidencia.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void openTracking() {
        TrackingStatusFragment fragment;
        if (currentPorte != null && currentPorte.hasDestinationCoordinates()) {
            fragment = TrackingStatusFragment.newInstance(
                    currentPorte.getId(),
                    currentPorte.getDestinoLat(),
                    currentPorte.getDestinoLon());
        } else {
            fragment = TrackingStatusFragment.newInstance(
                    currentPorte != null ? currentPorte.getId() : null);
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private boolean isTrackable(@Nullable Porte porte) {
        EstadoPorte state = porte != null ? porte.getEstadoPorte() : null;
        return state == EstadoPorte.ASIGNADO || state == EstadoPorte.EN_TRANSITO;
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
    }

    private void showEmpty(@NonNull String message) {
        loadingContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        emptyMessage.setText(message);
    }

    private void showError(@NonNull String message) {
        loadingContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        if (message.trim().isEmpty()) {
            errorMessage.setText(R.string.trip_detail_state_error_title);
        } else {
            errorMessage.setText(message);
        }
    }

    private void setActionsEnabled(boolean enabled) {
        primaryActionButton.setEnabled(enabled);
        secondaryActionButton.setEnabled(enabled);
    }

    private int getActionLabel(@NonNull PorteRepository.PorteAction action) {
        switch (action) {
            case ACCEPT_OFFER:
                return R.string.offer_accept_action;
            case START_TRIP:
                return R.string.trip_action_start;
            case COMPLETE_TRIP:
            default:
                return R.string.trip_action_complete;
        }
    }

    private int getActionHint(@NonNull PorteRepository.PorteAction action) {
        switch (action) {
            case ACCEPT_OFFER:
                return R.string.trip_action_hint_accept;
            case START_TRIP:
                return R.string.trip_action_hint_start;
            case COMPLETE_TRIP:
            default:
                return R.string.trip_action_hint_complete;
        }
    }
}
