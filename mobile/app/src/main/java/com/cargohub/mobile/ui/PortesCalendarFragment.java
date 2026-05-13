package com.cargohub.mobile.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;
import com.cargohub.mobile.util.PorteDateParser;
import com.google.android.material.button.MaterialButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PortesCalendarFragment extends Fragment {

    private final PorteRepository porteRepository = new PorteRepository();
    private final List<Porte> allTrips = new ArrayList<>();
    private final Map<LocalDate, List<Porte>> tripsByDay = new HashMap<>();
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate selectedDay = LocalDate.now();

    private TextView monthText;
    private TextView statsText;
    private TextView selectedDayText;
    private GridLayout calendarGrid;
    private PorteCardAdapter adapter;
    private MaterialButton previousMonthButton;
    private MaterialButton nextMonthButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_portes_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        monthText = view.findViewById(R.id.portesCalendarMonthText);
        statsText = view.findViewById(R.id.portesCalendarStatsText);
        selectedDayText = view.findViewById(R.id.portesCalendarSelectedDayText);
        calendarGrid = view.findViewById(R.id.portesCalendarGrid);
        previousMonthButton = view.findViewById(R.id.portesCalendarPrevButton);
        nextMonthButton = view.findViewById(R.id.portesCalendarNextButton);
        RecyclerView recyclerView = view.findViewById(R.id.portesCalendarRecyclerView);

        adapter = new PorteCardAdapter(this::openTripDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        previousMonthButton.setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            selectedDay = resolveDefaultDayForMonth(currentMonth);
            renderCalendar();
            renderSelectedDay();
        });
        nextMonthButton.setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            selectedDay = resolveDefaultDayForMonth(currentMonth);
            renderCalendar();
            renderSelectedDay();
        });

        loadTrips();
    }

    private void loadTrips() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            renderCalendar();
            renderSelectedDay();
            return;
        }
        porteRepository.getAssignedTrips(conductorId, this::handleTripsResult);
    }

    private void handleTripsResult(@NonNull RepositoryResult<List<Porte>> result) {
        if (!isAdded()) {
            return;
        }
        allTrips.clear();
        tripsByDay.clear();
        if (result.isSuccessful() && result.getData() != null) {
            allTrips.addAll(result.getData());
            for (Porte trip : allTrips) {
                LocalDate day = PorteDateParser.resolveTripDate(trip);
                if (day == null) {
                    continue;
                }
                List<Porte> dayTrips = tripsByDay.get(day);
                if (dayTrips == null) {
                    dayTrips = new ArrayList<>();
                    tripsByDay.put(day, dayTrips);
                }
                dayTrips.add(trip);
            }
        }
        selectedDay = resolveDefaultDayForMonth(currentMonth);
        renderCalendar();
        renderSelectedDay();
    }

    private void renderCalendar() {
        monthText.setText(capitalize(currentMonth.format(monthFormatter)));
        int markedDays = 0;
        for (LocalDate day : tripsByDay.keySet()) {
            if (YearMonth.from(day).equals(currentMonth)) {
                markedDays++;
            }
        }
        statsText.setText(getString(R.string.portes_calendar_days_with_trips, markedDays));

        calendarGrid.removeAllViews();
        addWeekHeaders();

        LocalDate firstDay = currentMonth.atDay(1);
        int leadingEmptyCells = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        int cellsCount = leadingEmptyCells + currentMonth.lengthOfMonth();
        int totalCells = ((cellsCount + 6) / 7) * 7;

        for (int i = 0; i < totalCells; i++) {
            if (i < leadingEmptyCells || i >= leadingEmptyCells + currentMonth.lengthOfMonth()) {
                calendarGrid.addView(createDayCell("") );
                continue;
            }
            int dayOfMonth = i - leadingEmptyCells + 1;
            LocalDate date = currentMonth.atDay(dayOfMonth);
            TextView dayView = createDayCell(String.valueOf(dayOfMonth));
            styleDayCell(dayView, date);
            dayView.setOnClickListener(v -> {
                selectedDay = date;
                renderCalendar();
                renderSelectedDay();
            });
            calendarGrid.addView(dayView);
        }
    }

    private void addWeekHeaders() {
        String[] labels = {"L", "M", "X", "J", "V", "S", "D"};
        for (String label : labels) {
            TextView header = createDayCell(label);
            header.setTypeface(Typeface.DEFAULT_BOLD);
            header.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_text_secondary));
            header.setBackground(null);
            calendarGrid.addView(header);
        }
    }

    @NonNull
    private TextView createDayCell(@NonNull String text) {
        TextView textView = new TextView(requireContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        textView.setLayoutParams(params);
        textView.setMinHeight((int) (44 * getResources().getDisplayMetrics().density));
        textView.setPadding(0, 8, 0, 8);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textView.setText(text);
        return textView;
    }

    private void styleDayCell(@NonNull TextView dayView, @NonNull LocalDate date) {
        int textColor = ContextCompat.getColor(requireContext(), R.color.ch_text_primary);
        int backgroundColor = ContextCompat.getColor(requireContext(), android.R.color.transparent);
        boolean hasTrips = tripsByDay.containsKey(date);
        boolean isSelected = date.equals(selectedDay);

        if (hasTrips) {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.ch_blue_100);
        }
        if (isSelected) {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.ch_success_soft);
            textColor = ContextCompat.getColor(requireContext(), R.color.ch_success_text);
            dayView.setTypeface(Typeface.DEFAULT_BOLD);
        }

        dayView.setTextColor(textColor);
        dayView.setBackgroundColor(backgroundColor);
    }

    private void renderSelectedDay() {
        if (selectedDay == null || !YearMonth.from(selectedDay).equals(currentMonth)) {
            selectedDay = resolveDefaultDayForMonth(currentMonth);
        }
        List<Porte> dayTrips = tripsByDay.get(selectedDay);
        int count = dayTrips != null ? dayTrips.size() : 0;
        selectedDayText.setText(getString(
                R.string.portes_calendar_selected_day,
                selectedDay.getDayOfMonth(),
                capitalize(selectedDay.format(DateTimeFormatter.ofPattern("MMMM", new Locale("es", "ES")))),
                count
        ));
        adapter.setPortes(dayTrips != null ? dayTrips : new ArrayList<>());
    }

    @NonNull
    private LocalDate resolveDefaultDayForMonth(@NonNull YearMonth month) {
        if (YearMonth.now().equals(month)) {
            return LocalDate.now();
        }

        LocalDate firstTripDay = null;
        for (LocalDate day : tripsByDay.keySet()) {
            if (YearMonth.from(day).equals(month) && (firstTripDay == null || day.isBefore(firstTripDay))) {
                firstTripDay = day;
            }
        }
        return firstTripDay != null ? firstTripDay : month.atDay(1);
    }

    @NonNull
    private String capitalize(@NonNull String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(new Locale("es", "ES")) + value.substring(1);
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
}
