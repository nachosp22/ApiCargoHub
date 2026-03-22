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

    private FragmentNuevaIncidenciaBinding binding;
    private final IncidenciaRepository repository = new IncidenciaRepository();
    private PorteSpinnerAdapter porteAdapter;
    private List<Porte> portesCache;

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
        setupSpinners();
        setupSubmitButton();
        loadPortes();
    }

    private void setupSpinners() {
        String[] severidades = new String[SeveridadIncidencia.values().length + 1];
        severidades[0] = "Seleccionar...";
        int idx = 1;
        for (SeveridadIncidencia s : SeveridadIncidencia.values()) {
            severidades[idx++] = s.name();
        }
        ArrayAdapter<String> severidadAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_porte_spinner,
                severidades
        );
        severidadAdapter.setDropDownViewResource(R.layout.item_porte_spinner_dropdown);
        binding.severidadSpinner.setAdapter(severidadAdapter);
        binding.severidadSpinner.setSelection(0);

        String[] prioridades = new String[PrioridadIncidencia.values().length + 1];
        prioridades[0] = "Seleccionar...";
        idx = 1;
        for (PrioridadIncidencia p : PrioridadIncidencia.values()) {
            prioridades[idx++] = p.name();
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
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
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
            Snackbar.make(binding.getRoot(),
                    R.string.incidencia_error_porte_requerido,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        Porte porte = portesCache.get(portePosition);
        if (porte.getId() == null) {
            Snackbar.make(binding.getRoot(),
                    R.string.incidencia_error_porte_requerido,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        String severidad = binding.severidadSpinner.getSelectedItem() != null
                ? binding.severidadSpinner.getSelectedItem().toString() : "MEDIA";
        String prioridad = binding.prioridadSpinner.getSelectedItem() != null
                ? binding.prioridadSpinner.getSelectedItem().toString() : "MEDIA";

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
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                    }
                });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
