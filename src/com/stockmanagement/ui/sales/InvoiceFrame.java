package com.stockmanagement.ui.sales;

import com.stockmanagement.sales.model.Invoice;
import com.stockmanagement.sales.model.Sale;
import com.stockmanagement.sales.model.SaleItem;
import com.stockmanagement.sales.service.SalesService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.print.PrinterException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// [UPDATED BY KASUN] Swing frame for viewing invoice history, inspecting line items, and printing receipts
public class InvoiceFrame extends JFrame {

    private final SalesService salesService = new SalesService();
    private final List<Invoice> loadedInvoices = new ArrayList<>();

    /** true when this window was opened right after a sale: it closes itself once the receipt is printed. */
    private final boolean closeAfterPrint;

    private JTextField txtSearch;
    private DefaultTableModel invoiceTableModel;
    private JTable tblInvoices;
    private JTextArea txtReceiptPreview;

    public InvoiceFrame() {
        this(null);
    }

    public InvoiceFrame(Sale newlyCompletedSale) {
        this.closeAfterPrint = newlyCompletedSale != null;
        setTitle("Invoices & Receipt Printing");
        setSize(1120, 660);
        setMinimumSize(new Dimension(960, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(buildTopSearchBar(), BorderLayout.NORTH);
        add(buildSplitContentPanel(), BorderLayout.CENTER);
        add(buildFooterBar(), BorderLayout.SOUTH);

        loadAllInvoices();

        if (newlyCompletedSale != null) {
            txtReceiptPreview.setText(formatSaleReceipt(newlyCompletedSale));
            txtReceiptPreview.setCaretPosition(0);
            selectInvoiceRowByNumber(newlyCompletedSale.getInvoiceNumber());
        }
    }

    private JPanel buildTopSearchBar() {
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));

        topBar.add(new JLabel("Invoice No / Customer:"));
        txtSearch = new JTextField(24);
        txtSearch.addActionListener(e -> filterInvoices());
        topBar.add(txtSearch);

        JButton btnSearch = new JButton("Search");
        btnSearch.addActionListener(e -> filterInvoices());
        topBar.add(btnSearch);

        JButton btnReset = new JButton("Show All");
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadAllInvoices();
        });
        topBar.add(btnReset);

        JLabel hint = new JLabel("<html><i>Find an old invoice to view or re-print it. Use the invoice no. printed on the receipt "
                + "(works for every customer) or a registered customer's name. Walk-in sales are all listed as "
                + "\"Walk-in Customer\".</i></html>");
        hint.setForeground(Color.DARK_GRAY);
        hint.setBorder(new EmptyBorder(0, 8, 4, 8));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new TitledBorder("Search Invoices"));
        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(hint, BorderLayout.SOUTH);
        return wrapper;
    }

    private JSplitPane buildSplitContentPanel() {
        // Left: Historical Invoices Table
        String[] columns = {"ID", "Invoice No", "Customer", "Date", "Payment", "Net Amount", "Status"};
        invoiceTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblInvoices = new JTable(invoiceTableModel);
        tblInvoices.setRowHeight(25);
        int[] widths = {40, 170, 150, 140, 70, 90, 70};
        for (int i = 0; i < widths.length; i++) {
            tblInvoices.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        tblInvoices.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                previewSelectedInvoice();
            }
        });

        JScrollPane leftScroll = new JScrollPane(tblInvoices);
        leftScroll.setBorder(new TitledBorder("Sales Invoices"));

        // Right: Receipt Preview Area
        txtReceiptPreview = new JTextArea();
        txtReceiptPreview.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtReceiptPreview.setEditable(false);
        txtReceiptPreview.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane rightScroll = new JScrollPane(txtReceiptPreview);
        rightScroll.setBorder(new TitledBorder("Printable Receipt Preview"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        splitPane.setDividerLocation(640);
        splitPane.setBorder(new EmptyBorder(0, 10, 0, 10));
        return splitPane;
    }

    private JPanel buildFooterBar() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));

        JButton btnRefresh = new JButton("Refresh List");
        btnRefresh.addActionListener(e -> loadAllInvoices());

        JButton btnPrint = new JButton("Print Receipt");
        btnPrint.setBackground(new Color(37, 99, 235));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnPrint.addActionListener(e -> {
            if (printCurrentPreview() && closeAfterPrint) {
                dispose();   // back to the Sales (POS) screen
            }
        });

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());

        footer.add(btnRefresh);
        footer.add(btnPrint);
        footer.add(btnClose);
        return footer;
    }

    private void loadAllInvoices() {
        try {
            loadedInvoices.clear();
            loadedInvoices.addAll(salesService.getAllInvoices());
            populateInvoiceTable(loadedInvoices);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load invoices: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterInvoices() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            populateInvoiceTable(loadedInvoices);
            return;
        }

        List<Invoice> filtered = new ArrayList<>();
        for (Invoice inv : loadedInvoices) {
            boolean matchNo = inv.getInvoiceNumber() != null && inv.getInvoiceNumber().toLowerCase().contains(keyword);
            boolean matchCust = inv.getCustomerName() != null && inv.getCustomerName().toLowerCase().contains(keyword);
            if (matchNo || matchCust) {
                filtered.add(inv);
            }
        }
        populateInvoiceTable(filtered);
    }

    private void populateInvoiceTable(List<Invoice> invoices) {
        invoiceTableModel.setRowCount(0);
        for (Invoice inv : invoices) {
            invoiceTableModel.addRow(new Object[]{
                    inv.getInvoiceId(),
                    inv.getInvoiceNumber(),
                    inv.getCustomerName(),
                    inv.getFormattedInvoiceDate(),
                    inv.getPaymentType(),
                    String.format("%.2f", inv.getNetAmount()),
                    inv.getStatus()
            });
        }
    }

    private void selectInvoiceRowByNumber(String invoiceNumber) {
        if (invoiceNumber == null) {
            return;
        }
        for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
            if (invoiceNumber.equals(invoiceTableModel.getValueAt(i, 1))) {
                tblInvoices.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void previewSelectedInvoice() {
        int row = tblInvoices.getSelectedRow();
        if (row < 0) {
            return;
        }
        int invoiceId = (int) invoiceTableModel.getValueAt(row, 0);
        Invoice selected = null;
        for (Invoice inv : loadedInvoices) {
            if (inv.getInvoiceId() == invoiceId) {
                selected = inv;
                break;
            }
        }
        if (selected == null) {
            return;
        }

        try {
            List<SaleItem> items = salesService.getInvoiceItems(invoiceId);
            txtReceiptPreview.setText(formatInvoiceReceipt(selected, items));
            txtReceiptPreview.setCaretPosition(0);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading invoice items: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Prints straight to the DEFAULT printer (no print dialog). Returns true when the job was sent. */
    private boolean printCurrentPreview() {
        if (txtReceiptPreview.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Select an invoice to print first.",
                    "Print Notice", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
        if (printer == null) {
            JOptionPane.showMessageDialog(this, "No default printer found. Set a default printer in Windows and try again.",
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            // showPrintDialog = false -> prints at once on the default printer
            return txtReceiptPreview.print(null, null, false, printer, null, false);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Printer error: " + ex.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private String formatSaleReceipt(Sale sale) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================\n");
        sb.append("                  SALES INVOICE                 \n");
        sb.append("================================================\n");
        sb.append("Invoice No : ").append(sale.getInvoiceNumber()).append("\n");
        sb.append("Date       : ").append(sale.getFormattedInvoiceDate()).append("\n");
        sb.append("Customer   : ").append(sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in Customer").append("\n");
        sb.append("Payment    : ").append(sale.getPaymentType()).append(" [").append(sale.getStatus()).append("]\n");
        if (sale.getDueDate() != null) {
            sb.append("Due Date   : ").append(sale.getDueDate()).append("\n");
        }
        sb.append("------------------------------------------------\n");
        sb.append(String.format("%-18s %-8s %4s %12s\n", "Item", "Batch", "Qty", "Total (Rs.)"));
        sb.append("------------------------------------------------\n");

        for (SaleItem item : sale.getItems()) {
            String name = item.getItemName();
            if (name != null && name.length() > 18) {
                name = name.substring(0, 15) + "...";
            }
            sb.append(String.format("%-18s %-8s %4d %12.2f\n",
                    name,
                    item.getBatchNumber() != null ? item.getBatchNumber() : "-",
                    item.getQuantity(),
                    item.getLineTotal()));
        }

        sb.append("------------------------------------------------\n");
        sb.append(String.format("Gross Total   : Rs. %10.2f\n", sale.getTotalAmount()));
        sb.append(String.format("Bulk Discount : Rs. %10.2f\n", sale.getDiscountAmount()));
        sb.append(String.format("NET PAYABLE   : Rs. %10.2f\n", sale.getNetAmount()));
        sb.append("------------------------------------------------\n");
        sb.append(String.format("Amount Paid   : Rs. %10.2f\n", sale.getAmountPaid()));
        sb.append(String.format("Balance       : Rs. %10.2f\n", sale.getBalanceAmount()));
        sb.append("================================================\n");
        sb.append("           Thank you for your business!         \n");
        sb.append("================================================\n");
        return sb.toString();
    }

    private String formatInvoiceReceipt(Invoice inv, List<SaleItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================\n");
        sb.append("              SALES INVOICE (COPY)              \n");
        sb.append("================================================\n");
        sb.append("Invoice No : ").append(inv.getInvoiceNumber()).append("\n");
        sb.append("Date       : ").append(inv.getFormattedInvoiceDate()).append("\n");
        sb.append("Customer   : ").append(inv.getCustomerName() != null ? inv.getCustomerName() : "Walk-in Customer").append("\n");
        sb.append("Payment    : ").append(inv.getPaymentType()).append(" [").append(inv.getStatus()).append("]\n");
        sb.append("------------------------------------------------\n");
        sb.append(String.format("%-18s %-8s %4s %12s\n", "Item", "Batch", "Qty", "Total (Rs.)"));
        sb.append("------------------------------------------------\n");

        for (SaleItem item : items) {
            String name = item.getItemName();
            if (name != null && name.length() > 18) {
                name = name.substring(0, 15) + "...";
            }
            sb.append(String.format("%-18s %-8s %4d %12.2f\n",
                    name,
                    item.getBatchNumber() != null ? item.getBatchNumber() : "-",
                    item.getQuantity(),
                    item.getLineTotal()));
        }

        sb.append("------------------------------------------------\n");
        sb.append(String.format("Gross Total   : Rs. %10.2f\n", inv.getTotalAmount()));
        sb.append(String.format("Bulk Discount : Rs. %10.2f\n", inv.getDiscountAmount()));
        sb.append(String.format("NET PAYABLE   : Rs. %10.2f\n", inv.getNetAmount()));
        sb.append("================================================\n");
        return sb.toString();
    }
}