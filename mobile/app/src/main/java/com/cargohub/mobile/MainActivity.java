package com.cargohub.mobile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import com.cargohub.mobile.databinding.ActivityMainBinding;
import com.cargohub.mobile.session.SessionManager;
import com.cargohub.mobile.ui.AgendaFragment;
import com.cargohub.mobile.ui.EstadisticasFragment;
import com.cargohub.mobile.ui.FacturacionFragment;
import com.cargohub.mobile.ui.HomeFragment;
import com.cargohub.mobile.ui.IncidenciasOptionsFragment;
import com.cargohub.mobile.ui.OfferInboxFragment;
import com.cargohub.mobile.ui.PortesFragment;
import com.cargohub.mobile.ui.ProfileFragment;
import com.cargohub.mobile.ui.TrackingStatusFragment;
import com.cargohub.mobile.ui.VehicleFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final SessionManager.SessionInvalidationListener sessionInvalidationListener =
            () -> runOnUiThread(this::ensureAuthenticatedSession);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.hasActiveSession()) {
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
        SessionManager.setInvalidationListener(sessionInvalidationListener);
        ensureAuthenticatedSession();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SessionManager.setInvalidationListener(null);
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
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new ProfileFragment())
                    .commit();
            return;
        }

        if (itemId == R.id.nav_vehicle) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new VehicleFragment())
                    .commit();
            return;
        }

        if (itemId == R.id.nav_offers) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new OfferInboxFragment())
                    .commit();
            return;
        }

        if (itemId == R.id.nav_portes) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new PortesFragment())
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
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new AgendaFragment())
                    .commit();
            return;
        }

        if (itemId == R.id.nav_billing) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new FacturacionFragment())
                    .commit();
            return;
        }

        if (itemId == R.id.nav_stats) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new EstadisticasFragment())
                    .commit();
        }
    }

    private void performLogout() {
        SessionManager.clearSession();
        LoginNavigator.openLoginAndFinish(this);
    }

    private void ensureAuthenticatedSession() {
        if (!SessionManager.hasActiveSession()) {
            LoginNavigator.openLoginAndFinish(this);
        }
    }
}
