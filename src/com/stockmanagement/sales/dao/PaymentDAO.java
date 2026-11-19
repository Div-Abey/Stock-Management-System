package com.stockmanagement.sales.dao;

import com.stockmanagement.sales.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Payments, received cheques and customer credit balance.
 * (Received cheques are written here for now; when you build supplier/dao/ChequeDAO you can move that part.)
 */
public class PaymentDAO {

    public void insertCustomerPayment(Connection conn, Payment payment, String paymentDateText) throws SQLException {
        String sql = "INSERT INTO customer_payments(customer_id, invoice_id, payment_date, amount_paid, payment_method) " +
                "VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, payment.getCustomerId());
            if (payment.getInvoiceId() != null) {
                pstmt.setInt(2, payment.getInvoiceId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, paymentDateText);
            pstmt.setDouble(4, payment.getAmountPaid());
            pstmt.setString(5, payment.getPaymentMethod());
            pstmt.executeUpdate();
        }
    }

    public void insertReceivedCheque(Connection conn, String chequeNumber, String bankName, double amount,
                                     String issueDate, String realizationDate, Integer customerId) throws SQLException {
        String sql = "INSERT INTO cheques(cheque_number, bank_name, amount, issue_date, realization_date, " +
                "cheque_type, customer_id, status) VALUES(?, ?, ?, ?, ?, 'RECEIVED', ?, 'PENDING')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chequeNumber);
            pstmt.setString(2, bankName);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, issueDate);
            pstmt.setString(5, realizationDate);
            if (customerId != null) {
                pstmt.setInt(6, customerId);
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }
            pstmt.executeUpdate();
        }
    }

    /** Returns {creditLimit, currentBalance} or null if the customer does not exist. */
    public double[] getCreditInfo(Connection conn, int customerId) throws SQLException {
        String sql = "SELECT credit_limit, current_balance FROM customers WHERE customer_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? new double[]{rs.getDouble(1), rs.getDouble(2)} : null;
            }
        }
    }

    public void increaseCustomerBalance(Connection conn, int customerId, double amount) throws SQLException {
        String sql = "UPDATE customers SET current_balance = current_balance + ? WHERE customer_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setInt(2, customerId);
            pstmt.executeUpdate();
        }
    }
}
