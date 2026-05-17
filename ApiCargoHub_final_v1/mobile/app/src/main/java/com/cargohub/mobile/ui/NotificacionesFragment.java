package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.NotificacionRepository;
import com.cargohub.mobile.data.model.Notificacion;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class NotificacionesFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private LinearLayout loadingContainer;
    private LinearLayout emptyContainer;
    private LinearLayout errorContainer;
    private TextView errorMessage;
    private MaterialButton retryButton;
    private MaterialButton markAllReadButton;

    private final NotificacionRepository repository = new NotificacionRepository();
    private NotificacionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notificaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupRetryButton();
        setupMarkAllReadButton();
        loadNotificaciones();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.notificacionesRecyclerView);
        loadingContainer = view.findViewById(R.id.loadingContainer);
        emptyContainer = view.findViewById(R.id.emptyContainer);
        errorContainer = view.findViewById(R.id.errorContainer);
        errorMessage = view.findViewById(R.id.errorMessage);
        retryButton = view.findViewById(R.id.retryButton);
        markAllReadButton = view.findViewById(R.id.markAllReadButton);
    }

    private void setupRecyclerView() {
        adapter = new NotificacionAdapter();
        adapter.setOnItemClickListener(this::onNotificacionClicked);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.ch_error, R.color.ch_warning);
        swipeRefresh.setOnRefreshListener(this::loadNotificaciones);
    }

    private void setupRetryButton() {
        retryButton.setOnClickListener(v -> loadNotificaciones());
    }

    private void setupMarkAllReadButton() {
        markAllReadButton.setOnClickListener(v -> {
            repository.markAllAsRead(new NotificacionRepository.MarcarTodasCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            getString(R.string.notificaciones_all_marked_read),
                            Toast.LENGTH_SHORT).show();
                    loadNotificaciones();
                }

                @Override
                public void onError(@NonNull String message) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadNotificaciones() {
        showLoading();

        repository.getNotificaciones(new NotificacionRepository.NotificacionesCallback() {
            @Override
            public void onSuccess(@NonNull List<Notificacion> notificaciones) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                if (notificaciones.isEmpty()) {
                    showEmpty();
                } else {
                    showContent(notificaciones);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                showError(message);
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

    private void showContent(List<Notificacion> notificaciones) {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.setNotificaciones(notificaciones);
    }

    private void showError(String message) {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        errorMessage.setText(message);
    }

    private void onNotificacionClicked(@NonNull Notificacion notificacion) {
        if (notificacion.getId() == null) return;

        // Mark as read first
        if (!notificacion.isLeida()) {
            repository.markAsRead(notificacion.getId(), new NotificacionRepository.MarcarLeidaCallback() {
                @Override
                public void onSuccess(@NonNull Notificacion updated) {
                    // Silently marked
                }

                @Override
                public void onError(@NonNull String message) {
                    // Non-critical, continue navigation
                }
            });
        }

        // Navigate based on type
        String tipo = notificacion.getTipo();
        Long referenciaId = notificacion.getReferenciaId();
        if (tipo == null || referenciaId == null) return;

        switch (tipo) {
            case "PORTE_ASIGNADO":
            case "PORTE_ACTUALIZADO":
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.contentFragmentContainer,
                                TripDetailFragment.newInstance(referenciaId))
                        .addToBackStack(null)
                        .commit();
                break;
            case "OFERTA_NUEVA":
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.contentFragmentContainer, new OfferInboxFragment())
                        .addToBackStack(null)
                        .commit();
                break;
        }
    }
}
