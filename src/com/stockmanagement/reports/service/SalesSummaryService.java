package com.stockmanagement.reports.service;

import com.stockmanagement.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

/** Small sales totals for the Dashboard home page (cancelled invoices are always left out). */
public class SalesSummaryService {

    public static class Summary {
        public final LocalDate from;
        public final LocalDate to;
        public final int invoiceCount;
        public final int itemsSold;
        public final double netSales;
        public final double discounts;
        public final double received;

        Summary(LocalDate from, LocalDate to, int invoiceCount, int itemsSold,
                double netSales, double discounts, double received) {
            this.from = from;
            this.to = to;
            this.invoiceCount = invoiceCount;
            this.itemsSold = itemsSold;
            this.netSales = netSales;
            this.discounts = discounts;
            this.received = received;
        }

        public double outstanding() {
            return Math.max(0, netSales - received);
        }

        /** Net sales divided by the number of days in the range. */
        public double averagePerDay() {
            long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
            return days <= 0 ? 0 : netSales / days;
        }
    }

    private static final String RANGE =
            "date(inv.invoice_date) BETWEEN ? AND ? AND inv.status <> 'CANCELLED'";

    public Summary today() throws SQLException {
        LocalDate today = LocalDate.now();
        return forRange(today, today);
    }

    public Summary lastMonth() throws SQLException {
        YearMonth last = YearMonth.now().minusMonths(1);
        return forRange(last.atDay(1), last.atEndOfMonth());
    }

    public Summary forRange(LocalDate from, LocalDate to) throws SQLException {
        String totals = "SELECT COUNT(*), COALESCE(SUM(inv.net_amount), 0), COALESCE(SUM(inv.discount_amount), 0), "
                + "COALESCE(SUM(inv.amount_paid), 0) FROM invoices inv WHERE " + RANGE;
        String qty = "SELECT COALESCE(SUM(ii.quantity), 0) FROM invoice_items ii "
                + "JOIN invoices inv ON inv.invoice_id = ii.invoice_id WHERE " + RANGE;

        try (Connection conn = DatabaseConnection.getConnection()) {
            int count;
            double net;
            double discount;
            double paid;
            int items;

            try (PreparedStatement ps = conn.prepareStatement(totals)) {
                ps.setString(1, from.toString());
                ps.setString(2, to.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    count = rs.getInt(1);
                    net = rs.getDouble(2);
                    discount = rs.getDouble(3);
                    paid = rs.getDouble(4);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(qty)) {
                ps.setString(1, from.toString());
                ps.setString(2, to.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    items = rs.getInt(1);
                }
            }
            return new Summary(from, to, count, items, net, discount, paid);
        }
    }
}
