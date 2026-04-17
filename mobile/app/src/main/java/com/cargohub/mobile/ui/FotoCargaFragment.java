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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.FotoCargaRepository;
import com.cargohub.mobile.data.model.FotoCarga;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class FotoCargaFragment extends Fragment {

    private static final String ARG_PORTE_ID = "porte_id";
    private static final int MAX_WIDTH = 800;

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

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap photo = (Bitmap) extras.get("data");
                        if (photo != null) {
                            pendingBitmap = resizeBitmap(photo, MAX_WIDTH);
                            showUploadDialog();
                        }
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
            cameraLauncher.launch(intent);
        } else {
            Snackbar.make(requireView(), "No se encontro una app de camara", Snackbar.LENGTH_LONG).show();
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
        String[] tipoLabels = {"Carga", "Descarga", "Daño"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, tipoLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoSpinner.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Subir Foto")
                .setView(dialogView)
                .setPositiveButton("Subir", (dialog, which) -> {
                    String tipo = tipos[tipoSpinner.getSelectedItemPosition()];
                    String descripcion = descInput.getText().toString().trim();
                    String base64 = bitmapToBase64(pendingBitmap);
                    uploadFoto(tipo, base64, descripcion);
                    pendingBitmap = null;
                })
                .setNegativeButton("Cancelar", (dialog, which) -> pendingBitmap = null)
                .show();
    }

    private void uploadFoto(String tipo, String base64, String descripcion) {
        repository.subirFoto(porteId, tipo, base64, descripcion, new FotoCargaRepository.FotoCallback() {
            @Override
            public void onSuccess(@NonNull FotoCarga foto) {
                if (!isAdded()) return;
                Snackbar.make(requireView(), "Foto subida correctamente", Snackbar.LENGTH_SHORT).show();
                loadFotos();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete(FotoCarga foto) {
        if (foto.getId() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar foto")
                .setMessage("¿Seguro que queres eliminar esta foto?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    repository.eliminarFoto(porteId, foto.getId(), new FotoCargaRepository.DeleteCallback() {
                        @Override
                        public void onSuccess() {
                            if (!isAdded()) return;
                            Snackbar.make(requireView(), "Foto eliminada", Snackbar.LENGTH_SHORT).show();
                            loadFotos();
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            if (!isAdded()) return;
                            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showFullImageDialog(FotoCarga foto) {
        if (foto.getFotoBase64() == null || !isAdded()) return;
        try {
            byte[] bytes = Base64.decode(foto.getFotoBase64(), Base64.DEFAULT);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            ImageView imageView = new ImageView(requireContext());
            imageView.setImageBitmap(bitmap);
            imageView.setAdjustViewBounds(true);
            imageView.setPadding(16, 16, 16, 16);

            String title = foto.getTipoLabel();
            if (foto.getDescripcion() != null && !foto.getDescripcion().isEmpty()) {
                title += " — " + foto.getDescripcion();
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle(title)
                    .setView(imageView)
                    .setPositiveButton("Cerrar", null)
                    .show();
        } catch (Exception ignored) {
            Snackbar.make(requireView(), "No se pudo cargar la imagen", Snackbar.LENGTH_SHORT).show();
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
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
