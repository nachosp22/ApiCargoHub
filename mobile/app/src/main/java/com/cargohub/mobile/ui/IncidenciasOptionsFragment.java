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

public class IncidenciasOptionsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_incidencias_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialCardView newIncidentCard = view.findViewById(R.id.incidenciaNewCard);
        MaterialCardView activeIncidentsCard = view.findViewById(R.id.incidenciaActiveCard);
        MaterialCardView historyIncidentsCard = view.findViewById(R.id.incidenciaHistoryCard);

        newIncidentCard.setOnClickListener(v -> navigateTo(new NuevaIncidenciaFragment()));
        activeIncidentsCard.setOnClickListener(v -> navigateTo(new IncidenciasActivasFragment()));
        historyIncidentsCard.setOnClickListener(v -> navigateTo(new HistorialIncidenciasFragment()));
    }

    private void navigateTo(Fragment fragment) {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
