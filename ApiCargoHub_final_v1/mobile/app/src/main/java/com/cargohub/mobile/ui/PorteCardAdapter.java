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

import java.util.ArrayList;
import java.util.List;

public class PorteCardAdapter extends RecyclerView.Adapter<PorteCardAdapter.PorteViewHolder> {

    public interface OnPorteClickListener {
        void onPorteSelected(@NonNull Porte porte);
    }

    private final List<Porte> portes = new ArrayList<>();
    private final OnPorteClickListener listener;

    public PorteCardAdapter(@NonNull OnPorteClickListener listener) {
        this.listener = listener;
    }

    public void setPortes(@NonNull List<Porte> items) {
        portes.clear();
        portes.addAll(items);
        notifyDataSetChanged();
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
        private final TextView routeText;
        private final TextView dateText;
        private final TextView kmText;
        private final TextView priceText;

        private PorteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.porteCardTitleText);
            stateText = itemView.findViewById(R.id.porteCardStateText);
            routeText = itemView.findViewById(R.id.porteCardRouteText);
            dateText = itemView.findViewById(R.id.porteCardDateText);
            kmText = itemView.findViewById(R.id.porteCardKmText);
            priceText = itemView.findViewById(R.id.porteCardPriceText);
        }

        private void bind(@NonNull Porte porte) {
            titleText.setText(UiFormatters.formatPorteShortTitle(porte));
            stateText.setVisibility(View.GONE);
            routeText.setText(UiFormatters.formatPorteRoute(porte));
            dateText.setText(itemView.getContext().getString(
                    R.string.offer_card_date_value,
                    UiFormatters.formatDateTime(porte.getFechaRecogida())
            ));
            kmText.setText(itemView.getContext().getString(
                    R.string.offer_card_km_value,
                    UiFormatters.formatPorteDistance(porte)
            ));
            priceText.setText(itemView.getContext().getString(
                    R.string.offer_card_price_value,
                    UiFormatters.formatPortePrice(porte)
            ));

            itemView.setContentDescription(itemView.getContext().getString(
                    R.string.offer_card_content_description,
                    UiFormatters.formatPorteShortTitle(porte),
                    UiFormatters.formatPorteRoute(porte)
            ));

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
