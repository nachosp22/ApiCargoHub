package com.cargohub.mobile.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.AgendaRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.cargohub.mobile.data.model.AgendaBloqueoRequest;
import com.cargohub.mobile.data.model.BloqueoRecurrente;
import com.cargohub.mobile.data.model.TipoBloqueoAgenda;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AgendaFragment extends Fragment {

    private static final String[] DAY_LABELS = {"L", "M", "X", "J", "V", "S", "D"};
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter ISO_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AgendaRepository agendaRepository = new AgendaRepository();

    private ProgressBar loadingProgress;
    private TextView rangeText;
    private TextView emptyText;
    private RecyclerView recyclerView;
    private AgendaAdapter adapter;
    private EditText titleInput;
    private EditText startInput;
    private EditText endInput;
    private Spinner typeSpinner;
    private TextInputLayout titleLayout;
    private TextInputLayout startLayout;
    private TextInputLayout endLayout;
    private LinearLayout recurringDaysContainer;

    // Internal date/time state for the pickers
    private LocalDateTime selectedStart;
    private LocalDateTime selectedEnd;

    // Local state for the 7 day toggles (true = blocked)
    private final boolean[] recurringState = new boolean[7];
    private final MaterialButton[] dayButtons = new MaterialButton[7];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agenda, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadingProgress = view.findViewById(R.id.agendaLoadingProgress);
        rangeText = view.findViewById(R.id.agendaRangeText);
        emptyText = view.findViewById(R.id.agendaEmptyText);
        recyclerView = view.findViewById(R.id.agendaRecyclerView);
        titleInput = view.findViewById(R.id.agendaTitleInput);
        startInput = view.findViewById(R.id.agendaStartInput);
        endInput = view.findViewById(R.id.agendaEndInput);
        typeSpinner = view.findViewById(R.id.agendaTypeSpinner);
        titleLayout = view.findViewById(R.id.agendaTitleLayout);
        startLayout = view.findViewById(R.id.agendaStartLayout);
        endLayout = view.findViewById(R.id.agendaEndLayout);
        recurringDaysContainer = view.findViewById(R.id.recurringDaysContainer);
        MaterialButton saveButton = view.findViewById(R.id.agendaSaveButton);
        MaterialButton refreshButton = view.findViewById(R.id.agendaRefreshButton);

        adapter = new AgendaAdapter(this::deleteBloqueo);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_porte_spinner, buildTypeLabels());
        spinnerAdapter.setDropDownViewResource(R.layout.item_porte_spinner_dropdown);
        typeSpinner.setAdapter(spinnerAdapter);

        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        selectedStart = now.plusDays(1).withHour(8);
        selectedEnd = now.plusDays(1).withHour(18);
        startInput.setText(selectedStart.format(DISPLAY_FORMAT));
        endInput.setText(selectedEnd.format(DISPLAY_FORMAT));

        setupDateTimePicker(startInput, startLayout, true);
        setupDateTimePicker(endInput, endLayout, false);

        setupRecurringDayButtons();

        saveButton.setOnClickListener(v -> saveBloqueo());
        refreshButton.setOnClickListener(v -> loadAgenda());
        loadAgenda();
        loadRecurringDays();
    }

    // ── Date/Time pickers ──

    private void setupDateTimePicker(EditText field, TextInputLayout layout, boolean isStart) {
        View.OnClickListener clickListener = v -> showDatePicker(field, layout, isStart);
        field.setOnClickListener(clickListener);
        layout.setEndIconOnClickListener(clickListener);
    }

    private void showDatePicker(EditText field, TextInputLayout layout, boolean isStart) {
        Calendar cal = Calendar.getInstance();
        LocalDateTime current = isStart ? selectedStart : selectedEnd;
        if (current != null) {
            cal.set(current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth());
        }

        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) ->
                        showTimePicker(field, layout, isStart, year, month + 1, dayOfMonth),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        // For "hasta", set min date to "desde" date
        if (!isStart && selectedStart != null) {
            Calendar minCal = Calendar.getInstance();
            minCal.set(selectedStart.getYear(), selectedStart.getMonthValue() - 1,
                    selectedStart.getDayOfMonth(), 0, 0, 0);
            datePicker.getDatePicker().setMinDate(minCal.getTimeInMillis());
        }

        datePicker.show();
    }

    private void showTimePicker(EditText field, TextInputLayout layout, boolean isStart,
                                int year, int month, int day) {
        LocalDateTime current = isStart ? selectedStart : selectedEnd;
        int hour = current != null ? current.getHour() : 8;
        int minute = current != null ? current.getMinute() : 0;

        new TimePickerDialog(requireContext(), (view, selectedHour, selectedMinute) -> {
            LocalDateTime dateTime = LocalDateTime.of(year, month, day, selectedHour, selectedMinute, 0);

            if (isStart) {
                selectedStart = dateTime;
                // If end is before new start, clear it
                if (selectedEnd != null && selectedEnd.isBefore(selectedStart)) {
                    selectedEnd = null;
                    endInput.setText("");
                }
            } else {
                // Validate that end is after start
                if (selectedStart != null && dateTime.isBefore(selectedStart)) {
                    layout.setError(getString(R.string.agenda_error_end_before_start));
                    return;
                }
                selectedEnd = dateTime;
            }

            layout.setError(null);
            field.setText(dateTime.format(DISPLAY_FORMAT));
        }, hour, minute, true).show();
    }

    // ── Recurring day chips ──

    private void setupRecurringDayButtons() {
        recurringDaysContainer.removeAllViews();
        int size = (int) (40 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < 7; i++) {
            MaterialButton btn = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(DAY_LABELS[i]);
            btn.setTextSize(13);
            btn.setAllCaps(false);
            btn.setInsetTop(0);
            btn.setInsetBottom(0);
            btn.setPadding(0, 0, 0, 0);
            btn.setMinWidth(size);
            btn.setMinimumWidth(size);
            btn.setMinHeight(size);
            btn.setMinimumHeight(size);
            btn.setCornerRadius(size / 2);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            btn.setLayoutParams(params);

            final int dayIndex = i;
            btn.setOnClickListener(v -> toggleRecurringDay(dayIndex));

            dayButtons[i] = btn;
            recurringDaysContainer.addView(btn);
            applyDayStyle(i, false);
        }
    }

    private void applyDayStyle(int index, boolean blocked) {
        MaterialButton btn = dayButtons[index];
        if (blocked) {
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.ch_error)));
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btn.setStrokeWidth(0);
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.ch_surface)));
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.ch_text_primary));
            btn.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.ch_outline)));
            btn.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        }
    }

    private void toggleRecurringDay(int index) {
        recurringState[index] = !recurringState[index];
        applyDayStyle(index, recurringState[index]);
        saveRecurringDays();
    }

    private void loadRecurringDays() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) return;

        agendaRepository.getBloqueoRecurrentes(conductorId, result -> {
            if (!isAdded()) return;
            if (!result.isSuccessful() || result.getData() == null) {
                Snackbar.make(requireView(), R.string.agenda_recurring_error, Snackbar.LENGTH_LONG).show();
                return;
            }
            List<BloqueoRecurrente> dias = result.getData();
            for (BloqueoRecurrente dia : dias) {
                int idx = dia.getDiaSemana() - 1; // 1-based to 0-based
                if (idx >= 0 && idx < 7) {
                    recurringState[idx] = dia.isActivo();
                    applyDayStyle(idx, dia.isActivo());
                }
            }
        });
    }

    private void saveRecurringDays() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) return;

        List<Integer> diasBloqueados = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if (recurringState[i]) {
                diasBloqueados.add(i + 1); // 0-based to 1-based
            }
        }

        agendaRepository.setBloqueoRecurrentes(conductorId, diasBloqueados, result -> {
            if (!isAdded()) return;
            if (!result.isSuccessful()) {
                Snackbar.make(requireView(), R.string.agenda_recurring_save_error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // ── Existing agenda logic (untouched) ──

    private String[] buildTypeLabels() {
        TipoBloqueoAgenda[] values = TipoBloqueoAgenda.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].name();
        }
        return labels;
    }

    private void loadAgenda() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
            return;
        }
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(30);
        rangeText.setText(getString(R.string.agenda_range_value, from.toLocalDate(), to.toLocalDate()));
        loadingProgress.setVisibility(View.VISIBLE);
        agendaRepository.getAgenda(
                conductorId,
                from.toLocalDate().toString(),
                to.toLocalDate().toString(),
                result -> handleAgendaResult(result, false)
        );
    }

    private void handleAgendaResult(@NonNull RepositoryResult<List<AgendaBloqueo>> result, boolean created) {
        if (!isAdded()) {
            return;
        }
        loadingProgress.setVisibility(View.GONE);
        if (!result.isSuccessful() || result.getData() == null) {
            Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
            return;
        }
        List<AgendaBloqueo> bloqueos = result.getData();
        adapter.setBloqueos(bloqueos);
        emptyText.setVisibility(bloqueos.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(bloqueos.isEmpty() ? View.GONE : View.VISIBLE);
        if (created) {
            Snackbar.make(requireView(), R.string.agenda_create_success, Snackbar.LENGTH_LONG).show();
        }
    }

    private void saveBloqueo() {
        clearErrors();
        String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
        if (title.isEmpty()) {
            titleLayout.setError(getString(R.string.agenda_error_title));
            return;
        }
        if (selectedStart == null) {
            startLayout.setError(getString(R.string.agenda_error_start));
            return;
        }
        if (selectedEnd == null) {
            endLayout.setError(getString(R.string.agenda_error_end));
            return;
        }
        if (selectedEnd.isBefore(selectedStart) || selectedEnd.isEqual(selectedStart)) {
            endLayout.setError(getString(R.string.agenda_error_end_before_start));
            return;
        }
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
            return;
        }
        String start = selectedStart.format(ISO_FORMAT);
        String end = selectedEnd.format(ISO_FORMAT);
        TipoBloqueoAgenda type = TipoBloqueoAgenda.valueOf(typeSpinner.getSelectedItem().toString());
        AgendaBloqueoRequest request = new AgendaBloqueoRequest(start, end, type, title);
        loadingProgress.setVisibility(View.VISIBLE);
        agendaRepository.createBloqueo(conductorId, request, result -> {
            if (!isAdded()) {
                return;
            }
            if (!result.isSuccessful()) {
                loadingProgress.setVisibility(View.GONE);
                Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
                return;
            }
            titleInput.setText("");
            loadAgenda();
        });
    }

    private void deleteBloqueo(@NonNull AgendaBloqueo bloqueo) {
        if (bloqueo.getId() == null) {
            return;
        }
        loadingProgress.setVisibility(View.VISIBLE);
        agendaRepository.deleteBloqueo(bloqueo.getId(), result -> {
            if (!isAdded()) {
                return;
            }
            if (!result.isSuccessful()) {
                loadingProgress.setVisibility(View.GONE);
                Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
                return;
            }
            Snackbar.make(requireView(), R.string.agenda_delete_success, Snackbar.LENGTH_LONG).show();
            loadAgenda();
        });
    }

    private void clearErrors() {
        titleLayout.setError(null);
        startLayout.setError(null);
        endLayout.setError(null);
    }
}
