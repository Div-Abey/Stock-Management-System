package com.stockmanagement.common.util;

import com.stockmanagement.common.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** Date helpers. Dates are stored in SQLite as ISO text (yyyy-MM-dd) so they sort and compare correctly. */
public final class DateUtil {

    /** Items that never expire (hardware, stationery...) use this far-future date so FIFO/expiry queries still work. */
    public static final String NO_EXPIRY_DATE = "9999-12-31";
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtil() { }

    public static boolean isNoExpiry(String isoDate) {
        return NO_EXPIRY_DATE.equals(isoDate);
    }

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    public static String display(String isoDate) {
        return isNoExpiry(isoDate) ? "No expiry" : isoDate;
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

    /** Days from today until the date (negative = already expired). Large number for "no expiry". */
    public static long daysUntil(String isoDate) {
        if (isNoExpiry(isoDate)) {
            return Long.MAX_VALUE / 4;
        }
        LocalDate d = parse(isoDate);
        return d == null ? Long.MAX_VALUE / 4 : ChronoUnit.DAYS.between(LocalDate.now(), d);
    }
}
