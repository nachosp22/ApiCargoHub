package com.cargohub.mobile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import com.cargohub.mobile.data.NotificacionRepository;
import com.cargohub.mobile.data.local.SyncManager;
import com.cargohub.mobile.databinding.ActivityMainBinding;
import com.cargohub.mobile.network.ConnectivityObserver;
import com.cargohub.mobile.session.SessionManager;
import com.cargohub.mobile.ui.AgendaFragment;
import com.cargohub.mobile.ui.EstadisticasFragment;
import com.cargohub.mobile.ui.FacturacionFragment;
import com.cargohub.mobile.ui.HomeFragment;
import com.cargohub.mobile.ui.IncidenciasOptionsFragment;
import com.cargohub.mobile.ui.NotificacionesFragment;
import com.cargohub.mobile.ui.OfferInboxFragment;
import com.cargohub.mobile.ui.PortesFragment;
import com.cargohub.mobile.ui.ProfileFragment;
import com.cargohub.mobile.ui.TrackingStatusFragment;
import com.cargohub.mobile.ui.VehicleFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ConnectivityObserver connectivityObserver;
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private static final long POLL_INTERVAL_MS = 30_000;
    private final NotificacionRepository notificacionRepository = new NotificacionRepository();
    private final Runnable pollUnreadRunnable = new Runnable() {
        @Override
        public void run() {
            pollUnreadCount();
            pollingHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };
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
        setupConnectivityObserver();
        if (savedInstanceState == null) {
            selectSection(R.id.nav_home);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionManager.setInvalidationListener(sessionInvalidationListener);
        ensureAuthenticatedSession();
        if (connectivityObserver != null) {
            connectivityObserver.start();
        }
        pollingHandler.post(pollUnreadRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SessionManager.setInvalidationListener(null);
        if (connectivityObserver != null) {
            connectivityObserver.stop();
        }
        pollingHandler.removeCallbacks(pollUnreadRunnable);
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

    private void setupConnectivityObserver() {
        connectivityObserver = ConnectivityObserver.getInstance(this);
        updateOfflineBanner(!connectivityObserver.isOnline());
        connectivityObserver.setListener(isOnline -> {
            updateOfflineBanner(!isOnline);
            if (isOnline) {
                SyncManager.getInstance(this).syncAll();
            }
        });
    }

    private void updateOfflineBanner(boolean showBanner) {
        if (binding != null && binding.offlineBanner != null) {
            binding.offlineBanner.setVisibility(showBanner ? View.VISIBLE : View.GONE);
        }
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

        if (itemId == R.id.nav_notifications) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contentFragmentContainer, new NotificacionesFragment())
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

    private void pollUnreadCount() {
        if (!SessionManager.hasActiveSession()) return;
        notificacionRepository.getUnreadCount(new NotificacionRepository.UnreadCountCallback() {
            @Override
            public void onSuccess(long count) {
                runOnUiThread(() -> updateNotificationBadge(count));
            }

            @Override
            public void onError(@androidx.annotation.NonNull String message) {
                // Silent fail — polling is best-effort
            }
        });
    }

    private void updateNotificationBadge(long count) {
        if (binding == null) return;
        android.view.MenuItem item = binding.navigationView.getMenu().findItem(R.id.nav_notifications);
        if (item == null) return;
        if (count > 0) {
            item.setTitle(getString(R.string.home_menu_section_notifications) + " (" + count + ")");
        } else {
            item.setTitle(getString(R.string.home_menu_section_notifications));
        }
    }
}
