package com.stockmanagement.ui.supplier;

import javax.swing.JFrame;

/** Stand-alone window around SupplierManagementPanel (the dashboard embeds the same panel). */
public class SupplierManagementFrame extends JFrame {

    public SupplierManagementFrame() {
        super("Supplier Management");
        add(new SupplierManagementPanel(1));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);
    }
}
