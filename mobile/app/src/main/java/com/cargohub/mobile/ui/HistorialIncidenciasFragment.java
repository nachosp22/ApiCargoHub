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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.IncidenciaRepository;
import com.cargohub.mobile.data.model.IncidenciaResponse;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class HistorialIncidenciasFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private LinearLayout loadingContainer;
    private LinearLayout emptyContainer;
    private LinearLayout errorContainer;
    private TextView errorMessage;
    private MaterialButton retryButton;

    private final IncidenciaRepository repository = new IncidenciaRepository();
    private IncidenciaAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_incidencias, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupRetryButton();
        loadIncidencias();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.incidenciasRecyclerView);
        loadingContainer = view.findViewById(R.id.loadingContainer);
        emptyContainer = view.findViewById(R.id.emptyContainer);
        errorContainer = view.findViewById(R.id.errorContainer);
        errorMessage = view.findViewById(R.id.errorMessage);
        retryButton = view.findViewById(R.id.retryButton);
    }

    private void setupRecyclerView() {
        adapter = new IncidenciaAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.ch_blue_500, R.color.ch_blue_600);
        swipeRefresh.setOnRefreshListener(this::loadIncidencias);
    }

    private void setupRetryButton() {
        retryButton.setOnClickListener(v -> loadIncidencias());
    }

    private void loadIncidencias() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showError(getString(R.string.incidencia_error_sesion));
            return;
        }

        showLoading();

        repository.getHistorial(conductorId, new IncidenciaRepository.IncidenciasCallback() {
            @Override
            public void onSuccess(@NonNull List<IncidenciaResponse> incidencias) {
                if (!isAdded()) {
                    return;
                }
                swipeRefresh.setRefreshing(false);
                if (incidencias.isEmpty()) {
                    showEmpty();
                } else {
                    showContent(incidencias);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                swipeRefresh.setRefreshing(false);
                handleError(message);
            }
        });
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showEmpty() {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showContent(List<IncidenciaResponse> incidencias) {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.setIncidencias(incidencias);
    }

    private void showError(String message) {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        errorMessage.setText(message);
    }

    private void handleError(String message) {
        if (message.contains("sesion") || message.contains("sesión")) {
            requireActivity().finish();
        } else {
            showError(message);
        }
    }
}
