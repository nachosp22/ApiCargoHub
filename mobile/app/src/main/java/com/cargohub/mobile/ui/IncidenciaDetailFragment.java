package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.IncidenciaRepository;
import com.cargohub.mobile.data.model.IncidenciaEventoResponse;
import com.cargohub.mobile.data.model.IncidenciaResponse;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class IncidenciaDetailFragment extends Fragment {

    private static final String ARG_INCIDENCIA_ID = "incidencia_id";

    private final IncidenciaRepository repository = new IncidenciaRepository();

    private long incidenciaId;
    private LinearLayout loadingContainer;
    private LinearLayout errorContainer;
    private LinearLayout contentContainer;
    private TextView errorText;
    private TextView titleText;
    private TextView stateText;
    private TextView metadataText;
    private TextView descriptionText;
    private TextView historyEmptyText;
    private TextView historyText;

    public static IncidenciaDetailFragment newInstance(long incidenciaId) {
        IncidenciaDetailFragment fragment = new IncidenciaDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_INCIDENCIA_ID, incidenciaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_incidencia_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        incidenciaId = requireArguments().getLong(ARG_INCIDENCIA_ID);

        loadingContainer = view.findViewById(R.id.incidenciaDetailLoadingContainer);
        errorContainer = view.findViewById(R.id.incidenciaDetailErrorContainer);
        contentContainer = view.findViewById(R.id.incidenciaDetailContentContainer);
        errorText = view.findViewById(R.id.incidenciaDetailErrorText);
        titleText = view.findViewById(R.id.incidenciaDetailTitle);
        stateText = view.findViewById(R.id.incidenciaDetailState);
        metadataText = view.findViewById(R.id.incidenciaDetailMetadata);
        descriptionText = view.findViewById(R.id.incidenciaDetailDescription);
        historyEmptyText = view.findViewById(R.id.incidenciaDetailHistoryEmpty);
        historyText = view.findViewById(R.id.incidenciaDetailHistoryText);
        MaterialButton retryButton = view.findViewById(R.id.incidenciaDetailRetryButton);
        retryButton.setOnClickListener(v -> loadData());

        loadData();
    }

    private void loadData() {
        showLoading();
        repository.getPorId(incidenciaId, new IncidenciaRepository.IncidenciaDetalleCallback() {
            @Override
            public void onSuccess(@NonNull IncidenciaResponse incidencia) {
                if (!isAdded()) {
                    return;
                }
                renderIncidencia(incidencia);
                showContent();
                loadHistory();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                showError(message);
            }
        });
    }

    private void loadHistory() {
        repository.getHistorialIncidencia(incidenciaId, new IncidenciaRepository.HistorialCallback() {
            @Override
            public void onSuccess(@NonNull List<IncidenciaEventoResponse> historial) {
                if (!isAdded()) {
                    return;
                }
                renderHistory(historial);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                historyEmptyText.setText(message);
                historyEmptyText.setVisibility(View.VISIBLE);
                historyText.setVisibility(View.GONE);
            }
        });
    }

    private void renderIncidencia(@NonNull IncidenciaResponse incidencia) {
        String title = incidencia.getTitulo();
        if (title == null || title.trim().isEmpty()) {
            title = getString(R.string.incidencia_detail_title_fallback, incidenciaId);
        }
        titleText.setText(title);

        String estado = incidencia.getEstado() != null
                ? incidencia.getEstado().name().replace("_", " ")
                : getString(R.string.incidencia_detail_state_unknown);
        stateText.setText(estado);

        String severidad = incidencia.getSeveridad() != null ? incidencia.getSeveridad().name() : "-";
        String prioridad = incidencia.getPrioridad() != null ? incidencia.getPrioridad().name() : "-";
        String fecha = UiFormatters.formatDateTime(incidencia.getFechaReporte());
        metadataText.setText(getString(R.string.incidencia_detail_metadata, severidad, prioridad, fecha));

        String description = incidencia.getDescripcion();
        if (description == null || description.trim().isEmpty()) {
            description = getString(R.string.incidencia_detail_description_fallback);
        }
        descriptionText.setText(description);
    }

    private void renderHistory(@NonNull List<IncidenciaEventoResponse> historial) {
        if (historial.isEmpty()) {
            historyEmptyText.setText(R.string.incidencia_detail_history_empty);
            historyEmptyText.setVisibility(View.VISIBLE);
            historyText.setVisibility(View.GONE);
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (IncidenciaEventoResponse evento : historial) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            String fecha = UiFormatters.formatDateTime(evento.getFecha());
            String accion = evento.getAccion() != null ? evento.getAccion() : getString(R.string.incidencia_detail_history_action_default);
            String estadoNuevo = evento.getEstadoNuevo() != null
                    ? evento.getEstadoNuevo().name().replace("_", " ")
                    : getString(R.string.incidencia_detail_state_unknown);
            String comentario = evento.getComentario();
            if (comentario == null || comentario.trim().isEmpty()) {
                comentario = getString(R.string.incidencia_detail_history_comment_empty);
            }
            builder.append(String.format(Locale.getDefault(), "%s - %s (%s)\n%s", fecha, accion, estadoNuevo, comentario));
        }
        historyText.setText(builder.toString());
        historyEmptyText.setVisibility(View.GONE);
        historyText.setVisibility(View.VISIBLE);
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
    }

    private void showError(@NonNull String message) {
        loadingContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        if (message.trim().isEmpty()) {
            errorText.setText(R.string.incidencia_detail_error_default);
        } else {
            errorText.setText(message);
        }
    }

    private void showContent() {
        loadingContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.VISIBLE);
    }
}
