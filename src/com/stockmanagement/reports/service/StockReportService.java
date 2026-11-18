package com.stockmanagement.reports.service;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.reports.model.ReportTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Stock reports. Stock = SUM(batches.quantity_remaining). */
public class StockReportService {

    /** Current stock of every item with its value at cost (batch purchase price) and at retail price. */
    public ReportTable currentStock() throws SQLException {
        String sql = "SELECT i.item_code, i.item_name, c.category_name, u.unit_name, " +
                "COALESCE(SUM(b.quantity_remaining),0) AS qty, i.reorder_level, " +
                "COALESCE(SUM(b.quantity_remaining * b.purchase_price),0) AS cost_value, " +
                "COALESCE(SUM(b.quantity_remaining),0) * i.retail_price AS retail_value " +
                "FROM items i JOIN categories c ON c.category_id = i.category_id " +
                "JOIN units u ON u.unit_id = i.base_unit_id " +
                "LEFT JOIN batches b ON b.item_id = i.item_id " +
                "GROUP BY i.item_id ORDER BY i.item_name";
        ReportTable t = new ReportTable("Current Stock Report",
                "Item Code", "Item Name", "Category", "Unit", "Qty In Stock", "Reorder Level",
                "Value @ Cost (Rs.)", "Value @ Retail (Rs.)", "Status");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int qty = rs.getInt(5);
                int reorder = rs.getInt(6);
                String status = qty <= 0 ? "OUT OF STOCK" : qty <= reorder ? "LOW" : "OK";
                t.addRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), qty, reorder,
                        rs.getDouble(7), rs.getDouble(8), status);
            }
        }
        t.addSummary("Items: " + t.getRows().size());
        t.addSummary("Total stock value at cost: " + ReportFormat.money(t.sumColumn(6)));
        t.addSummary("Total stock value at retail: " + ReportFormat.money(t.sumColumn(7)));
        return t;
    }

    /** Items at or below their reorder level (same rule as ItemDAO.listLowStockItems). */
    public ReportTable lowStock() throws SQLException {
        String sql = "SELECT i.item_code, i.item_name, COALESCE(SUM(b.quantity_remaining),0) AS qty, i.reorder_level " +
                "FROM items i LEFT JOIN batches b ON b.item_id = i.item_id " +
                "GROUP BY i.item_id HAVING qty <= i.reorder_level ORDER BY qty";
        ReportTable t = new ReportTable("Low Stock Report", "Item Code", "Item Name", "Qty In Stock", "Reorder Level", "Shortage");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int qty = rs.getInt(3);
                int reorder = rs.getInt(4);
                t.addRow(rs.getString(1), rs.getString(2), qty, reorder, Math.max(0, reorder - qty));
            }
        }
        t.addSummary("Items needing reorder: " + t.getRows().size());
        return t;
    }

    /** Batches that have expired or expire within the next {@code days} days and still hold stock. */
    public ReportTable expiry(int days) throws SQLException {
        if (days < 0) {
            throw new IllegalArgumentException("Days cannot be negative.");
        }
        String sql = "SELECT i.item_code, i.item_name, b.batch_number, b.expiry_date, b.quantity_remaining, " +
                "CAST(julianday(b.expiry_date) - julianday(date('now','localtime')) AS INTEGER) AS days_left, " +
                "b.quantity_remaining * b.purchase_price AS cost_value " +
                "FROM batches b JOIN items i ON i.item_id = b.item_id " +
                "WHERE b.quantity_remaining > 0 AND b.expiry_date <= date('now','localtime', '+' || ? || ' days') " +
                "ORDER BY b.expiry_date";
        ReportTable t = new ReportTable("Expiry Report  (expired + expiring within " + days + " days)",
                "Item Code", "Item Name", "Batch", "Expiry Date", "Qty Remaining", "Days Left", "Value @ Cost (Rs.)", "Status");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int left = rs.getInt(6);
                    t.addRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5),
                            left, rs.getDouble(7), left < 0 ? "EXPIRED" : "EXPIRING SOON");
                }
            }
        }
        t.addSummary("Batches listed: " + t.getRows().size());
        t.addSummary("Value at risk: " + ReportFormat.money(t.sumColumn(6)));
        return t;
    }

    /** Every batch that still holds stock (FIFO order per item). */
    public ReportTable batchStock() throws SQLException {
        String sql = "SELECT i.item_code, i.item_name, b.batch_number, b.expiry_date, b.quantity_received, " +
                "b.quantity_remaining, b.purchase_price FROM batches b JOIN items i ON i.item_id = b.item_id " +
                "WHERE b.quantity_remaining > 0 ORDER BY i.item_name, b.batch_id";
        ReportTable t = new ReportTable("Batch-wise Stock Report",
                "Item Code", "Item Name", "Batch", "Expiry Date", "Qty Received", "Qty Remaining", "Purchase Price (Rs.)");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                t.addRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), rs.getInt(6), rs.getDouble(7));
            }
        }
        t.addSummary("Batches with stock: " + t.getRows().size());
        return t;
    }
}
