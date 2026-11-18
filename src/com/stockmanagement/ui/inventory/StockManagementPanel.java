package com.stockmanagement.ui.inventory;

import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.inventory.model.Item;
import com.stockmanagement.inventory.model.StockBatch;
import com.stockmanagement.inventory.model.Unit;
import com.stockmanagement.inventory.service.ItemService;
import com.stockmanagement.inventory.service.StockAlertService;
import com.stockmanagement.inventory.service.StockAlertService.AlertSummary;
import com.stockmanagement.inventory.service.StockService;
import com.stockmanagement.inventory.service.StockService.ReceiveRequest;
import com.stockmanagement.inventory.service.UnitConversionService;
import com.stockmanagement.ui.components.InventoryNavBar;
import com.stockmanagement.ui.components.UiKit;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Left: receive stock (item, batch number, expiry date, quantity in Box/Carton/Piece).
 * Right: every batch in sell order (earliest expiry first) with alerts: low stock, expiring soon, expired.
 * Shown as a tab inside {@link InventoryPanel}.
 */
public class StockManagementPanel extends JPanel implements InventoryNavBar.Refreshable {

    private static final int WARN_DAYS = StockAlertService.DEFAULT_EXPIRY_WARNING_DAYS;

    private final ItemService itemService = new ItemService();
    private final StockService stockService = new StockService();
    private final UnitConversionService unitService = new UnitConversionService();
    private final StockAlertService alertService = new StockAlertService();
    private final boolean canEdit;
    private final int userId;
    private final List<StockBatch> rows = new ArrayList<>();
    private boolean loading;

    private final JTextField txtScan = new JTextField(16);
    private final JComboBox<Item> cmbItem = new JComboBox<>();
    private final JLabel lblItemInfo = new JLabel("<html>&nbsp;<br>&nbsp;</html>");
    private final JTextField txtBatch = new JTextField(14);
    private final JSpinner spnExpiry = UiKit.dateSpinner(LocalDate.now().plusMonths(6));
    private final JCheckBox chkNoExpiry = new JCheckBox("No expiry");
    private final JSpinner spnQty = UiKit.intSpinner(1, 1, 10_000_000);
    private final JComboBox<Unit> cmbUnit = new JComboBox<>();
    private final JLabel lblConversion = new JLabel(" ");
    private final JSpinner spnPrice = UiKit.moneySpinner();
    private final JTextField txtReference = new JTextField(16);
    private final JLabel lblAlerts = new JLabel(" ");
    private final DefaultTableModel model = UiKit.readOnlyModel("Code", "Item", "Batch No", "Expiry", "Days Left",
            "Remaining", "Unit", "Status");
    private final JTable table = UiKit.table(model);

