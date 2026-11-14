package com.stockmanagement.sales.dao;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.sales.model.Invoice;
import com.stockmanagement.sales.model.SaleItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read side for the invoice history / receipt screen. */
public class InvoiceDAO {

    public List<Invoice> findAll() throws SQLException {
        String sql = "SELECT inv.invoice_id, inv.invoice_number, " +
                "       COALESCE(c.customer_name, 'Walk-in Customer') AS customer_name, " +
                "       inv.invoice_date, inv.payment_type, inv.total_amount, inv.discount_amount, " +
                "       inv.net_amount, inv.amount_paid, inv.due_date, inv.status " +
                "FROM invoices inv LEFT JOIN customers c ON c.customer_id = inv.customer_id " +
                "ORDER BY inv.invoice_id DESC";
        List<Invoice> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Invoice inv = new Invoice();
                inv.setInvoiceId(rs.getInt("invoice_id"));
                inv.setInvoiceNumber(rs.getString("invoice_number"));
                inv.setCustomerName(rs.getString("customer_name"));
                inv.setInvoiceDate(rs.getString("invoice_date"));
                inv.setPaymentType(rs.getString("payment_type"));
                inv.setTotalAmount(rs.getDouble("total_amount"));
                inv.setDiscountAmount(rs.getDouble("discount_amount"));
                inv.setNetAmount(rs.getDouble("net_amount"));
                inv.setAmountPaid(rs.getDouble("amount_paid"));
                inv.setDueDate(rs.getString("due_date"));
                inv.setStatus(rs.getString("status"));
                list.add(inv);
            }
        }
        return list;
    }

    public List<SaleItem> findItems(int invoiceId) throws SQLException {
        String sql = "SELECT ii.item_id, i.item_code, i.item_name, ii.unit_id, u.unit_name, ii.quantity, " +
                "       ii.unit_price, ii.discount_applied, ii.line_total, ii.batch_id, b.batch_number " +
                "FROM invoice_items ii " +
                "JOIN items i ON i.item_id = ii.item_id " +
                "JOIN units u ON u.unit_id = ii.unit_id " +
                "JOIN batches b ON b.batch_id = ii.batch_id " +
                "WHERE ii.invoice_id = ? ORDER BY ii.invoice_item_id";
        List<SaleItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, invoiceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = new SaleItem(
                            rs.getInt("item_id"), rs.getString("item_code"), rs.getString("item_name"),
                            rs.getInt("unit_id"), rs.getInt("quantity"), rs.getDouble("unit_price"));
                    item.setUnitName(rs.getString("unit_name"));
                    item.setDiscountApplied(rs.getDouble("discount_applied"));
                    item.setLineTotal(rs.getDouble("line_total"));
                    item.setBatchId(rs.getInt("batch_id"));
                    item.setBatchNumber(rs.getString("batch_number"));
                    list.add(item);
                }
            }
        }
        return list;
    }
}
