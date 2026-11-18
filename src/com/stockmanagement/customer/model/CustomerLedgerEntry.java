package com.stockmanagement.customer.model;

import java.time.LocalDate;

/** One line of a customer account statement. Debit = customer owes more, credit = customer paid. */
public class CustomerLedgerEntry {
    private final LocalDate date;
    private final String sortKey;       // full stored date-time, used only for ordering
    private final String entryType;     // INVOICE, PAYMENT, RETURNED CHEQUE
    private final String reference;
    private final String description;
    private final double debit;
    private final double credit;
    private double balance;             // running balance, filled in by the service

    public CustomerLedgerEntry(LocalDate date, String sortKey, String entryType, String reference,
                               String description, double debit, double credit) {
        this.date = date;
        this.sortKey = sortKey == null ? "" : sortKey;
        this.entryType = entryType;
        this.reference = reference == null ? "" : reference;
        this.description = description == null ? "" : description;
        this.debit = debit;
        this.credit = credit;
    }

    public LocalDate getDate() { return date; }
    public String getSortKey() { return sortKey; }
    public String getEntryType() { return entryType; }
    public String getReference() { return reference; }
    public String getDescription() { return description; }
    public double getDebit() { return debit; }
    public double getCredit() { return credit; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}
