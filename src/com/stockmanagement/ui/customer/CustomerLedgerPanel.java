package com.stockmanagement.ui.customer;

import com.stockmanagement.common.util.CurrencyUtil;
import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.customer.model.AccountStatement;
import com.stockmanagement.customer.model.Customer;
import com.stockmanagement.customer.model.CustomerLedgerEntry;
import com.stockmanagement.customer.model.CustomerPayment;
import com.stockmanagement.customer.service.CustomerLedgerService;
import com.stockmanagement.customer.service.CustomerService;
import com.stockmanagement.reports.export.PDFExporter;
import com.stockmanagement.reports.export.StatementFormatter;
import com.stockmanagement.ui.components.Dialogs;
import com.stockmanagement.ui.components.PaymentDialog;
import com.stockmanagement.ui.components.PaymentDialog.PaymentInput;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

/** 3.1 Customer ledger: credit limit, balance, settlement and payment history, statement as PDF / print. */
public class CustomerLedgerPanel extends JPanel {

    private final CustomerService customerService = new CustomerService();
    private final CustomerLedgerService ledgerService = new CustomerLedgerService();

    private final JComboBox<Customer> customerBox = new JComboBox<>();
    private final JTextField fromField = new JTextField(DateUtil.format(LocalDate.now().withDayOfMonth(1)), 9);
    private final JTextField toField = new JTextField(DateUtil.format(LocalDate.now()), 9);
    private final JLabel summary = new JLabel(" ");
    private boolean loading;

    private final DefaultTableModel ledgerModel = new DefaultTableModel(
            new String[]{"Date", "Type", "Reference", "Details", "Debit", "Credit", "Balance"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel paymentModel = new DefaultTableModel(
            new String[]{"Date", "Amount", "Method", "Invoice"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private AccountStatement current;

    public CustomerLedgerPanel() {
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("Customer:")); top.add(customerBox);
        top.add(new JLabel("From:")); top.add(fromField);
        top.add(new JLabel("To:")); top.add(toField);
        JButton load = new JButton("Load");
        JButton pay = new JButton("Record Payment");
        JButton pdf = new JButton("Export PDF");
        JButton print = new JButton("Print");
        top.add(load); top.add(pay); top.add(pdf); top.add(print);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Account Statement", new JScrollPane(new JTable(ledgerModel)));
        tabs.addTab("Payment History", new JScrollPane(new JTable(paymentModel)));

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);

        load.addActionListener(e -> Dialogs.run(this, this::loadLedger));
        customerBox.addActionListener(e -> { if (!loading) Dialogs.run(this, this::loadLedger); });
        pay.addActionListener(e -> recordPayment());
        pdf.addActionListener(e -> exportPdf());
        print.addActionListener(e -> printStatement());
        refresh();
    }

    /** Reloads the customer list (keeps the selected customer) and the ledger. */
    public void refresh() {
        Dialogs.run(this, () -> {
            Customer selected = (Customer) customerBox.getSelectedItem();
            loading = true;
            try {
                customerBox.removeAllItems();
                Customer reselect = null;
                for (Customer c : customerService.getAllCustomers()) {
                    customerBox.addItem(c);
                    if (selected != null && c.getCustomerId() == selected.getCustomerId()) reselect = c;
                }
                if (reselect != null) customerBox.setSelectedItem(reselect);
            } finally {
                loading = false;
            }
            loadLedger();
        });
    }

    private void loadLedger() throws SQLException {
        Customer selected = (Customer) customerBox.getSelectedItem();
        ledgerModel.setRowCount(0);
        paymentModel.setRowCount(0);
        current = null;
        if (selected == null) {
            summary.setText("  No customers yet. Add one in the Customers tab.");
            return;
        }
        int id = selected.getCustomerId();
        Customer c = customerService.getCustomer(id);   // fresh balance
        current = ledgerService.buildStatement(id, DateUtil.parse(fromField.getText()), DateUtil.parse(toField.getText()));

        ledgerModel.addRow(new Object[]{"", "OPENING", "", "", "", "", CurrencyUtil.format(current.getOpeningBalance())});
        for (CustomerLedgerEntry e : current.getEntries()) {
            ledgerModel.addRow(new Object[]{DateUtil.format(e.getDate()), e.getEntryType(), e.getReference(), e.getDescription(),
                    e.getDebit() > 0 ? CurrencyUtil.format(e.getDebit()) : "",
                    e.getCredit() > 0 ? CurrencyUtil.format(e.getCredit()) : "", CurrencyUtil.format(e.getBalance())});
        }
        for (CustomerPayment p : ledgerService.getPaymentHistory(id)) {
            paymentModel.addRow(new Object[]{p.getPaymentDate(), CurrencyUtil.format(p.getAmountPaid()),
                    p.getPaymentMethod(), p.getInvoiceNumber()});
        }
        summary.setText(String.format("  Credit limit: Rs. %s   |   Owes now: Rs. %s   |   Available credit: Rs. %s   |   Statement closing balance: Rs. %s",
                CurrencyUtil.format(c.getCreditLimit()), CurrencyUtil.format(c.getCurrentBalance()),
                CurrencyUtil.format(c.getAvailableCredit()), CurrencyUtil.format(current.getClosingBalance())));
    }

    private void recordPayment() {
        Customer c = (Customer) customerBox.getSelectedItem();
        if (c == null) return;
        Dialogs.run(this, () -> {
            Customer fresh = customerService.getCustomer(c.getCustomerId());
            if (fresh.getCurrentBalance() <= 0.005) {
                Dialogs.info(this, fresh.getCustomerName() + " does not owe anything.");
                return;
            }
            PaymentInput in = PaymentDialog.show(this, "Payment from " + fresh.getCustomerName(), fresh.getCurrentBalance());
            if (in == null) return;
            ledgerService.recordPayment(fresh.getCustomerId(), in.amount, in.method, in.chequeNumber, in.bankName, in.realizationDate);
            loadLedger();
            Dialogs.info(this, "Payment recorded.");
        });
    }

    private void exportPdf() {
        if (current == null) { Dialogs.info(this, "Load a customer statement first."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("statement-" + current.getCustomer().getCustomerId() + "-" + current.getFrom() + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            PDFExporter.exportText(StatementFormatter.toLines(current), fc.getSelectedFile().toPath());
            Dialogs.info(this, "Saved " + fc.getSelectedFile().getName());
        } catch (IOException ex) {
            Dialogs.error(this, "Could not save the PDF: " + ex.getMessage());
        }
    }

    private void printStatement() {
        if (current == null) { Dialogs.info(this, "Load a customer statement first."); return; }
        JTextArea area = new JTextArea(StatementFormatter.toText(current));
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
        try {
            area.print();
        } catch (PrinterException ex) {
            Dialogs.error(this, "Printing failed: " + ex.getMessage());
        }
    }
}
