package com.cargohub.mobile.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.EstadisticasRepository;
import com.cargohub.mobile.data.model.EstadisticasConductor;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EstadisticasFragment extends Fragment {

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

    private final EstadisticasRepository repository = new EstadisticasRepository();

    private SwipeRefreshLayout swipeRefresh;
    private View contentContainer;
    private View loadingContainer;
    private View errorContainer;
    private TextView errorMessage;
    private View retryButton;

    // KPIs
    private TextView kpiPortesCompletados;
    private TextView kpiIngresoTotal;
    private TextView kpiMediaPorPorte;
    private TextView kpiKmRecorridos;
    private TextView kpiPortesEnCurso;

    // Chart & chips
    private LinearLayout chartContainer;
    private ChipGroup estadosChipGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_estadisticas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        contentContainer = view.findViewById(R.id.contentContainer);
        loadingContainer = view.findViewById(R.id.loadingContainer);
        errorContainer = view.findViewById(R.id.errorContainer);
        errorMessage = view.findViewById(R.id.errorMessage);
        retryButton = view.findViewById(R.id.retryButton);

        kpiPortesCompletados = view.findViewById(R.id.kpiPortesCompletados);
        kpiIngresoTotal = view.findViewById(R.id.kpiIngresoTotal);
        kpiMediaPorPorte = view.findViewById(R.id.kpiMediaPorPorte);
        kpiKmRecorridos = view.findViewById(R.id.kpiKmRecorridos);
        kpiPortesEnCurso = view.findViewById(R.id.kpiPortesEnCurso);

        chartContainer = view.findViewById(R.id.chartContainer);
        estadosChipGroup = view.findViewById(R.id.estadosChipGroup);

        swipeRefresh.setOnRefreshListener(this::loadData);
        retryButton.setOnClickListener(v -> loadData());

        loadData();
    }

    private void loadData() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null) {
            showError(getString(R.string.stats_error_missing_driver_id));
            return;
        }

        showLoading();

        repository.getEstadisticas(conductorId, null, null,
                new EstadisticasRepository.EstadisticasCallback() {
                    @Override
                    public void onSuccess(@NonNull EstadisticasConductor stats) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            bindData(stats);
                            showContent();
                        });
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> showError(message));
                    }
                });
    }

    private void bindData(@NonNull EstadisticasConductor stats) {
        // KPIs
        kpiPortesCompletados.setText(String.valueOf(safeL(stats.getPortesCompletados())));
        kpiIngresoTotal.setText(CURRENCY.format(safeD(stats.getIngresoTotal())));
        kpiMediaPorPorte.setText(CURRENCY.format(safeD(stats.getMediaPorPorte())));
        kpiKmRecorridos.setText(String.format(Locale.US, "%.0f km", safeD(stats.getKmRecorridos())));
        kpiPortesEnCurso.setText(String.valueOf(safeL(stats.getPortesEnCurso())));

        // Bar chart
        buildBarChart(stats.getIngresoPorMes());

        // Estado chips
        buildEstadoChips(stats.getPortesPorEstado());
    }

    private void buildBarChart(@Nullable List<EstadisticasConductor.IngresoMensual> datos) {
        chartContainer.removeAllViews();
        if (datos == null || datos.isEmpty()) return;

        // Show last 6 months only
        int start = Math.max(0, datos.size() - 6);
        List<EstadisticasConductor.IngresoMensual> visible = datos.subList(start, datos.size());

        // Find max for proportional heights
        double max = 0;
        for (EstadisticasConductor.IngresoMensual m : visible) {
            if (m.getTotal() != null && m.getTotal() > max) max = m.getTotal();
        }
        if (max == 0) max = 1;

        int barColor = ContextCompat.getColor(requireContext(), R.color.ch_primary);
        int labelColor = ContextCompat.getColor(requireContext(), R.color.ch_text_secondary);
        int valueColor = ContextCompat.getColor(requireContext(), R.color.ch_text_primary);

        for (EstadisticasConductor.IngresoMensual m : visible) {
            LinearLayout column = new LinearLayout(requireContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);

            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            int marginPx = dpToPx(4);
            colParams.setMargins(marginPx, 0, marginPx, 0);
            column.setLayoutParams(colParams);
            column.setMinimumWidth(dpToPx(48));

            // Value label on top
            TextView valueTv = new TextView(requireContext());
            double total = m.getTotal() != null ? m.getTotal() : 0;
            valueTv.setText(String.format(Locale.US, "%.0f€", total));
            valueTv.setTextSize(10);
            valueTv.setTextColor(valueColor);
            valueTv.setGravity(Gravity.CENTER);
            column.addView(valueTv, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            // Bar
            View bar = new View(requireContext());
            bar.setBackgroundColor(barColor);
            float weight = (float) (total / max);
            int barHeight = (int) (dpToPx(120) * Math.max(weight, 0.02f));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, barHeight);
            barParams.topMargin = dpToPx(4);
            bar.setLayoutParams(barParams);

            // Round top corners
            android.graphics.drawable.GradientDrawable barBg = new android.graphics.drawable.GradientDrawable();
            barBg.setColor(barColor);
            barBg.setCornerRadii(new float[]{dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4), 0, 0, 0, 0});
            bar.setBackground(barBg);

            column.addView(bar);

            // Month label
            TextView monthTv = new TextView(requireContext());
            String mesLabel = m.getMes() != null && m.getMes().length() >= 7
                    ? m.getMes().substring(5) : "?";
            monthTv.setText(mesLabel);
            monthTv.setTextSize(11);
            monthTv.setTextColor(labelColor);
            monthTv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            monthParams.topMargin = dpToPx(4);
            monthTv.setLayoutParams(monthParams);
            column.addView(monthTv);

            chartContainer.addView(column);
        }
    }

    private void buildEstadoChips(@Nullable Map<String, Long> porEstado) {
        estadosChipGroup.removeAllViews();
        if (porEstado == null || porEstado.isEmpty()) return;

        for (Map.Entry<String, Long> entry : porEstado.entrySet()) {
            Chip chip = new Chip(requireContext());
            chip.setText(formatEstado(entry.getKey()) + ": " + entry.getValue());
            chip.setClickable(false);
            chip.setCheckable(false);

            int[] colors = getEstadoColors(entry.getKey());
            chip.setChipBackgroundColor(ColorStateList.valueOf(colors[0]));
            chip.setTextColor(colors[1]);

            estadosChipGroup.addView(chip);
        }
    }

    @NonNull
    private String formatEstado(@NonNull String raw) {
        switch (raw) {
            case "ENTREGADO": return "Completado";
            case "FACTURADO": return "Facturado";
            case "CANCELADO": return "Cancelado";
            case "EN_TRANSITO": return "En curso";
            case "ASIGNADO": return "Asignado";
            case "PENDIENTE": return "Pendiente";
            default: return raw;
        }
    }

    private int[] getEstadoColors(@NonNull String raw) {
        switch (raw) {
            case "ENTREGADO":
            case "FACTURADO":
                return new int[]{
                        ContextCompat.getColor(requireContext(), R.color.ch_success_soft),
                        ContextCompat.getColor(requireContext(), R.color.ch_success_text)};
            case "CANCELADO":
                return new int[]{
                        ContextCompat.getColor(requireContext(), R.color.ch_error_soft),
                        ContextCompat.getColor(requireContext(), R.color.ch_error_text)};
            case "EN_TRANSITO":
                return new int[]{
                        ContextCompat.getColor(requireContext(), R.color.ch_warning_soft),
                        ContextCompat.getColor(requireContext(), R.color.ch_warning_text)};
            default:
                return new int[]{
                        ContextCompat.getColor(requireContext(), R.color.ch_surface_alt),
                        ContextCompat.getColor(requireContext(), R.color.ch_text_secondary)};
        }
    }

    // ── State management ──

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void showContent() {
        contentContainer.setVisibility(View.VISIBLE);
        loadingContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void showError(@NonNull String message) {
        errorContainer.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.GONE);
        errorMessage.setText(message);
        swipeRefresh.setRefreshing(false);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private static long safeL(@Nullable Long v) { return v != null ? v : 0; }
    private static double safeD(@Nullable Double v) { return v != null ? v : 0.0; }
}
