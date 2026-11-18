package com.stockmanagement.common.util;

import com.stockmanagement.common.exception.ValidationException;

public final class ValidationUtil {
    private ValidationUtil() { }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }

    public static String optionalText(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    public static double requirePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
        return value;
    }

    public static double requireNotNegative(double value, String fieldName) {
        if (value < 0) {
            throw new ValidationException(fieldName + " cannot be negative.");
        }
        return value;
    }

    public static int parseWholeNumber(String text, String fieldName) {
        int n;
        try {
            n = Integer.parseInt(text.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new ValidationException(fieldName + " must be a whole number.");
        }
        if (n <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
        return n;
    }
}
