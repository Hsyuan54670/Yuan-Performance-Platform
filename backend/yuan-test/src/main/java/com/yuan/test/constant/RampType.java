package com.yuan.test.constant;

import java.util.Locale;

public enum RampType {
    LINEAR,
    STAIR;

    public static RampType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return LINEAR;
        }

        try {
            return RampType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return LINEAR;
        }
    }
}
