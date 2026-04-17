package com.cargohub.mobile.tracking;

import com.cargohub.mobile.R;

public enum MotivoPausa {
    COMIDA(R.string.pause_reason_food, "\uD83C\uDF5E"),
    DESCANSO(R.string.pause_reason_rest, "\uD83D\uDE34"),
    TACOGRAFO(R.string.pause_reason_tachograph, "\uD83D\uDCCB"),
    REPOSTAJE(R.string.pause_reason_refuel, "\u26FD"),
    AVERIA(R.string.pause_reason_breakdown, "\uD83D\uDD27"),
    CARGA_DESCARGA(R.string.pause_reason_loading, "\uD83D\uDCE6"),
    OTRO(R.string.pause_reason_other, "\u2753");

    private final int labelResId;
    private final String emoji;

    MotivoPausa(int labelResId, String emoji) {
        this.labelResId = labelResId;
        this.emoji = emoji;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public String getEmoji() {
        return emoji;
    }
}
