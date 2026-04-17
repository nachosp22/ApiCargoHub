package com.cargohub.mobile.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.model.Notificacion;

import java.util.ArrayList;
import java.util.List;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder> {

    private final List<Notificacion> notificaciones = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Notificacion notificacion);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setNotificaciones(List<Notificacion> nuevas) {
        notificaciones.clear();
        if (nuevas != null) {
            notificaciones.addAll(nuevas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notificacion, parent, false);
        return new NotificacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacionViewHolder holder, int position) {
        Notificacion notificacion = notificaciones.get(position);
        holder.bind(notificacion);
    }

    @Override
    public int getItemCount() {
        return notificaciones.size();
    }

    class NotificacionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tituloText;
        private final TextView tipoText;
        private final TextView mensajeText;
        private final TextView fechaText;
        private final View unreadIndicator;

        NotificacionViewHolder(@NonNull View itemView) {
            super(itemView);
            tituloText = itemView.findViewById(R.id.notificacionTitulo);
            tipoText = itemView.findViewById(R.id.notificacionTipo);
            mensajeText = itemView.findViewById(R.id.notificacionMensaje);
            fechaText = itemView.findViewById(R.id.notificacionFecha);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(notificaciones.get(pos));
                }
            });
        }

        void bind(Notificacion notificacion) {
            Context ctx = itemView.getContext();

            tituloText.setText(notificacion.getTitulo());
            mensajeText.setText(notificacion.getMensaje());

            // Unread styling
            if (!notificacion.isLeida()) {
                unreadIndicator.setVisibility(View.VISIBLE);
                tituloText.setTypeface(tituloText.getTypeface(), Typeface.BOLD);
            } else {
                unreadIndicator.setVisibility(View.GONE);
                tituloText.setTypeface(Typeface.DEFAULT);
            }

            // Type badge
            String tipo = notificacion.getTipo();
            if (tipo != null) {
                String displayType = tipo.replace("_", " ");
                tipoText.setText(displayType);
                int[] colors = getTipoColors(ctx, tipo);
                tipoText.setTextColor(colors[0]);
                tipoText.getBackground().setTint(colors[1]);
            }

            // Date
            String fecha = notificacion.getFechaCreacion();
            if (fecha != null && !fecha.isEmpty()) {
                fechaText.setText(fecha.substring(0, Math.min(16, fecha.length())));
            } else {
                fechaText.setText("");
            }
        }

        private int[] getTipoColors(Context ctx, String tipo) {
            switch (tipo) {
                case "PORTE_ASIGNADO":
                    return new int[]{
                            ctx.getColor(R.color.ch_success_text),
                            ctx.getColor(R.color.ch_success_soft)
                    };
                case "OFERTA_NUEVA":
                    return new int[]{
                            ctx.getColor(R.color.ch_warning_text),
                            ctx.getColor(R.color.ch_warning_soft)
                    };
                case "PORTE_ACTUALIZADO":
                default:
                    return new int[]{
                            ctx.getColor(R.color.ch_text_secondary),
                            ctx.getColor(R.color.ch_surface_alt)
                    };
            }
        }
    }
}
