package com.stockmanagement.ui.customer;

import javax.swing.JFrame;

/** Stand-alone window around CustomerManagementPanel (the dashboard embeds the same panel). */
public class CustomerManagementFrame extends JFrame {

    public CustomerManagementFrame() {
        super("Customer Management");
        add(new CustomerManagementPanel());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 480);
        setLocationRelativeTo(null);
    }
}
