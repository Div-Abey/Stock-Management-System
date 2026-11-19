package com.stockmanagement.sales.dao;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.sales.model.BulkDiscount;
import com.stockmanagement.sales.model.Sale;
import com.stockmanagement.sales.model.SaleItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Saves sales (invoices + invoice_items) and holds the pricing look-ups used while billing. */
public class SaleDAO {

    // ---------- writes: all take the Connection of the sale transaction ----------

    public String generateInvoiceNumber(Connection conn) throws SQLException {
        String prefix = "INV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        String sql = "SELECT COUNT(*) FROM invoices WHERE invoice_number LIKE ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, prefix + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                int next = rs.next() ? rs.getInt(1) + 1 : 1;
                return prefix + String.format("%04d", next);
            }
        }
    }

    /** Inserts the invoice header and returns the new invoice_id. */
    public int insertInvoice(Connection conn, Sale sale, String invoiceDateText) throws SQLException {
        String sql = "INSERT INTO invoices(invoice_number, customer_id, cashier_id, invoice_date, payment_type, " +
                "total_amount, discount_amount, net_amount, status, amount_paid, due_date) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, sale.getInvoiceNumber());
            if (sale.getCustomerId() != null) {
                pstmt.setInt(2, sale.getCustomerId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setInt(3, sale.getCashierId());
            pstmt.setString(4, invoiceDateText);
            pstmt.setString(5, sale.getPaymentType());
            pstmt.setDouble(6, sale.getTotalAmount());
            pstmt.setDouble(7, sale.getDiscountAmount());
            pstmt.setDouble(8, sale.getNetAmount());
            pstmt.setString(9, sale.getStatus());
            pstmt.setDouble(10, sale.getAmountPaid());
            if (sale.getDueDate() != null) {
                pstmt.setString(11, sale.getDueDate().toString());
            } else {
                pstmt.setNull(11, Types.VARCHAR);
            }
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not create the invoice record.");
    }

    public void insertInvoiceItem(Connection conn, int invoiceId, SaleItem item) throws SQLException {
        String sql = "INSERT INTO invoice_items(invoice_id, item_id, batch_id, unit_id, quantity, unit_price, " +
                "discount_applied, line_total) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, invoiceId);
            pstmt.setInt(2, item.getItemId());
            pstmt.setInt(3, item.getBatchId());
            pstmt.setInt(4, item.getUnitId());
            pstmt.setInt(5, item.getQuantity());
            pstmt.setDouble(6, item.getUnitPrice());
            pstmt.setDouble(7, item.getDiscountApplied());
            pstmt.setDouble(8, item.getLineTotal());
            pstmt.executeUpdate();
        }
    }

    // ---------- pricing look-ups (read only, own connection) ----------

    /** Best rule for this quantity = the one with the highest min_quantity that is still <= quantity. */
    public BulkDiscount findBestBulkDiscount(int itemId, int quantity) throws SQLException {
        String sql = "SELECT discount_id, item_id, min_quantity, discount_percentage, discount_amount " +
                "FROM bulk_discounts WHERE item_id = ? AND min_quantity <= ? " +
                "ORDER BY min_quantity DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            pstmt.setInt(2, quantity);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new BulkDiscount(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getDouble(4), rs.getDouble(5));
                }
            }
        }
        return null;
    }

    /** Special price from customer_pricing, or null when this customer has none for the item. */
    public Double findSpecialPrice(int customerId, int itemId) throws SQLException {
        String sql = "SELECT special_price FROM customer_pricing WHERE customer_id = ? AND item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setInt(2, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : null;
            }
        }
    }
}
