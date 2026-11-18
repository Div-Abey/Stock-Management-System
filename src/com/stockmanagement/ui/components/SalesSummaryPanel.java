package com.stockmanagement.ui.components;

import com.stockmanagement.reports.service.SalesSummaryService;
import com.stockmanagement.reports.service.SalesSummaryService.Summary;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/** Home page card showing sales totals for either today or the previous calendar month. */
public class SalesSummaryPanel extends JPanel {

    public enum Period { TODAY, LAST_MONTH }

    private final Period period;
    private final SalesSummaryService service = new SalesSummaryService();

    private final JLabel bigNumber = new JLabel("Rs. 0.00");
    private final JLabel caption = new JLabel("Net sales");
    private final JPanel rows = new JPanel(new GridBagLayout());

    public SalesSummaryPanel(Period period) {
        super(new BorderLayout(0, 8));
        this.period = period;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(titleText()),
                BorderFactory.createEmptyBorder(4, 10, 8, 10)));

        bigNumber.setFont(new Font("SansSerif", Font.BOLD, 28));
        caption.setFont(new Font("SansSerif", Font.PLAIN, 12));
        caption.setForeground(Color.GRAY);

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);
        head.add(bigNumber);
        head.add(caption);

        add(head, BorderLayout.NORTH);
        add(rows, BorderLayout.CENTER);

        refresh();
        new Timer(60_000, e -> refresh()).start();
    }

    private String titleText() {
        if (period == Period.TODAY) {
            return "Today's Sales";
        }
        YearMonth last = YearMonth.now().minusMonths(1);
        return "Last Month Sales (" + last.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + ")";
    }

    public void refresh() {
        rows.removeAll();
        try {
            Summary s = period == Period.TODAY ? service.today() : service.lastMonth();
            bigNumber.setText("Rs. " + UiKit.money(s.netSales));
            int r = 0;
            addRow(r++, "Invoices", String.valueOf(s.invoiceCount), null);
            addRow(r++, "Items sold", String.valueOf(s.itemsSold), null);
            addRow(r++, "Discounts given", "Rs. " + UiKit.money(s.discounts), null);
            addRow(r++, "Amount received", "Rs. " + UiKit.money(s.received), null);
            addRow(r++, "Outstanding (credit)", "Rs. " + UiKit.money(s.outstanding()), null);
            if (period == Period.LAST_MONTH) {
                addRow(r++, "Average per day", "Rs. " + UiKit.money(s.averagePerDay()), null);
            }
        } catch (SQLException e) {
            bigNumber.setText("--");
            JLabel err = new JLabel("Could not load sales: " + e.getMessage());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            rows.add(err, c);
        }
        rows.revalidate();
        rows.repaint();
    }

    private void addRow(int row, String label, String value, Color valueColor) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(3, 0, 3, 0);
        c.anchor = GridBagConstraints.WEST;

        JLabel name = new JLabel(label);
        name.setFont(new Font("SansSerif", Font.PLAIN, 13));
        c.gridx = 0;
        c.weightx = 1;
        rows.add(name, c);

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 13));
        if (valueColor != null) val.setForeground(valueColor);
        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        rows.add(val, c);
    }
}
