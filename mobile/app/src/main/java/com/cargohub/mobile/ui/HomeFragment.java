package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.ConductorRepository;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.session.SessionManager;

public class HomeFragment extends Fragment {

    private final ConductorRepository conductorRepository = new ConductorRepository();

    private TextView profileNameText;
    private TextView profileEmailText;
    private TextView profileRoleText;
    private TextView profileConductorIdText;
    private TextView profileDetailHintText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileNameText = view.findViewById(R.id.profileNameText);
        profileEmailText = view.findViewById(R.id.profileEmailText);
        profileRoleText = view.findViewById(R.id.profileRoleText);
        profileConductorIdText = view.findViewById(R.id.profileConductorIdText);
        profileDetailHintText = view.findViewById(R.id.profileDetailHintText);

        View quickCurrentTripCard = view.findViewById(R.id.quickCurrentTripCard);
        View quickUpcomingTripsCard = view.findViewById(R.id.quickUpcomingTripsCard);
        View quickNewIncidentCard = view.findViewById(R.id.quickNewIncidentCard);

        quickCurrentTripCard.setOnClickListener(v -> showPlaceholderAction(R.string.home_quick_current_trip));
        quickUpcomingTripsCard.setOnClickListener(v -> showPlaceholderAction(R.string.home_quick_upcoming_trips));
        quickNewIncidentCard.setOnClickListener(v -> showPlaceholderAction(R.string.home_quick_new_incident));

        loadConductorProfile();
    }

    private void showPlaceholderAction(int labelRes) {
        if (getContext() == null) {
            return;
        }
        String message = getString(R.string.home_placeholder_action, getString(labelRes));
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadConductorProfile() {
        profileConductorIdText.setText(getCurrentUserText());
        profileRoleText.setText(R.string.home_profile_role_default);

        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showProfileFallback(
                    getString(R.string.home_profile_name_placeholder),
                    getString(R.string.home_profile_email_placeholder),
                    getString(R.string.home_profile_error_missing_id)
            );
            return;
        }

        showProfileLoading();
        conductorRepository.getConductorProfile(conductorId, new ConductorRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull ConductorProfileResponse profile) {
                if (!isAdded()) {
                    return;
                }
                renderProfileSuccess(profile, conductorId);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                showProfileFallback(
                        getString(R.string.home_profile_name_value, conductorId),
                        getString(R.string.home_profile_email_placeholder),
                        message
                );
            }
        });
    }

    private void showProfileLoading() {
        profileNameText.setText(R.string.home_profile_loading_title);
        profileEmailText.setText(R.string.home_profile_loading_body);
        profileDetailHintText.setText(R.string.home_profile_loading_hint);
    }

    private void renderProfileSuccess(@NonNull ConductorProfileResponse profile, long fallbackConductorId) {
        String fullName = profile.getNombreCompleto();
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = getString(R.string.home_profile_name_value, fallbackConductorId);
        }

        String email = profile.getEmail();
        if (email == null || email.trim().isEmpty()) {
            email = getString(R.string.home_profile_email_placeholder);
        }

        Long profileId = profile.getId();
        long resolvedId = (profileId != null && profileId > 0) ? profileId : fallbackConductorId;
        profileConductorIdText.setText(getString(R.string.home_profile_id_value, resolvedId));
        profileNameText.setText(fullName);
        profileEmailText.setText(email);
        profileDetailHintText.setText(buildPrimaryData(profile));
    }

    private void showProfileFallback(String name, String email, String hint) {
        profileNameText.setText(name);
        profileEmailText.setText(email);
        profileDetailHintText.setText(hint);
    }

    private String buildPrimaryData(@NonNull ConductorProfileResponse profile) {
        String phone = safeText(profile.getTelefono());
        String dni = safeText(profile.getDni());
        String city = safeText(profile.getCiudadBase());
        return getString(R.string.home_profile_primary_data_value, phone, dni, city);
    }

    private String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return getString(R.string.home_profile_primary_data_na);
        }
        return value.trim();
    }

    private String getCurrentUserText() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            return getString(R.string.home_profile_id_unknown);
        }
        return getString(R.string.home_profile_id_value, conductorId);
    }
}
