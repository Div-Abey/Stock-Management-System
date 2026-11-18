package com.stockmanagement.ui.components;

import com.stockmanagement.inventory.model.Item;
import com.stockmanagement.inventory.model.StockBatch;
import com.stockmanagement.inventory.service.StockAlertService;
import com.stockmanagement.inventory.service.StockAlertService.AlertSummary;
import com.stockmanagement.supplier.model.Cheque;
import com.stockmanagement.supplier.service.ChequeService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * "Alerts" box for the Dashboard home page, split into sub sections:
 * Out of Stock, Low Stock, Expired Batches, Expiring Soon and Cheques. Click a category button to collapse/expand it.
 * Refreshes itself every minute.
 */
public class AlertsPanel extends JPanel {

    private static final int MAX_LINES = 6;

    private final StockAlertService alertService = new StockAlertService();
    private final ChequeService chequeService = new ChequeService();
    private final JPanel list = new JPanel();

    public AlertsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Alerts"));

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        // wrapper keeps the sections at their natural height and stretches them to the full width
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(list, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll, BorderLayout.CENTER);

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

            // low stock list already includes zero-stock items, so split it
            List<Item> outOfStock = new ArrayList<>();
            List<Item> lowStock = new ArrayList<>();
            for (Item i : s.lowStock) {
                (i.getCurrentStock() <= 0 ? outOfStock : lowStock).add(i);
            }

            List<String> out = new ArrayList<>();
            for (Item i : outOfStock) {
                out.add(i.getItemName() + " (" + i.getItemCode() + ")");
            }
            addSection("Out of Stock", out);

            List<String> low = new ArrayList<>();
            for (Item i : lowStock) {
                low.add(i.getItemName() + ": " + i.getCurrentStock() + " / " + i.getReorderLevel()
                        + " " + i.getBaseUnitName());
            }
            addSection("Low Stock", low);

            List<String> expired = new ArrayList<>();
            for (StockBatch b : s.expired) {
                expired.add(b.getItemName() + " [" + b.getBatchNumber() + "] qty " + b.getQuantityRemaining()
                        + ", expired " + b.getExpiryDate());
            }
            addSection("Expired Batches", expired);

            List<String> soon = new ArrayList<>();
            for (StockBatch b : s.expiringSoon) {
                soon.add(b.getItemName() + " [" + b.getBatchNumber() + "] expires " + b.getExpiryDate());
            }
            addSection("Expiring within " + StockAlertService.DEFAULT_EXPIRY_WARNING_DAYS + " days", soon);
        } catch (SQLException e) {
            addSection("Stock alerts", List.of("Could not load stock alerts: " + e.getMessage()));
        }

        try {
            List<String> cheques = new ArrayList<>();
            for (Cheque c : chequeService.getDueCheques()) {
                cheques.add("DUE: " + chequeService.describe(c));
            }
            for (Cheque c : chequeService.getUpcomingCheques(3)) {
                cheques.add("Soon: " + chequeService.describe(c));
            }
            addSection("Cheques", cheques);
        } catch (SQLException e) {
            addSection("Cheques", List.of("Could not load cheque reminders: " + e.getMessage()));
        }

        list.revalidate();
        list.repaint();
    }

    /** One collapsible sub section: a button with a count badge, then up to MAX_LINES lines. */
    private void addSection(String title, List<String> lines) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(2, 16, 6, 4));

        if (lines.isEmpty()) {
            body.add(line("Nothing to report.", Color.GRAY, false));
        } else {
            for (int i = 0; i < lines.size() && i < MAX_LINES; i++) {
                body.add(line(lines.get(i), null, false));
            }
            if (lines.size() > MAX_LINES) {
                body.add(line("...and " + (lines.size() - MAX_LINES) + " more", Color.GRAY, false));
            }
        }
        body.setVisible(!lines.isEmpty());

        JButton header = new JButton(title + "  (" + lines.size() + ")");
        header.setFont(new Font("Arial", Font.BOLD, 13));
        header.setHorizontalAlignment(SwingConstants.LEFT);
        header.setFocusPainted(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
        header.addActionListener(e -> {
            boolean show = !body.isVisible();
            body.setVisible(show);
            list.revalidate();
            list.repaint();
        });

        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(header);
        list.add(body);
        list.add(Box.createVerticalStrut(4));
    }

    private JLabel line(String text, Color color, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 12));
        if (color != null) l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
