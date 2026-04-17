package com.cargohub.mobile.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.model.EstadoVehiculo;
import com.cargohub.mobile.data.model.Vehiculo;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class VehiculoAdapter extends RecyclerView.Adapter<VehiculoAdapter.VehiculoViewHolder> {

    public interface VehiculoActionListener {
        void onToggleState(@NonNull Vehiculo vehiculo);
    }

    private final List<Vehiculo> vehiculos = new ArrayList<>();
    private final VehiculoActionListener listener;

    public VehiculoAdapter(@NonNull VehiculoActionListener listener) {
        this.listener = listener;
    }

    public void setVehiculos(@NonNull List<Vehiculo> items) {
        vehiculos.clear();
        vehiculos.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VehiculoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehiculo, parent, false);
        return new VehiculoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehiculoViewHolder holder, int position) {
        holder.bind(vehiculos.get(position));
    }

    @Override
    public int getItemCount() {
        return vehiculos.size();
    }

    final class VehiculoViewHolder extends RecyclerView.ViewHolder {

        private final TextView matriculaText;
        private final TextView summaryText;
        private final TextView dimensionsText;
        private final MaterialButton stateButton;

        private VehiculoViewHolder(@NonNull View itemView) {
            super(itemView);
            matriculaText = itemView.findViewById(R.id.vehiculoItemMatriculaText);
            summaryText = itemView.findViewById(R.id.vehiculoItemSummaryText);
            dimensionsText = itemView.findViewById(R.id.vehiculoItemDimensionsText);
            stateButton = itemView.findViewById(R.id.vehiculoItemStateButton);
        }

        private void bind(@NonNull Vehiculo vehiculo) {
            String vehicleName = UiFormatters.valueOrFallback(vehiculo.getMatricula(), "Matricula pendiente")
                    + " - "
                    + UiFormatters.valueOrFallback(vehiculo.getMarca(), "Marca")
                    + " "
                    + UiFormatters.valueOrFallback(vehiculo.getModelo(), "Modelo");
            matriculaText.setText(vehicleName.trim());
            summaryText.setText(UiFormatters.formatVehiculoSummary(vehiculo));
            dimensionsText.setText(UiFormatters.formatVehiculoDimensions(vehiculo));

            // Progressive disclosure: dimensions hidden, toggle button visible but compact.
            // Tap card to expand dimensions.
            dimensionsText.setVisibility(View.GONE);
            boolean active = vehiculo.getEstado() == EstadoVehiculo.DISPONIBLE;
            stateButton.setText(active ? R.string.vehicle_action_deactivate : R.string.vehicle_action_activate);
            stateButton.setOnClickListener(v -> listener.onToggleState(vehiculo));

            itemView.setOnClickListener(v -> {
                boolean isVisible = dimensionsText.getVisibility() == View.VISIBLE;
                dimensionsText.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            });
        }
    }
}
