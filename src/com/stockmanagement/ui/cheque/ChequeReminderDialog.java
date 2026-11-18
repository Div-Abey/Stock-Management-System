package com.stockmanagement.ui.cheque;

import com.stockmanagement.supplier.model.Cheque;
import com.stockmanagement.supplier.service.ChequeService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/** Shows the "cheque reached its realization date" reminder pop-up (each cheque pops up once). */
public final class ChequeReminderDialog {
    private ChequeReminderDialog() { }

    /** Returns the number of cheques that are due (new or old) so the dashboard can show it in Alerts. */
    public static void showIfNeeded(Component parent, ChequeService service) {
        try {
            List<Cheque> fresh = service.collectNewReminders();
            if (fresh.isEmpty()) return;
            StringBuilder sb = new StringBuilder("These cheques reached their realization date:\n\n");
            for (Cheque c : fresh) sb.append(" - ").append(service.describe(c)).append('\n');
            JTextArea area = new JTextArea(sb.toString(), Math.min(14, fresh.size() + 3), 80);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(parent, new JScrollPane(area), "Cheque reminder", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            System.err.println("Cheque reminder error: " + ex.getMessage());
        }
    }
}
