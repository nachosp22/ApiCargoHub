package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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
    private CheckBox trampillaCheckbox;
    private TextInputLayout matriculaLayout;
    private TextInputLayout marcaLayout;
    private TextInputLayout modeloLayout;

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
        trampillaCheckbox = view.findViewById(R.id.vehicleTrampillaCheckbox);
        matriculaLayout = view.findViewById(R.id.vehicleMatriculaLayout);
        marcaLayout = view.findViewById(R.id.vehicleMarcaLayout);
        modeloLayout = view.findViewById(R.id.vehicleModeloLayout);
        MaterialButton saveButton = view.findViewById(R.id.vehicleSaveButton);
        MaterialButton refreshButton = view.findViewById(R.id.vehicleRefreshButton);

        adapter = new VehiculoAdapter(this::toggleVehiculoState);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_porte_spinner, buildTypeLabels());
        spinnerAdapter.setDropDownViewResource(R.layout.item_porte_spinner_dropdown);
        tipoSpinner.setAdapter(spinnerAdapter);

        saveButton.setOnClickListener(v -> saveVehiculo());
        refreshButton.setOnClickListener(v -> loadVehiculos());
        loadVehiculos();
    }

    private String[] buildTypeLabels() {
        TipoVehiculo[] values = TipoVehiculo.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].name();
        }
        return labels;
    }

    private void loadVehiculos() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
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
            Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
            return;
        }
        List<Vehiculo> vehiculos = result.getData();
        adapter.setVehiculos(vehiculos);
        emptyText.setVisibility(vehiculos.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(vehiculos.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void saveVehiculo() {
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
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
            return;
        }
        VehiculoUpsertRequest request = new VehiculoUpsertRequest(
                matricula,
                marca,
                modelo,
                TipoVehiculo.valueOf(tipoSpinner.getSelectedItem().toString()),
                parseInteger(capacidadInput),
                parseInteger(largoInput),
                parseInteger(anchoInput),
                parseInteger(altoInput),
                trampillaCheckbox.isChecked()
        );
        loadingProgress.setVisibility(View.VISIBLE);
        vehiculoRepository.createVehiculo(conductorId, request, result -> {
            if (!isAdded()) {
                return;
            }
            if (!result.isSuccessful()) {
                loadingProgress.setVisibility(View.GONE);
                Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
                return;
            }
            clearForm();
            Snackbar.make(requireView(), R.string.vehicle_create_success, Snackbar.LENGTH_LONG).show();
            loadVehiculos();
        });
    }

    private void toggleVehiculoState(@NonNull Vehiculo vehiculo) {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0 || vehiculo.getId() == null) {
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
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
            Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
            return;
        }
        Snackbar.make(requireView(), successMessageRes, Snackbar.LENGTH_LONG).show();
        loadVehiculos();
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
        trampillaCheckbox.setChecked(false);
    }
}
