package com.stockmanagement.ui.inventory;

import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.inventory.model.Category;
import com.stockmanagement.inventory.model.Item;
import com.stockmanagement.inventory.model.Unit;
import com.stockmanagement.inventory.service.ItemService;
import com.stockmanagement.inventory.service.UnitConversionService;
import com.stockmanagement.ui.components.InventoryNavBar;
import com.stockmanagement.ui.components.UiKit;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Add / edit / delete items: code, barcode, name, category, base unit, re-order level and prices.
 * Shown as a tab inside {@link InventoryPanel}; data is (re)loaded each time the tab is shown.
 */
public class ItemManagementPanel extends JPanel implements InventoryNavBar.Refreshable {

    private final ItemService itemService = new ItemService();
    private final UnitConversionService unitService = new UnitConversionService();
    private final boolean canEdit;
    private final List<Item> rows = new ArrayList<>();
    private Item selected;   // null = adding a new item
    private boolean loading;

    private final JTextField txtSearch = new JTextField();
    private final JTextField txtCode = new JTextField(16);
    private final JTextField txtBarcode = new JTextField(16);
    private final JTextField txtName = new JTextField(16);
    private final JComboBox<Category> cmbCategory = new JComboBox<>();
    private final JComboBox<Unit> cmbUnit = new JComboBox<>();
    private final JSpinner spnReorder = UiKit.intSpinner(10, 0, 10_000_000);
    private final JSpinner spnRetail = UiKit.moneySpinner();
    private final JSpinner spnWholesale = UiKit.moneySpinner();
    private final JLabel lblMode = new JLabel("New item");
    private final JButton btnSave = new JButton("Add Item");
    private final JButton btnUpdate = new JButton("Update");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnClear = new JButton("Clear");
    private final DefaultTableModel model = UiKit.readOnlyModel("Code", "Barcode", "Item Name", "Category", "Unit",
            "In Stock", "Re-order", "Retail (Rs.)", "Status");
    private final JTable table = UiKit.table(model);

