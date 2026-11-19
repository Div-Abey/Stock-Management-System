package com.stockmanagement.ui.supplier;

import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.common.util.CurrencyUtil;
import com.stockmanagement.common.util.ValidationUtil;
import com.stockmanagement.reports.export.PDFExporter;
import com.stockmanagement.reports.export.PurchaseOrderFormatter;
import com.stockmanagement.supplier.dao.PurchaseOrderDAO.ItemChoice;
import com.stockmanagement.supplier.model.PurchaseOrder;
import com.stockmanagement.supplier.model.PurchaseOrderItem;
import com.stockmanagement.supplier.model.Supplier;
import com.stockmanagement.supplier.service.PurchaseOrderService;
import com.stockmanagement.supplier.service.SupplierService;
import com.stockmanagement.ui.components.Dialogs;
import com.stockmanagement.ui.components.FormLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 3.2 Purchase orders: build an order, save it, then send it (PDF / e-mail) to the supplier. */
public class PurchaseOrderPanel extends JPanel {

    private final PurchaseOrderService poService = new PurchaseOrderService();
    private final SupplierService supplierService = new SupplierService();
    private final int userId;

    private final JComboBox<Supplier> supplierBox = new JComboBox<>();
    private final JComboBox<ItemChoice> itemBox = new JComboBox<>();
    private final JTextField qtyField = new JTextField("1", 6);
    private final JTextField priceField = new JTextField("0.00", 9);
    private final JLabel totalLabel = new JLabel("Order total: Rs. 0.00");
    private final List<PurchaseOrderItem> lines = new ArrayList<>();

    private final DefaultTableModel lineModel = SupplierManagementPanel.readOnly("Code", "Item", "Unit", "Qty", "Unit price", "Line total");
    private final JTable lineTable = new JTable(lineModel);
    private final DefaultTableModel poModel = SupplierManagementPanel.readOnly("PO No", "Supplier", "Order date", "Sent", "Status", "Total");
    private final JTable poTable = new JTable(poModel);
    private List<PurchaseOrder> shownOrders = new ArrayList<>();
    private boolean loading;

