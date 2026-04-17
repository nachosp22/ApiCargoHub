package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.FacturaRepository;
import com.cargohub.mobile.data.model.Factura;
import com.cargohub.mobile.data.model.FacturaResumen;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Locale;

public class FacturacionFragment extends Fragment {

    private final FacturaRepository repository = new FacturaRepository();
    private FacturaAdapter adapter;

    // State
    private String periodoActual = "MES";
    private int currentPage = 0;
    private int totalPages = 1;
    private boolean isLoadingMore = false;

    // Views
    private SwipeRefreshLayout swipeRefresh;
    private View contentContainer;
    private View loadingContainer;
    private View emptyContainer;
    private View errorContainer;
    private TextView errorMessage;
    private TextView resumenFacturado;
    private TextView resumenPagado;
    private TextView resumenPendiente;
    private ChipGroup periodoChipGroup;
    private Chip chipSemana;
    private Chip chipMes;
    private Chip chipAnio;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_facturacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();
        setupChips();
        setupSwipeRefresh();
        setupRetry(view);

        // Default: Mes selected
        chipMes.setChecked(true);
        loadAll();
    }

    private void bindViews(View v) {
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        contentContainer = v.findViewById(R.id.contentContainer);
        loadingContainer = v.findViewById(R.id.loadingContainer);
        emptyContainer = v.findViewById(R.id.emptyContainer);
        errorContainer = v.findViewById(R.id.errorContainer);
        errorMessage = v.findViewById(R.id.errorMessage);
        resumenFacturado = v.findViewById(R.id.resumenFacturado);
        resumenPagado = v.findViewById(R.id.resumenPagado);
        resumenPendiente = v.findViewById(R.id.resumenPendiente);
        periodoChipGroup = v.findViewById(R.id.periodoChipGroup);
        chipSemana = v.findViewById(R.id.chipSemana);
        chipMes = v.findViewById(R.id.chipMes);
        chipAnio = v.findViewById(R.id.chipAnio);
        recyclerView = v.findViewById(R.id.facturasRecyclerView);
    }

    private void setupRecyclerView() {
        adapter = new FacturaAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);

        // Pagination: load more when reaching the end
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                int totalItemCount = lm.getItemCount();
                int lastVisible = lm.findLastVisibleItemPosition();
                if (!isLoadingMore && currentPage + 1 < totalPages && lastVisible >= totalItemCount - 3) {
                    loadMoreFacturas();
                }
            }
        });
    }

    private void setupChips() {
        periodoChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                periodoActual = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipSemana) {
                    periodoActual = "SEMANA";
                } else if (id == R.id.chipMes) {
                    periodoActual = "MES";
                } else if (id == R.id.chipAnio) {
                    periodoActual = "ANIO";
                }
            }
            loadAll();
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.ch_primary);
        swipeRefresh.setOnRefreshListener(this::loadAll);
    }

    private void setupRetry(View v) {
        MaterialButton retryBtn = v.findViewById(R.id.retryButton);
        retryBtn.setOnClickListener(btn -> loadAll());
    }

    // ── Data loading ──

    private void loadAll() {
        currentPage = 0;
        totalPages = 1;
        showState(State.LOADING);
        loadResumen();
        loadFacturas(true);
    }

    private void loadResumen() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null) return;

        repository.getResumen(conductorId, periodoActual, new FacturaRepository.ResumenCallback() {
            @Override
            public void onSuccess(@NonNull FacturaResumen resumen) {
                if (!isAdded()) return;
                resumenFacturado.setText(formatCurrency(resumen.getTotalFacturado()));
                resumenPagado.setText(formatCurrency(resumen.getTotalPagado()));
                resumenPendiente.setText(formatCurrency(resumen.getTotalPendiente()));
            }

            @Override
            public void onError(@NonNull String message) {
                // Resumen error is non-critical, facturas list drives the state
            }
        });
    }

    private void loadFacturas(boolean replace) {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null) {
            showState(State.ERROR);
            errorMessage.setText("No se pudo identificar al conductor.");
            return;
        }

        repository.getFacturas(conductorId, null, null, null, currentPage, 20,
                new FacturaRepository.FacturasCallback() {
                    @Override
                    public void onSuccess(@NonNull List<Factura> facturas, int pages) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        isLoadingMore = false;
                        totalPages = pages;

                        if (replace) {
                            adapter.setFacturas(facturas);
                            if (facturas.isEmpty()) {
                                showState(State.EMPTY);
                            } else {
                                showState(State.CONTENT);
                            }
                        } else {
                            adapter.addFacturas(facturas);
                        }
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        isLoadingMore = false;
                        if (replace) {
                            showState(State.ERROR);
                            errorMessage.setText(message);
                        }
                    }
                });
    }

    private void loadMoreFacturas() {
        isLoadingMore = true;
        currentPage++;
        loadFacturas(false);
    }

    // ── UI state management ──

    private enum State { LOADING, CONTENT, EMPTY, ERROR }

    private void showState(State state) {
        contentContainer.setVisibility(state == State.CONTENT ? View.VISIBLE : View.GONE);
        loadingContainer.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        emptyContainer.setVisibility(state == State.EMPTY ? View.VISIBLE : View.GONE);
        errorContainer.setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);
    }

    private String formatCurrency(Double amount) {
        if (amount == null) return "0,00 €";
        return String.format(Locale.forLanguageTag("es"), "%,.2f €", amount);
    }
}
