package com.stockmanagement.ui.customer;

import javax.swing.JFrame;

/** Stand-alone window around CustomerLedgerPanel (the dashboard embeds the same panel). */
public class CustomerLedgerFrame extends JFrame {

    public CustomerLedgerFrame() {
        super("Customer Ledger");
        add(new CustomerLedgerPanel());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 520);
        setLocationRelativeTo(null);
    }
}
