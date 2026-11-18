package com.stockmanagement.customer.dao;

import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.customer.model.CustomerLedgerEntry;
import com.stockmanagement.customer.model.CustomerPayment;
import com.stockmanagement.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** SQL behind the customer ledger: invoices, payments and settlement of unpaid invoices. */
public class CustomerLedgerDAO {

    /** An unpaid or part-paid invoice. */
    public static class OpenInvoice {
        public final int invoiceId;
        public final double outstanding;

        public OpenInvoice(int invoiceId, double outstanding) {
            this.invoiceId = invoiceId;
            this.outstanding = outstanding;
        }
    }

    /** Every invoice (debit) and payment (credit) of the customer, unsorted. Running balance is added by the service. */
    public List<CustomerLedgerEntry> findAllEntries(int customerId) throws SQLException {
        List<CustomerLedgerEntry> list = new ArrayList<>();
        String invoiceSql = "SELECT invoice_number, invoice_date, payment_type, net_amount FROM invoices " +
                "WHERE customer_id = ? AND status <> 'CANCELLED'";
        String paymentSql = "SELECT p.payment_date, p.amount_paid, p.payment_method, i.invoice_number " +
                "FROM customer_payments p LEFT JOIN invoices i ON i.invoice_id = p.invoice_id WHERE p.customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(invoiceSql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String stored = rs.getString("invoice_date");
                        list.add(new CustomerLedgerEntry(DateUtil.fromDb(stored), stored + "-1", "INVOICE",
                                rs.getString("invoice_number"), "Sale (" + rs.getString("payment_type") + ")",
                                rs.getDouble("net_amount"), 0));
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(paymentSql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String stored = rs.getString("payment_date");
                        double amount = rs.getDouble("amount_paid");
                        String method = rs.getString("payment_method");
                        String invoiceNo = rs.getString("invoice_number");
                        if (amount >= 0) {
                            list.add(new CustomerLedgerEntry(DateUtil.fromDb(stored), stored + "-2", "PAYMENT",
                                    invoiceNo, "Received by " + method, 0, amount));
                        } else {
                            // a negative payment is a returned (bounced) cheque: the customer owes it again
                            list.add(new CustomerLedgerEntry(DateUtil.fromDb(stored), stored + "-2", "RETURNED CHEQUE",
                                    invoiceNo, "Bounced cheque", -amount, 0));
                        }
                    }
                }
            }
        }
        return list;
    }

    /** Payment / settlement history, newest first. */
    public List<CustomerPayment> findPayments(int customerId) throws SQLException {
        String sql = "SELECT p.payment_id, p.payment_date, p.amount_paid, p.payment_method, i.invoice_number " +
                "FROM customer_payments p LEFT JOIN invoices i ON i.invoice_id = p.invoice_id " +
                "WHERE p.customer_id = ? ORDER BY p.payment_id DESC";
        List<CustomerPayment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CustomerPayment(rs.getInt(1), rs.getString(2), rs.getDouble(3), rs.getString(4), rs.getString(5)));
                }
            }
        }
        return list;
    }

    /** Oldest first, so a payment settles the oldest invoices first. */
    public List<OpenInvoice> findOpenInvoices(Connection conn, int customerId) throws SQLException {
        String sql = "SELECT invoice_id, net_amount - amount_paid FROM invoices " +
                "WHERE customer_id = ? AND status IN ('UNPAID', 'PARTIAL') AND net_amount - amount_paid > 0.005 " +
                "ORDER BY invoice_id";
        List<OpenInvoice> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OpenInvoice(rs.getInt(1), rs.getDouble(2)));
                }
            }
        }
        return list;
    }

    public void applyToInvoice(Connection conn, int invoiceId, double amount) throws SQLException {
        String sql = "UPDATE invoices SET amount_paid = amount_paid + ?, " +
                "status = CASE WHEN net_amount - (amount_paid + ?) <= 0.005 THEN 'PAID' ELSE 'PARTIAL' END " +
                "WHERE invoice_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setDouble(2, amount);
            ps.setInt(3, invoiceId);
            ps.executeUpdate();
        }
    }

    public void insertPayment(Connection conn, int customerId, Integer invoiceId, String paymentDateTime,
                              double amount, String method) throws SQLException {
        String sql = "INSERT INTO customer_payments(customer_id, invoice_id, payment_date, amount_paid, payment_method) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            if (invoiceId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, invoiceId);
            }
            ps.setString(3, paymentDateTime);
            ps.setDouble(4, amount);
            ps.setString(5, method);
            ps.executeUpdate();
        }
    }
}
