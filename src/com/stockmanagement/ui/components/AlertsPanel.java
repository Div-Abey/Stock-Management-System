package com.stockmanagement.ui.components;

import com.stockmanagement.inventory.model.Item;
import com.stockmanagement.inventory.model.StockBatch;
import com.stockmanagement.inventory.service.StockAlertService;
import com.stockmanagement.inventory.service.StockAlertService.AlertSummary;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/** Compact "Alerts" box for the Dashboard home page. Refreshes itself every minute. */
public class AlertsPanel extends JPanel {

    private final StockAlertService alertService = new StockAlertService();
    private final JPanel list = new JPanel();

    public AlertsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Alerts"));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(new JScrollPane(list), BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshAlerts());
        add(refresh, BorderLayout.SOUTH);

        refreshAlerts();
        new Timer(60_000, e -> refreshAlerts()).start();
    }

    public void refreshAlerts() {
        list.removeAll();
        try {
            AlertSummary s = alertService.getSummary(StockAlertService.DEFAULT_EXPIRY_WARNING_DAYS);
            if (s.total() == 0) {
                line("No alerts - stock levels and expiry dates are fine.", false);
            }
            if (!s.lowStock.isEmpty()) {
                line(s.lowStock.size() + " item(s) at or below re-order level", true);
                int shown = 0;
                for (Item i : s.lowStock) {
                    if (shown++ == 6) { line("   ...and " + (s.lowStock.size() - 6) + " more", false); break; }
                    line("   " + i.getItemName() + ": " + i.getCurrentStock() + " / " + i.getReorderLevel() + " " + i.getBaseUnitName(), false);
                }
            }
            if (!s.expired.isEmpty()) {
                line(s.expired.size() + " expired batch(es) still in stock", true);
                for (StockBatch b : s.expired.subList(0, Math.min(4, s.expired.size()))) {
                    line("   " + b.getItemName() + " [" + b.getBatchNumber() + "] " + b.getQuantityRemaining(), false);
                }
            }
            if (!s.expiringSoon.isEmpty()) {
                line(s.expiringSoon.size() + " batch(es) expiring within " + StockAlertService.DEFAULT_EXPIRY_WARNING_DAYS + " days", true);
                for (StockBatch b : s.expiringSoon.subList(0, Math.min(4, s.expiringSoon.size()))) {
                    line("   " + b.getItemName() + " [" + b.getBatchNumber() + "] " + b.getExpiryDate(), false);
                }
            }
        } catch (SQLException e) {
            line("Could not load alerts: " + e.getMessage(), false);
        }
        list.revalidate();
        list.repaint();
    }

    private void line(String text, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 13));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(l);
    }
}
