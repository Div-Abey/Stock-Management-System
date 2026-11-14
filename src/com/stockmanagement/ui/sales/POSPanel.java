package com.stockmanagement.ui.sales;

import com.stockmanagement.inventory.dao.ItemDAO.CustomerSummary;
import com.stockmanagement.inventory.dao.ItemDAO.ItemStockSummary;
import com.stockmanagement.sales.model.Sale;
import com.stockmanagement.sales.model.SaleItem;
import com.stockmanagement.sales.service.DiscountService;
import com.stockmanagement.sales.service.SalesService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** The POS screen as a panel: used inside the Dashboard content area (and by POSFrame when a separate window is wanted). */
public class POSPanel extends JPanel {

    private final SalesService salesService = new SalesService();
    private final List<SaleItem> cartItems = new ArrayList<>();
    private final int currentCashierId;
    private final String currentCashierName;

    private JComboBox<CustomerSummary> cmbCustomers;
    private JTextField txtBarcodeScan;
    private JComboBox<ItemStockSummary> cmbItems;
    private JSpinner spnQuantity;
    private DefaultTableModel cartTableModel;
    private JTable tblCart;

    private JLabel lblGrossTotal;
    private JLabel lblTotalDiscount;
    private JLabel lblNetPayable;

    public POSPanel() {
        this(1, "System Admin");
    }

    public POSPanel(int cashierId, String cashierName) {
        this.currentCashierId = cashierId > 0 ? cashierId : 1;
        this.currentCashierName = (cashierName != null && !cashierName.isBlank()) ? cashierName : "Cashier #" + this.currentCashierId;

        setLayout(new BorderLayout(10, 10));

        add(buildTopControlsPanel(), BorderLayout.NORTH);
        add(buildCartTablePanel(), BorderLayout.CENTER);
        add(buildBottomSummaryPanel(), BorderLayout.SOUTH);

        refreshMasterData();
    }

