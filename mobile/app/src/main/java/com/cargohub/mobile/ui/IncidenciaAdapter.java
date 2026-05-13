package com.cargohub.mobile.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.model.EstadoIncidencia;
import com.cargohub.mobile.data.model.IncidenciaResponse;
import com.cargohub.mobile.data.model.SeveridadIncidencia;

import java.util.ArrayList;
import java.util.List;

public class IncidenciaAdapter extends RecyclerView.Adapter<IncidenciaAdapter.IncidenciaViewHolder> {

    private final List<IncidenciaResponse> incidencias = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(IncidenciaResponse incidencia);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setIncidencias(List<IncidenciaResponse> nuevas) {
        incidencias.clear();
        if (nuevas != null) {
            incidencias.addAll(nuevas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IncidenciaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incidencia, parent, false);
        return new IncidenciaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidenciaViewHolder holder, int position) {
        IncidenciaResponse incidencia = incidencias.get(position);
        holder.bind(incidencia);
    }

    @Override
    public int getItemCount() {
        return incidencias.size();
    }

    class IncidenciaViewHolder extends RecyclerView.ViewHolder {
        private final TextView tituloText;
        private final TextView estadoText;
        private final TextView descripcionText;
        private final TextView severidadText;
        private final TextView fechaText;

        IncidenciaViewHolder(@NonNull View itemView) {
            super(itemView);
            tituloText = itemView.findViewById(R.id.incidenciaTitulo);
            estadoText = itemView.findViewById(R.id.incidenciaEstado);
            descripcionText = itemView.findViewById(R.id.incidenciaDescripcion);
            severidadText = itemView.findViewById(R.id.incidenciaSeveridad);
            fechaText = itemView.findViewById(R.id.incidenciaFecha);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(incidencias.get(pos));
                }
            });
        }

        void bind(IncidenciaResponse incidencia) {
            Context ctx = itemView.getContext();
            tituloText.setText(incidencia.getTitulo() != null ? incidencia.getTitulo() : "");
            descripcionText.setText(incidencia.getDescripcion() != null ? incidencia.getDescripcion() : "");

            EstadoIncidencia estado = incidencia.getEstado();
            if (estado != null) {
                estadoText.setText(estado.getDisplayName());
                int[] colors = getEstadoColors(ctx, estado);
                estadoText.setTextColor(colors[0]);
                Drawable background = estadoText.getBackground();
                if (background != null) {
                    background.setTint(colors[1]);
                }
            }

            SeveridadIncidencia severidad = incidencia.getSeveridad();
            if (severidad != null) {
                severidadText.setText(ctx.getString(R.string.incidencia_severity_label, severidad.getDisplayName()));
                int severityColor = getSeverityColor(ctx, severidad);
                severidadText.setTextColor(severityColor);
                severidadText.setBackgroundTintList(ColorStateList.valueOf(getSeveritySoftColor(ctx, severidad)));
            }

            String fecha = incidencia.getFechaReporte();
            if (fecha != null && !fecha.isEmpty()) {
                fechaText.setText(fecha.substring(0, Math.min(16, fecha.length())));
            } else {
                fechaText.setText("");
            }
        }

        private int[] getEstadoColors(Context ctx, EstadoIncidencia estado) {
            switch (estado) {
                case ABIERTA:
                    return new int[]{
                            ctx.getColor(R.color.ch_error_text),
                            ctx.getColor(R.color.ch_error_soft)
                    };
                case EN_REVISION:
                    return new int[]{
                            ctx.getColor(R.color.ch_warning_text),
                            ctx.getColor(R.color.ch_warning_soft)
                    };
                case RESUELTA:
                    return new int[]{
                            ctx.getColor(R.color.ch_success_text),
                            ctx.getColor(R.color.ch_success_soft)
                    };
                case DESESTIMADA:
                default:
                    return new int[]{
                            ctx.getColor(R.color.ch_text_secondary),
                            ctx.getColor(R.color.ch_surface_alt)
                    };
            }
        }

        private int getSeverityColor(Context ctx, SeveridadIncidencia severidad) {
            switch (severidad) {
                case ALTA:
                    return ctx.getColor(R.color.ch_error);
                case MEDIA:
                    return ctx.getColor(R.color.ch_warning);
                case BAJA:
                default:
                    return ctx.getColor(R.color.ch_success);
            }
        }

        private int getSeveritySoftColor(Context ctx, SeveridadIncidencia severidad) {
            switch (severidad) {
                case ALTA:
                    return ctx.getColor(R.color.ch_error_soft);
                case MEDIA:
                    return ctx.getColor(R.color.ch_warning_soft);
                case BAJA:
                default:
                    return ctx.getColor(R.color.ch_success_soft);
            }
        }
    }
}
