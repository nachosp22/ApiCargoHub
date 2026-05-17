package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.VehiculoRepository;
import com.cargohub.mobile.data.model.EstadoVehiculo;
import com.cargohub.mobile.data.model.TipoVehiculo;
import com.cargohub.mobile.data.model.Vehiculo;
import com.cargohub.mobile.data.model.VehiculoUpsertRequest;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class VehicleFragment extends Fragment {

    private final VehiculoRepository vehiculoRepository = new VehiculoRepository();
    private TipoVehiculo[] tipoVehiculoOptions = new TipoVehiculo[0];

    private ProgressBar loadingProgress;
    private TextView emptyText;
    private RecyclerView recyclerView;
    private VehiculoAdapter adapter;
    private EditText matriculaInput;
    private EditText marcaInput;
    private EditText modeloInput;
    private EditText capacidadInput;
    private EditText largoInput;
    private EditText anchoInput;
    private EditText altoInput;
    private Spinner tipoSpinner;
    private TextInputLayout matriculaLayout;
    private TextInputLayout marcaLayout;
    private TextInputLayout modeloLayout;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    private View vehicleFormCard;
    private MaterialButton newVehicleButton;
    private List<Vehiculo> currentVehiculos = java.util.Collections.emptyList();

    @Nullable
    private Vehiculo editingVehiculo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vehicle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadingProgress = view.findViewById(R.id.vehicleLoadingProgress);
        emptyText = view.findViewById(R.id.vehicleEmptyText);
        recyclerView = view.findViewById(R.id.vehicleRecyclerView);
        matriculaInput = view.findViewById(R.id.vehicleMatriculaInput);
        marcaInput = view.findViewById(R.id.vehicleMarcaInput);
        modeloInput = view.findViewById(R.id.vehicleModeloInput);
        capacidadInput = view.findViewById(R.id.vehicleCapacidadInput);
        largoInput = view.findViewById(R.id.vehicleLargoInput);
        anchoInput = view.findViewById(R.id.vehicleAnchoInput);
        altoInput = view.findViewById(R.id.vehicleAltoInput);
        tipoSpinner = view.findViewById(R.id.vehicleTipoSpinner);
        matriculaLayout = view.findViewById(R.id.vehicleMatriculaLayout);
        marcaLayout = view.findViewById(R.id.vehicleMarcaLayout);
        modeloLayout = view.findViewById(R.id.vehicleModeloLayout);

        adapter = new VehiculoAdapter(new VehiculoAdapter.VehiculoActionListener() {
            @Override
            public void onToggleState(@NonNull Vehiculo vehiculo) {
                VehicleFragment.this.toggleVehiculoState(vehiculo);
            }

            @Override
            public void onEdit(@NonNull Vehiculo vehiculo) {
                VehicleFragment.this.startEditing(vehiculo);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_vehicle_spinner, buildTypeLabels());
        spinnerAdapter.setDropDownViewResource(R.layout.item_vehicle_spinner_dropdown);
        tipoSpinner.setAdapter(spinnerAdapter);

        saveButton = view.findViewById(R.id.vehicleSaveButton);
        cancelButton = view.findViewById(R.id.vehicleCancelButton);
        MaterialButton refreshButton = view.findViewById(R.id.vehicleRefreshButton);
        vehicleFormCard = view.findViewById(R.id.vehicleFormCard);
        newVehicleButton = view.findViewById(R.id.vehicleNewButton);

        saveButton.setOnClickListener(v -> saveOrUpdateVehiculo());
        refreshButton.setOnClickListener(v -> clearForm());
        cancelButton.setOnClickListener(v -> cancelEditing());
        newVehicleButton.setOnClickListener(v -> showForm());
        loadVehiculos();
    }

    private String[] buildTypeLabels() {
        tipoVehiculoOptions = TipoVehiculo.values();
        String[] labels = new String[tipoVehiculoOptions.length + 1];
        labels[0] = getString(R.string.vehicle_tipo_label);
        for (int i = 0; i < tipoVehiculoOptions.length; i++) {
            labels[i + 1] = tipoVehiculoOptions[i].getDisplayName();
        }
        return labels;
    }

    private void loadVehiculos() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showSnackbar(R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG);
            return;
        }
        loadingProgress.setVisibility(View.VISIBLE);
        vehiculoRepository.getVehiculos(conductorId, this::handleVehiculosResult);
    }

    private void handleVehiculosResult(@NonNull RepositoryResult<List<Vehiculo>> result) {
        if (!isAdded()) {
            return;
        }
        loadingProgress.setVisibility(View.GONE);
        if (!result.isSuccessful() || result.getData() == null) {
            showApiError(result.getMessage(), Snackbar.LENGTH_LONG);
            return;
        }
        List<Vehiculo> vehiculos = result.getData();
        currentVehiculos = vehiculos;
        adapter.setVehiculos(vehiculos);
        emptyText.setVisibility(vehiculos.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(vehiculos.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void saveOrUpdateVehiculo() {
        clearErrors();
        String matricula = textValue(matriculaInput);
        String marca = textValue(marcaInput);
        String modelo = textValue(modeloInput);
        if (matricula.isEmpty()) {
            matriculaLayout.setError(getString(R.string.vehicle_error_matricula));
            return;
        }
        if (marca.isEmpty()) {
            marcaLayout.setError(getString(R.string.vehicle_error_marca));
            return;
        }
        if (modelo.isEmpty()) {
            modeloLayout.setError(getString(R.string.vehicle_error_modelo));
            return;
        }
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showSnackbar(R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG);
            return;
        }
        int selectedIndex = tipoSpinner.getSelectedItemPosition();
        TipoVehiculo selectedTipo = (selectedIndex > 0 && selectedIndex <= tipoVehiculoOptions.length)
                ? tipoVehiculoOptions[selectedIndex - 1]
                : TipoVehiculo.FURGONETA;

        VehiculoUpsertRequest request = new VehiculoUpsertRequest(
                matricula,
                marca,
                modelo,
                selectedTipo,
                parseInteger(capacidadInput),
                parseInteger(largoInput),
                parseInteger(anchoInput),
                parseInteger(altoInput)
        );
        loadingProgress.setVisibility(View.VISIBLE);

        if (editingVehiculo != null && editingVehiculo.getId() != null) {
            vehiculoRepository.updateVehiculo(conductorId, editingVehiculo.getId(), request, result -> {
                if (!isAdded()) return;
                if (!result.isSuccessful()) {
                    loadingProgress.setVisibility(View.GONE);
                    showApiError(result.getMessage(), Snackbar.LENGTH_LONG);
                    return;
                }
                cancelEditing();
                showSnackbar(R.string.vehicle_update_success, Snackbar.LENGTH_LONG);
                loadVehiculos();
            });
        } else {
            vehiculoRepository.createVehiculo(conductorId, request, result -> {
                if (!isAdded()) return;
                if (!result.isSuccessful()) {
                    loadingProgress.setVisibility(View.GONE);
                    showApiError(result.getMessage(), Snackbar.LENGTH_LONG);
                    return;
                }
                clearForm();
                showSnackbar(R.string.vehicle_create_success, Snackbar.LENGTH_LONG);
                loadVehiculos();
            });
        }
    }

    private void startEditing(@NonNull Vehiculo vehiculo) {
        editingVehiculo = vehiculo;
        matriculaInput.setText(valueOrEmpty(vehiculo.getMatricula()));
        marcaInput.setText(valueOrEmpty(vehiculo.getMarca()));
        modeloInput.setText(valueOrEmpty(vehiculo.getModelo()));
        capacidadInput.setText(vehiculo.getCapacidadCargaKg() != null ? String.valueOf(vehiculo.getCapacidadCargaKg()) : "");
        largoInput.setText(vehiculo.getLargoUtilMm() != null ? String.valueOf(vehiculo.getLargoUtilMm()) : "");
        anchoInput.setText(vehiculo.getAnchoUtilMm() != null ? String.valueOf(vehiculo.getAnchoUtilMm()) : "");
        altoInput.setText(vehiculo.getAltoUtilMm() != null ? String.valueOf(vehiculo.getAltoUtilMm()) : "");

        if (vehiculo.getTipo() != null) {
            for (int i = 0; i < tipoVehiculoOptions.length; i++) {
                if (tipoVehiculoOptions[i] == vehiculo.getTipo()) {
                    tipoSpinner.setSelection(i + 1);
                    break;
                }
            }
        }

        saveButton.setText(R.string.vehicle_update_action);
        cancelButton.setVisibility(View.VISIBLE);
        showForm();

        recyclerView.smoothScrollToPosition(0);
    }

    private void cancelEditing() {
        editingVehiculo = null;
        clearForm();
        saveButton.setText(R.string.vehicle_save_action);
        cancelButton.setVisibility(View.GONE);
    }

    private void toggleVehiculoState(@NonNull Vehiculo vehiculo) {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0 || vehiculo.getId() == null) {
            showSnackbar(R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG);
            return;
        }
        loadingProgress.setVisibility(View.VISIBLE);
        if (vehiculo.getEstado() == EstadoVehiculo.DISPONIBLE) {
            vehiculoRepository.deactivateVehiculo(conductorId, vehiculo.getId(), result -> handleToggleResult(result, R.string.vehicle_deactivate_success));
        } else {
            vehiculoRepository.activateVehiculo(conductorId, vehiculo.getId(), result -> handleToggleResult(result, R.string.vehicle_activate_success));
        }
    }

    private void handleToggleResult(@NonNull RepositoryResult<Void> result, int successMessageRes) {
        if (!isAdded()) {
            return;
        }
        if (!result.isSuccessful()) {
            loadingProgress.setVisibility(View.GONE);
            showApiError(result.getMessage(), Snackbar.LENGTH_LONG);
            return;
        }
        showSnackbar(successMessageRes, Snackbar.LENGTH_LONG);
        loadVehiculos();
    }

    private void showSnackbar(int messageResId, int duration) {
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, messageResId, duration).show();
        }
    }

    private void showApiError(@Nullable String message, int duration) {
        String safeMessage = (message == null || message.trim().isEmpty())
                ? getString(R.string.generic_api_error_short)
                : message;
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, safeMessage, duration).show();
        }
    }

    @Nullable
    private Integer parseInteger(@NonNull EditText input) {
        String raw = textValue(input);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @NonNull
    private String textValue(@NonNull EditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    private void clearErrors() {
        matriculaLayout.setError(null);
        marcaLayout.setError(null);
        modeloLayout.setError(null);
    }

    private void clearForm() {
        matriculaInput.setText("");
        marcaInput.setText("");
        modeloInput.setText("");
        capacidadInput.setText("");
        largoInput.setText("");
        anchoInput.setText("");
        altoInput.setText("");
        tipoSpinner.setSelection(0);
        hideForm();
    }

    private void showForm() {
        vehicleFormCard.setVisibility(View.VISIBLE);
        newVehicleButton.setVisibility(View.GONE);
    }

    private void hideForm() {
        vehicleFormCard.setVisibility(View.GONE);
        newVehicleButton.setVisibility(View.VISIBLE);
    }

    @NonNull
    private String valueOrEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
