package com.cargohub.mobile.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.Porte;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;

/**
 * Full-screen signature capture fragment for delivery confirmation.
 * Converts drawn signature to base64 PNG and POSTs to /api/portes/{id}/firma.
 */
public class FirmaEntregaFragment extends Fragment {

    private static final String ARG_PORTE_ID = "porte_id";

    private final PorteRepository porteRepository = new PorteRepository();

    private SignatureView signatureView;
    private TextInputEditText firmadoPorInput;
    private MaterialButton limpiarButton;
    private MaterialButton confirmarButton;
    private ProgressBar progressBar;

    private long porteId;

    public static FirmaEntregaFragment newInstance(long porteId) {
        FirmaEntregaFragment fragment = new FirmaEntregaFragment();
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
        return inflater.inflate(R.layout.fragment_firma_entrega, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            porteId = getArguments().getLong(ARG_PORTE_ID, -1);
        }

        signatureView = view.findViewById(R.id.firmaSignatureView);
        firmadoPorInput = view.findViewById(R.id.firmaFirmadoPorInput);
        limpiarButton = view.findViewById(R.id.firmaLimpiarButton);
        confirmarButton = view.findViewById(R.id.firmaConfirmarButton);
        progressBar = view.findViewById(R.id.firmaProgressBar);

        limpiarButton.setOnClickListener(v -> signatureView.clear());
        confirmarButton.setOnClickListener(v -> onConfirmarEntrega());
    }

    private void onConfirmarEntrega() {
        String firmadoPor = firmadoPorInput.getText() != null
                ? firmadoPorInput.getText().toString().trim()
                : "";

        if (firmadoPor.isEmpty()) {
            firmadoPorInput.setError("Introduce el nombre del firmante");
            firmadoPorInput.requestFocus();
            return;
        }

        if (!signatureView.hasSignature()) {
            Snackbar.make(requireView(), "Dibuja la firma antes de confirmar", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Convert bitmap to base64 PNG
        Bitmap bmp = signatureView.getSignatureBitmap();
        if (bmp == null) {
            Snackbar.make(requireView(), "Error al capturar la firma", Snackbar.LENGTH_SHORT).show();
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String firmaBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

        // Disable UI and show progress
        setLoading(true);

        porteRepository.firmarEntrega(porteId, firmaBase64, firmadoPor, this::handleFirmaResult);
    }

    private void handleFirmaResult(@NonNull RepositoryResult<Porte> result) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            setLoading(false);

            if (result.isSuccessful()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Entrega confirmada")
                        .setMessage("La firma se ha registrado correctamente. El porte ha sido marcado como entregado.")
                        .setPositiveButton(android.R.string.ok, (d, w) -> navigateBack())
                        .setCancelable(false)
                        .show();
            } else {
                Snackbar.make(requireView(),
                        result.getMessage() != null ? result.getMessage() : "Error al registrar la firma",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        confirmarButton.setEnabled(!loading);
        limpiarButton.setEnabled(!loading);
        firmadoPorInput.setEnabled(!loading);
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }
}
