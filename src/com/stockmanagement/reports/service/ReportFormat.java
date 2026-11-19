package com.stockmanagement.reports.service;

import java.time.LocalDate;

/** Small helpers shared by the report services. */
final class ReportFormat {

    private ReportFormat() {
    }

    static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    static String money(double v) {
        return String.format("Rs. %,.2f", v);
    }

    static void checkRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Please select both the From and To dates.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("The From date cannot be after the To date.");
        }
    }
}
