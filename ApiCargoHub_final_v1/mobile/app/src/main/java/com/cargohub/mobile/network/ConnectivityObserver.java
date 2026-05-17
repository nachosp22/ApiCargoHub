package com.cargohub.mobile.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private boolean online = false;
    private boolean registered = false;
    private Call<Void> healthCall;

    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    refreshState();
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
                    if (hasInternet) {
                        checkBackendReachable();
                    } else {
                        cancelHealthCall();
                        updateState(false);
                    }
                }
            };

    private ConnectivityObserver(@NonNull Context context) {
        connectivityManager = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        online = false;
        refreshState();
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
        refreshState();
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
        cancelHealthCall();
        registered = false;
    }

    public boolean isOnline() {
        return online;
    }

    public void refreshState() {
        if (!hasValidatedInternet()) {
            cancelHealthCall();
            updateState(false);
            return;
        }
        checkBackendReachable();
    }

    private boolean hasValidatedInternet() {
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        if (caps == null) return false;
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private boolean checkCurrentState() {
        return hasValidatedInternet();
    }

    private void checkBackendReachable() {
        cancelHealthCall();
        Call<Void> call = ApiClient.getInstance().health();
        healthCall = call;
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (call != healthCall) return;
                updateState(response.isSuccessful());
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (call.isCanceled() || call != healthCall) return;
                updateState(false);
            }
        });
    }

    private void cancelHealthCall() {
        if (healthCall != null) {
            healthCall.cancel();
            healthCall = null;
        }
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