    private JPanel buildTopControlsPanel() {
        JPanel topWrapper = new JPanel(new BorderLayout(0, 8));
        topWrapper.setBorder(new EmptyBorder(10, 10, 0, 10));

        // Block 1: customer + barcode. GridBag keeps both lines inside the panel at any width
        // (the old FlowLayout wrapped and cut the extra buttons off, which looked like a stray button).
        JPanel row1 = new JPanel(new GridBagLayout());
        row1.setBorder(new TitledBorder("Customer & Barcode Scanner"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridy = 0;
        c.gridx = 0;
        row1.add(new JLabel("Customer:"), c);

        cmbCustomers = new JComboBox<>();
        cmbCustomers.setPreferredSize(new Dimension(260, 28));
        cmbCustomers.setMinimumSize(new Dimension(120, 28));
        cmbCustomers.addActionListener(e -> recalculateCartForSelectedCustomer());
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        row1.add(cmbCustomers, c);

        JButton btnOpenInvoices = new JButton("Invoices & History");
        btnOpenInvoices.addActionListener(e -> new InvoiceFrame().setVisible(true));
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        row1.add(btnOpenInvoices, c);

        c.gridy = 1;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        row1.add(new JLabel("Scan Barcode / Code:"), c);

        txtBarcodeScan = new JTextField(16);
        txtBarcodeScan.setPreferredSize(new Dimension(170, 28));
        txtBarcodeScan.setMinimumSize(new Dimension(100, 28));
        txtBarcodeScan.addActionListener(e -> handleBarcodeScan());
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        row1.add(txtBarcodeScan, c);

        JButton btnQuickScan = new JButton("Quick Add");
        btnQuickScan.addActionListener(e -> handleBarcodeScan());
        c.gridx = 2;
        c.weightx = 0;
        row1.add(btnQuickScan, c);

        // Block 2: product, quantity and the add / refresh buttons
        JPanel row2 = new JPanel(new GridBagLayout());
        row2.setBorder(new TitledBorder("Select Product"));
        GridBagConstraints d = new GridBagConstraints();
        d.insets = new Insets(4, 6, 4, 6);
        d.anchor = GridBagConstraints.WEST;

        d.gridy = 0;
        d.gridx = 0;
        row2.add(new JLabel("Available Stock Item:"), d);

        cmbItems = new JComboBox<>();
        cmbItems.setPreferredSize(new Dimension(380, 28));
        cmbItems.setMinimumSize(new Dimension(120, 28));
        d.gridx = 1;
        d.weightx = 1;
        d.fill = GridBagConstraints.HORIZONTAL;
        row2.add(cmbItems, d);

        JPanel qtyBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        qtyBox.setOpaque(false);
        qtyBox.add(new JLabel("Qty:"));
        spnQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        spnQuantity.setPreferredSize(new Dimension(75, 28));
        qtyBox.add(spnQuantity);

        JButton btnAddCart = new JButton("Add to Cart");
        btnAddCart.setBackground(new Color(37, 99, 235));
        btnAddCart.setForeground(Color.WHITE);
        btnAddCart.addActionListener(e -> handleAddSelectedItem());
        qtyBox.add(btnAddCart);

        // reloads the stock list + customer list from the database (see tooltip)
        JButton btnRefreshStock = new JButton("Refresh Stock");
        btnRefreshStock.setToolTipText("Reload the item list (current stock) and customer list from the database. The cart is not changed.");
        btnRefreshStock.addActionListener(e -> refreshMasterData());
        qtyBox.add(btnRefreshStock);

        d.gridx = 2;
        d.weightx = 0;
        d.fill = GridBagConstraints.NONE;
        row2.add(qtyBox, d);

        JLabel lblBulkOffer = new JLabel("Offer: " + DiscountService.describeDefaultRule());
        lblBulkOffer.setForeground(new Color(22, 163, 74));
        lblBulkOffer.setFont(new Font("SansSerif", Font.BOLD, 12));
        d.gridy = 1;
        d.gridx = 0;
        d.gridwidth = 3;
        row2.add(lblBulkOffer, d);

        topWrapper.add(row1, BorderLayout.NORTH);
        topWrapper.add(row2, BorderLayout.CENTER);
        return topWrapper;
    }

    private JPanel buildCartTablePanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(6, 6));
        centerPanel.setBorder(new EmptyBorder(0, 10, 0, 10));

        String[] columns = {"Code", "Item Name", "Unit", "Unit Price (Rs.)", "Qty", "Bulk Discount (Rs.)", "Line Total (Rs.)"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblCart = new JTable(cartTableModel);
        tblCart.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(tblCart);
        scrollPane.setBorder(new TitledBorder("Current POS Cart"));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel cartActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        JButton btnRemoveRow = new JButton("Remove Selected Item");
        btnRemoveRow.addActionListener(e -> removeSelectedCartItem());

        JButton btnClearCart = new JButton("Clear Cart");
        btnClearCart.addActionListener(e -> clearCart());

        cartActions.add(btnRemoveRow);
        cartActions.add(btnClearCart);
        centerPanel.add(cartActions, BorderLayout.SOUTH);

        return centerPanel;
    }

    private JPanel buildBottomSummaryPanel() {
        JPanel bottomBar = new JPanel(new BorderLayout(12, 10));
        bottomBar.setBorder(new EmptyBorder(8, 12, 12, 12));
        bottomBar.setBackground(new Color(241, 245, 249));

        JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        totalsPanel.setOpaque(false);

        lblGrossTotal = new JLabel("Gross: Rs. 0.00");
        lblGrossTotal.setFont(new Font("SansSerif", Font.PLAIN, 14));

        lblTotalDiscount = new JLabel("Discount: Rs. 0.00");
        lblTotalDiscount.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTotalDiscount.setForeground(new Color(22, 163, 74));

        lblNetPayable = new JLabel("Net Payable: Rs. 0.00");
        lblNetPayable.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblNetPayable.setForeground(new Color(15, 23, 42));

        totalsPanel.add(lblGrossTotal);
        totalsPanel.add(lblTotalDiscount);
        totalsPanel.add(lblNetPayable);

        JButton btnProceedBilling = new JButton("Proceed to Checkout  ->");
        btnProceedBilling.setBackground(new Color(22, 163, 74));
        btnProceedBilling.setForeground(Color.WHITE);
        btnProceedBilling.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnProceedBilling.setPreferredSize(new Dimension(215, 42));
        btnProceedBilling.addActionListener(e -> openBillingCheckout());

        bottomBar.add(totalsPanel, BorderLayout.WEST);
        bottomBar.add(btnProceedBilling, BorderLayout.EAST);

        return bottomBar;
    }

    public void refreshMasterData() {
        try {
            CustomerSummary prevCustomer = (CustomerSummary) cmbCustomers.getSelectedItem();
            cmbCustomers.removeAllItems();
            for (CustomerSummary c : salesService.getCustomers()) {
                cmbCustomers.addItem(c);
                if (prevCustomer != null
                        && prevCustomer.getCustomerId() != null
                        && prevCustomer.getCustomerId().equals(c.getCustomerId())) {
                    cmbCustomers.setSelectedItem(c);
                }
            }

            cmbItems.removeAllItems();
            for (ItemStockSummary item : salesService.getAvailableItems()) {
                cmbItems.addItem(item);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading POS items/customers: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleBarcodeScan() {
        String code = txtBarcodeScan.getText().trim();
        if (code.isEmpty()) {
            return;
        }
        try {
            ItemStockSummary item = salesService.findItemByCodeOrBarcode(code);
            if (item == null || item.getAvailableStock() <= 0) {
                JOptionPane.showMessageDialog(this, "Item not found or out of stock: " + code,
                        "Barcode Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            addItemToCart(item, 1);
            txtBarcodeScan.setText("");
            txtBarcodeScan.requestFocus();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Scan lookup failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleAddSelectedItem() {
        ItemStockSummary selected = (ItemStockSummary) cmbItems.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select an available item.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int qty = (int) spnQuantity.getValue();
        addItemToCart(selected, qty);
        spnQuantity.setValue(1);
    }

    private void addItemToCart(ItemStockSummary stockItem, int qtyToAdd) {
        try {
            CustomerSummary customer = (CustomerSummary) cmbCustomers.getSelectedItem();

            SaleItem existing = null;
            for (SaleItem s : cartItems) {
                if (s.getItemId() == stockItem.getItemId()) {
                    existing = s;
                    break;
                }
            }

            int updatedQty = qtyToAdd + (existing != null ? existing.getQuantity() : 0);
            if (updatedQty > stockItem.getAvailableStock()) {
                JOptionPane.showMessageDialog(this,
                        "Requested quantity exceeds available stock (" + stockItem.getAvailableStock() + " " + stockItem.getBaseUnitName() + ").",
                        "Stock Limit", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (existing != null) {
                existing.setQuantity(updatedQty);
                salesService.getBillingService().applyPricingAndDiscount(existing, stockItem, customer);
            } else {
                SaleItem newItem = new SaleItem(
                        stockItem.getItemId(),
                        stockItem.getItemCode(),
                        stockItem.getItemName(),
                        stockItem.getBaseUnitId(),
                        updatedQty,
                        stockItem.getRetailPrice()
                );
                newItem.setUnitName(stockItem.getBaseUnitName());
                salesService.getBillingService().applyPricingAndDiscount(newItem, stockItem, customer);
                cartItems.add(newItem);
            }

            refreshCartDisplay();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to apply pricing/discount: " + ex.getMessage(),
                    "Calculation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalculateCartForSelectedCustomer() {
        if (cartItems.isEmpty()) {
            return;
        }
        try {
            CustomerSummary customer = (CustomerSummary) cmbCustomers.getSelectedItem();
            for (SaleItem s : cartItems) {
                ItemStockSummary stockItem = salesService.findItemByCodeOrBarcode(s.getItemCode());
                if (stockItem != null) {
                    salesService.getBillingService().applyPricingAndDiscount(s, stockItem, customer);
                }
            }
            refreshCartDisplay();
        } catch (SQLException ignored) {
        }
    }

    private void removeSelectedCartItem() {
        int selectedRow = tblCart.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < cartItems.size()) {
            cartItems.remove(selectedRow);
            refreshCartDisplay();
        }
    }

    private void clearCart() {
        cartItems.clear();
        refreshCartDisplay();
    }

    private void refreshCartDisplay() {
        cartTableModel.setRowCount(0);
        for (SaleItem item : cartItems) {
            cartTableModel.addRow(new Object[]{
                    item.getItemCode(),
                    item.getItemName(),
                    item.getUnitName() != null ? item.getUnitName() : "",
                    String.format("%.2f", item.getUnitPrice()),
                    item.getQuantity(),
                    String.format("%.2f", item.getDiscountApplied()),
                    String.format("%.2f", item.getLineTotal())
            });
        }

        double gross = salesService.getBillingService().calculateCartGrossTotal(cartItems);
        double discount = salesService.getBillingService().calculateCartDiscountTotal(cartItems);
        double net = salesService.getBillingService().calculateCartNetPayable(cartItems);

        lblGrossTotal.setText(String.format("Gross: Rs. %.2f", gross));
        lblTotalDiscount.setText(String.format("Discount: -Rs. %.2f", discount));
        lblNetPayable.setText(String.format("Net Payable: Rs. %.2f", net));
    }

    private void openBillingCheckout() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty. Add items before proceeding to checkout.",
                    "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CustomerSummary customer = (CustomerSummary) cmbCustomers.getSelectedItem();
        Sale draftSale = new Sale();
        draftSale.setCustomerId(customer != null ? customer.getCustomerId() : null);
        draftSale.setCustomerName(customer != null ? customer.getCustomerName() : "Walk-in Customer");
        draftSale.setCashierId(currentCashierId);
        draftSale.setItems(new ArrayList<>(cartItems));

        BillingFrame billingFrame = new BillingFrame(draftSale, salesService, () -> {
            clearCart();
            refreshMasterData();
        });
        billingFrame.setVisible(true);
    }
}