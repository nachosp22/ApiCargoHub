package com.cargohub.mobile.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AgendaAdapter extends RecyclerView.Adapter<AgendaAdapter.AgendaViewHolder> {

    public interface AgendaActionListener {
        void onDelete(@NonNull AgendaBloqueo bloqueo);
    }

    private final List<AgendaBloqueo> bloqueos = new ArrayList<>();
    private final AgendaActionListener listener;

    public AgendaAdapter(@NonNull AgendaActionListener listener) {
        this.listener = listener;
    }

    public void setBloqueos(@NonNull List<AgendaBloqueo> items) {
        bloqueos.clear();
        bloqueos.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AgendaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_agenda_bloqueo, parent, false);
        return new AgendaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AgendaViewHolder holder, int position) {
        holder.bind(bloqueos.get(position));
    }

    @Override
    public int getItemCount() {
        return bloqueos.size();
    }

    final class AgendaViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleText;
        private final TextView typeText;
        private final TextView rangeText;
        private final MaterialButton deleteButton;

        private AgendaViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.agendaItemTitleText);
            typeText = itemView.findViewById(R.id.agendaItemTypeText);
            rangeText = itemView.findViewById(R.id.agendaItemRangeText);
            deleteButton = itemView.findViewById(R.id.agendaItemDeleteButton);
        }

        private void bind(@NonNull AgendaBloqueo bloqueo) {
            titleText.setText(UiFormatters.valueOrFallback(bloqueo.getTitulo(), "Bloqueo operativo"));
            typeText.setText(bloqueo.getTipo() != null ? bloqueo.getTipo().name() : "TIPO PENDIENTE");
            rangeText.setText(UiFormatters.formatAgendaRange(bloqueo));

            // Progressive disclosure: delete button hidden by default.
            // Tap card to reveal delete action.
            deleteButton.setVisibility(View.GONE);
            deleteButton.setOnClickListener(v -> listener.onDelete(bloqueo));

            itemView.setOnClickListener(v -> {
                boolean isVisible = deleteButton.getVisibility() == View.VISIBLE;
                deleteButton.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            });
        }
    }
}
