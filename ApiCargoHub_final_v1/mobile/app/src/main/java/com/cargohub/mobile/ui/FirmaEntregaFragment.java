package com.cargohub.mobile.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private TextView porteInfoText;
    private TextView rutaText;
    private TextView mercanciaText;
    private TextView pesoVolumenText;

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
        porteInfoText = view.findViewById(R.id.firmaPorteInfoText);
        rutaText = view.findViewById(R.id.firmaRutaText);
        mercanciaText = view.findViewById(R.id.firmaMercanciaText);
        pesoVolumenText = view.findViewById(R.id.firmaPesoVolumenText);

        limpiarButton.setOnClickListener(v -> signatureView.clear());
        confirmarButton.setOnClickListener(v -> onConfirmarEntrega());

        loadPortePreview();
    }

    private void loadPortePreview() {
        porteInfoText.setText(getString(R.string.signature_preview_porte_loading, porteId));
        rutaText.setText(getString(R.string.signature_preview_route, "—", "—"));
        mercanciaText.setText(getString(R.string.signature_preview_merchandise, "—"));
        pesoVolumenText.setText(getString(R.string.signature_preview_weight_volume, "—", "—"));

        porteRepository.getPorteDetail(porteId, result -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!result.isSuccessful() || result.getData() == null) {
                    porteInfoText.setText(getString(R.string.signature_preview_porte_fallback, porteId));
                    return;
                }

                Porte porte = result.getData();
                String estado = porte.getEstado() != null ? porte.getEstado() : "—";
                porteInfoText.setText(getString(R.string.signature_preview_porte, porteId, estado));
                rutaText.setText(getString(
                        R.string.signature_preview_route,
                        nullToDash(porte.getOrigen()),
                        nullToDash(porte.getDestino())
                ));
                mercanciaText.setText(getString(
                        R.string.signature_preview_merchandise,
                        nullToDash(porte.getDescripcionMercancia())
                ));
                pesoVolumenText.setText(getString(
                        R.string.signature_preview_weight_volume,
                        formatWeight(porte.getPesoTotalKg()),
                        formatVolume(porte.getVolumenTotalM3())
                ));
            });
        });
    }

    private void onConfirmarEntrega() {
        String firmadoPor = firmadoPorInput.getText() != null
                ? firmadoPorInput.getText().toString().trim()
                : "";

        if (firmadoPor.isEmpty()) {
            firmadoPorInput.setError(getString(R.string.signature_error_signer_required));
            firmadoPorInput.requestFocus();
            return;
        }

        if (!signatureView.hasSignature()) {
            showSnackbar(getString(R.string.signature_error_draw_before_confirm), Snackbar.LENGTH_SHORT);
            return;
        }

        Bitmap bmp = signatureView.getSignatureBitmap();
        if (bmp == null) {
            showSnackbar(getString(R.string.signature_error_capture_failed), Snackbar.LENGTH_SHORT);
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String firmaBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

        setLoading(true);

        porteRepository.firmarEntrega(porteId, firmaBase64, firmadoPor, this::handleFirmaResult);
    }

    private void handleFirmaResult(@NonNull RepositoryResult<Porte> result) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            setLoading(false);

            if (result.isSuccessful()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.signature_success_delivery_confirmed_title)
                        .setMessage(R.string.signature_success_delivery_confirmed_message)
                        .setPositiveButton(android.R.string.ok, (d, w) -> navigateBack())
                        .setCancelable(false)
                        .show();
            } else {
                showApiError(result.getMessage(), Snackbar.LENGTH_LONG);
            }
        });
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

    private String nullToDash(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    private String formatWeight(@Nullable Double value) {
        return value == null ? "—" : String.format(java.util.Locale.getDefault(), "%.2f kg", value);
    }

    private String formatVolume(@Nullable Double value) {
        return value == null ? "—" : String.format(java.util.Locale.getDefault(), "%.2f m³", value);
    }
}
