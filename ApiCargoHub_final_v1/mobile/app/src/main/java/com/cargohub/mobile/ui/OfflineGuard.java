package com.cargohub.mobile.ui;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;
import com.cargohub.mobile.network.ConnectivityObserver;

/**
 * Helper for fragments that need network-only features.
 * Shows a "Sin conexión" message instead of attempting network calls.
 */
public final class OfflineGuard {

    private OfflineGuard() {
    }

    /**
     * Returns true if offline. Shows a toast and optionally sets a message view.
     * Call at the start of data-loading methods in network-only fragments.
     */
    public static boolean blockIfOffline(@NonNull Context context,
                                          @Nullable View messageView) {
        if (ConnectivityObserver.getInstance(context).isOnline()) {
            return false;
        }

        String message = context.getString(R.string.offline_feature_unavailable);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        if (messageView instanceof android.widget.TextView) {
            ((android.widget.TextView) messageView).setText(message);
            messageView.setVisibility(View.VISIBLE);
        }

        return true;
    }
}
