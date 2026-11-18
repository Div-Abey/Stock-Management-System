package com.stockmanagement.common.util;

import com.stockmanagement.common.exception.ValidationException;

public final class CurrencyUtil {
    private CurrencyUtil() { }

    public static double parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Enter an amount.");
        }
        try {
            return Double.parseDouble(text.trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            throw new ValidationException("'" + text.trim() + "' is not a valid amount.");
        }
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
