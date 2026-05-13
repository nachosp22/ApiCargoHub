package com.cargohub.mobile.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class RecordTrackingPauseRequest {

    @NonNull
    private final String motivo;
    @Nullable
    private final String nota;
    @NonNull
    private final OffsetDateTime startedAt;
    @Nullable
    private final OffsetDateTime endedAt;

    public RecordTrackingPauseRequest(@NonNull String motivo,
                                      @Nullable String nota,
                                      long startedAtMillis) {
        this(motivo, nota, startedAtMillis, 0L);
    }

    public RecordTrackingPauseRequest(@NonNull String motivo,
                                      @Nullable String nota,
                                      long startedAtMillis,
                                      long endedAtMillis) {
        this.motivo = motivo;
        this.nota = nota;
        this.startedAt = Instant.ofEpochMilli(startedAtMillis).atOffset(ZoneOffset.UTC);
        this.endedAt = endedAtMillis > 0
                ? Instant.ofEpochMilli(endedAtMillis).atOffset(ZoneOffset.UTC)
                : null;
    }

    @NonNull
    public String getMotivo() {
        return motivo;
    }

    @Nullable
    public String getNota() {
        return nota;
    }

    @NonNull
    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    @Nullable
    public OffsetDateTime getEndedAt() {
        return endedAt;
    }
}
