package com.stockmanagement.customer.model;

import java.time.LocalDate;
import java.util.List;

/** A customer's account statement for a date range (the monthly statement). */
public class AccountStatement {
    private final Customer customer;
    private final LocalDate from;
    private final LocalDate to;
    private final double openingBalance;
    private final List<CustomerLedgerEntry> entries;
    private final double closingBalance;

    public AccountStatement(Customer customer, LocalDate from, LocalDate to, double openingBalance,
                            List<CustomerLedgerEntry> entries, double closingBalance) {
        this.customer = customer;
        this.from = from;
        this.to = to;
        this.openingBalance = openingBalance;
        this.entries = entries;
        this.closingBalance = closingBalance;
    }

    public Customer getCustomer() { return customer; }
    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
    public double getOpeningBalance() { return openingBalance; }
    public List<CustomerLedgerEntry> getEntries() { return entries; }
    public double getClosingBalance() { return closingBalance; }

    public double getTotalDebit() {
        double t = 0;
        for (CustomerLedgerEntry e : entries) t += e.getDebit();
        return t;
    }

    public double getTotalCredit() {
        double t = 0;
        for (CustomerLedgerEntry e : entries) t += e.getCredit();
        return t;
    }
}
