package com.stockmanagement.ui.customer;

import com.stockmanagement.common.enums.CustomerType;
import com.stockmanagement.common.util.CurrencyUtil;
import com.stockmanagement.customer.model.Customer;
import com.stockmanagement.customer.service.CreditService;
import com.stockmanagement.customer.service.CustomerService;
import com.stockmanagement.ui.components.Dialogs;
import com.stockmanagement.ui.components.FormLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/** 3.1 Customer accounts: add / edit customers and their credit limit. Plugs into the dashboard content area. */
public class CustomerManagementPanel extends JPanel {

    private final CustomerService customerService = new CustomerService();
    private final CreditService creditService = new CreditService();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Type", "Phone", "Credit Limit", "Owes", "Available"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField nameField = new JTextField(50);
    private final JTextField phoneField = new JTextField(50);
    private final JTextField emailField = new JTextField(50);
    private final JComboBox<CustomerType> typeBox = new JComboBox<>(CustomerType.values());
    private final JTextField limitField = new JTextField("0", 9);
    private List<Customer> shown;

    public CustomerManagementPanel() {
        setLayout(new BorderLayout(8, 8));

        JButton add = new JButton("Add New");
        JButton update = new JButton("Update Selected");
        JButton clear = new JButton("Clear");
        JPanel form = FormLayout.form("Customer details (select a row to edit it)",

                FormLayout.row(new JLabel("Name:"), nameField),

                FormLayout.row(new JLabel("Phone:"), phoneField),

                FormLayout.row(new JLabel("Email:"), emailField),

                FormLayout.row(new JLabel("Type:"), typeBox),

                FormLayout.row(new JLabel("Credit limit:"), limitField),

                FormLayout.row(add, update, clear)
        );

        add(form, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        add.addActionListener(e -> Dialogs.run(this, () -> {
            customerService.addCustomer(nameField.getText(), phoneField.getText(), emailField.getText(),
                    (CustomerType) typeBox.getSelectedItem(), CurrencyUtil.parse(limitField.getText()));
            clearForm();
            refresh();
        }));
        update.addActionListener(e -> Dialogs.run(this, () -> {
            int row = table.getSelectedRow();
            if (row < 0) { Dialogs.info(this, "Select a customer row first."); return; }
            customerService.updateCustomer(shown.get(row).getCustomerId(), nameField.getText(), phoneField.getText(),
                    emailField.getText(), (CustomerType) typeBox.getSelectedItem(), CurrencyUtil.parse(limitField.getText()));
            refresh();
        }));
        clear.addActionListener(e -> { clearForm(); table.clearSelection(); });
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && shown != null && row < shown.size()) {
                Customer c = shown.get(row);
                nameField.setText(c.getCustomerName());
                phoneField.setText(c.getPhone());
                emailField.setText(c.getEmail());
                typeBox.setSelectedItem(c.getCustomerType());
                limitField.setText(String.format("%.2f", c.getCreditLimit()));
            }
        });
        refresh();
    }

    /** Reloads the table from the database. */
    public void refresh() {
        Dialogs.run(this, () -> {
            shown = customerService.getAllCustomers();
            model.setRowCount(0);
            for (Customer c : shown) {
                model.addRow(new Object[]{c.getCustomerId(), c.getCustomerName(), c.getCustomerType(), c.getPhone(),
                        CurrencyUtil.format(c.getCreditLimit()), CurrencyUtil.format(c.getCurrentBalance()),
                        CurrencyUtil.format(creditService.getAvailableCredit(c))});
            }
        });
    }

    private void clearForm() {
        nameField.setText(""); phoneField.setText(""); emailField.setText(""); limitField.setText("0");
        typeBox.setSelectedItem(CustomerType.RETAIL);
    }
}
