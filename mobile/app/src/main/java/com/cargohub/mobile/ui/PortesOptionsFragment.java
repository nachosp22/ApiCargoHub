package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        activeTripCard.setOnClickListener(v -> navigateTo(TripListFragment.newInstance(TripListFragment.MODE_ACTIVE)));
        upcomingTripsCard.setOnClickListener(v -> navigateTo(TripListFragment.newInstance(TripListFragment.MODE_UPCOMING)));
        historyTripsCard.setOnClickListener(v -> navigateTo(TripListFragment.newInstance(TripListFragment.MODE_HISTORY)));
    }

    private void navigateTo(@NonNull Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
