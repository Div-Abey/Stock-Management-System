package com.stockmanagement.customer.service;

import com.stockmanagement.common.enums.ChequeType;
import com.stockmanagement.common.enums.PaymentMethod;
import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.common.util.CurrencyUtil;
import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.common.util.ValidationUtil;
import com.stockmanagement.customer.dao.CustomerDAO;
import com.stockmanagement.customer.dao.CustomerLedgerDAO;
import com.stockmanagement.customer.dao.CustomerLedgerDAO.OpenInvoice;
import com.stockmanagement.customer.model.AccountStatement;
import com.stockmanagement.customer.model.Customer;
import com.stockmanagement.customer.model.CustomerLedgerEntry;
import com.stockmanagement.customer.model.CustomerPayment;
import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.supplier.dao.ChequeDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Customer ledger (3.1). The ledger is built from the real records, so it always agrees with the sales screen:
 * every invoice of the customer is a debit, every payment a credit, and the closing balance equals customers.current_balance.
 */
public class CustomerLedgerService {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final CustomerLedgerDAO ledgerDAO = new CustomerLedgerDAO();
    private final ChequeDAO chequeDAO = new ChequeDAO();

    /** Statement for a date range (a month, normally) with opening and closing balance. */
    public AccountStatement buildStatement(int customerId, LocalDate from, LocalDate to) throws SQLException {
        if (from.isAfter(to)) {
            throw new ValidationException("The 'From' date must not be after the 'To' date.");
        }
        Customer customer = customerDAO.findById(customerId);
        if (customer == null) {
            throw new ValidationException("Customer not found.");
        }
        List<CustomerLedgerEntry> all = ledgerDAO.findAllEntries(customerId);
        all.sort(Comparator.comparing(CustomerLedgerEntry::getSortKey));

        double opening = 0;
        double running = 0;
        List<CustomerLedgerEntry> inRange = new ArrayList<>();
        for (CustomerLedgerEntry e : all) {
            LocalDate d = e.getDate();
            if (d == null) {
                continue;
            }
            if (d.isBefore(from)) {
                opening += e.getDebit() - e.getCredit();
            } else if (!d.isAfter(to)) {
                inRange.add(e);
            }
        }
        opening = CurrencyUtil.round2(opening);
        running = opening;
        for (CustomerLedgerEntry e : inRange) {
            running = CurrencyUtil.round2(running + e.getDebit() - e.getCredit());
            e.setBalance(running);
        }
        return new AccountStatement(customer, from, to, opening, inRange, running);
    }

    public List<CustomerPayment> getPaymentHistory(int customerId) throws SQLException {
        return ledgerDAO.findPayments(customerId);
    }

    /**
     * Records money received from a customer: settles the oldest unpaid invoices first, lowers the balance and,
     * for a cheque, stores it as a pending RECEIVED cheque so the realization reminder works.
     */
    public void recordPayment(int customerId, double amount, PaymentMethod method, String chequeNumber,
                              String bankName, LocalDate realizationDate) throws SQLException {
        ValidationUtil.requirePositive(amount, "Payment amount");
        amount = CurrencyUtil.round2(amount);
        if (method == null) {
            throw new ValidationException("Choose a payment method.");
        }
        if (method == PaymentMethod.CHEQUE) {
            ValidationUtil.requireText(chequeNumber, "Cheque number");
            ValidationUtil.requireText(bankName, "Bank");
            if (realizationDate == null) {
                throw new ValidationException("Enter the realization date of the cheque.");
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Customer customer = customerDAO.findById(conn, customerId);
                if (customer == null) {
                    throw new ValidationException("Customer not found.");
                }
                if (amount > customer.getCurrentBalance() + 0.005) {
                    throw new ValidationException(String.format("The customer only owes Rs. %s. Enter a smaller amount.",
                            CurrencyUtil.format(customer.getCurrentBalance())));
                }
                String now = LocalDateTime.now().format(DateUtil.DATE_TIME);
                double left = amount;
                for (OpenInvoice inv : ledgerDAO.findOpenInvoices(conn, customerId)) {
                    if (left <= 0.005) {
                        break;
                    }
                    double part = CurrencyUtil.round2(Math.min(left, inv.outstanding));
                    ledgerDAO.applyToInvoice(conn, inv.invoiceId, part);
                    ledgerDAO.insertPayment(conn, customerId, inv.invoiceId, now, part, method.name());
                    left = CurrencyUtil.round2(left - part);
                }
                if (left > 0.005) {
                    ledgerDAO.insertPayment(conn, customerId, null, now, left, method.name());
                }
                customerDAO.adjustBalance(conn, customerId, -amount);

                if (method == PaymentMethod.CHEQUE) {
                    chequeDAO.insert(conn, chequeNumber.trim(), bankName.trim(), amount, LocalDate.now(),
                            realizationDate, ChequeType.RECEIVED, customerId, null);
                }
                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Used when a received cheque bounces: the customer owes the amount again (shown as RETURNED CHEQUE). */
    public void reversePayment(Connection conn, int customerId, double amount) throws SQLException {
        String now = LocalDateTime.now().format(DateUtil.DATE_TIME);
        ledgerDAO.insertPayment(conn, customerId, null, now, -amount, PaymentMethod.CHEQUE.name());
        customerDAO.adjustBalance(conn, customerId, amount);
    }
}
