package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cargohub.mobile.data.ConductorRepository;
import com.cargohub.mobile.data.VehiculoRepository;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.data.model.EstadoVehiculo;
import com.cargohub.mobile.data.model.Vehiculo;
import com.cargohub.mobile.network.ApiClient;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private final ConductorRepository conductorRepository = new ConductorRepository();
    private final VehiculoRepository vehiculoRepository = new VehiculoRepository();

    private ShapeableImageView profileImage;
    private TextView profileNameText;
    private TextView profileIdText;
    private TextView profileDniText;
    private TextView profileMatriculaText;

    @Nullable
    private Long conductorId;

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

        profileImage = view.findViewById(R.id.homeProfileImage);
        profileNameText = view.findViewById(R.id.homeProfileName);
        profileIdText = view.findViewById(R.id.homeProfileId);
        profileDniText = view.findViewById(R.id.homeProfileDni);
        profileMatriculaText = view.findViewById(R.id.homeProfileMatricula);

        MaterialCardView trackingCard = view.findViewById(R.id.pdaTrackingCard);
        MaterialCardView offersCard = view.findViewById(R.id.pdaOffersCard);
        MaterialCardView portesCard = view.findViewById(R.id.pdaPortesCard);
        MaterialCardView incidentsCard = view.findViewById(R.id.pdaIncidentsCard);
        MaterialCardView agendaCard = view.findViewById(R.id.pdaAgendaCard);
        MaterialCardView billingCard = view.findViewById(R.id.pdaBillingCard);
        MaterialButton editProfileButton = view.findViewById(R.id.homeEditProfileButton);

        trackingCard.setOnClickListener(v -> navigateTo(new TrackingStatusFragment()));
        offersCard.setOnClickListener(v -> navigateTo(new OfferInboxFragment()));
        portesCard.setOnClickListener(v -> navigateTo(new PortesFragment()));
        incidentsCard.setOnClickListener(v -> navigateTo(new IncidenciasOptionsFragment()));
        agendaCard.setOnClickListener(v -> navigateTo(AgendaFragment.newInstance(AgendaFragment.MODE_GENERAL)));
        billingCard.setOnClickListener(v -> navigateTo(new FacturacionDashboardFragment()));
        editProfileButton.setOnClickListener(v -> navigateTo(new ProfileFragment()));

        conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showSnackbar(R.string.home_profile_error_missing_id);
            return;
        }

        profileIdText.setText(getString(R.string.home_profile_id_value, conductorId));
        loadProfile();
        loadActiveVehicle();
        loadProfilePhoto();
    }

    private void loadProfile() {
        if (conductorId == null) return;
        conductorRepository.getConductorProfile(conductorId, new ConductorRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull ConductorProfileResponse profile) {
                if (!isAdded()) return;
                bindProfile(profile);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                showSnackbar(message);
            }
        });
    }

    private void bindProfile(@NonNull ConductorProfileResponse profile) {
        String fullName = profile.getNombreCompleto();
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = getString(R.string.home_profile_name_placeholder);
        }
        profileNameText.setText(fullName);

        String dni = profile.getDni();
        if (dni != null && !dni.trim().isEmpty()) {
            profileDniText.setText("DNI: " + dni);
            profileDniText.setVisibility(View.VISIBLE);
        } else {
            profileDniText.setVisibility(View.GONE);
        }
    }

    private void loadActiveVehicle() {
        if (conductorId == null || conductorId <= 0) return;
        vehiculoRepository.getVehiculos(conductorId, result -> {
            if (!isAdded()) return;
            if (!result.isSuccessful() || result.getData() == null) {
                profileMatriculaText.setVisibility(View.GONE);
                return;
            }
            List<Vehiculo> vehiculos = result.getData();
            Vehiculo active = null;
            for (Vehiculo v : vehiculos) {
                if (v.getEstado() == EstadoVehiculo.DISPONIBLE) {
                    active = v;
                    break;
                }
            }
            if (active != null && active.getMatricula() != null && !active.getMatricula().trim().isEmpty()) {
                profileMatriculaText.setText("Matrícula: " + active.getMatricula());
                profileMatriculaText.setVisibility(View.VISIBLE);
            } else {
                profileMatriculaText.setVisibility(View.GONE);
            }
        });
    }

    private void loadProfilePhoto() {
        ApiClient.getInstance().obtenerFotoPerfil().enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call,
                                   @NonNull Response<Map<String, String>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    String url = response.body().get("url");
                    if (url != null && !url.isEmpty()) {
                        Glide.with(HomeFragment.this)
                                .load(url)
                                .transform(new CircleCrop())
                                .placeholder(R.drawable.ic_nav_profile)
                                .error(R.drawable.ic_nav_profile)
                                .into(profileImage);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                // Silently keep placeholder
            }
        });
    }

    private void navigateTo(@NonNull Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showSnackbar(@StringRes int messageRes) {
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, messageRes, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showSnackbar(@Nullable String message) {
        View view = getView();
        if (!isAdded() || view == null) return;
        String safeMessage = (message == null || message.trim().isEmpty())
                ? getString(R.string.generic_api_error_short)
                : message;
        Snackbar.make(view, safeMessage, Snackbar.LENGTH_LONG).show();
    }
}
