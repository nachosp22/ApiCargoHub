package com.cargohub.mobile.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.model.Factura;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FacturaAdapter extends RecyclerView.Adapter<FacturaAdapter.FacturaViewHolder> {

    private final List<Factura> facturas = new ArrayList<>();
    private int expandedPosition = RecyclerView.NO_POSITION;

    public void setFacturas(List<Factura> nuevas) {
        facturas.clear();
        if (nuevas != null) {
            facturas.addAll(nuevas);
        }
        expandedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    public void addFacturas(List<Factura> nuevas) {
        if (nuevas == null || nuevas.isEmpty()) return;
        int start = facturas.size();
        facturas.addAll(nuevas);
        notifyItemRangeInserted(start, nuevas.size());
    }

    @NonNull
    @Override
    public FacturaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_factura, parent, false);
        return new FacturaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacturaViewHolder holder, int position) {
        holder.bind(facturas.get(position), position == expandedPosition);
    }

    @Override
    public int getItemCount() {
        return facturas.size();
    }

    class FacturaViewHolder extends RecyclerView.ViewHolder {
        private final TextView numeroText;
        private final TextView estadoText;
        private final TextView fechaText;
        private final TextView totalText;
        private final LinearLayout detalleContainer;
        private final TextView baseText;
        private final TextView ivaText;
        private final TextView porteIdText;

        FacturaViewHolder(@NonNull View itemView) {
            super(itemView);
            numeroText = itemView.findViewById(R.id.facturaNumero);
            estadoText = itemView.findViewById(R.id.facturaEstado);
            fechaText = itemView.findViewById(R.id.facturaFecha);
            totalText = itemView.findViewById(R.id.facturaTotal);
            detalleContainer = itemView.findViewById(R.id.facturaDetalleContainer);
            baseText = itemView.findViewById(R.id.facturaBase);
            ivaText = itemView.findViewById(R.id.facturaIva);
            porteIdText = itemView.findViewById(R.id.facturaPorteId);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                int prev = expandedPosition;
                if (expandedPosition == pos) {
                    expandedPosition = RecyclerView.NO_POSITION;
                } else {
                    expandedPosition = pos;
                }
                if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev);
                notifyItemChanged(pos);
            });
        }

        void bind(Factura factura, boolean expanded) {
            Context ctx = itemView.getContext();

            // Numero
            String numero = factura.getNumeroSerie();
            numeroText.setText(numero != null ? numero : "—");

            // Estado badge
            if (factura.isPagada()) {
                estadoText.setText(ctx.getString(R.string.billing_estado_pagada));
                estadoText.setTextColor(ctx.getColor(R.color.ch_success_text));
                estadoText.getBackground().setTint(ctx.getColor(R.color.ch_success_soft));
            } else {
                estadoText.setText(ctx.getString(R.string.billing_estado_pendiente));
                estadoText.setTextColor(ctx.getColor(R.color.ch_warning_text));
                estadoText.getBackground().setTint(ctx.getColor(R.color.ch_warning_soft));
            }

            // Fecha
            String fecha = factura.getFechaEmision();
            if (fecha != null && fecha.length() >= 10) {
                // API returns yyyy-MM-dd, display as dd/MM/yyyy
                String[] parts = fecha.substring(0, 10).split("-");
                if (parts.length == 3) {
                    fechaText.setText(parts[2] + "/" + parts[1] + "/" + parts[0]);
                } else {
                    fechaText.setText(fecha.substring(0, 10));
                }
            } else {
                fechaText.setText("—");
            }

            // Total
            totalText.setText(formatCurrency(factura.getImporteTotal()));

            // Expandable detail
            detalleContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (expanded) {
                baseText.setText(formatCurrency(factura.getBaseImponible()));
                ivaText.setText(formatCurrency(factura.getIva()));
                Long porteId = factura.getPorteId();
                porteIdText.setText(porteId != null ? "Porte #" + porteId : "—");
            }
        }

        private String formatCurrency(Double amount) {
            if (amount == null) return "0,00 €";
            return String.format(Locale.forLanguageTag("es"), "%,.2f €", amount);
        }
    }
}
