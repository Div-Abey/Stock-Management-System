package com.stockmanagement.supplier.service;

import com.stockmanagement.common.enums.ChequeStatus;
import com.stockmanagement.common.enums.ChequeType;
import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.common.util.CurrencyUtil;
import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.common.util.ValidationUtil;
import com.stockmanagement.customer.service.CustomerLedgerService;
import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.supplier.dao.ChequeDAO;
import com.stockmanagement.supplier.model.Cheque;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 3.3 Cheque management: record post-dated cheques, clear / bounce them and raise realization-date reminders. */
public class ChequeService {

    private final ChequeDAO chequeDAO = new ChequeDAO();
    private final CustomerLedgerService ledgerService = new CustomerLedgerService();


    /**
     * Records a cheque (number, bank, amount, date). RECEIVED needs a customer, ISSUED needs a supplier.
     * This only records the cheque; payments made through the Customer Ledger / Accounts Payable screens
     * record their cheque automatically.
     */
    public void recordCheque(ChequeType type, Integer customerId, Integer supplierId, String number, String bank,
                             double amount, LocalDate issueDate, LocalDate realizationDate) throws SQLException {
        ValidationUtil.requireText(number, "Cheque number");
        ValidationUtil.requireText(bank, "Bank");
        ValidationUtil.requirePositive(amount, "Cheque amount");
        if (type == ChequeType.RECEIVED && customerId == null) {
            throw new ValidationException("Select the customer who gave the cheque.");
        }
        if (type == ChequeType.ISSUED && supplierId == null) {
            throw new ValidationException("Select the supplier the cheque is issued to.");
        }
        if (realizationDate.isBefore(issueDate)) {
            throw new ValidationException("The realization date cannot be before the cheque date.");
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            chequeDAO.insert(conn, number.trim(), bank.trim(), CurrencyUtil.round2(amount), issueDate, realizationDate,
                    type, type == ChequeType.RECEIVED ? customerId : null, type == ChequeType.ISSUED ? supplierId : null);
        }
    }

    public List<Cheque> getAll() throws SQLException {
        return chequeDAO.findAll();
    }

    public void markCleared(int chequeId) throws SQLException {
        changeStatus(chequeId, ChequeStatus.CLEARED);
    }

    /** Entered by mistake: only the cheque record is cancelled, balances are not changed. */
    public void cancel(int chequeId) throws SQLException {
        changeStatus(chequeId, ChequeStatus.CANCELLED);
    }

    /** Bounced: the customer owes the amount again (received) or we owe the supplier again (issued). */
    public void markBounced(int chequeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Cheque c = requirePending(conn, chequeId);
                chequeDAO.updateStatus(conn, chequeId, ChequeStatus.BOUNCED);

                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Pending cheques whose realization date is today or already passed. */
    public List<Cheque> getDueCheques() throws SQLException {
        return chequeDAO.findPendingUpTo(LocalDate.now());
    }

    /** Pending cheques that are due within the next few days (not yet today). */
    public List<Cheque> getUpcomingCheques(int days) throws SQLException {
        List<Cheque> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Cheque c : chequeDAO.findPendingUpTo(today.plusDays(days))) {
            if (c.getRealizationDate().isAfter(today)) {
                list.add(c);
            }
        }
        return list;
    }

    /**
     * Cheques that need a NEW reminder (due, and no reminder shown before). They are marked as reminded and a
     * notification_logs row is written, so each cheque pops up once; the dashboard alert list keeps showing it.
     */
    public List<Cheque> collectNewReminders() throws SQLException {
        List<Cheque> fresh = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (Cheque c : chequeDAO.findPendingUpTo(LocalDate.now())) {
                    if (!c.isReminderSent()) {
                        fresh.add(c);
                        chequeDAO.markReminderSent(conn, c.getChequeId());
                        chequeDAO.logNotification(conn, describe(c));
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
        return fresh;
    }

    public String describe(Cheque c) {
        return String.format("%s cheque %s (%s) Rs. %s - %s, due %s: %s", c.getChequeType() == ChequeType.RECEIVED ? "Received" : "Issued",
                c.getChequeNumber(), c.getBankName(), CurrencyUtil.format(c.getAmount()), c.getPartyName(),
                DateUtil.format(c.getRealizationDate()), c.getAction());
    }

    private void changeStatus(int chequeId, ChequeStatus status) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            requirePending(conn, chequeId);
            chequeDAO.updateStatus(conn, chequeId, status);
        }
    }

    private Cheque requirePending(Connection conn, int chequeId) throws SQLException {
        Cheque c = chequeDAO.findById(conn, chequeId);
        if (c == null) {
            throw new ValidationException("Cheque not found.");
        }
        if (c.getStatus() != ChequeStatus.PENDING) {
            throw new ValidationException("This cheque is already " + c.getStatus() + ".");
        }
        return c;
    }
}
