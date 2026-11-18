package com.stockmanagement.common.util;

import com.stockmanagement.common.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateUtil {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtil() { }

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    public static LocalDate parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Enter a date as yyyy-MM-dd (for example 2026-12-31).");
        }
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException ex) {
            throw new ValidationException("'" + text.trim() + "' is not a valid date. Use yyyy-MM-dd (for example 2026-12-31).");
        }
    }

    public static LocalDate fromDb(String stored) {
        if (stored == null || stored.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(stored.substring(0, 10), DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
