package com.stockmanagement.ui.sales;

import javax.swing.*;
import java.awt.*;

/** Stand-alone POS window (the Dashboard now embeds POSPanel instead). */
public class POSFrame extends JFrame {

    public POSFrame() {
        this(1, "System Admin");
    }

    public POSFrame(int cashierId, String cashierName) {
        POSPanel panel = new POSPanel(cashierId, cashierName);
        setTitle("Point of Sale (POS) - " + (cashierName != null ? cashierName : "Cashier #" + cashierId));
        setSize(1120, 680);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setContentPane(panel);
    }
}
