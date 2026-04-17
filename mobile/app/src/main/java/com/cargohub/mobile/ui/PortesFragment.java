package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.cargohub.mobile.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class PortesFragment extends Fragment {

    private static final String[] TAB_MODES = {
            TripListFragment.MODE_UPCOMING,
            TripListFragment.MODE_ACTIVE,
            TripListFragment.MODE_HISTORY
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_portes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.portesTabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.portesViewPager);

        PortesPagerAdapter adapter = new PortesPagerAdapter(this, TAB_MODES);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.portes_tab_upcoming);
                    break;
                case 1:
                    tab.setText(R.string.portes_tab_active);
                    break;
                case 2:
                    tab.setText(R.string.portes_tab_history);
                    break;
            }
        }).attach();
    }
}
