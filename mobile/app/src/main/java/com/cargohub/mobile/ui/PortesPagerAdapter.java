package com.cargohub.mobile.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class PortesPagerAdapter extends FragmentStateAdapter {

    private final String[] modes;

    public PortesPagerAdapter(@NonNull Fragment fragment, @NonNull String[] modes) {
        super(fragment);
        this.modes = modes;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return TripListFragment.newInstance(modes[position]);
    }

    @Override
    public int getItemCount() {
        return modes.length;
    }
}
