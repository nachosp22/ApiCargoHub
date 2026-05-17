package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.IncidenciaRepository;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.data.model.PrioridadIncidencia;
import com.cargohub.mobile.data.model.SeveridadIncidencia;
import com.cargohub.mobile.databinding.FragmentNuevaIncidenciaBinding;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class NuevaIncidenciaFragment extends Fragment {

    private static final String ARG_PORTE_ID = "porte_id";
    private static final int PLACEHOLDER_SELECTION_INDEX = 0;

    private FragmentNuevaIncidenciaBinding binding;
    private final IncidenciaRepository repository = new IncidenciaRepository();
    private PorteSpinnerAdapter porteAdapter;
    private SeveridadIncidencia[] severidadOptions = new SeveridadIncidencia[0];
    private PrioridadIncidencia[] prioridadOptions = new PrioridadIncidencia[0];
    private List<Porte> portesCache;
    private Long preselectedPorteId;

    public static NuevaIncidenciaFragment newInstance(@Nullable Long porteId) {
        NuevaIncidenciaFragment fragment = new NuevaIncidenciaFragment();
        Bundle args = new Bundle();
        if (porteId != null) {
            args.putLong(ARG_PORTE_ID, porteId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNuevaIncidenciaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(ARG_PORTE_ID)) {
            preselectedPorteId = getArguments().getLong(ARG_PORTE_ID);
        }
        setupSpinners();
        setupSubmitButton();
        loadPortes();
    }

    private void setupSpinners() {
        severidadOptions = SeveridadIncidencia.values();
        String[] severidades = new String[severidadOptions.length + 1];
        severidades[0] = "Severidad";
        int idx = 1;
        for (SeveridadIncidencia s : severidadOptions) {
            severidades[idx++] = s.getDisplayName();
        }
        ArrayAdapter<String> severidadAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_porte_spinner,
                severidades
        );
        severidadAdapter.setDropDownViewResource(R.layout.item_porte_spinner_dropdown);
        binding.severidadSpinner.setAdapter(severidadAdapter);
        binding.severidadSpinner.setSelection(0);

        prioridadOptions = PrioridadIncidencia.values();
        String[] prioridades = new String[prioridadOptions.length + 1];
        prioridades[0] = "Prioridad";
        idx = 1;
        for (PrioridadIncidencia p : prioridadOptions) {
            prioridades[idx++] = p.getDisplayName();
        }
        ArrayAdapter<String> prioridadAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_porte_spinner,
                prioridades
        );
        prioridadAdapter.setDropDownViewResource(R.layout.item_porte_spinner_dropdown);
        binding.prioridadSpinner.setAdapter(prioridadAdapter);
        binding.prioridadSpinner.setSelection(0);

        porteAdapter = new PorteSpinnerAdapter(requireContext());
        binding.porteSpinner.setAdapter(porteAdapter);
    }

    private void setupSubmitButton() {
        binding.submitButton.setOnClickListener(v -> validarYEnviar());
    }

    private void loadPortes() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showNoPortesState();
            return;
        }

        repository.getPortesDelConductor(conductorId, new IncidenciaRepository.PortesCallback() {
            @Override
            public void onSuccess(@NonNull List<Porte> portes) {
                if (!isAdded()) {
                    return;
                }
                portesCache = portes;
                if (portes == null || portes.isEmpty()) {
                    showNoPortesState();
                } else {
                    showPortes(portes);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                showNoPortesState();
                showSnackbar(message, R.string.generic_api_error_short, Snackbar.LENGTH_LONG);
            }
        });
    }

    private void showNoPortesState() {
        binding.noPortesCard.setVisibility(View.VISIBLE);
        binding.porteInputLayout.setEnabled(false);
        binding.submitButton.setEnabled(false);
    }

    private void showPortes(List<Porte> portes) {
        binding.noPortesCard.setVisibility(View.GONE);
        binding.porteInputLayout.setEnabled(true);
        binding.submitButton.setEnabled(true);
        porteAdapter.setPortes(portes);
        applyPreselectedPorte();
    }

    private void applyPreselectedPorte() {
        if (preselectedPorteId == null || portesCache == null) {
            return;
        }
        for (int i = 0; i < portesCache.size(); i++) {
            Porte porte = portesCache.get(i);
            if (porte.getId() != null && preselectedPorteId.equals(porte.getId())) {
                binding.porteSpinner.setSelection(i);
                binding.porteSpinner.setEnabled(false);
                binding.porteInputLayout.setHelperText(getString(R.string.incidencia_preselected_trip_helper));
                return;
            }
        }
    }

    private void validarYEnviar() {
        binding.tituloInputLayout.setError(null);
        binding.descripcionInputLayout.setError(null);

        String titulo = binding.tituloInput.getText() != null
                ? binding.tituloInput.getText().toString().trim() : "";
        String descripcion = binding.descripcionInput.getText() != null
                ? binding.descripcionInput.getText().toString().trim() : "";

        if (titulo.isEmpty()) {
            binding.tituloInputLayout.setError(getString(R.string.incidencia_error_titulo_requerido));
            binding.tituloInput.requestFocus();
            return;
        }

        if (descripcion.isEmpty()) {
            binding.descripcionInputLayout.setError(getString(R.string.incidencia_error_descripcion_requerida));
            binding.descripcionInput.requestFocus();
            return;
        }

        int portePosition = binding.porteSpinner.getSelectedItemPosition();
        if (portePosition < 0 || portesCache == null || portePosition >= portesCache.size()) {
            showSnackbar(R.string.incidencia_error_porte_requerido, Snackbar.LENGTH_SHORT);
            return;
        }

        Porte porte = portesCache.get(portePosition);
        if (porte.getId() == null) {
            showSnackbar(R.string.incidencia_error_porte_requerido, Snackbar.LENGTH_SHORT);
            return;
        }

        int severidadPosition = binding.severidadSpinner.getSelectedItemPosition();
        if (severidadPosition <= PLACEHOLDER_SELECTION_INDEX) {
            showSnackbar(R.string.incidencia_error_severidad_requerida, Snackbar.LENGTH_SHORT);
            return;
        }

        int prioridadPosition = binding.prioridadSpinner.getSelectedItemPosition();
        if (prioridadPosition <= PLACEHOLDER_SELECTION_INDEX) {
            showSnackbar(R.string.incidencia_error_prioridad_requerida, Snackbar.LENGTH_SHORT);
            return;
        }

        String severidad = toBackendSeverity(severidadPosition);
        String prioridad = toBackendPriority(prioridadPosition);

        enviarIncidencia(porte.getId(), titulo, descripcion, severidad, prioridad);
    }

    private void enviarIncidencia(long porteId, String titulo, String descripcion,
                                   String severidad, String prioridad) {
        setLoading(true);

        repository.crearIncidencia(porteId, titulo, descripcion, severidad, prioridad,
                new IncidenciaRepository.CrearCallback() {
                    @Override
                    public void onSuccess(@NonNull com.cargohub.mobile.data.model.IncidenciaResponse incidencia) {
                        if (!isAdded()) {
                            return;
                        }
                        setLoading(false);
                        Toast.makeText(requireContext(),
                                R.string.incidencia_exito,
                                Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (!isAdded()) {
                            return;
                        }
                        setLoading(false);
                        showSnackbar(message, R.string.generic_api_error_short, Snackbar.LENGTH_LONG);
                    }
                });
    }

    private void showSnackbar(@StringRes int messageRes, int duration) {
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, messageRes, duration).show();
        }
    }

    private void showSnackbar(@Nullable String message, @StringRes int fallbackMessageRes, int duration) {
        String resolvedMessage = message != null ? message.trim() : "";
        if (resolvedMessage.isEmpty()) {
            showSnackbar(fallbackMessageRes, duration);
            return;
        }
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, resolvedMessage, duration).show();
        }
    }

    private void setLoading(boolean loading) {
        binding.loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.submitButton.setEnabled(!loading);
        binding.tituloInput.setEnabled(!loading);
        binding.descripcionInput.setEnabled(!loading);
        binding.porteSpinner.setEnabled(!loading);
        binding.severidadSpinner.setEnabled(!loading);
        binding.prioridadSpinner.setEnabled(!loading);
    }

    private void limpiarFormulario() {
        binding.tituloInput.setText("");
        binding.descripcionInput.setText("");
        binding.severidadSpinner.setSelection(0);
        binding.prioridadSpinner.setSelection(0);
        if (porteAdapter.getCount() > 0) {
            binding.porteSpinner.setSelection(0);
        }
        binding.tituloInputLayout.setError(null);
        binding.descripcionInputLayout.setError(null);
    }

    @NonNull
    private String toBackendSeverity(int spinnerPosition) {
        int enumIndex = spinnerPosition - 1;
        if (enumIndex < 0 || enumIndex >= severidadOptions.length) {
            return SeveridadIncidencia.MEDIA.name();
        }
        return severidadOptions[enumIndex].name();
    }

    @NonNull
    private String toBackendPriority(int spinnerPosition) {
        int enumIndex = spinnerPosition - 1;
        if (enumIndex < 0 || enumIndex >= prioridadOptions.length) {
            return PrioridadIncidencia.MEDIA.name();
        }
        return prioridadOptions[enumIndex].name();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
