package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;
import com.cargohub.mobile.util.PorteDateParser;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FacturacionDashboardFragment extends Fragment {

    private static final double IVA_RATE = 1.21d;
    private static final double CONDUCTOR_SHARE = 0.90d;

    private final PorteRepository porteRepository = new PorteRepository();
    private TextView generatedValueText;
    private TextView forecastValueText;
    private TextView yearGeneratedValueText;
    private AutoCompleteTextView monthFilterInput;
    private AutoCompleteTextView yearFilterInput;

    private final List<Porte> cachedTrips = new ArrayList<>();
    private final List<Integer> availableYears = new ArrayList<>();
    private boolean isFilterBinding = false;
    private int selectedYear;
    private int selectedMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_facturacion_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        generatedValueText = view.findViewById(R.id.billingGeneratedValueText);
        forecastValueText = view.findViewById(R.id.billingForecastValueText);
        yearGeneratedValueText = view.findViewById(R.id.billingYearGeneratedValueText);
        monthFilterInput = view.findViewById(R.id.billingMonthFilterInput);
        yearFilterInput = view.findViewById(R.id.billingYearFilterInput);

        TextView nominasLink = view.findViewById(R.id.billingNominasLink);
        nominasLink.setOnClickListener(v -> Toast.makeText(
                requireContext(),
                R.string.billing_nominas_placeholder,
                Toast.LENGTH_SHORT
        ).show());

        loadBillingKpis();
    }

    private void loadBillingKpis() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            bindFallback();
            return;
        }
        porteRepository.getAssignedTrips(conductorId, this::handleTripsResult);
    }

    private void handleTripsResult(@NonNull RepositoryResult<List<Porte>> result) {
        if (!isAdded() || getView() == null) {
            return;
        }
        if (!result.isSuccessful() || result.getData() == null) {
            bindFallback();
            return;
        }

        cachedTrips.clear();
        cachedTrips.addAll(result.getData());
        bindFilters(cachedTrips);
        applyFiltersAndRender();
    }

    private void bindFilters(@NonNull List<Porte> trips) {
        Set<Integer> yearSet = new LinkedHashSet<>();
        for (Porte porte : trips) {
            LocalDate date = PorteDateParser.resolveTripDate(porte);
            if (date != null) {
                yearSet.add(date.getYear());
            }
        }
        yearSet.add(LocalDate.now().getYear());

        availableYears.clear();
        availableYears.addAll(yearSet);
        availableYears.sort(Comparator.reverseOrder());

        if (availableYears.isEmpty()) {
            availableYears.add(LocalDate.now().getYear());
        }

        if (selectedYear == 0) {
            selectedYear = YearMonth.now().getYear();
        }
        if (!availableYears.contains(selectedYear)) {
            selectedYear = availableYears.get(0);
        }
        if (selectedMonth == 0) {
            selectedMonth = YearMonth.now().getMonthValue();
        }

        List<String> monthLabels = buildMonthLabels();
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                monthLabels
        );
        monthFilterInput.setAdapter(monthAdapter);
        monthFilterInput.setOnItemClickListener((parent, view, position, id) -> {
            if (isFilterBinding) {
                return;
            }
            selectedMonth = position + 1;
            applyFiltersAndRender();
        });

        List<String> yearLabels = new ArrayList<>();
        for (Integer year : availableYears) {
            yearLabels.add(String.valueOf(year));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                yearLabels
        );
        yearFilterInput.setAdapter(yearAdapter);
        yearFilterInput.setOnItemClickListener((parent, view, position, id) -> {
            if (isFilterBinding) {
                return;
            }
            selectedYear = availableYears.get(position);
            applyFiltersAndRender();
        });

        isFilterBinding = true;
        monthFilterInput.setText(monthLabels.get(selectedMonth - 1), false);
        yearFilterInput.setText(String.valueOf(selectedYear), false);
        isFilterBinding = false;
    }

    private void applyFiltersAndRender() {
        YearMonth selectedYearMonth = YearMonth.of(selectedYear, selectedMonth);

        BillingKpis monthKpis = BillingKpis.fromTrips(cachedTrips, selectedYearMonth);
        double yearGenerated = BillingKpis.generatedYear(cachedTrips, selectedYear);
        generatedValueText.setText(formatCurrency(monthKpis.generatedMonth));
        forecastValueText.setText(formatCurrency(monthKpis.forecastMonth));
        yearGeneratedValueText.setText(formatCurrency(yearGenerated));
    }

    private void bindFallback() {
        generatedValueText.setText("—");
        forecastValueText.setText("—");
        yearGeneratedValueText.setText("—");
    }

    @NonNull
    private List<String> buildMonthLabels() {
        List<String> labels = new ArrayList<>();
        Locale locale = Locale.forLanguageTag("es");
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(2026, month);
            String monthLabel = ym.getMonth().getDisplayName(TextStyle.FULL, locale);
            labels.add(capitalize(monthLabel));
        }
        return labels;
    }

    @NonNull
    private String capitalize(@NonNull String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.forLanguageTag("es")) + value.substring(1);
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.forLanguageTag("es"), "%,.2f €", amount);
    }

    private static double conductorAmountFromGross(double grossAmount) {
        return (grossAmount / IVA_RATE) * CONDUCTOR_SHARE;
    }

    private static final class BillingKpis {
        final double generatedMonth;
        final double forecastMonth;
        private BillingKpis(double generatedMonth,
                            double forecastMonth) {
            this.generatedMonth = generatedMonth;
            this.forecastMonth = forecastMonth;
        }

        @NonNull
        static BillingKpis fromTrips(@NonNull List<Porte> trips, @NonNull YearMonth month) {
            double generated = 0d;
            double pending = 0d;

            for (Porte porte : trips) {
                if (!isSameMonth(porte, month)) {
                    continue;
                }
                double amount = conductorAmountFromGross(porte.getPrecio() != null ? porte.getPrecio() : 0d);
                EstadoPorte state = porte.getEstadoPorte();
                if (state == EstadoPorte.ENTREGADO || state == EstadoPorte.FACTURADO) {
                    generated += amount;
                } else if (state == EstadoPorte.ASIGNADO || state == EstadoPorte.EN_TRANSITO || state == EstadoPorte.PENDIENTE) {
                    pending += amount;
                }
            }

            return new BillingKpis(generated, generated + pending);
        }

        static double generatedYear(@NonNull List<Porte> trips, int targetYear) {
            double generated = 0d;
            for (Porte porte : trips) {
                LocalDate tripDate = resolveTripDate(porte);
                if (tripDate == null || tripDate.getYear() != targetYear) {
                    continue;
                }
                EstadoPorte state = porte.getEstadoPorte();
                if (state == EstadoPorte.ENTREGADO || state == EstadoPorte.FACTURADO) {
                    generated += conductorAmountFromGross(porte.getPrecio() != null ? porte.getPrecio() : 0d);
                }
            }
            return generated;
        }

        private static boolean isSameMonth(@NonNull Porte porte, @NonNull YearMonth target) {
            LocalDate tripDate = resolveTripDate(porte);
            return tripDate != null && YearMonth.from(tripDate).equals(target);
        }

        @Nullable
        private static LocalDate resolveTripDate(@NonNull Porte porte) {
            return PorteDateParser.resolveTripDate(porte);
        }
    }
}
