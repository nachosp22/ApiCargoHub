package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

public class IncidenciasOptionsFragment extends Fragment {

    private static final String TAG_ACTIVAS = "activas";
    private static final String TAG_RESUELTAS = "resueltas";

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

        MaterialButton btnNueva = view.findViewById(R.id.btnNuevaIncidencia);
        TabLayout tabLayout = view.findViewById(R.id.incidenciasTabLayout);

        btnNueva.setOnClickListener(v -> navigateTo(new NuevaIncidenciaFragment()));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Default tab
        showTab(0);
    }

    private void showTab(int position) {
        Fragment fragment = position == 0 ? new IncidenciasActivasFragment() : new HistorialIncidenciasFragment();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.incidenciasContentContainer, fragment)
                .commitNow();
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
