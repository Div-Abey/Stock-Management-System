package com.stockmanagement.reports.export;

import com.stockmanagement.common.util.CurrencyUtil;
import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.customer.model.AccountStatement;
import com.stockmanagement.customer.model.Customer;
import com.stockmanagement.customer.model.CustomerLedgerEntry;

import java.util.ArrayList;
import java.util.List;

/** Turns a customer account statement into printable text lines (used for the PDF and for printing). */
public final class StatementFormatter {
    private StatementFormatter() { }

    public static List<String> toLines(AccountStatement s) {
        Customer c = s.getCustomer();
        List<String> out = new ArrayList<>();
        out.add("CUSTOMER ACCOUNT STATEMENT");
        out.add("=".repeat(98));
        out.add("Customer     : " + c.getCustomerName() + "  (ID " + c.getCustomerId() + ")");
        out.add("Phone        : " + c.getPhone());
        out.add("Period       : " + DateUtil.format(s.getFrom()) + " to " + DateUtil.format(s.getTo()));
        out.add("Credit limit : Rs. " + CurrencyUtil.format(c.getCreditLimit()));
        out.add("Owes now     : Rs. " + CurrencyUtil.format(c.getCurrentBalance()));
        out.add("Available    : Rs. " + CurrencyUtil.format(c.getAvailableCredit()));
        out.add("");
        out.add(String.format("%-10s %-15s %-14s %-20s %12s %12s %12s", "Date", "Type", "Reference", "Details", "Debit", "Credit", "Balance"));
        out.add("-".repeat(98));
        out.add(String.format("%-10s %-15s %-14s %-20s %12s %12s %12s", "", "OPENING", "", "", "", "", CurrencyUtil.format(s.getOpeningBalance())));
        for (CustomerLedgerEntry e : s.getEntries()) {
            out.add(String.format("%-10s %-15s %-14s %-20s %12s %12s %12s", DateUtil.format(e.getDate()), cut(e.getEntryType(), 15),
                    cut(e.getReference(), 14), cut(e.getDescription(), 20),
                    e.getDebit() > 0 ? CurrencyUtil.format(e.getDebit()) : "",
                    e.getCredit() > 0 ? CurrencyUtil.format(e.getCredit()) : "", CurrencyUtil.format(e.getBalance())));
        }
        out.add("-".repeat(98));
        out.add(String.format("%-62s %12s %12s %12s", "TOTALS / CLOSING BALANCE", CurrencyUtil.format(s.getTotalDebit()),
                CurrencyUtil.format(s.getTotalCredit()), CurrencyUtil.format(s.getClosingBalance())));
        return out;
    }

    public static String toText(AccountStatement s) {
        return String.join("\n", toLines(s));
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + ".";
    }
}
