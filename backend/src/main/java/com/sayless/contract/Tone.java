package com.sayless.contract;

import java.util.Locale;

public enum Tone {
    PROFESSIONAL, CASUAL, DIPLOMATIC, FUNNY;

    private final String wireValue = name().toLowerCase(Locale.ROOT);

    public String wireValue() {
        return wireValue;
    }

    /** Exact match only: no trimming, case folding, or default tone. */
    public static Tone fromWireValue(String value) {
        for (Tone tone : values()) {
            if (tone.wireValue.equals(value)) {
                return tone;
            }
        }
        throw new RewriteException(ErrorCode.VALIDATION_ERROR);
    }
}
