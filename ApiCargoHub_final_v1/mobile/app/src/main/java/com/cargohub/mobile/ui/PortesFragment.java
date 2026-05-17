package com.cargohub.mobile.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.cargohub.mobile.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PortesFragment extends Fragment {

    private static final DateTimeFormatter FILTER_DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "ES"));

    public static final String REQUEST_KEY_FILTERS = "trip_filters_request";
    public static final String RESULT_MIN_PRICE = "f_min_price";
    public static final String RESULT_MAX_PRICE = "f_max_price";
    public static final String RESULT_MIN_KM = "f_min_km";
    public static final String RESULT_MAX_KM = "f_max_km";
    public static final String RESULT_DAY = "f_day";

    private static final String[] TAB_MODES = {
            TripListFragment.MODE_UPCOMING,
            TripListFragment.MODE_HISTORY
    };

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
        return inflater.inflate(R.layout.fragment_portes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            filterMinPrice = savedInstanceState.getString(RESULT_MIN_PRICE, "");
            filterMaxPrice = savedInstanceState.getString(RESULT_MAX_PRICE, "");
            filterMinKm = savedInstanceState.getString(RESULT_MIN_KM, "");
            filterMaxKm = savedInstanceState.getString(RESULT_MAX_KM, "");
            filterDay = savedInstanceState.getString(RESULT_DAY, "");
        }

        TabLayout tabLayout = view.findViewById(R.id.portesTabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.portesViewPager);
        MaterialButton filterButton = view.findViewById(R.id.portesFilterToggleButton);

        PortesPagerAdapter adapter = new PortesPagerAdapter(this, TAB_MODES);
        viewPager.setAdapter(adapter);
        filterButton.setOnClickListener(v -> showFilterBottomSheet());

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.portes_tab_upcoming);
                    break;
                case 1:
                    tab.setText(R.string.portes_tab_history);
                    break;
            }
        }).attach();

        publishFilters();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(RESULT_MIN_PRICE, filterMinPrice);
        outState.putString(RESULT_MAX_PRICE, filterMaxPrice);
        outState.putString(RESULT_MIN_KM, filterMinKm);
        outState.putString(RESULT_MAX_KM, filterMaxKm);
        outState.putString(RESULT_DAY, filterDay);
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_trip_filters, null);
        dialog.setContentView(sheet);

        EditText minPrice = sheet.findViewById(R.id.bsTripFilterMinPrice);
        EditText maxPrice = sheet.findViewById(R.id.bsTripFilterMaxPrice);
        EditText minKm = sheet.findViewById(R.id.bsTripFilterMinKm);
        EditText maxKm = sheet.findViewById(R.id.bsTripFilterMaxKm);
        EditText day = sheet.findViewById(R.id.bsTripFilterDay);
        MaterialButton applyBtn = sheet.findViewById(R.id.bsTripFilterApplyButton);
        MaterialButton clearBtn = sheet.findViewById(R.id.bsTripFilterClearButton);

        minPrice.setText(filterMinPrice);
        maxPrice.setText(filterMaxPrice);
        minKm.setText(filterMinKm);
        maxKm.setText(filterMaxKm);
        day.setText(filterDay);
        day.setOnClickListener(v -> showDatePicker(day));

        applyBtn.setOnClickListener(v -> {
            filterMinPrice = readText(minPrice);
            filterMaxPrice = readText(maxPrice);
            filterMinKm = readText(minKm);
            filterMaxKm = readText(maxKm);
            filterDay = readText(day);
            publishFilters();
            dialog.dismiss();
        });

        clearBtn.setOnClickListener(v -> {
            filterMinPrice = "";
            filterMaxPrice = "";
            filterMinKm = "";
            filterMaxKm = "";
            filterDay = "";
            publishFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void publishFilters() {
        Bundle result = new Bundle();
        result.putString(RESULT_MIN_PRICE, filterMinPrice);
        result.putString(RESULT_MAX_PRICE, filterMaxPrice);
        result.putString(RESULT_MIN_KM, filterMinKm);
        result.putString(RESULT_MAX_KM, filterMaxKm);
        result.putString(RESULT_DAY, filterDay);
        getChildFragmentManager().setFragmentResult(REQUEST_KEY_FILTERS, result);
    }

    @NonNull
    private String readText(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void showDatePicker(@NonNull EditText target) {
        LocalDate initialDate = parseFilterDate(readText(target));
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
}
