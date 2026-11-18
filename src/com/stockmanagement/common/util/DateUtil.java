package com.stockmanagement.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** Date helpers. Dates are stored in SQLite as ISO text (yyyy-MM-dd) so they sort and compare correctly. */
public final class DateUtil {

    /** Items that never expire (hardware, stationery...) use this far-future date so FIFO/expiry queries still work. */
    public static final String NO_EXPIRY_DATE = "9999-12-31";

    private DateUtil() {
    }

    public static boolean isNoExpiry(String isoDate) {
        return NO_EXPIRY_DATE.equals(isoDate);
    }

    public static String display(String isoDate) {
        return isNoExpiry(isoDate) ? "No expiry" : isoDate;
    }

    public static LocalDate parse(String isoDate) {
        try {
            return LocalDate.parse(isoDate);
        } catch (DateTimeParseException | NullPointerException e) {
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
