package com.cargohub.mobile.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.FotoCargaRepository;
import com.cargohub.mobile.data.model.FotoCarga;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class FotoCargaFragment extends Fragment {

    private static final String ARG_PORTE_ID = "porte_id";
    private static final int MAX_WIDTH = 1200;

    private final FotoCargaRepository repository = new FotoCargaRepository();
    private FotoCargaAdapter adapter;
    private long porteId;

    private LinearLayout loadingContainer;
    private LinearLayout emptyContainer;
    private LinearLayout errorContainer;
    private RecyclerView fotosRecyclerView;
    private TextView errorMessage;

    // Camera state for pending upload
    private Bitmap pendingBitmap;
    private Uri pendingCameraUri;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Bitmap photo = readBitmapFromPendingUri();
                    if (photo == null && result.getData() != null && result.getData().getExtras() != null) {
                        photo = (Bitmap) result.getData().getExtras().get("data");
                    }
                    if (photo != null) {
                        pendingBitmap = resizeBitmap(photo, MAX_WIDTH);
                        showUploadDialog();
                    }
                }
            });

    public static FotoCargaFragment newInstance(long porteId) {
        FotoCargaFragment fragment = new FotoCargaFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PORTE_ID, porteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_foto_carga, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        porteId = requireArguments().getLong(ARG_PORTE_ID);

        loadingContainer = view.findViewById(R.id.fotoCargaLoadingContainer);
        emptyContainer = view.findViewById(R.id.fotoCargaEmptyContainer);
        errorContainer = view.findViewById(R.id.fotoCargaErrorContainer);
        fotosRecyclerView = view.findViewById(R.id.fotoCargaRecyclerView);
        errorMessage = view.findViewById(R.id.fotoCargaErrorMessage);
        FloatingActionButton fab = view.findViewById(R.id.fotoCargaFab);

        adapter = new FotoCargaAdapter();
        adapter.setOnFotoActionListener(new FotoCargaAdapter.OnFotoActionListener() {
            @Override
            public void onFotoClick(FotoCarga foto) {
                showFullImageDialog(foto);
            }

            @Override
            public void onFotoDelete(FotoCarga foto) {
                confirmDelete(foto);
            }
        });

        fotosRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        fotosRecyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> openCamera());
        view.findViewById(R.id.fotoCargaRetryButton).setOnClickListener(v -> loadFotos());

        loadFotos();
    }

    private void loadFotos() {
        showLoading();
        repository.getFotosPorPorte(porteId, new FotoCargaRepository.FotosCallback() {
            @Override
            public void onSuccess(@NonNull List<FotoCarga> fotos) {
                if (!isAdded()) return;
                if (fotos.isEmpty()) {
                    showEmpty();
                } else {
                    adapter.setFotos(fotos);
                    showContent();
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                showError(message);
            }
        });
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            pendingCameraUri = createCameraImageUri();
            if (pendingCameraUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            cameraLauncher.launch(intent);
        } else {
            showSnackbar(getString(R.string.photo_error_no_camera_app), Snackbar.LENGTH_LONG);
        }
    }

    @Nullable
    private Uri createCameraImageUri() {
        try {
            File directory = new File(requireContext().getCacheDir(), "camera");
            if (!directory.exists() && !directory.mkdirs()) {
                return null;
            }
            File image = File.createTempFile("cargo_photo_", ".jpg", directory);
            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", image);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private Bitmap readBitmapFromPendingUri() {
        if (pendingCameraUri == null) return null;
        try (InputStream input = requireContext().getContentResolver().openInputStream(pendingCameraUri)) {
            return input == null ? null : BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            return null;
        }
    }

    private void showUploadDialog() {
        if (pendingBitmap == null || !isAdded()) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_foto_carga_upload, null);

        ImageView preview = dialogView.findViewById(R.id.fotoPreview);
        EditText descInput = dialogView.findViewById(R.id.fotoDescripcionInput);
        Spinner tipoSpinner = dialogView.findViewById(R.id.fotoTipoSpinner);

        preview.setImageBitmap(pendingBitmap);

        String[] tipos = {"CARGA", "DESCARGA", "DANO"};
        String[] tipoLabels = {
                getString(R.string.photo_type_label_carga),
                getString(R.string.photo_type_label_descarga),
                getString(R.string.photo_type_label_dano)
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, tipoLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoSpinner.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.photo_upload_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.photo_upload_action, (dialog, which) -> {
                    String tipo = tipos[tipoSpinner.getSelectedItemPosition()];
                    String descripcion = descInput.getText().toString().trim();
                    String base64 = bitmapToBase64(pendingBitmap);
                    uploadFoto(tipo, base64, descripcion);
                    pendingBitmap = null;
                })
                .setNegativeButton(R.string.photo_cancel_action, (dialog, which) -> pendingBitmap = null)
                .show();
    }

    private void uploadFoto(String tipo, String base64, String descripcion) {
        repository.subirFoto(porteId, tipo, base64, descripcion, new FotoCargaRepository.FotoCallback() {
            @Override
            public void onSuccess(@NonNull FotoCarga foto) {
                if (!isAdded()) return;
                showSnackbar(getString(R.string.photo_upload_success), Snackbar.LENGTH_SHORT);
                loadFotos();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                showApiError(message, Snackbar.LENGTH_LONG);
            }
        });
    }

    private void confirmDelete(FotoCarga foto) {
        if (foto.getId() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.photo_delete_dialog_title)
                .setMessage(R.string.photo_delete_dialog_message)
                .setPositiveButton(R.string.photo_delete_action, (dialog, which) -> {
                    repository.eliminarFoto(porteId, foto.getId(), new FotoCargaRepository.DeleteCallback() {
                        @Override
                        public void onSuccess() {
                            if (!isAdded()) return;
                            showSnackbar(getString(R.string.photo_delete_success), Snackbar.LENGTH_SHORT);
                            loadFotos();
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            if (!isAdded()) return;
                            showApiError(message, Snackbar.LENGTH_LONG);
                        }
                    });
                })
                .setNegativeButton(R.string.photo_cancel_action, null)
                .show();
    }

    private void showFullImageDialog(FotoCarga foto) {
        if (foto.getFotoUrl() == null || !isAdded()) return;

        ImageView imageView = new ImageView(requireContext());
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(16, 16, 16, 16);

        com.bumptech.glide.Glide.with(requireContext())
                .load(foto.getFotoUrl())
                .into(imageView);

        String title = foto.getTipoLabel();
        if (foto.getDescripcion() != null && !foto.getDescripcion().isEmpty()) {
            title += " — " + foto.getDescripcion();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(imageView)
                .setPositiveButton(R.string.photo_close_action, null)
                .show();
    }

    private void showSnackbar(@NonNull String message, int duration) {
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, message, duration).show();
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

    private Bitmap resizeBitmap(Bitmap original, int maxWidth) {
        if (original.getWidth() <= maxWidth) return original;
        float ratio = (float) maxWidth / original.getWidth();
        int newHeight = Math.round(original.getHeight() * ratio);
        return Bitmap.createScaledBitmap(original, maxWidth, newHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        fotosRecyclerView.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        fotosRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        fotosRecyclerView.setVisibility(View.GONE);
    }

    private void showError(String message) {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        fotosRecyclerView.setVisibility(View.GONE);
        errorMessage.setText(message);
    }
}
