package com.cargohub.mobile.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/**
 * Observes network connectivity changes and notifies listeners on the main thread.
 * Uses ConnectivityManager.NetworkCallback (API 24+, our min is 26).
 */
public final class ConnectivityObserver {

    public interface Listener {
        void onConnectivityChanged(boolean isOnline);
    }

    private static volatile ConnectivityObserver INSTANCE;

    private final ConnectivityManager connectivityManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Listener listener;
    private boolean online = true;
    private boolean registered = false;

    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    updateState(true);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    updateState(false);
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network,
                                                  @NonNull NetworkCapabilities capabilities) {
                    boolean hasInternet = capabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && capabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    updateState(hasInternet);
                }
            };

    private ConnectivityObserver(@NonNull Context context) {
        connectivityManager = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        online = checkCurrentState();
    }

    public static ConnectivityObserver getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (ConnectivityObserver.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConnectivityObserver(context);
                }
            }
        }
        return INSTANCE;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (registered) return;
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
        registered = true;
    }

    public void stop() {
        if (!registered) return;
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered
        }
        registered = false;
    }

    public boolean isOnline() {
        return online;
    }

    private boolean checkCurrentState() {
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        if (caps == null) return false;
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void updateState(boolean newState) {
        if (online == newState) return;
        online = newState;
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onConnectivityChanged(newState);
            }
        });
    }
}
