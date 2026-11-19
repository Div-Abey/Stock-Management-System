package com.stockmanagement.ui.reports;

import javax.swing.*;
import java.awt.*;

/** The "Reports" screen for the Dashboard content area: one tab per report group. */
public class ReportsPanel extends JPanel {

    private final JTabbedPane tabs = new JTabbedPane();
    private final ReportViewPanel[] panels = {
            new SalesReportPanel(),
            new StockReportPanel(),
            new ProfitLossReportPanel()
    };

    public ReportsPanel() {
        setLayout(new BorderLayout());
        tabs.setFont(new Font("Arial", Font.BOLD, 13));
        tabs.addTab("Sales Reports", panels[0]);
        tabs.addTab("Stock Reports", panels[1]);
        tabs.addTab("Profit & Loss", panels[2]);
        add(tabs, BorderLayout.CENTER);

        tabs.addChangeListener(e -> refreshSelected());
        refreshSelected();
    }

    /** Re-run the report on the visible tab (called each time the Reports menu is opened). */
    public void refreshSelected() {
        Component c = tabs.getSelectedComponent();
        if (c instanceof ReportViewPanel) {
            ((ReportViewPanel) c).refresh();
        }
    }
}
