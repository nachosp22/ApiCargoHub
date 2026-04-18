package com.cargohub.mobile.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cargohub.mobile.LoginNavigator;
import com.cargohub.mobile.R;
import com.cargohub.mobile.data.ConductorRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.ConductorProfileUpdateRequest;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.network.ApiClient;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final int MAX_PHOTO_WIDTH = 600;
    private static final int MAX_PHOTO_BYTES = 500 * 1024; // 500KB

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
    private ShapeableImageView avatarImage;

    @Nullable
    private Long conductorId;

    @Nullable
    private String currentFotoUrl;

    // ── Camera launcher (reuses pattern from FotoCargaFragment) ──

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap photo = (Bitmap) extras.get("data");
                        if (photo != null) {
                            uploadProfilePhoto(resizeBitmap(photo, MAX_PHOTO_WIDTH));
                        }
                    }
                }
            });

    // ── Gallery launcher ──

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null
                        && result.getData().getData() != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                requireActivity().getContentResolver(),
                                result.getData().getData());
                        if (bitmap != null) {
                            uploadProfilePhoto(resizeBitmap(bitmap, MAX_PHOTO_WIDTH));
                        }
                    } catch (Exception e) {
                        Snackbar.make(requireView(), R.string.profile_photo_upload_error, Snackbar.LENGTH_LONG).show();
                    }
                }
            });

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
        avatarImage = view.findViewById(R.id.profileAvatarImage);
        FloatingActionButton avatarEditFab = view.findViewById(R.id.profileAvatarEditFab);
        MaterialButton agendaButton = view.findViewById(R.id.profileOpenAgendaButton);
        MaterialButton vehicleButton = view.findViewById(R.id.profileOpenVehicleButton);
        MaterialButton saveChangesButton = view.findViewById(R.id.profileSaveChangesButton);
        MaterialButton deactivateButton = view.findViewById(R.id.profileDeactivateButton);

        agendaButton.setOnClickListener(v -> navigateTo(new AgendaFragment()));
        vehicleButton.setOnClickListener(v -> navigateTo(new VehicleFragment()));
        saveChangesButton.setOnClickListener(v -> saveProfileChanges());
        deactivateButton.setOnClickListener(v -> confirmDeactivateAccount());

        avatarImage.setOnClickListener(v -> showPhotoOptionsDialog());
        avatarEditFab.setOnClickListener(v -> showPhotoOptionsDialog());

        loadProfile();
        loadProfilePhoto();
    }

    // ══════════════════════════════════════════════════════════════
    // Profile photo
    // ══════════════════════════════════════════════════════════════

    private void showPhotoOptionsDialog() {
        String[] options;
        if (currentFotoUrl != null) {
            options = new String[]{
                    getString(R.string.profile_photo_take),
                    getString(R.string.profile_photo_gallery),
                    getString(R.string.profile_photo_remove)
            };
        } else {
            options = new String[]{
                    getString(R.string.profile_photo_take),
                    getString(R.string.profile_photo_gallery)
            };
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_photo_dialog_title)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openCamera();
                            break;
                        case 1:
                            openGallery();
                            break;
                        case 2:
                            deleteProfilePhoto();
                            break;
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        } else {
            Snackbar.make(requireView(), R.string.profile_photo_no_camera, Snackbar.LENGTH_LONG).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void uploadProfilePhoto(@NonNull Bitmap bitmap) {
        String base64 = bitmapToBase64(bitmap);

        // Check size (base64 is ~33% larger than raw bytes)
        byte[] rawBytes = Base64.decode(base64, Base64.DEFAULT);
        if (rawBytes.length > MAX_PHOTO_BYTES) {
            Snackbar.make(requireView(), R.string.profile_photo_too_large, Snackbar.LENGTH_LONG).show();
            return;
        }

        loadingProgress.setVisibility(View.VISIBLE);

        Map<String, String> body = new HashMap<>();
        body.put("imagen", base64);

        ApiClient.getInstance().subirFotoPerfil(body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call,
                                   @NonNull Response<Map<String, String>> response) {
                if (!isAdded()) return;
                loadingProgress.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String url = response.body().get("url");
                    currentFotoUrl = url;
                    loadAvatarFromUrl(url);
                    Snackbar.make(requireView(), R.string.profile_photo_upload_success, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(requireView(), R.string.profile_photo_upload_error, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingProgress.setVisibility(View.GONE);
                Snackbar.make(requireView(), R.string.profile_photo_upload_error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void deleteProfilePhoto() {
        loadingProgress.setVisibility(View.VISIBLE);
        ApiClient.getInstance().eliminarFotoPerfil().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;
                loadingProgress.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    currentFotoUrl = null;
                    avatarImage.setImageResource(R.drawable.ic_nav_profile);
                    Snackbar.make(requireView(), R.string.profile_photo_remove_success, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(requireView(), R.string.profile_photo_remove_error, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loadingProgress.setVisibility(View.GONE);
                Snackbar.make(requireView(), R.string.profile_photo_remove_error, Snackbar.LENGTH_LONG).show();
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
                        currentFotoUrl = url;
                        loadAvatarFromUrl(url);
                    }
                }
                // 204 or error — keep default placeholder
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                // Silently keep placeholder
            }
        });
    }

    private void loadAvatarFromUrl(@Nullable String url) {
        if (!isAdded() || url == null || url.isEmpty()) return;
        Glide.with(this)
                .load(url)
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_nav_profile)
                .error(R.drawable.ic_nav_profile)
                .into(avatarImage);
    }

    // ══════════════════════════════════════════════════════════════
    // Image helpers (pattern from FotoCargaFragment)
    // ══════════════════════════════════════════════════════════════

    private Bitmap resizeBitmap(@NonNull Bitmap original, int maxWidth) {
        if (original.getWidth() <= maxWidth) return original;
        float ratio = (float) maxWidth / original.getWidth();
        int newHeight = Math.round(original.getHeight() * ratio);
        return Bitmap.createScaledBitmap(original, maxWidth, newHeight, true);
    }

    private String bitmapToBase64(@NonNull Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    // ══════════════════════════════════════════════════════════════
    // Existing profile logic (unchanged)
    // ══════════════════════════════════════════════════════════════

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
