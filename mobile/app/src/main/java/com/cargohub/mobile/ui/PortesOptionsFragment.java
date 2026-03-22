package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;
import com.google.android.material.card.MaterialCardView;

public class PortesOptionsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_portes_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialCardView activeTripCard = view.findViewById(R.id.porteActiveCard);
        MaterialCardView upcomingTripsCard = view.findViewById(R.id.porteUpcomingCard);
        MaterialCardView historyTripsCard = view.findViewById(R.id.porteHistoryCard);

        activeTripCard.setOnClickListener(v -> showSoon(R.string.home_menu_section_trip_active));
        upcomingTripsCard.setOnClickListener(v -> showSoon(R.string.home_menu_section_trip_upcoming));
        historyTripsCard.setOnClickListener(v -> showSoon(R.string.home_menu_section_trip_history));
    }

    private void showSoon(int labelRes) {
        if (getContext() == null) {
            return;
        }
        String message = getString(R.string.home_placeholder_action, getString(labelRes));
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
