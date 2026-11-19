package com.stockmanagement.ui.components;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Small shared Swing helpers so every inventory screen looks like the POS screen (same colours, spacing, fonts). */
public final class UiKit {


    private UiKit() {
    }

    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(26);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        return t;
    }

    /** Sets the starting width of each column, in pixels (the table still stretches to fit the window). */
    public static void columnWidths(JTable t, int... widths) {
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public static JSpinner intSpinner(int value, int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        s.setPreferredSize(new Dimension(110, 28));
        return s;
    }

    public static JSpinner moneySpinner() {
        JSpinner s = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100000000.0, 1.0));
        s.setEditor(new JSpinner.NumberEditor(s, "0.00"));
        s.setPreferredSize(new Dimension(110, 28));
        return s;
    }

    public static JSpinner dateSpinner(LocalDate initial) {
        JSpinner s = new JSpinner(new SpinnerDateModel(toDate(initial), null, null, java.util.Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd"));
        s.setPreferredSize(new Dimension(130, 28));
        return s;
    }

    public static LocalDate dateOf(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static double doubleOf(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }

    public static int intOf(JSpinner s) {
        return ((Number) s.getValue()).intValue();
    }

    public static String money(double v) {
        return String.format("%,.2f", v);
    }

    /** Adds "label  [component]" as one form row (label in column 0, component stretching in column 1). */
    public static void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    public static JPanel rowOf(JComponent... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        for (JComponent c : parts) p.add(c);
        return p;
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
