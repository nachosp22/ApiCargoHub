package com.cargohub.mobile.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class PorteCardAdapter extends RecyclerView.Adapter<PorteCardAdapter.PorteViewHolder> {

    public interface OnPorteClickListener {
        void onPorteSelected(@NonNull Porte porte);
    }

    private final List<Porte> portes = new ArrayList<>();
    private final OnPorteClickListener listener;
    private String ctaLabel;

    public PorteCardAdapter(@NonNull OnPorteClickListener listener) {
        this.listener = listener;
    }

    public void setPortes(@NonNull List<Porte> items) {
        portes.clear();
        portes.addAll(items);
        notifyDataSetChanged();
    }

    public void setCtaLabel(String ctaLabel) {
        this.ctaLabel = ctaLabel;
    }

    @NonNull
    @Override
    public PorteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_porte_card, parent, false);
        return new PorteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PorteViewHolder holder, int position) {
        holder.bind(portes.get(position));
    }

    @Override
    public int getItemCount() {
        return portes.size();
    }

    final class PorteViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleText;
        private final TextView stateText;
        private final TextView scheduleText;
        private final TextView cargoText;
        private final TextView priceText;
        private final MaterialButton actionButton;

        private PorteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.porteCardTitleText);
            stateText = itemView.findViewById(R.id.porteCardStateText);
            scheduleText = itemView.findViewById(R.id.porteCardScheduleText);
            cargoText = itemView.findViewById(R.id.porteCardCargoText);
            priceText = itemView.findViewById(R.id.porteCardPriceText);
            actionButton = itemView.findViewById(R.id.porteCardActionButton);
        }

        private void bind(@NonNull Porte porte) {
            titleText.setText(UiFormatters.formatPorteTitle(porte));
            stateText.setText(UiFormatters.formatPorteState(porte));
            scheduleText.setText(UiFormatters.formatPorteSchedule(porte));
            cargoText.setText(UiFormatters.formatPorteCargo(porte));
            priceText.setText(UiFormatters.formatPortePrice(porte));
            applyStateColor(stateText, porte.getEstadoPorte());

            // Progressive disclosure: hide cargo and action button in list view.
            // Tapping the card navigates to detail where full info is shown.
            cargoText.setVisibility(View.GONE);
            actionButton.setVisibility(View.GONE);

            itemView.setOnClickListener(view -> listener.onPorteSelected(porte));
        }
    }

    private void applyStateColor(@NonNull TextView stateText, EstadoPorte state) {
        int backgroundRes = R.color.ch_blue_100;
        int textRes = R.color.ch_blue_700;
        if (state == EstadoPorte.PENDIENTE) {
            backgroundRes = R.color.ch_warning_soft;
            textRes = R.color.ch_warning_text;
        } else if (state == EstadoPorte.ASIGNADO || state == EstadoPorte.EN_TRANSITO) {
            backgroundRes = R.color.ch_success_soft;
            textRes = R.color.ch_success_text;
        } else if (state == EstadoPorte.ENTREGADO) {
            backgroundRes = R.color.ch_blue_50;
            textRes = R.color.ch_blue_700;
        }
        stateText.setBackgroundResource(R.drawable.bg_state_chip);
        stateText.setBackgroundTintList(stateText.getContext().getColorStateList(backgroundRes));
        stateText.setTextColor(stateText.getContext().getColor(textRes));
    }
}
