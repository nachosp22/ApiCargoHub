package com.cargohub.mobile;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cargohub.mobile.data.local.SyncManager;
import com.cargohub.mobile.databinding.ActivityMainBinding;
import com.cargohub.mobile.network.ConnectivityObserver;
import com.cargohub.mobile.session.SessionManager;
import com.cargohub.mobile.ui.HomeFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ConnectivityObserver connectivityObserver;
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setupActions();
        setupConnectivityObserver();
        if (savedInstanceState == null) {
            showHome();
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        SessionManager.setInvalidationListener(null);
        if (connectivityObserver != null) {
            connectivityObserver.stop();
        }
    }

    private void setupActions() {
        binding.homeMenuButton.setOnClickListener(v -> performLogout());
        binding.homeLogo.setOnClickListener(v -> showHome());
        binding.homeBrandText.setOnClickListener(v -> showHome());
        applyTopBarWindowInsets();
        applyNavBarInsets();
    }

    private void applyTopBarWindowInsets() {
        final int baseTopPadding = binding.topBarContainer.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBarContainer, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(
                    view.getPaddingLeft(),
                    baseTopPadding + insets.top,
                    view.getPaddingRight(),
                    view.getPaddingBottom());
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.topBarContainer);
    }

    private void applyNavBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentSurfaceCard, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    insets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.contentSurfaceCard);
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

    private void showHome() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, new HomeFragment())
                .commit();
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
