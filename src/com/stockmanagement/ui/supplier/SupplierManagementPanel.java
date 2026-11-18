package com.stockmanagement.ui.supplier;

import com.stockmanagement.supplier.model.Supplier;
import com.stockmanagement.supplier.service.SupplierService;
import com.stockmanagement.ui.components.Dialogs;
import com.stockmanagement.ui.components.FormLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** "Suppliers" menu screen (3.2): Suppliers and Purchase Orders. */
public class SupplierManagementPanel extends JPanel {

    private final SupplierService supplierService = new SupplierService();
    private final PurchaseOrderPanel poPanel;

    private final DefaultTableModel supplierModel = readOnly("ID", "Name", "Phone", "Email", "Address");
    private final JTable supplierTable = new JTable(supplierModel);
    private final JTextField nameField = new JTextField(50);
    private final JTextField phoneField = new JTextField(50);
    private final JTextField emailField = new JTextField(50);
    private final JTextField addressField = new JTextField(50);
    private List<Supplier> shownSuppliers;

    public SupplierManagementPanel(int userId) {
        setLayout(new BorderLayout());
        poPanel = new PurchaseOrderPanel(userId);

        JTabbedPane main = new JTabbedPane();
        main.addTab("Suppliers", buildSuppliersTab());
        main.addTab("Purchase Orders", poPanel);
        main.addChangeListener(e -> refresh());
        add(main, BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildSuppliersTab() {
        JButton add = new JButton("Add New");
        JButton update = new JButton("Update Selected");
        JButton clear = new JButton("Clear");
        JPanel form = FormLayout.form("Supplier details (select a row to edit it)",
                FormLayout.row(new JLabel("Name:"), nameField),
                FormLayout.row(new JLabel("Phone:"), phoneField),
                FormLayout.row(new JLabel("Email:"), emailField),
                FormLayout.row(new JLabel("Address:"), addressField),
                FormLayout.row(add, update, clear));

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(supplierTable), BorderLayout.CENTER);

        add.addActionListener(e -> Dialogs.run(this, () -> {
            supplierService.addSupplier(nameField.getText(), phoneField.getText(), emailField.getText(), addressField.getText());
            clearForm();
            refresh();
        }));
        update.addActionListener(e -> Dialogs.run(this, () -> {
            int row = supplierTable.getSelectedRow();
            if (row < 0) { Dialogs.info(this, "Select a supplier row first."); return; }
            supplierService.updateSupplier(shownSuppliers.get(row).getSupplierId(), nameField.getText(),
                    phoneField.getText(), emailField.getText(), addressField.getText());
            refresh();
        }));
        clear.addActionListener(e -> { clearForm(); supplierTable.clearSelection(); });
        supplierTable.getSelectionModel().addListSelectionListener(e -> {
            int row = supplierTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && shownSuppliers != null && row < shownSuppliers.size()) {
                Supplier s = shownSuppliers.get(row);
                nameField.setText(s.getName());
                phoneField.setText(s.getPhone());
                emailField.setText(s.getEmail());
                addressField.setText(s.getAddress());
            }
        });
        return p;
    }

    /** Reloads the suppliers table and the purchase-order lists. */
    public void refresh() {
        Dialogs.run(this, () -> {
            shownSuppliers = supplierService.getAllSuppliers();
            supplierModel.setRowCount(0);
            for (Supplier s : shownSuppliers) {
                supplierModel.addRow(new Object[]{s.getSupplierId(), s.getName(), s.getPhone(), s.getEmail(), s.getAddress()});
            }
            poPanel.refresh();
        });
    }

    private void clearForm() {
        nameField.setText(""); phoneField.setText(""); emailField.setText(""); addressField.setText("");
    }

    static DefaultTableModel readOnly(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}