    public StockManagementPanel(int userId, String role) {
        super(new BorderLayout(10, 10));
        this.userId = userId > 0 ? userId : 1;
        this.canEdit = InventoryNavBar.canEdit(role);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildForm(), BorderLayout.WEST);
        add(buildBatchPanel(), BorderLayout.CENTER);
        // No data is loaded here: InventoryPanel calls refreshData() once this panel is on screen.
    }

    // ---------------------------------------------------------------- UI

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("Receive Stock"));

        txtScan.setToolTipText("Scan the barcode or type the item code, then press Enter");
        txtScan.addActionListener(e -> scanItem());
        cmbItem.addActionListener(e -> { if (!loading) onItemChanged(); });
        JButton btnAuto = new JButton("Auto");
        btnAuto.addActionListener(e -> txtBatch.setText("B" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)));
        JPanel batchRow = new JPanel(new BorderLayout(4, 0));
        batchRow.add(txtBatch, BorderLayout.CENTER);
        batchRow.add(btnAuto, BorderLayout.EAST);
        chkNoExpiry.addActionListener(e -> spnExpiry.setEnabled(!chkNoExpiry.isSelected()));
        spnQty.addChangeListener(e -> updateConversionLabel());
        cmbUnit.addActionListener(e -> { if (!loading) updateConversionLabel(); });
        lblConversion.setFont(new Font("SansSerif", Font.BOLD, 12));

        UiKit.addRow(form, 0, "Scan Barcode / Code", txtScan);
        UiKit.addRow(form, 1, "Item *", cmbItem);
        UiKit.addRow(form, 2, "", lblItemInfo);
        UiKit.addRow(form, 3, "Batch Number *", batchRow);
        UiKit.addRow(form, 4, "Expiry Date *", UiKit.rowOf(spnExpiry, chkNoExpiry));
        UiKit.addRow(form, 5, "Quantity *", UiKit.rowOf(spnQty, cmbUnit));
        UiKit.addRow(form, 6, "", lblConversion);
        UiKit.addRow(form, 7, "Price per unit (Rs.)", spnPrice);
        UiKit.addRow(form, 8, "Supplier Invoice No.", txtReference);

        JButton btnReceive = new JButton("Receive Stock");
        btnReceive.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnReceive.setPreferredSize(new Dimension(200, 38));
        btnReceive.addActionListener(e -> receive());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 9; c.gridx = 0; c.gridwidth = 2; c.insets = new Insets(12, 6, 6, 6);
        c.weighty = 1; c.anchor = GridBagConstraints.NORTH; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(btnReceive, c);

        if (!canEdit) {
            for (JComponent x : new JComponent[]{txtScan, cmbItem, txtBatch, btnAuto, spnExpiry, chkNoExpiry, spnQty,
                    cmbUnit, spnPrice, txtReference, btnReceive}) {
                x.setEnabled(false);
            }
        }
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(form, BorderLayout.NORTH);
        wrap.setPreferredSize(new Dimension(430, 0));
        return wrap;
    }

    private JPanel buildBatchPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        lblAlerts.setFont(new Font("SansSerif", Font.BOLD, 13));
        p.add(lblAlerts, BorderLayout.NORTH);

        UiKit.columnWidths(table, 70, 210, 100, 100, 70, 90, 60, 130);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new TitledBorder("Stock batches (earliest expiry is sold first)"));
        p.add(sp, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton adjust = new JButton("Adjust Selected Batch");
        adjust.addActionListener(e -> adjust());
        JButton writeOff = new JButton("Write Off Expired Stock");
        writeOff.addActionListener(e -> writeOff());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshData());
        adjust.setEnabled(canEdit);
        writeOff.setEnabled(canEdit);
        btns.add(adjust);
        btns.add(writeOff);
        btns.add(refresh);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    // -------------------------------------------------------------- data

    @Override
    public void refreshData() {
        try {
            loading = true;
            Item keep = (Item) cmbItem.getSelectedItem();
            cmbItem.removeAllItems();
            for (Item i : itemService.search(null)) {
                cmbItem.addItem(i);
                if (keep != null && keep.getItemId() == i.getItemId()) cmbItem.setSelectedItem(i);
            }
        } catch (SQLException e) {
            UiKit.error(this, "Could not load items: " + e.getMessage());
        } finally {
            loading = false;
        }
        onItemChanged();
        loadBatches();
    }

    private void loadBatches() {
        try {
            rows.clear();
            model.setRowCount(0);
            for (StockBatch b : stockService.getBatches(null, false)) {
                rows.add(b);
                model.addRow(new Object[]{b.getItemCode(), b.getItemName(), b.getBatchNumber(), DateUtil.display(b.getExpiryDate()),
                        DateUtil.isNoExpiry(b.getExpiryDate()) ? "-" : b.getDaysToExpiry(),
                        b.getQuantityRemaining(), b.getBaseUnitName(), b.getStatus(WARN_DAYS)});
            }
            AlertSummary s = alertService.getSummary(WARN_DAYS);
            lblAlerts.setText("Alerts:  " + s.lowStock.size() + " low stock   |   " + s.expiringSoon.size()
                    + " expiring within " + WARN_DAYS + " days   |   " + s.expired.size() + " expired");
            StringBuilder tip = new StringBuilder("<html>");
            int n = 0;
            for (Item i : s.lowStock) {
                if (n++ == 10) { tip.append("..."); break; }
                tip.append("Low: ").append(i.getItemName()).append(" (").append(i.getCurrentStock()).append("/")
                        .append(i.getReorderLevel()).append(")<br>");
            }
            lblAlerts.setToolTipText(s.lowStock.isEmpty() ? null : tip.toString());
        } catch (SQLException e) {
            UiKit.error(this, "Could not load batches: " + e.getMessage());
        }
    }

    private void scanItem() {
        String code = txtScan.getText().trim();
        if (code.isEmpty()) return;
        try {
            Item found = itemService.findByCodeOrBarcode(code);
            if (found == null) {
                UiKit.warn(this, "No item with code or barcode \"" + code + "\". Add it in Item Management first.");
                return;
            }
            for (int k = 0; k < cmbItem.getItemCount(); k++) {
                if (cmbItem.getItemAt(k).getItemId() == found.getItemId()) cmbItem.setSelectedIndex(k);
            }
            txtScan.setText("");
            txtBatch.requestFocus();
        } catch (SQLException e) {
            UiKit.error(this, e.getMessage());
        }
    }

    private void onItemChanged() {
        Item item = (Item) cmbItem.getSelectedItem();
        loading = true;
        cmbUnit.removeAllItems();
        loading = false;
        if (item == null) {
            lblItemInfo.setText(" ");
            lblConversion.setText(" ");
            return;
        }
        try {
            Item fresh = itemService.findById(item.getItemId());
            if (fresh != null) item = fresh;
            lblItemInfo.setText("<html>Base unit: " + item.getBaseUnitName() + "  |  In stock: " + item.getCurrentStock()
                    + "<br>Re-order level: " + item.getReorderLevel() + "</html>");
            loading = true;
            for (Unit u : unitService.getUnitsFor(item)) cmbUnit.addItem(u);
            loading = false;
        } catch (SQLException e) {
            loading = false;
            UiKit.error(this, e.getMessage());
        }
        updateConversionLabel();
    }

    private void updateConversionLabel() {
        Item item = (Item) cmbItem.getSelectedItem();
        Unit unit = (Unit) cmbUnit.getSelectedItem();
        if (item == null || unit == null) {
            lblConversion.setText(" ");
            return;
        }
        try {
            int base = unitService.toBaseUnits(item.getItemId(), unit.getUnitId(), UiKit.intOf(spnQty));
            lblConversion.setText("= " + base + " " + item.getBaseUnitName() + " added to stock  (" + unitService.describe(item, unit) + ")");
        } catch (ValidationException e) {
            lblConversion.setText(e.getMessage());
        } catch (SQLException e) {
            lblConversion.setText(e.getMessage());
        }
    }

    private void receive() {
        Item item = (Item) cmbItem.getSelectedItem();
        Unit unit = (Unit) cmbUnit.getSelectedItem();
        if (item == null || unit == null) {
            UiKit.warn(this, "Select an item first.");
            return;
        }
        ReceiveRequest r = new ReceiveRequest();
        r.itemId = item.getItemId();
        r.batchNumber = txtBatch.getText();
        r.expiryDate = chkNoExpiry.isSelected() ? null : UiKit.dateOf(spnExpiry);
        r.quantity = UiKit.intOf(spnQty);
        r.purchaseUnitId = unit.getUnitId();
        r.pricePerPurchaseUnit = UiKit.doubleOf(spnPrice);
        r.reference = txtReference.getText();
        r.userId = userId;
        try {
            StockBatch b = stockService.receiveStock(r);
            UiKit.info(this, "Stock received into batch " + b.getBatchNumber() + ".\nBatch now holds "
                    + b.getQuantityRemaining() + " " + item.getBaseUnitName() + ".");
            txtBatch.setText("");
            txtReference.setText("");
            spnQty.setValue(1);
            onItemChanged();
            loadBatches();
        } catch (ValidationException e) {
            UiKit.warn(this, e.getMessage());
        } catch (SQLException e) {
            UiKit.error(this, "Database error: " + e.getMessage());
        }
    }

    private void adjust() {
        int v = table.getSelectedRow();
        if (v < 0) {
            UiKit.warn(this, "Select a batch in the table first.");
            return;
        }
        StockBatch b = rows.get(table.convertRowIndexToModel(v));
        JSpinner qty = UiKit.intSpinner(b.getQuantityRemaining(), 0, 100_000_000);
        JTextField reason = new JTextField(20);
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(new JLabel(b.getItemName() + " - batch " + b.getBatchNumber() + " (now " + b.getQuantityRemaining() + " " + b.getBaseUnitName() + ")"));
        p.add(new JLabel("Actual quantity on the shelf:"));
        p.add(qty);
        p.add(new JLabel("Reason (damaged, stock-take...):"));
        p.add(reason);
        if (JOptionPane.showConfirmDialog(this, p, "Adjust Batch", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            stockService.adjustBatch(b.getBatchId(), UiKit.intOf(qty), reason.getText(), userId);
            onItemChanged();
            loadBatches();
        } catch (ValidationException e) {
            UiKit.warn(this, e.getMessage());
        } catch (SQLException e) {
            UiKit.error(this, "Database error: " + e.getMessage());
        }
    }

    private void writeOff() {
        if (!UiKit.confirm(this, "Set all expired batches that still hold stock to zero?")) return;
        try {
            int n = stockService.writeOffExpired(userId);
            UiKit.info(this, n + " expired batch(es) written off.");
            onItemChanged();
            loadBatches();
        } catch (SQLException e) {
            UiKit.error(this, "Database error: " + e.getMessage());
        }
    }
}
