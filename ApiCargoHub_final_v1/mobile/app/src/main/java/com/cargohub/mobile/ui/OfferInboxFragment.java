package com.cargohub.mobile.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OfferInboxFragment extends Fragment {

    public static final String REQUEST_KEY_REFRESH = "offer_refresh_request";
    public static final String RESULT_KEY_REFRESH = "refresh";
    private static final DateTimeFormatter FILTER_DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "ES"));

    private final PorteRepository porteRepository = new PorteRepository();

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayout loadingContainer;
    private LinearLayout emptyContainer;
    private LinearLayout errorContainer;
    private TextView emptyMessage;
    private TextView errorMessage;
    private PorteCardAdapter adapter;
    private final List<Porte> allOffers = new ArrayList<>();

    // Filter state
    private String filterMinPrice = "";
    private String filterMaxPrice = "";
    private String filterMinKm = "";
    private String filterMaxKm = "";
    private String filterDay = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offer_inbox, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout = view.findViewById(R.id.offerSwipeRefresh);
        recyclerView = view.findViewById(R.id.offerRecyclerView);
        loadingContainer = view.findViewById(R.id.offerLoadingContainer);
        emptyContainer = view.findViewById(R.id.offerEmptyContainer);
        errorContainer = view.findViewById(R.id.offerErrorContainer);
        emptyMessage = view.findViewById(R.id.offerEmptyMessage);
        errorMessage = view.findViewById(R.id.offerErrorMessage);
        MaterialButton filterToggleButton = view.findViewById(R.id.offerFilterToggleButton);
        MaterialButton retryButton = view.findViewById(R.id.offerRetryButton);

        filterToggleButton.setOnClickListener(v -> showFilterBottomSheet());
        retryButton.setOnClickListener(v -> loadOffers());

        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_REFRESH, getViewLifecycleOwner(), (requestKey, result) -> {
            if (result.getBoolean(RESULT_KEY_REFRESH, false)) {
                loadOffers();
            }
        });

        if (savedInstanceState != null) {
            filterMinPrice = savedInstanceState.getString("f_min_price", "");
            filterMaxPrice = savedInstanceState.getString("f_max_price", "");
            filterMinKm = savedInstanceState.getString("f_min_km", "");
            filterMaxKm = savedInstanceState.getString("f_max_km", "");
            filterDay = savedInstanceState.getString("f_day", "");
        }

        adapter = new PorteCardAdapter(this::openOfferDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.ch_accent_primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadOffers);
        loadOffers();
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_offer_filters, null);
        dialog.setContentView(sheet);

        EditText minPrice = sheet.findViewById(R.id.bsFilterMinPrice);
        EditText maxPrice = sheet.findViewById(R.id.bsFilterMaxPrice);
        EditText minKm = sheet.findViewById(R.id.bsFilterMinKm);
        EditText maxKm = sheet.findViewById(R.id.bsFilterMaxKm);
        EditText day = sheet.findViewById(R.id.bsFilterDay);
        MaterialButton applyBtn = sheet.findViewById(R.id.bsFilterApplyButton);
        MaterialButton clearBtn = sheet.findViewById(R.id.bsFilterClearButton);

        minPrice.setText(filterMinPrice);
        maxPrice.setText(filterMaxPrice);
        minKm.setText(filterMinKm);
        maxKm.setText(filterMaxKm);
        day.setText(filterDay);
        day.setOnClickListener(v -> showDatePicker(day));

        applyBtn.setOnClickListener(v -> {
            filterMinPrice = getTextValue(minPrice);
            filterMaxPrice = getTextValue(maxPrice);
            filterMinKm = getTextValue(minKm);
            filterMaxKm = getTextValue(maxKm);
            filterDay = getTextValue(day);
            applyCurrentFilter();
            dialog.dismiss();
        });

        clearBtn.setOnClickListener(v -> {
            filterMinPrice = "";
            filterMaxPrice = "";
            filterMinKm = "";
            filterMaxKm = "";
            filterDay = "";
            minPrice.setText("");
            maxPrice.setText("");
            minKm.setText("");
            maxKm.setText("");
            day.setText("");
            applyCurrentFilter();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadOffers() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showError(getString(R.string.incidencia_error_sesion));
            return;
        }
        showLoading();
        porteRepository.getOffers(conductorId, this::handleOffersResult);
    }

    private void handleOffersResult(@NonNull RepositoryResult<List<Porte>> result) {
        if (!isAdded()) {
            return;
        }
        swipeRefreshLayout.setRefreshing(false);
        if (!result.isSuccessful()) {
            showError(result.getMessage());
            return;
        }
        List<Porte> offers = result.getData();
        allOffers.clear();
        if (offers != null) {
            allOffers.addAll(offers);
        }
        applyCurrentFilter();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("f_min_price", filterMinPrice);
        outState.putString("f_max_price", filterMaxPrice);
        outState.putString("f_min_km", filterMinKm);
        outState.putString("f_max_km", filterMaxKm);
        outState.putString("f_day", filterDay);
    }

    private void applyCurrentFilter() {
        List<Porte> filteredOffers = filterOffers(allOffers);
        if (filteredOffers.isEmpty()) {
            showEmpty();
            adapter.setPortes(filteredOffers);
            return;
        }
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.setPortes(filteredOffers);
    }

    @NonNull
    private List<Porte> filterOffers(@NonNull List<Porte> source) {
        Double minPrice = parseDecimal(filterMinPrice);
        Double maxPrice = parseDecimal(filterMaxPrice);
        Double minKm = parseDecimal(filterMinKm);
        Double maxKm = parseDecimal(filterMaxKm);
        String dayFilter = normalizeDayFilter(filterDay);

        List<Porte> filtered = new ArrayList<>();
        for (Porte porte : source) {
            if (!matchesDecimalRange(porte.getPrecio(), minPrice, maxPrice)) continue;
            if (!matchesDecimalRange(porte.getDistanciaKm(), minKm, maxKm)) continue;
            if (!matchesDay(porte, dayFilter)) continue;
            filtered.add(porte);
        }
        return filtered;
    }

    private void showEmpty() {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyMessage.setText(hasAnyFilter() ? R.string.offer_list_empty_filtered : R.string.offer_list_empty_all);
    }

    @NonNull
    private String getTextValue(@Nullable EditText editText) {
        return editText == null || editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void showDatePicker(@NonNull EditText target) {
        LocalDate initialDate = parseFilterDate(getTextValue(target));
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> target.setText(LocalDate.of(year, month + 1, dayOfMonth).format(FILTER_DAY_FORMATTER)),
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );
        dialog.show();
    }

    @NonNull
    private LocalDate parseFilterDate(@NonNull String value) {
        if (!value.trim().isEmpty()) {
            try {
                return LocalDate.parse(value.trim(), FILTER_DAY_FORMATTER);
            } catch (Exception ignored) {
            }
        }
        return LocalDate.now();
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

    private boolean hasAnyFilter() {
        return !filterMinPrice.isEmpty() || !filterMaxPrice.isEmpty()
                || !filterMinKm.isEmpty() || !filterMaxKm.isEmpty()
                || !filterDay.isEmpty();
    }

    private void openOfferDetail(@NonNull Porte porte) {
        if (porte.getId() == null) {
            showSnackbar(R.string.offer_missing_id, Snackbar.LENGTH_SHORT);
            return;
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, OfferDetailFragment.newInstance(porte.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showError(@NonNull String message) {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        if (message.trim().isEmpty()) {
            errorMessage.setText(R.string.offer_list_state_error_body);
        } else {
            errorMessage.setText(message);
        }
    }

    private void showSnackbar(@StringRes int messageRes, int duration) {
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, messageRes, duration).show();
        }
    }
}
