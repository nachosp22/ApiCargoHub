package com.cargohub.mobile.tracking;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Fetches driving routes from the public OSRM demo server.
 * Rate-limited: max 1 request per 60 seconds enforced internally.
 */
public class OsrmRouteService {

    public interface Callback {
        void onRouteReady(@NonNull RouteInfo route);
        void onRouteError(@NonNull String message);
    }

    private static final String OSRM_BASE = "https://router.project-osrm.org/route/v1/driving/";
    private static final long MIN_REQUEST_INTERVAL_MS = 30_000L;
    private static final long MOVEMENT_COOLDOWN_MS = 10_000L;
    private static final float MIN_POSITION_CHANGE_METERS = 100f;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private long lastRequestTimeMs;
    private double lastRequestLat;
    private double lastRequestLon;
    @Nullable
    private RouteInfo cachedRoute;

    /**
     * Request a route. Respects rate limiting and position change threshold.
     * If cached route is still valid, returns it immediately.
     */
    public void fetchRoute(double fromLat, double fromLon,
                           double toLat, double toLon,
                           boolean forceRefresh,
                           @NonNull Callback callback) {

        long now = System.currentTimeMillis();
        boolean intervalOk = (now - lastRequestTimeMs) >= MIN_REQUEST_INTERVAL_MS;
        boolean positionChanged = distanceBetween(fromLat, fromLon, lastRequestLat, lastRequestLon) >= MIN_POSITION_CHANGE_METERS;
        boolean movementCooldownOk = (now - lastRequestTimeMs) >= MOVEMENT_COOLDOWN_MS;

        if (!forceRefresh && !positionChanged && cachedRoute != null) {
            callback.onRouteReady(cachedRoute);
            return;
        }

        if (!forceRefresh && positionChanged && !movementCooldownOk && cachedRoute != null) {
            callback.onRouteReady(cachedRoute);
            return;
        }

        if (!forceRefresh && !intervalOk && !positionChanged && cachedRoute != null) {
            callback.onRouteReady(cachedRoute);
            return;
        }

        lastRequestTimeMs = now;
        lastRequestLat = fromLat;
        lastRequestLon = fromLon;

        executor.execute(() -> {
            try {
                String urlStr = String.format(Locale.US,
                        "%s%f,%f;%f,%f?overview=full&geometries=polyline",
                        OSRM_BASE, fromLon, fromLat, toLon, toLat);

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestProperty("User-Agent", "CargoHub-Mobile/1.0");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    postError(callback, "OSRM respondio con codigo " + code);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }

                JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                String status = json.has("code") ? json.get("code").getAsString() : "";
                if (!"Ok".equals(status)) {
                    postError(callback, "OSRM: " + status);
                    return;
                }

                JsonArray routes = json.getAsJsonArray("routes");
                if (routes == null || routes.size() == 0) {
                    postError(callback, "No se encontro ruta.");
                    return;
                }

                JsonObject route = routes.get(0).getAsJsonObject();
                double duration = route.get("duration").getAsDouble();
                double distance = route.get("distance").getAsDouble();
                String encodedGeometry = route.get("geometry").getAsString();

                List<GeoPoint> points = decodePolyline(encodedGeometry);
                RouteInfo routeInfo = new RouteInfo(duration, distance, points);
                cachedRoute = routeInfo;

                mainHandler.post(() -> callback.onRouteReady(routeInfo));

            } catch (Exception e) {
                postError(callback, "Error de red al calcular ruta: " + e.getMessage());
            }
        });
    }

    public void clearCache() {
        cachedRoute = null;
        lastRequestTimeMs = 0;
    }

    private void postError(@NonNull Callback callback, @NonNull String msg) {
        mainHandler.post(() -> callback.onRouteError(msg));
    }

    /**
     * Decode Google-encoded polyline to list of GeoPoints.
     */
    @NonNull
    private static List<GeoPoint> decodePolyline(@NonNull String encoded) {
        List<GeoPoint> points = new ArrayList<>();
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < encoded.length()) {
            int result = 0;
            int shift = 0;
            int b;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 0;
            shift = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            points.add(new GeoPoint(lat / 1e5, lng / 1e5));
        }
        return points;
    }

    private static float distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }
}
