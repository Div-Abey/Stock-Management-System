package com.stockmanagement.ui.cheque;

import com.stockmanagement.common.enums.ChequeStatus;
import com.stockmanagement.common.enums.ChequeType;
import com.stockmanagement.common.util.CurrencyUtil;
import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.customer.model.Customer;
import com.stockmanagement.customer.service.CustomerService;
import com.stockmanagement.supplier.model.Cheque;
import com.stockmanagement.supplier.model.Supplier;
import com.stockmanagement.supplier.service.ChequeService;
import com.stockmanagement.supplier.service.SupplierService;
import com.stockmanagement.ui.components.Dialogs;
import com.stockmanagement.ui.components.FormLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 3.3 Cheque management: post-dated cheques from customers and to suppliers, with realization-date reminders. */
public class ChequePanel extends JPanel {

    private static final String[] FILTERS = {"All", "Pending", "Due / overdue", "Cleared", "Bounced", "Cancelled"};

    private final ChequeService chequeService = new ChequeService();
    private final CustomerService customerService = new CustomerService();
    private final SupplierService supplierService = new SupplierService();

    private final JComboBox<ChequeType> typeBox = new JComboBox<>(ChequeType.values());
    private final JComboBox<Object> partyBox = new JComboBox<>();
    private final JTextField numberField = new JTextField(10);
    private final JTextField bankField = new JTextField(14);
    private final JTextField amountField = new JTextField("0.00", 10);
    private final JTextField issueField = new JTextField(DateUtil.format(LocalDate.now()), 9);
    private final JTextField realizeField = new JTextField(DateUtil.format(LocalDate.now().plusDays(30)), 9);
    private final JComboBox<String> filterBox = new JComboBox<>(FILTERS);
    private final JLabel reminderLabel = new JLabel(" ");

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Type", "Party", "Cheque No", "Bank", "Amount", "Cheque date", "Realization date", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<Cheque> shown = new ArrayList<>();
    private boolean loading;

    public ChequePanel() {
        setLayout(new BorderLayout(8, 8));

        JButton add = new JButton("Record Cheque");
        JPanel form = FormLayout.form("Record a cheque (cheques paid through Customer Ledger / Accounts Payable are added automatically)",
                FormLayout.row(new JLabel("Type:"), typeBox, new JLabel("Customer / Supplier:"), partyBox),
                FormLayout.row(new JLabel("Cheque No:"), numberField, new JLabel("Bank:"), bankField, new JLabel("Amount (Rs.):"), amountField),
                FormLayout.row(new JLabel("Cheque date:"), issueField, new JLabel("Realization date:"), realizeField, add));

        JButton cleared = new JButton("Mark Cleared");
        JButton bounced = new JButton("Mark Bounced");
        JButton cancel = new JButton("Cancel Entry");
        JButton reload = new JButton("Refresh");
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(new JLabel("Show:")); bar.add(filterBox);
        bar.add(cleared); bar.add(bounced); bar.add(cancel); bar.add(reload);

        JPanel north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.NORTH);
        north.add(bar, BorderLayout.CENTER);
        north.add(reminderLabel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // rows that are due today / overdue are shown in red
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                if (!sel) {
                    boolean due = row < shown.size() && shown.get(row).isDue(LocalDate.now());
                    c.setBackground(due ? new Color(255, 220, 220) : t.getBackground());
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });

        typeBox.addActionListener(e -> { if (!loading) Dialogs.run(this, this::loadParties); });
        filterBox.addActionListener(e -> { if (!loading) Dialogs.run(this, this::loadTable); });
        reload.addActionListener(e -> refresh());
        add.addActionListener(e -> Dialogs.run(this, this::recordCheque));
        cleared.addActionListener(e -> change("clear", id -> chequeService.markCleared(id)));
        bounced.addActionListener(e -> change("mark as bounced", id -> chequeService.markBounced(id)));
        cancel.addActionListener(e -> change("cancel", id -> chequeService.cancel(id)));
        refresh();
    }

    private interface IdAction { void run(int chequeId) throws java.sql.SQLException; }

    public void refresh() {
        Dialogs.run(this, () -> {
            loadParties();
            loadTable();
        });
    }

    private void loadParties() throws java.sql.SQLException {
        loading = true;
        try {
            partyBox.removeAllItems();
            if (typeBox.getSelectedItem() == ChequeType.RECEIVED) {
                for (Customer c : customerService.getAllCustomers()) partyBox.addItem(c);
            } else {
                for (Supplier s : supplierService.getAllSuppliers()) partyBox.addItem(s);
            }
        } finally {
            loading = false;
        }
    }

    private void loadTable() throws java.sql.SQLException {
        String filter = (String) filterBox.getSelectedItem();
        LocalDate today = LocalDate.now();
        shown = new ArrayList<>();
        model.setRowCount(0);
        int dueCount = 0;
        for (Cheque c : chequeService.getAll()) {
            if (c.isDue(today)) dueCount++;
            boolean ok;
            switch (filter == null ? "All" : filter) {
                case "Pending": ok = c.getStatus() == ChequeStatus.PENDING; break;
                case "Due / overdue": ok = c.isDue(today); break;
                case "Cleared": ok = c.getStatus() == ChequeStatus.CLEARED; break;
                case "Bounced": ok = c.getStatus() == ChequeStatus.BOUNCED; break;
                case "Cancelled": ok = c.getStatus() == ChequeStatus.CANCELLED; break;
                default: ok = true;
            }
            if (!ok) continue;
            shown.add(c);
            model.addRow(new Object[]{c.getChequeId(), c.getChequeType(), c.getPartyName(), c.getChequeNumber(), c.getBankName(),
                    CurrencyUtil.format(c.getAmount()), DateUtil.format(c.getIssueDate()), DateUtil.format(c.getRealizationDate()), c.getStatus()});
        }
        reminderLabel.setText(dueCount == 0 ? "  No cheques need action today."
                : "  REMINDER: " + dueCount + " cheque(s) reached their realization date - bank the received ones / fund the issued ones (rows in red).");
        reminderLabel.setForeground(dueCount == 0 ? Color.DARK_GRAY : new Color(12, 180, 0));
    }

    private void recordCheque() throws java.sql.SQLException {
        Object party = partyBox.getSelectedItem();
        ChequeType type = (ChequeType) typeBox.getSelectedItem();
        Integer customerId = party instanceof Customer ? ((Customer) party).getCustomerId() : null;
        Integer supplierId = party instanceof Supplier ? ((Supplier) party).getSupplierId() : null;
        chequeService.recordCheque(type, customerId, supplierId, numberField.getText(), bankField.getText(),
                CurrencyUtil.parse(amountField.getText()), DateUtil.parse(issueField.getText()), DateUtil.parse(realizeField.getText()));
        numberField.setText(""); bankField.setText(""); amountField.setText("0.00");
        loadTable();
    }

    private void change(String verb, IdAction action) {
        Dialogs.run(this, () -> {
            int row = table.getSelectedRow();
            if (row < 0) { Dialogs.info(this, "Select a cheque first."); return; }
            Cheque c = shown.get(row);
            if (JOptionPane.showConfirmDialog(this, "Do you want to " + verb + " cheque " + c.getChequeNumber() + "?", "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            action.run(c.getChequeId());
            loadTable();
        });
    }
}
