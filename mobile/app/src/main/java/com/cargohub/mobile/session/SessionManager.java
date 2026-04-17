package com.cargohub.mobile.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.model.LoginResponse;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class SessionManager {

    private static final String PREFS_NAME = "cargohub_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_CONDUCTOR_ID = "conductor_id";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EXPIRES_AT = "expires_at";

    public interface SessionInvalidationListener {
        void onSessionInvalidated();
    }

    private static volatile SharedPreferences prefs;
    private static volatile SessionInvalidationListener invalidationListener;

    private SessionManager() {
    }

    public static void init(@NonNull Context context) {
        if (prefs == null) {
            synchronized (SessionManager.class) {
                if (prefs == null) {
                    prefs = context.getApplicationContext()
                            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                }
            }
        }
    }

    public static void saveAccessToken(@NonNull String token) {
        SharedPreferences sharedPreferences = ensurePrefs();
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token.trim()).apply();
    }

    public static void saveLoginSession(@NonNull LoginResponse loginResponse) {
        saveSession(SessionSnapshot.fromLoginResponse(loginResponse));
    }

    public static void saveSession(@NonNull SessionSnapshot snapshot) {
        SharedPreferences.Editor editor = ensurePrefs().edit();
        String token = snapshot.getAccessToken();
        if (token != null && !token.trim().isEmpty()) {
            editor.putString(KEY_ACCESS_TOKEN, token.trim());
        } else {
            editor.remove(KEY_ACCESS_TOKEN);
        }

        Long conductorId = snapshot.getConductorId();
        if (conductorId != null && conductorId > 0) {
            editor.putLong(KEY_CONDUCTOR_ID, conductorId);
        } else {
            editor.remove(KEY_CONDUCTOR_ID);
        }

        String role = snapshot.getRole();
        if (role != null && !role.trim().isEmpty()) {
            editor.putString(KEY_ROLE, role.trim());
        } else {
            editor.remove(KEY_ROLE);
        }

        Long expiresAt = snapshot.getExpiresAtEpochMs();
        if (expiresAt != null && expiresAt > 0) {
            editor.putLong(KEY_EXPIRES_AT, expiresAt);
        } else {
            editor.remove(KEY_EXPIRES_AT);
        }
        editor.apply();
    }

    @Nullable
    public static String getAccessToken() {
        SharedPreferences sharedPreferences = ensurePrefs();
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public static boolean hasSession() {
        String token = getAccessToken();
        return token != null && !token.trim().isEmpty();
    }

    public static boolean hasActiveSession() {
        SessionSnapshot snapshot = getSessionSnapshot();
        if (!snapshot.isActive(System.currentTimeMillis())) {
            clearSession();
            return false;
        }
        return true;
    }

    public static void saveConductorId(long conductorId) {
        SharedPreferences sharedPreferences = ensurePrefs();
        sharedPreferences.edit().putLong(KEY_CONDUCTOR_ID, conductorId).apply();
    }

    @Nullable
    public static String getRole() {
        return ensurePrefs().getString(KEY_ROLE, null);
    }

    @Nullable
    public static Long getExpiresAtEpochMs() {
        SharedPreferences sharedPreferences = ensurePrefs();
        if (!sharedPreferences.contains(KEY_EXPIRES_AT)) {
            return null;
        }
        return sharedPreferences.getLong(KEY_EXPIRES_AT, -1L);
    }

    @NonNull
    public static SessionSnapshot getSessionSnapshot() {
        return new SessionSnapshot(
                getAccessToken(),
                getRole(),
                getConductorId(),
                getExpiresAtEpochMs()
        );
    }

    @Nullable
    public static Long getConductorId() {
        SharedPreferences sharedPreferences = ensurePrefs();
        if (!sharedPreferences.contains(KEY_CONDUCTOR_ID)) {
            return null;
        }
        return sharedPreferences.getLong(KEY_CONDUCTOR_ID, -1L);
    }

    @Nullable
    public static Long resolveConductorId() {
        Long stored = getConductorId();
        if (stored != null && stored > 0) {
            return stored;
        }
        String token = getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return extractUserIdFromToken(token);
    }

    public static void clearSession() {
        SharedPreferences sharedPreferences = ensurePrefs();
        sharedPreferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_CONDUCTOR_ID)
                .remove(KEY_ROLE)
                .remove(KEY_EXPIRES_AT)
                .apply();
    }

    public static void handleUnauthorized() {
        clearSession();
        SessionInvalidationListener listener = invalidationListener;
        if (listener != null) {
            listener.onSessionInvalidated();
        }
    }

    public static void setInvalidationListener(@Nullable SessionInvalidationListener listener) {
        invalidationListener = listener;
    }

    @NonNull
    private static SharedPreferences ensurePrefs() {
        if (prefs == null) {
            throw new IllegalStateException("SessionManager not initialized");
        }
        return prefs;
    }

    @Nullable
    private static Long extractUserIdFromToken(@NonNull String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(payload);
            if (!json.has("userId")) {
                return null;
            }
            return json.optLong("userId", -1L);
        } catch (Exception ignored) {
            return null;
        }
    }
}
