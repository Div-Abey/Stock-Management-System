package com.stockmanagement.ui.customer;

import javax.swing.*;
import java.awt.*;

/** "Customers" menu screen: customer accounts + ledger / statements, in tabs like the Sales screen. */
public class CustomerModulePanel extends JPanel {

    private final CustomerManagementPanel accounts = new CustomerManagementPanel();
    private final CustomerLedgerPanel ledger = new CustomerLedgerPanel();

    public CustomerModulePanel() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Customer Accounts", accounts);
        tabs.addTab("Ledger & Statements", ledger);
        tabs.addChangeListener(e -> refresh());
        add(tabs, BorderLayout.CENTER);
    }

    /** Called by the dashboard every time the menu button is clicked. */
    public void refresh() {
        accounts.refresh();
        ledger.refresh();
    }
}
