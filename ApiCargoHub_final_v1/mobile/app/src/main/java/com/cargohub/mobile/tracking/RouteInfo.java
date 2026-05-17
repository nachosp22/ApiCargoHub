package com.cargohub.mobile.tracking;

import androidx.annotation.NonNull;

import org.osmdroid.util.GeoPoint;

import java.util.List;

/**
 * Holds parsed route data from OSRM.
 */
public class RouteInfo {

    private final double durationSeconds;
    private final double distanceMeters;
    private final List<GeoPoint> geometry;

    public RouteInfo(double durationSeconds, double distanceMeters, @NonNull List<GeoPoint> geometry) {
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.geometry = geometry;
    }

    /** Route duration in seconds. */
    public double getDurationSeconds() {
        return durationSeconds;
    }

    /** Route distance in meters. */
    public double getDistanceMeters() {
        return distanceMeters;
    }

    /** Decoded polyline geometry as GeoPoints. */
    @NonNull
    public List<GeoPoint> getGeometry() {
        return geometry;
    }

    /** Distance in km, formatted to 1 decimal. */
    @NonNull
    public String getDistanceKmFormatted() {
        double km = distanceMeters / 1000.0;
        return String.format(java.util.Locale.US, "%.1f km", km);
    }

    /** ETA formatted as "~X min" or "~X h Y min". */
    @NonNull
    public String getEtaFormatted() {
        int totalMin = (int) Math.ceil(durationSeconds / 60.0);
        if (totalMin < 60) {
            return "~" + totalMin + " min";
        }
        int hours = totalMin / 60;
        int mins = totalMin % 60;
        return "~" + hours + " h " + mins + " min";
    }

    /** Arrival time formatted as HH:mm in Europe/Madrid timezone. */
    @NonNull
    public String getArrivalTimeFormatted() {
        long arrivalMs = System.currentTimeMillis() + (long) (durationSeconds * 1000);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Madrid"));
        return sdf.format(new java.util.Date(arrivalMs));
    }
}
