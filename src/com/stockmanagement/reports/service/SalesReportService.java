package com.stockmanagement.reports.service;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.reports.model.ReportTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Sales reports (cancelled invoices are always left out). Dates are inclusive. */
public class SalesReportService {

    private static final String RANGE = "date(inv.invoice_date) BETWEEN ? AND ? AND inv.status <> 'CANCELLED'";

    /** One row per day. */
    public ReportTable dailySales(LocalDate from, LocalDate to) throws SQLException {
        ReportFormat.checkRange(from, to);
        String sql = "SELECT date(inv.invoice_date) AS d, COUNT(*) AS n, SUM(inv.total_amount), SUM(inv.discount_amount), " +
                "SUM(inv.net_amount), SUM(inv.amount_paid) FROM invoices inv WHERE " + RANGE +
                " GROUP BY d ORDER BY d";
        ReportTable t = new ReportTable("Daily Sales Report  (" + from + " to " + to + ")",
                "Date", "Invoices", "Gross (Rs.)", "Discount (Rs.)", "Net Sales (Rs.)", "Received (Rs.)", "Outstanding (Rs.)");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double net = rs.getDouble(5);
                    double paid = rs.getDouble(6);
                    t.addRow(rs.getString(1), rs.getInt(2), rs.getDouble(3), rs.getDouble(4), net, paid,
                            ReportFormat.round2(net - paid));
                }
            }
        }
        t.addSummary("Invoices: " + (long) t.sumColumn(1));
        t.addSummary("Total net sales: " + ReportFormat.money(t.sumColumn(4)));
        t.addSummary("Total received: " + ReportFormat.money(t.sumColumn(5)));
        t.addSummary("Total outstanding: " + ReportFormat.money(t.sumColumn(4) - t.sumColumn(5)));
        return t;
    }

    /** One row per invoice. */
    public ReportTable invoiceList(LocalDate from, LocalDate to) throws SQLException {
        ReportFormat.checkRange(from, to);
        String sql = "SELECT inv.invoice_number, inv.invoice_date, COALESCE(c.customer_name,'Walk-in Customer'), " +
                "u.username, inv.payment_type, inv.net_amount, inv.amount_paid, inv.status " +
                "FROM invoices inv LEFT JOIN customers c ON c.customer_id = inv.customer_id " +
                "JOIN users u ON u.user_id = inv.cashier_id WHERE " + RANGE + " ORDER BY inv.invoice_id";
        ReportTable t = new ReportTable("Invoice List  (" + from + " to " + to + ")",
                "Invoice No", "Date / Time", "Customer", "Cashier", "Payment", "Net (Rs.)", "Paid (Rs.)", "Status");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    t.addRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getDouble(6), rs.getDouble(7), rs.getString(8));
                }
            }
        }
        t.addSummary("Invoices: " + t.getRows().size());
        t.addSummary("Total net sales: " + ReportFormat.money(t.sumColumn(5)));
        t.addSummary("Total received: " + ReportFormat.money(t.sumColumn(6)));
        return t;
    }

    /** Best sellers first. */
    public ReportTable salesByItem(LocalDate from, LocalDate to) throws SQLException {
        ReportFormat.checkRange(from, to);
        String sql = "SELECT i.item_code, i.item_name, SUM(ii.quantity) AS qty, SUM(ii.discount_applied), SUM(ii.line_total) AS revenue " +
                "FROM invoice_items ii JOIN invoices inv ON inv.invoice_id = ii.invoice_id " +
                "JOIN items i ON i.item_id = ii.item_id WHERE " + RANGE +
                " GROUP BY i.item_id ORDER BY revenue DESC";
        ReportTable t = new ReportTable("Sales by Item  (" + from + " to " + to + ")",
                "Item Code", "Item Name", "Qty Sold", "Discount (Rs.)", "Revenue (Rs.)");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    t.addRow(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getDouble(4), rs.getDouble(5));
                }
            }
        }
        t.addSummary("Total quantity sold: " + (long) t.sumColumn(2));
        t.addSummary("Total revenue: " + ReportFormat.money(t.sumColumn(4)));
        return t;
    }

    /** Cash / credit / cheque / card split. */
    public ReportTable salesByPaymentType(LocalDate from, LocalDate to) throws SQLException {
        ReportFormat.checkRange(from, to);
        String sql = "SELECT inv.payment_type, COUNT(*), SUM(inv.net_amount), SUM(inv.amount_paid) FROM invoices inv WHERE " +
                RANGE + " GROUP BY inv.payment_type ORDER BY 3 DESC";
        ReportTable t = new ReportTable("Sales by Payment Type  (" + from + " to " + to + ")",
                "Payment Type", "Invoices", "Net Sales (Rs.)", "Received (Rs.)", "Outstanding (Rs.)");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double net = rs.getDouble(3);
                    double paid = rs.getDouble(4);
                    t.addRow(rs.getString(1), rs.getInt(2), net, paid, ReportFormat.round2(net - paid));
                }
            }
        }
        t.addSummary("Total net sales: " + ReportFormat.money(t.sumColumn(2)));
        return t;
    }
}
