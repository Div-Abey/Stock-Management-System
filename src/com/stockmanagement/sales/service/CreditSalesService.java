package com.stockmanagement.sales.service;

import com.stockmanagement.sales.dao.PaymentDAO;

import java.sql.Connection;
import java.sql.SQLException;

/** Credit-limit checks and receivable updates for sales that are not fully paid. */
public class CreditSalesService {

    private final PaymentDAO paymentDAO;

    public CreditSalesService(PaymentDAO paymentDAO) {
        this.paymentDAO = paymentDAO;
    }

    public void assertCreditAllowed(Connection conn, Integer customerId, double dueAmount) throws SQLException {
        if (customerId == null) {
            throw new SQLException("Select a registered customer for credit / part-payment sales (walk-in customers must pay in full).");
        }
        double[] info = paymentDAO.getCreditInfo(conn, customerId);
        if (info == null) {
            throw new SQLException("Customer not found.");
        }
        double limit = info[0];
        double balance = info[1];
        if (balance + dueAmount > limit + 0.005) {
            throw new SQLException(String.format(
                    "Credit limit exceeded.%nLimit: Rs. %.2f | Current balance: Rs. %.2f | This sale on credit: Rs. %.2f",
                    limit, balance, dueAmount));
        }
    }

    public void addToCustomerBalance(Connection conn, int customerId, double dueAmount) throws SQLException {
        paymentDAO.increaseCustomerBalance(conn, customerId, dueAmount);
    }
}