    public ItemManagementPanel(String role) {
        super(new BorderLayout(10, 10));
        this.canEdit = InventoryNavBar.canEdit(role);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildForm(), BorderLayout.WEST);
        add(buildTablePanel(), BorderLayout.CENTER);
        // No data is loaded here: InventoryPanel calls refreshData() once this panel is on screen.
    }

    // ---------------------------------------------------------------- UI

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("Item Details"));

        lblMode.setFont(new Font("SansSerif", Font.BOLD, 13));
        txtBarcode.setToolTipText("Type the barcode printed on the product, or click here and scan it");

        UiKit.addRow(form, 0, "", lblMode);
        UiKit.addRow(form, 1, "Item Code *", txtCode);
        UiKit.addRow(form, 2, "Barcode", txtBarcode);
        UiKit.addRow(form, 3, "Item Name *", txtName);
        UiKit.addRow(form, 4, "Category *", cmbCategory);
        UiKit.addRow(form, 5, "Base Unit *", cmbUnit);
        UiKit.addRow(form, 6, "Re-order Level", spnReorder);
        UiKit.addRow(form, 7, "Retail Price (Rs.)", spnRetail);
        UiKit.addRow(form, 8, "Wholesale Price (Rs.)", spnWholesale);

        btnSave.addActionListener(e -> saveNew());
        btnUpdate.addActionListener(e -> updateSelected());
        btnDelete.addActionListener(e -> deleteSelected());
        btnClear.addActionListener(e -> clearForm());
        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 6));
        buttons.add(btnSave);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = 9; bc.gridx = 0; bc.gridwidth = 2; bc.fill = GridBagConstraints.HORIZONTAL;
        bc.insets = new Insets(12, 6, 6, 6); bc.weighty = 1; bc.anchor = GridBagConstraints.NORTH;
        form.add(buttons, bc);

        if (!canEdit) {
            for (JComponent c : new JComponent[]{txtCode, txtBarcode, txtName, cmbCategory, cmbUnit, spnReorder,
                    spnRetail, spnWholesale, btnSave, btnUpdate, btnDelete, btnClear}) {
                c.setEnabled(false);
            }
        }
        form.setPreferredSize(new Dimension(340, 0));
        return form;
    }

    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        JPanel search = new JPanel(new BorderLayout(6, 0));
        search.add(new JLabel("Search (code / name / barcode):"), BorderLayout.WEST);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loadTable(); }
            public void removeUpdate(DocumentEvent e) { loadTable(); }
            public void changedUpdate(DocumentEvent e) { loadTable(); }
        });
        search.add(txtSearch, BorderLayout.CENTER);
        p.add(search, BorderLayout.NORTH);

        UiKit.columnWidths(table, 70, 115, 150, 80, 50, 60, 60, 70, 90);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !loading) onRowSelected();
        });
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new TitledBorder("Items"));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // -------------------------------------------------------------- data

    @Override
    public void refreshData() {
        try {
            loading = true;
            cmbCategory.removeAllItems();
            for (Category c : itemService.getCategories()) cmbCategory.addItem(c);
            cmbUnit.removeAllItems();
            for (Unit u : unitService.getUnits()) cmbUnit.addItem(u);
        } catch (SQLException e) {
            UiKit.error(this, "Could not load categories/units: " + e.getMessage());
        } finally {
            loading = false;
        }
        loadTable();
        if (selected == null && txtCode.getText().isBlank() && canEdit) autoCode();
    }

    private void loadTable() {
        try {
            int keepId = selected == null ? -1 : selected.getItemId();
            loading = true;
            rows.clear();
            model.setRowCount(0);
            for (Item i : itemService.search(txtSearch.getText())) {
                rows.add(i);
                String status = i.getCurrentStock() <= 0 ? "OUT OF STOCK" : (i.isLowStock() ? "LOW STOCK" : "OK");
                model.addRow(new Object[]{i.getItemCode(), i.getBarcode() == null ? "" : i.getBarcode(), i.getItemName(),
                        i.getCategoryName(), i.getBaseUnitName(), i.getCurrentStock(), i.getReorderLevel(),
                        UiKit.money(i.getRetailPrice()), status});
            }
            loading = false;
            for (int r = 0; r < rows.size(); r++) {
                if (rows.get(r).getItemId() == keepId) {
                    int v = table.convertRowIndexToView(r);
                    table.setRowSelectionInterval(v, v);
                }
            }
        } catch (SQLException e) {
            loading = false;
            UiKit.error(this, "Could not load items: " + e.getMessage());
        }
    }

    private void onRowSelected() {
        int v = table.getSelectedRow();
        if (v < 0) return;
        Item i = rows.get(table.convertRowIndexToModel(v));
        selected = i;
        txtCode.setText(i.getItemCode());
        txtBarcode.setText(i.getBarcode() == null ? "" : i.getBarcode());
        txtName.setText(i.getItemName());
        for (int k = 0; k < cmbCategory.getItemCount(); k++) {
            if (cmbCategory.getItemAt(k).getCategoryId() == i.getCategoryId()) cmbCategory.setSelectedIndex(k);
        }
        for (int k = 0; k < cmbUnit.getItemCount(); k++) {
            if (cmbUnit.getItemAt(k).getUnitId() == i.getBaseUnitId()) cmbUnit.setSelectedIndex(k);
        }
        spnReorder.setValue(i.getReorderLevel());
        spnRetail.setValue(i.getRetailPrice());
        spnWholesale.setValue(i.getWholesalePrice());
        lblMode.setText("Editing: " + i.getItemCode());
        try {
            boolean locked = itemService.hasStockHistory(i.getItemId());
            cmbUnit.setEnabled(canEdit && !locked);
            cmbUnit.setToolTipText(locked ? "Base unit is locked because this item already has stock records" : null);
        } catch (SQLException ignored) {
        }
        updateButtons();
    }

    private void updateButtons() {
        btnSave.setEnabled(canEdit && selected == null);
        btnUpdate.setEnabled(canEdit && selected != null);
        btnDelete.setEnabled(canEdit && selected != null);
    }

    private void clearForm() {
        selected = null;
        table.clearSelection();
        txtBarcode.setText("");
        txtName.setText("");
        spnReorder.setValue(10);
        spnRetail.setValue(0.0);
        spnWholesale.setValue(0.0);
        lblMode.setText("New item");
        cmbUnit.setEnabled(canEdit);
        cmbUnit.setToolTipText(null);
        if (canEdit) autoCode(); else txtCode.setText("");
        updateButtons();
        txtName.requestFocus();
    }

    private void autoCode() {
        try {
            txtCode.setText(itemService.nextItemCode());
        } catch (SQLException e) {
            UiKit.error(this, "Could not create a code: " + e.getMessage());
        }
    }

    private Item readForm() {
        Item i = new Item();
        i.setItemId(selected == null ? 0 : selected.getItemId());
        i.setItemCode(txtCode.getText());
        i.setBarcode(txtBarcode.getText());
        i.setItemName(txtName.getText());
        Category c = (Category) cmbCategory.getSelectedItem();
        Unit u = (Unit) cmbUnit.getSelectedItem();
        i.setCategoryId(c == null ? 0 : c.getCategoryId());
        i.setBaseUnitId(u == null ? 0 : u.getUnitId());
        i.setReorderLevel(UiKit.intOf(spnReorder));
        i.setRetailPrice(UiKit.doubleOf(spnRetail));
        i.setWholesalePrice(UiKit.doubleOf(spnWholesale));
        return i;
    }

    private void saveNew() {
        try {
            itemService.addItem(readForm());
            UiKit.info(this, "Item added. Use \"Stock Entry & Expiry\" to receive stock for it.");
            clearForm();
            loadTable();
        } catch (ValidationException e) {
            UiKit.warn(this, e.getMessage());
        } catch (SQLException e) {
            UiKit.error(this, "Database error: " + e.getMessage());
        }
    }

    private void updateSelected() {
        if (selected == null) return;
        try {
            itemService.updateItem(readForm());
            UiKit.info(this, "Item updated.");
            loadTable();
        } catch (ValidationException e) {
            UiKit.warn(this, e.getMessage());
        } catch (SQLException e) {
            UiKit.error(this, "Database error: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        if (selected == null || !UiKit.confirm(this, "Delete item \"" + selected.getItemName() + "\"?")) return;
        try {
            itemService.deleteItem(selected);
            clearForm();
            loadTable();
        } catch (ValidationException e) {
            UiKit.warn(this, e.getMessage());
        } catch (SQLException e) {
            UiKit.error(this, "Database error: " + e.getMessage());
        }
    }
}
