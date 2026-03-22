package com.cargohub.mobile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import com.cargohub.mobile.databinding.ActivityMainBinding;
import com.cargohub.mobile.session.SessionManager;
import com.cargohub.mobile.ui.HomeFragment;
import com.cargohub.mobile.ui.IncidenciasOptionsFragment;
import com.cargohub.mobile.ui.PlaceholderSectionFragment;
import com.cargohub.mobile.ui.PortesOptionsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.hasSession()) {
            LoginNavigator.openLoginAndFinish(this);
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupActions();
        if (savedInstanceState == null) {
            selectSection(R.id.nav_home);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!SessionManager.hasSession()) {
            LoginNavigator.openLoginAndFinish(this);
        }
    }

    @Override
    public void onBackPressed() {
        if (binding != null && binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }

    private void setupActions() {
        binding.homeMenuButton.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.drawerLogoutButton.setOnClickListener(v -> performLogout());
        binding.navigationView.setNavigationItemSelectedListener(item -> {
            selectSection(item.getItemId());
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void selectSection(int itemId) {
        binding.navigationView.setCheckedItem(itemId);

        if (itemId == R.id.nav_home) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new HomeFragment())
                    .commit();
            return;
        }

        if (itemId == R.id.nav_profile) {
            openPlaceholder(getString(R.string.home_menu_section_profile), getString(R.string.section_profile_placeholder));
            return;
        }

        if (itemId == R.id.nav_ports) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new PortesOptionsFragment())
                    .commit();
            return;
        }

        if (itemId == R.id.nav_incidents) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new IncidenciasOptionsFragment())
                    .commit();
            return;
        }

        if (itemId == R.id.nav_agenda) {
            openPlaceholder(getString(R.string.home_menu_section_agenda), getString(R.string.section_agenda_placeholder));
            return;
        }

        if (itemId == R.id.nav_vehicle) {
            openPlaceholder(getString(R.string.home_menu_section_vehicle), getString(R.string.section_vehicle_placeholder));
            return;
        }

        if (itemId == R.id.nav_billing) {
            openPlaceholder(getString(R.string.home_menu_section_billing), getString(R.string.section_billing_placeholder));
        }
    }

    private void openPlaceholder(String title, String message) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, PlaceholderSectionFragment.newInstance(title, message))
                .commit();
    }

    private void performLogout() {
        SessionManager.clearSession();
        LoginNavigator.openLoginAndFinish(this);
    }
}