    public PurchaseOrderPanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout(8, 8));

        JButton addLine = new JButton("Add Line");
        JButton removeLine = new JButton("Remove Line");
        JButton create = new JButton("Create Purchase Order");
        JPanel form = FormLayout.form("New purchase order",
                FormLayout.row(new JLabel("Supplier:"), supplierBox),
                FormLayout.row(new JLabel("Item:"), itemBox, new JLabel("Qty:"), qtyField, new JLabel("Unit price:"), priceField, addLine, removeLine),
                FormLayout.row(totalLabel, create));
        JPanel linesPanel = new JPanel(new BorderLayout());
        linesPanel.add(new JScrollPane(lineTable));
        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.NORTH);
        top.add(linesPanel, BorderLayout.CENTER);

        JButton send = new JButton("Send / Export PDF");
        JButton view = new JButton("View Details");
        JButton cancel = new JButton("Cancel PO");
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(send); bar.add(view); bar.add(cancel);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("Purchase orders"));
        bottom.add(bar, BorderLayout.NORTH);
        bottom.add(new JScrollPane(poTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);

        itemBox.addActionListener(e -> {
            ItemChoice c = (ItemChoice) itemBox.getSelectedItem();
            if (!loading && c != null) priceField.setText(String.format("%.2f", c.lastPrice));
        });
        addLine.addActionListener(e -> Dialogs.run(this, this::addLine));
        removeLine.addActionListener(e -> {
            int row = lineTable.getSelectedRow();
            if (row >= 0) { lines.remove(row); redrawLines(); }
        });
        create.addActionListener(e -> Dialogs.run(this, this::createOrder));
        send.addActionListener(e -> Dialogs.run(this, this::sendOrder));
        view.addActionListener(e -> Dialogs.run(this, this::viewOrder));
        cancel.addActionListener(e -> Dialogs.run(this, () -> {
            PurchaseOrder po = selectedOrder();
            if (po == null) return;
            if (JOptionPane.showConfirmDialog(this, "Cancel " + po.getNumber() + "?", "Cancel PO", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            poService.cancel(po.getPoId());
            refresh();
        }));
    }

    /** Reloads suppliers, items and the order list (keeps the order being typed). */
    public void refresh() {
        Dialogs.run(this, () -> {
            loading = true;
            try {
                Supplier s = (Supplier) supplierBox.getSelectedItem();
                supplierBox.removeAllItems();
                for (Supplier x : supplierService.getAllSuppliers()) {
                    supplierBox.addItem(x);
                    if (s != null && x.getSupplierId() == s.getSupplierId()) supplierBox.setSelectedItem(x);
                }
                ItemChoice ic = (ItemChoice) itemBox.getSelectedItem();
                itemBox.removeAllItems();
                for (ItemChoice x : poService.getItemChoices()) {
                    itemBox.addItem(x);
                    if (ic != null && x.itemId == ic.itemId) itemBox.setSelectedItem(x);
                }
            } finally {
                loading = false;
            }
            shownOrders = poService.getAll();
            poModel.setRowCount(0);
            for (PurchaseOrder po : shownOrders) {
                poModel.addRow(new Object[]{po.getNumber(), po.getSupplierName(), po.getOrderDate(), po.getSentDate(),
                        po.getStatus(), CurrencyUtil.format(po.getTotal())});
            }
        });
    }

    private void addLine() {
        ItemChoice c = (ItemChoice) itemBox.getSelectedItem();
        if (c == null) throw new ValidationException("There are no items yet. Add items in Inventory first.");
        int qty = ValidationUtil.parseWholeNumber(qtyField.getText(), "Quantity");
        double price = ValidationUtil.requireNotNegative(CurrencyUtil.parse(priceField.getText()), "Unit price");
        lines.add(new PurchaseOrderItem(c.itemId, c.code, c.name, c.unitId, c.unitName, qty, price));
        redrawLines();
    }

    private void redrawLines() {
        lineModel.setRowCount(0);
        double total = 0;
        for (PurchaseOrderItem l : lines) {
            lineModel.addRow(new Object[]{l.getItemCode(), l.getItemName(), l.getUnitName(), l.getQuantity(),
                    CurrencyUtil.format(l.getUnitPrice()), CurrencyUtil.format(l.getLineTotal())});
            total += l.getLineTotal();
        }
        totalLabel.setText("Order total: Rs. " + CurrencyUtil.format(total) + "   ");
    }

    private void createOrder() throws SQLException {
        Supplier s = (Supplier) supplierBox.getSelectedItem();
        if (s == null) throw new ValidationException("Select a supplier (add one in the Suppliers tab first).");
        int id = poService.createPurchaseOrder(s.getSupplierId(), new ArrayList<>(lines), userId);
        lines.clear();
        redrawLines();
        refresh();
        Dialogs.info(this, String.format("Purchase order PO-%05d created. Select it below and press 'Send / Export PDF' to dispatch it.", id));
    }

    private PurchaseOrder selectedOrder() {
        int row = poTable.getSelectedRow();
        if (row < 0) {
            Dialogs.info(this, "Select a purchase order in the list first.");
            return null;
        }
        return shownOrders.get(row);
    }

    private void viewOrder() throws SQLException {
        PurchaseOrder row = selectedOrder();
        if (row == null) return;
        JTextArea area = new JTextArea(PurchaseOrderFormatter.toText(poService.getPurchaseOrder(row.getPoId())), 18, 90);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), row.getNumber(), JOptionPane.PLAIN_MESSAGE);
    }

    /** Dispatch: stamps the sent date, saves a PDF and opens the e-mail program with the order text when the supplier has an e-mail. */
    private void sendOrder() throws SQLException {
        PurchaseOrder row = selectedOrder();
        if (row == null) return;
        PurchaseOrder po = poService.dispatch(row.getPoId());
        refresh();

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(po.getNumber() + ".pdf"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                PDFExporter.exportText(PurchaseOrderFormatter.toLines(po), fc.getSelectedFile().toPath());
            } catch (IOException ex) {
                Dialogs.error(this, "Could not save the PDF: " + ex.getMessage());
            }
        }
        Supplier supplier = supplierService.getSupplier(po.getSupplierId());
        if (!supplier.getEmail().isEmpty() && Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            try {
                String uri = "mailto:" + supplier.getEmail() + "?subject=" + enc("Purchase Order " + po.getNumber())
                        + "&body=" + enc(PurchaseOrderFormatter.toText(po));
                Desktop.getDesktop().mail(new URI(uri));
            } catch (Exception ex) {
                Dialogs.info(this, "Could not open the e-mail program. Attach the saved PDF and send it to " + supplier.getEmail());
            }
        } else {
            Dialogs.info(this, po.getNumber() + " marked as sent. Send the PDF to the supplier"
                    + (supplier.getEmail().isEmpty() ? " (no e-mail saved for this supplier)." : "."));
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
