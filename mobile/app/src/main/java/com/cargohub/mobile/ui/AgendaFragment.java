package com.cargohub.mobile.ui;

import android.app.DatePickerDialog;
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
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.AgendaRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.cargohub.mobile.data.model.AgendaBloqueoRequest;
import com.cargohub.mobile.data.model.TipoBloqueoAgenda;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class AgendaFragment extends Fragment {

    public static final String ARG_MODE = "agenda_mode";
    public static final String MODE_GENERAL = "general";
    public static final String MODE_VACATIONS = "vacations";
    public static final String MODE_WORK_DAYS = "work_days";

    private static final String[] DAY_LABELS = {"L", "M", "X", "J", "V", "S", "D"};
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AgendaRepository agendaRepository = new AgendaRepository();

    private ProgressBar loadingProgress;
    private TextView titleText;
    private TextView modeText;
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
    private MaterialButton updateRecurringButton;
    private List<TipoBloqueoAgenda> availableTypes = new ArrayList<>();

    // Internal date/time state for the pickers
    private LocalDateTime selectedStart;
    private LocalDateTime selectedEnd;

    // Local state for the 7 day toggles (true = working day)
    private final boolean[] recurringState = new boolean[7];
    private final boolean[] persistedRecurringState = new boolean[7];
    private final MaterialButton[] dayButtons = new MaterialButton[7];

    @NonNull
    public static AgendaFragment newInstance(@NonNull String mode) {
        AgendaFragment fragment = new AgendaFragment();
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
        return inflater.inflate(R.layout.fragment_agenda, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleText = view.findViewById(R.id.agendaTitleText);
        modeText = view.findViewById(R.id.agendaModeText);
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
        updateRecurringButton = view.findViewById(R.id.agendaRecurringSaveButton);
        MaterialButton saveButton = view.findViewById(R.id.agendaSaveButton);
        MaterialButton refreshButton = view.findViewById(R.id.agendaRefreshButton);

        adapter = new AgendaAdapter(this::deleteBloqueo);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_porte_spinner,
                buildTypeLabels()
        );
        spinnerAdapter.setDropDownViewResource(R.layout.item_porte_spinner_dropdown);
        typeSpinner.setAdapter(spinnerAdapter);
        applyModeDefaults();

        LocalDate today = LocalDate.now();
        selectedStart = today.plusDays(1).atStartOfDay();
        selectedEnd = today.plusDays(1).atTime(23, 59, 59);
        startInput.setText(selectedStart.format(DISPLAY_FORMAT));
        endInput.setText(selectedEnd.format(DISPLAY_FORMAT));

        setupDatePicker(startInput, startLayout, true);
        setupDatePicker(endInput, endLayout, false);

        setupRecurringDayButtons();

        saveButton.setOnClickListener(v -> saveBloqueo());
        refreshButton.setOnClickListener(v -> loadAgenda());
        updateRecurringButton.setOnClickListener(v -> saveRecurringDays());
        updateRecurringButton.setEnabled(false);
        bindModeUi();
        loadAgenda();
        loadRecurringDays();
    }

    private void bindModeUi() {
        String mode = getCurrentMode();
        if (MODE_VACATIONS.equals(mode)) {
            titleText.setText(R.string.agenda_mode_vacations_title);
            modeText.setText(R.string.agenda_mode_vacations_subtitle);
            return;
        }
        if (MODE_WORK_DAYS.equals(mode)) {
            titleText.setText(R.string.agenda_mode_work_days_title);
            modeText.setText(R.string.agenda_mode_work_days_subtitle);
            return;
        }
        titleText.setText(R.string.home_menu_section_agenda);
        modeText.setText(R.string.agenda_options_subtitle);
    }

    private void applyModeDefaults() {
        String mode = getCurrentMode();
        if (MODE_VACATIONS.equals(mode)) {
            preselectType(TipoBloqueoAgenda.VACACIONES);
            applyDefaultTitleIfEmpty(R.string.agenda_default_title_vacations);
            return;
        }
        if (MODE_WORK_DAYS.equals(mode)) {
            preselectType(TipoBloqueoAgenda.DESCANSO_SEMANAL);
            applyDefaultTitleIfEmpty(R.string.agenda_default_title_work_day_block);
        }
    }

    private void preselectType(@NonNull TipoBloqueoAgenda preferredType) {
        for (int i = 0; i < availableTypes.size(); i++) {
            if (preferredType == availableTypes.get(i)) {
                typeSpinner.setSelection(i);
                return;
            }
        }
    }

    private void applyDefaultTitleIfEmpty(int titleResId) {
        if (titleInput.getText() == null) {
            titleInput.setText(titleResId);
            return;
        }
        String currentTitle = titleInput.getText().toString().trim();
        if (currentTitle.isEmpty()) {
            titleInput.setText(titleResId);
        }
    }

    @NonNull
    private String getCurrentMode() {
        Bundle args = getArguments();
        if (args == null) {
            return MODE_GENERAL;
        }
        String mode = args.getString(ARG_MODE);
        if (MODE_VACATIONS.equals(mode) || MODE_WORK_DAYS.equals(mode) || MODE_GENERAL.equals(mode)) {
            return mode;
        }
        return MODE_GENERAL;
    }

    // ── Date pickers ──

    private void setupDatePicker(EditText field, TextInputLayout layout, boolean isStart) {
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
                (view, year, month, dayOfMonth) -> {
                    LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    LocalDateTime dateTime = isStart
                            ? selectedDate.atStartOfDay()
                            : selectedDate.atTime(23, 59, 59);

                    if (isStart) {
                        selectedStart = dateTime;
                        if (selectedEnd != null && selectedEnd.isBefore(selectedStart)) {
                            selectedEnd = null;
                            endInput.setText("");
                        }
                    } else {
                        if (selectedStart != null && dateTime.isBefore(selectedStart)) {
                            layout.setError(getString(R.string.agenda_error_end_before_start));
                            return;
                        }
                        selectedEnd = dateTime;
                    }

                    layout.setError(null);
                    field.setText(selectedDate.format(DISPLAY_FORMAT));
                },
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

    // ── Working day chips ──

    private void setupRecurringDayButtons() {
        recurringDaysContainer.removeAllViews();
        int size = (int) (44 * getResources().getDisplayMetrics().density);
        int margin = (int) (2 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < 7; i++) {
            MaterialButton btn = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(DAY_LABELS[i]);
            btn.setTextSize(13);
            btn.setAllCaps(false);
            btn.setInsetTop(0);
            btn.setInsetBottom(0);
            btn.setPadding(0, 0, 0, 0);
            btn.setMinWidth(0);
            btn.setMinimumWidth(0);
            btn.setMinHeight(size);
            btn.setMinimumHeight(size);
            btn.setCornerRadius(size / 2);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, size, 1f);
            params.setMargins(margin, 0, margin, 0);
            btn.setLayoutParams(params);

            final int dayIndex = i;
            btn.setOnClickListener(v -> toggleRecurringDay(dayIndex));

            dayButtons[i] = btn;
            recurringDaysContainer.addView(btn);
            applyDayStyle(i, false);
        }
    }

    private void applyDayStyle(int index, boolean isWorkingDay) {
        MaterialButton btn = dayButtons[index];
        if (isWorkingDay) {
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.ch_primary)));
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
        updateRecurringButtonState();
    }

    private void loadRecurringDays() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) return;

        agendaRepository.getDiasLaborables(conductorId, result -> {
            if (!isAdded()) return;
            if (!result.isSuccessful() || result.getData() == null) {
                showSnackbar(R.string.agenda_recurring_error, Snackbar.LENGTH_LONG);
                return;
            }
            List<Integer> diasLaborables = result.getData();
            for (int i = 0; i < 7; i++) {
                boolean isWorkingDay = diasLaborables.contains(i + 1);
                recurringState[i] = isWorkingDay;
                persistedRecurringState[i] = isWorkingDay;
                applyDayStyle(i, isWorkingDay);
            }
            updateRecurringButtonState();
        });
    }

    private void saveRecurringDays() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) return;

        List<Integer> diasLaborables = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if (recurringState[i]) {
                diasLaborables.add(i + 1); // 0-based to 1-based
            }
        }

        agendaRepository.setDiasLaborables(conductorId, diasLaborables, result -> {
            if (!isAdded()) return;
            if (!result.isSuccessful()) {
                showSnackbar(R.string.agenda_recurring_save_error, Snackbar.LENGTH_LONG);
                return;
            }
            if (result.getData() != null) {
                List<Integer> persistedDays = result.getData();
                for (int i = 0; i < 7; i++) {
                    boolean isWorkingDay = persistedDays.contains(i + 1);
                    recurringState[i] = isWorkingDay;
                    persistedRecurringState[i] = isWorkingDay;
                    applyDayStyle(i, isWorkingDay);
                }
            }
            updateRecurringButtonState();
            showSnackbar(R.string.agenda_recurring_save_success, Snackbar.LENGTH_LONG);
        });
    }

    private void updateRecurringButtonState() {
        boolean changed = false;
        for (int i = 0; i < 7; i++) {
            if (recurringState[i] != persistedRecurringState[i]) {
                changed = true;
                break;
            }
        }
        updateRecurringButton.setEnabled(changed);
    }

    // ── Existing agenda logic (untouched) ──

    private String[] buildTypeLabels() {
        availableTypes = Arrays.asList(TipoBloqueoAgenda.values());
        TipoBloqueoAgenda[] values = TipoBloqueoAgenda.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].getDisplayName();
        }
        return labels;
    }

    private void loadAgenda() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showSnackbar(R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG);
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
            showSnackbar(result.getMessage(), R.string.generic_api_error_short, Snackbar.LENGTH_LONG);
            return;
        }
        List<AgendaBloqueo> bloqueos = result.getData();
        adapter.setBloqueos(bloqueos);
        emptyText.setVisibility(bloqueos.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(bloqueos.isEmpty() ? View.GONE : View.VISIBLE);
        if (created) {
            showSnackbar(R.string.agenda_create_success, Snackbar.LENGTH_LONG);
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
            showSnackbar(R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG);
            return;
        }
        String start = selectedStart.format(ISO_FORMAT);
        String end = selectedEnd.format(ISO_FORMAT);
        TipoBloqueoAgenda type = resolveSelectedType();
        AgendaBloqueoRequest request = new AgendaBloqueoRequest(start, end, type, title);
        loadingProgress.setVisibility(View.VISIBLE);
        agendaRepository.createBloqueo(conductorId, request, result -> {
            if (!isAdded()) {
                return;
            }
            if (!result.isSuccessful()) {
                loadingProgress.setVisibility(View.GONE);
                showSnackbar(result.getMessage(), R.string.generic_api_error_short, Snackbar.LENGTH_LONG);
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
                showSnackbar(result.getMessage(), R.string.generic_api_error_short, Snackbar.LENGTH_LONG);
                return;
            }
            showSnackbar(R.string.agenda_delete_success, Snackbar.LENGTH_LONG);
            loadAgenda();
        });
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

    private void clearErrors() {
        titleLayout.setError(null);
        startLayout.setError(null);
        endLayout.setError(null);
    }

    @NonNull
    private TipoBloqueoAgenda resolveSelectedType() {
        int selectedPosition = typeSpinner.getSelectedItemPosition();
        if (selectedPosition >= 0 && selectedPosition < availableTypes.size()) {
            return availableTypes.get(selectedPosition);
        }
        Object selectedItem = typeSpinner.getSelectedItem();
        return TipoBloqueoAgenda.fromRawValue(selectedItem != null ? selectedItem.toString() : null);
    }
}
