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
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class TripListFragment extends Fragment {

    private static final String ARG_MODE = "trip_mode";
    public static final String MODE_ACTIVE = "active";
    public static final String MODE_UPCOMING = "upcoming";
    public static final String MODE_HISTORY = "history";

    private final PorteRepository porteRepository = new PorteRepository();

    private String mode = MODE_ACTIVE;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayout loadingContainer;
    private LinearLayout emptyContainer;
    private LinearLayout errorContainer;
    private TextView headingText;
    private TextView subtitleText;
    private TextView emptyText;
    private TextView errorMessage;
    private PorteCardAdapter adapter;

    public static TripListFragment newInstance(@NonNull String mode) {
        TripListFragment fragment = new TripListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            mode = getArguments().getString(ARG_MODE, MODE_ACTIVE);
        }
        swipeRefreshLayout = view.findViewById(R.id.tripSwipeRefresh);
        recyclerView = view.findViewById(R.id.tripRecyclerView);
        loadingContainer = view.findViewById(R.id.tripLoadingContainer);
        emptyContainer = view.findViewById(R.id.tripEmptyContainer);
        errorContainer = view.findViewById(R.id.tripErrorContainer);
        headingText = view.findViewById(R.id.tripHeadingText);
        subtitleText = view.findViewById(R.id.tripSubtitleText);
        emptyText = view.findViewById(R.id.tripEmptyText);
        errorMessage = view.findViewById(R.id.tripErrorMessage);
        MaterialButton retryButton = view.findViewById(R.id.tripRetryButton);
        retryButton.setOnClickListener(v -> loadTrips());

        configureModeTexts();
        adapter = new PorteCardAdapter(this::openTripDetail);
        adapter.setCtaLabel(getString(R.string.trip_list_default_cta));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setColorSchemeResources(R.color.ch_accent_primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadTrips);
        loadTrips();
    }

    private void configureModeTexts() {
        if (MODE_ACTIVE.equals(mode)) {
            headingText.setText(R.string.home_menu_section_trip_active);
            subtitleText.setText(R.string.trip_active_subtitle);
            emptyText.setText(R.string.trip_empty_active);
        } else if (MODE_UPCOMING.equals(mode)) {
            headingText.setText(R.string.home_menu_section_trip_upcoming);
            subtitleText.setText(R.string.trip_upcoming_subtitle);
            emptyText.setText(R.string.trip_empty_upcoming);
        } else {
            headingText.setText(R.string.home_menu_section_trip_history);
            subtitleText.setText(R.string.trip_history_subtitle);
            emptyText.setText(R.string.trip_empty_history);
        }
    }

    private void loadTrips() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showError(getString(R.string.incidencia_error_sesion));
            return;
        }
        showLoading();
        porteRepository.getAssignedTrips(conductorId, this::handleTripsResult);
    }

    private void handleTripsResult(@NonNull RepositoryResult<List<Porte>> result) {
        if (!isAdded()) {
            return;
        }
        swipeRefreshLayout.setRefreshing(false);
        if (!result.isSuccessful()) {
            showError(result.getMessage());
            return;
        }
        List<Porte> filteredTrips = filterTrips(result.getData());
        if (filteredTrips.isEmpty()) {
            showEmpty();
            return;
        }
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.setPortes(filteredTrips);
    }

    @NonNull
    private List<Porte> filterTrips(@Nullable List<Porte> allTrips) {
        List<Porte> filtered = new ArrayList<>();
        if (allTrips == null) {
            return filtered;
        }
        for (Porte porte : allTrips) {
            EstadoPorte state = porte.getEstadoPorte();
            if (MODE_ACTIVE.equals(mode) && (state == EstadoPorte.ASIGNADO || state == EstadoPorte.EN_TRANSITO)) {
                filtered.add(porte);
            } else if (MODE_UPCOMING.equals(mode) && state == EstadoPorte.ASIGNADO) {
                filtered.add(porte);
            } else if (MODE_HISTORY.equals(mode) && state == EstadoPorte.ENTREGADO) {
                filtered.add(porte);
            }
        }
        return filtered;
    }

    private void openTripDetail(@NonNull Porte porte) {
        if (porte.getId() == null) {
            return;
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, TripDetailFragment.newInstance(porte.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showEmpty() {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showError(@NonNull String message) {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        if (message.trim().isEmpty()) {
            errorMessage.setText(R.string.trip_list_state_error_title);
        } else {
            errorMessage.setText(message);
        }
    }
}
