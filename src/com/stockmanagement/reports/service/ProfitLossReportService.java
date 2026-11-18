package com.stockmanagement.reports.service;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.reports.model.ReportTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Gross profit = sales revenue (invoice_items.line_total, already after discounts)
 *              - cost of goods sold (quantity x purchase price of the batch that was sold, i.e. FIFO cost).
 * Operating expenses are not stored in the database yet, so this is GROSS profit.
 */
public class ProfitLossReportService {

    private static final String RANGE = "date(inv.invoice_date) BETWEEN ? AND ? AND inv.status <> 'CANCELLED'";

    /** Profit per item, best first. */
    public ReportTable profitByItem(LocalDate from, LocalDate to) throws SQLException {
        ReportFormat.checkRange(from, to);
        String sql = "SELECT i.item_code, i.item_name, SUM(ii.quantity), SUM(ii.line_total) AS revenue, " +
                "SUM(ii.quantity * b.purchase_price) AS cost " +
                "FROM invoice_items ii JOIN invoices inv ON inv.invoice_id = ii.invoice_id " +
                "JOIN items i ON i.item_id = ii.item_id JOIN batches b ON b.batch_id = ii.batch_id " +
                "WHERE " + RANGE + " GROUP BY i.item_id ORDER BY (revenue - cost) DESC";
        ReportTable t = new ReportTable("Profit & Loss by Item  (" + from + " to " + to + ")",
                "Item Code", "Item Name", "Qty Sold", "Revenue (Rs.)", "Cost (Rs.)", "Gross Profit (Rs.)", "Margin %");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double revenue = rs.getDouble(4);
                    double cost = rs.getDouble(5);
                    t.addRow(rs.getString(1), rs.getString(2), rs.getInt(3), revenue, cost,
                            ReportFormat.round2(revenue - cost), margin(revenue, cost));
                }
            }
        }
        addSummary(t, 3, 4);
        return t;
    }

    /** Profit per day. */
    public ReportTable profitByDay(LocalDate from, LocalDate to) throws SQLException {
        ReportFormat.checkRange(from, to);
        String sql = "SELECT date(inv.invoice_date) AS d, SUM(ii.line_total) AS revenue, SUM(ii.quantity * b.purchase_price) AS cost " +
                "FROM invoice_items ii JOIN invoices inv ON inv.invoice_id = ii.invoice_id " +
                "JOIN batches b ON b.batch_id = ii.batch_id WHERE " + RANGE + " GROUP BY d ORDER BY d";
        ReportTable t = new ReportTable("Profit & Loss by Day  (" + from + " to " + to + ")",
                "Date", "Revenue (Rs.)", "Cost (Rs.)", "Gross Profit (Rs.)", "Margin %");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double revenue = rs.getDouble(2);
                    double cost = rs.getDouble(3);
                    t.addRow(rs.getString(1), revenue, cost, ReportFormat.round2(revenue - cost), margin(revenue, cost));
                }
            }
        }
        addSummary(t, 1, 2);
        return t;
    }

    private void addSummary(ReportTable t, int revenueCol, int costCol) {
        double revenue = t.sumColumn(revenueCol);
        double cost = t.sumColumn(costCol);
        double profit = revenue - cost;
        t.addSummary("Total revenue: " + ReportFormat.money(revenue));
        t.addSummary("Cost of goods sold: " + ReportFormat.money(cost));
        t.addSummary((profit >= 0 ? "GROSS PROFIT: " : "GROSS LOSS: ") + ReportFormat.money(Math.abs(profit))
                + "   (margin " + String.format("%.2f", margin(revenue, cost)) + "%)");
        t.addSummary("Note: operating expenses are not recorded yet, so this is gross profit.");
    }

    private double margin(double revenue, double cost) {
        return revenue <= 0 ? 0.0 : ReportFormat.round2((revenue - cost) / revenue * 100.0);
    }
}
