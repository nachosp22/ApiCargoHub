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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TripListFragment extends Fragment {

    private static final String ARG_MODE = "trip_mode";
    public static final String REQUEST_KEY_TRIP_MUTATED = "trip_mutated";
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
    private TextView emptyText;
    private TextView errorMessage;
    private PorteCardAdapter adapter;
    private final List<Porte> allTrips = new ArrayList<>();

    private String filterMinPrice = "";
    private String filterMaxPrice = "";
    private String filterMinKm = "";
    private String filterMaxKm = "";
    private String filterDay = "";

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
        emptyText = view.findViewById(R.id.tripEmptyText);
        errorMessage = view.findViewById(R.id.tripErrorMessage);
        MaterialButton retryButton = view.findViewById(R.id.tripRetryButton);
        retryButton.setOnClickListener(v -> loadTrips());

        configureModeTexts();
        adapter = new PorteCardAdapter(this::openTripDetail);

        getParentFragmentManager().setFragmentResultListener(
                PortesFragment.REQUEST_KEY_FILTERS,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    filterMinPrice = result.getString(PortesFragment.RESULT_MIN_PRICE, "");
                    filterMaxPrice = result.getString(PortesFragment.RESULT_MAX_PRICE, "");
                    filterMinKm = result.getString(PortesFragment.RESULT_MIN_KM, "");
                    filterMaxKm = result.getString(PortesFragment.RESULT_MAX_KM, "");
                    filterDay = result.getString(PortesFragment.RESULT_DAY, "");
                    applyCurrentFilter();
                });
        getParentFragmentManager().setFragmentResultListener(
                REQUEST_KEY_TRIP_MUTATED,
                getViewLifecycleOwner(),
                (requestKey, result) -> loadTrips());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setColorSchemeResources(R.color.ch_accent_primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadTrips);
        loadTrips();
    }

    private void configureModeTexts() {
        if (MODE_ACTIVE.equals(mode)) {
            emptyText.setText(R.string.trip_empty_active);
        } else if (MODE_UPCOMING.equals(mode)) {
            emptyText.setText(R.string.trip_empty_upcoming);
        } else {
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
        allTrips.clear();
        if (result.getData() != null) {
            allTrips.addAll(result.getData());
        }
        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        List<Porte> filteredTrips = filterTrips(allTrips);
        if (filteredTrips.isEmpty()) {
            showEmpty();
            adapter.setPortes(filteredTrips);
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
        Double minPrice = parseDecimal(filterMinPrice);
        Double maxPrice = parseDecimal(filterMaxPrice);
        Double minKm = parseDecimal(filterMinKm);
        Double maxKm = parseDecimal(filterMaxKm);
        String dayFilter = normalizeDayFilter(filterDay);
        if (allTrips == null) {
            return filtered;
        }
        for (Porte porte : allTrips) {
            EstadoPorte state = porte.getEstadoPorte();
            if (!matchesDecimalRange(porte.getPrecio(), minPrice, maxPrice)) continue;
            if (!matchesDecimalRange(porte.getDistanciaKm(), minKm, maxKm)) continue;
            if (!matchesDay(porte, dayFilter)) continue;
            if (MODE_ACTIVE.equals(mode) && (state == EstadoPorte.EN_RECOGIDA || state == EstadoPorte.EN_TRANSITO)) {
                filtered.add(porte);
            } else if (MODE_UPCOMING.equals(mode) && state == EstadoPorte.ASIGNADO) {
                filtered.add(porte);
            } else if (MODE_HISTORY.equals(mode) && (state == EstadoPorte.ENTREGADO || state == EstadoPorte.FACTURADO)) {
                filtered.add(porte);
            }
        }
        sortTrips(filtered);
        return filtered;
    }

    private void sortTrips(@NonNull List<Porte> trips) {
        Comparator<Porte> comparator = Comparator.comparingLong(this::sortTimestamp);
        if (MODE_HISTORY.equals(mode)) {
            comparator = comparator.reversed();
        }
        Collections.sort(trips, comparator);
    }

    private long sortTimestamp(@NonNull Porte porte) {
        String rawDate = MODE_HISTORY.equals(mode) ? porte.getFechaEntrega() : porte.getFechaRecogida();
        Long parsed = parseTimestamp(rawDate);
        if (parsed != null) {
            return parsed;
        }
        parsed = parseTimestamp(porte.getFechaRecogida());
        if (parsed != null) {
            return parsed;
        }
        return MODE_HISTORY.equals(mode) ? Long.MIN_VALUE : Long.MAX_VALUE;
    }

    @Nullable
    private Long parseTimestamp(@Nullable String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }
        String value = rawValue.trim();
        try {
            return OffsetDateTime.parse(value).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void openTripDetail(@NonNull Porte porte) {
        if (porte.getId() == null) {
            return;
        }
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, TripDetailFragment.newInstance(porte.getId()))
                .addToBackStack(null)
                .commit();
    }

    @Nullable
    private Double parseDecimal(@NonNull String value) {
        String clean = value.replace(',', '.');
        if (clean.isEmpty()) return null;
        try { return Double.parseDouble(clean); } catch (NumberFormatException ignored) { return null; }
    }

    private boolean matchesDecimalRange(@Nullable Double value, @Nullable Double min, @Nullable Double max) {
        if (min == null && max == null) return true;
        if (value == null) return false;
        return (min == null || value >= min) && (max == null || value <= max);
    }

    private boolean matchesDay(@NonNull Porte porte, @NonNull String dayFilter) {
        if (dayFilter.isEmpty()) return true;
        return UiFormatters.normalizeDateFilterValue(porte.getFechaRecogida()).contains(dayFilter)
                || UiFormatters.normalizeDateFilterValue(porte.getFechaEntrega()).contains(dayFilter);
    }

    @NonNull
    private String normalizeDayFilter(@Nullable String value) {
        if (value == null) return "";
        return value.trim().replace('-', '/').toLowerCase(Locale.ROOT);
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
