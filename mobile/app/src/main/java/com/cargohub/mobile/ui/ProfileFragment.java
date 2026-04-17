package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.LoginNavigator;
import com.cargohub.mobile.R;
import com.cargohub.mobile.data.ConductorRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.ConductorProfileUpdateRequest;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

public class ProfileFragment extends Fragment {

    private final ConductorRepository conductorRepository = new ConductorRepository();

    private ProgressBar loadingProgress;
    private TextView nameText;
    private TextView emailText;
    private TextView idText;
    private TextView detailText;
    private EditText nombreInput;
    private EditText apellidosInput;
    private EditText telefonoInput;
    private EditText ciudadInput;
    private TextInputLayout nombreLayout;

    @Nullable
    private Long conductorId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadingProgress = view.findViewById(R.id.profileLoadingProgress);
        nameText = view.findViewById(R.id.profileNameTextView);
        emailText = view.findViewById(R.id.profileEmailTextView);
        idText = view.findViewById(R.id.profileIdTextView);
        detailText = view.findViewById(R.id.profileDetailTextView);
        nombreInput = view.findViewById(R.id.profileNombreInput);
        apellidosInput = view.findViewById(R.id.profileApellidosInput);
        telefonoInput = view.findViewById(R.id.profileTelefonoInput);
        ciudadInput = view.findViewById(R.id.profileCiudadInput);
        nombreLayout = view.findViewById(R.id.profileNombreLayout);
        MaterialButton agendaButton = view.findViewById(R.id.profileOpenAgendaButton);
        MaterialButton vehicleButton = view.findViewById(R.id.profileOpenVehicleButton);
        MaterialButton saveChangesButton = view.findViewById(R.id.profileSaveChangesButton);
        MaterialButton deactivateButton = view.findViewById(R.id.profileDeactivateButton);

        agendaButton.setOnClickListener(v -> navigateTo(new AgendaFragment()));
        vehicleButton.setOnClickListener(v -> navigateTo(new VehicleFragment()));
        saveChangesButton.setOnClickListener(v -> saveProfileChanges());
        deactivateButton.setOnClickListener(v -> confirmDeactivateAccount());
        loadProfile();
    }

    private void loadProfile() {
        conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
            return;
        }
        loadingProgress.setVisibility(View.VISIBLE);
        idText.setText(getString(R.string.profile_id_value, conductorId));
        conductorRepository.getConductorProfile(conductorId, new ConductorRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull ConductorProfileResponse profile) {
                if (!isAdded()) {
                    return;
                }
                loadingProgress.setVisibility(View.GONE);
                nameText.setText(UiFormatters.valueOrFallback(profile.getNombreCompleto(), getString(R.string.profile_name_fallback, conductorId)));
                emailText.setText(UiFormatters.valueOrFallback(profile.getEmail(), getString(R.string.home_profile_email_placeholder)));
                detailText.setText(UiFormatters.formatProfileSummary(profile));
                bindEditableFields(profile);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                loadingProgress.setVisibility(View.GONE);
                nameText.setText(getString(R.string.profile_name_fallback, conductorId));
                emailText.setText(getString(R.string.home_profile_email_placeholder));
                detailText.setText(message);
            }
        });
    }

    private void bindEditableFields(@NonNull ConductorProfileResponse profile) {
        nombreInput.setText(valueOrEmpty(profile.getNombre()));
        apellidosInput.setText(valueOrEmpty(profile.getApellidos()));
        telefonoInput.setText(valueOrEmpty(profile.getTelefono()));
        ciudadInput.setText(valueOrEmpty(profile.getCiudadBase()));
    }

    private void saveProfileChanges() {
        if (conductorId == null || conductorId <= 0) {
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
            return;
        }
        nombreLayout.setError(null);
        String nombre = textValue(nombreInput);
        if (nombre.isEmpty()) {
            nombreLayout.setError(getString(R.string.profile_edit_error_nombre));
            return;
        }
        ConductorProfileUpdateRequest request = new ConductorProfileUpdateRequest(
                nombre,
                textValue(apellidosInput),
                textValue(telefonoInput),
                textValue(ciudadInput)
        );
        loadingProgress.setVisibility(View.VISIBLE);
        conductorRepository.updateConductorProfile(conductorId, request, this::handleProfileUpdateResult);
    }

    private void handleProfileUpdateResult(@NonNull RepositoryResult<ConductorProfileResponse> result) {
        if (!isAdded()) {
            return;
        }
        loadingProgress.setVisibility(View.GONE);
        if (!result.isSuccessful() || result.getData() == null) {
            Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
            return;
        }
        ConductorProfileResponse profile = result.getData();
        nameText.setText(UiFormatters.valueOrFallback(profile.getNombreCompleto(), getString(R.string.profile_name_fallback, conductorId != null ? conductorId : 0)));
        emailText.setText(UiFormatters.valueOrFallback(profile.getEmail(), getString(R.string.home_profile_email_placeholder)));
        detailText.setText(UiFormatters.formatProfileSummary(profile));
        bindEditableFields(profile);
        Snackbar.make(requireView(), R.string.profile_save_success, Snackbar.LENGTH_LONG).show();
    }

    private void confirmDeactivateAccount() {
        if (conductorId == null || conductorId <= 0) {
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_deactivate_dialog_title)
                .setMessage(R.string.profile_deactivate_dialog_message)
                .setNegativeButton(R.string.profile_deactivate_dialog_cancel, null)
                .setPositiveButton(R.string.profile_deactivate_dialog_confirm, (dialog, which) -> deactivateAccount())
                .show();
    }

    private void deactivateAccount() {
        if (conductorId == null || conductorId <= 0) {
            return;
        }
        loadingProgress.setVisibility(View.VISIBLE);
        conductorRepository.deactivateConductor(conductorId, result -> {
            if (!isAdded()) {
                return;
            }
            loadingProgress.setVisibility(View.GONE);
            if (!result.isSuccessful()) {
                Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
                return;
            }
            SessionManager.clearSession();
            LoginNavigator.openLoginAndFinish(requireActivity());
        });
    }

    @NonNull
    private String textValue(@NonNull EditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    @NonNull
    private String valueOrEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    private void navigateTo(@NonNull Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
