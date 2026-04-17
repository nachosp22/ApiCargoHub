package com.cargohub.mobile.tracking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TrackingPause {

    private final MotivoPausa motivo;
    @Nullable
    private final String nota;
    private final long horaInicio;
    private long horaFin;

    public TrackingPause(@NonNull MotivoPausa motivo, @Nullable String nota, long horaInicio) {
        this.motivo = motivo;
        this.nota = nota;
        this.horaInicio = horaInicio;
    }

    @NonNull
    public MotivoPausa getMotivo() {
        return motivo;
    }

    @Nullable
    public String getNota() {
        return nota;
    }

    public long getHoraInicio() {
        return horaInicio;
    }

    public long getHoraFin() {
        return horaFin;
    }

    public boolean isActive() {
        return horaFin == 0;
    }

    public void cerrar(long horaFin) {
        this.horaFin = horaFin;
    }

    /** Duration in milliseconds. If active, uses current time. */
    public long getDurationMs() {
        long end = horaFin > 0 ? horaFin : System.currentTimeMillis();
        return end - horaInicio;
    }
}
