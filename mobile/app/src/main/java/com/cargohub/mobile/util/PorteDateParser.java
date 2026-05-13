package com.cargohub.mobile.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.model.Porte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class PorteDateParser {

    private PorteDateParser() {
    }

    @Nullable
    public static LocalDate resolveTripDate(@NonNull Porte porte) {
        LocalDate pickup = parseFlexibleDate(porte.getFechaRecogida());
        if (pickup != null) {
            return pickup;
        }
        return parseFlexibleDate(porte.getFechaEntrega());
    }

    @Nullable
    public static LocalDate parseFlexibleDate(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String value = raw.trim();
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
        } catch (Exception ignored) {
            return null;
        }
    }
}
