package com.stockmanagement.ui.inventory;

import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.inventory.model.Item;
import com.stockmanagement.inventory.model.Unit;
import com.stockmanagement.inventory.model.UnitConversion;
import com.stockmanagement.inventory.service.ItemService;
import com.stockmanagement.inventory.service.UnitConversionService;
import com.stockmanagement.ui.components.InventoryNavBar;
import com.stockmanagement.ui.components.UiKit;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Units (Carton, Box, Piece, Kg, Gram) on the left, "1 Box = 12 Piece" rules on the right.
 * Shown as a tab inside {@link InventoryPanel}.
 */
public class UnitManagementPanel extends JPanel implements InventoryNavBar.Refreshable {

    private final UnitConversionService service = new UnitConversionService();
    private final ItemService itemService = new ItemService();
    private final List<Unit> units = new ArrayList<>();
    private final List<UnitConversion> conversions = new ArrayList<>();

    private final DefaultTableModel unitModel = UiKit.readOnlyModel("Unit");
    private final DefaultTableModel convModel = UiKit.readOnlyModel("Applies To", "Rule");
    private final JTable unitTable = UiKit.table(unitModel);
    private final JTable convTable = UiKit.table(convModel);
    private final JTextField txtUnit = new JTextField(12);
    private final JComboBox<Item> cmbItem = new JComboBox<>();
    private final JComboBox<Unit> cmbFrom = new JComboBox<>();
    private final JComboBox<Unit> cmbTo = new JComboBox<>();
    private final JSpinner spnRatio = new JSpinner(new SpinnerNumberModel(12.0, 0.0001, 100000000.0, 1.0));
    private final JLabel lblPreview = new JLabel(" ");

    public UnitManagementPanel(String role) {
        super(new GridLayout(1, 2, 12, 0));
        boolean canEdit = InventoryNavBar.canEdit(role);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildUnitsSide(canEdit));
        add(buildConversionSide(canEdit));
        // No data is loaded here: InventoryPanel calls refreshData() once this panel is on screen.
    }

    private JPanel buildUnitsSide(boolean canEdit) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new TitledBorder("Units"));
        p.add(new JScrollPane(unitTable), BorderLayout.CENTER);

        JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton add = new JButton("Add Unit");
        add.addActionListener(e -> addUnit());
        JButton del = new JButton("Delete Selected");
        del.addActionListener(e -> deleteUnit());
        f.add(new JLabel("Name:"));
        f.add(txtUnit);
        f.add(add);
        f.add(del);
        p.add(f, BorderLayout.SOUTH);
        for (JComponent c : new JComponent[]{txtUnit, add, del}) c.setEnabled(canEdit);
        return p;
    }

    private JPanel buildConversionSide(boolean canEdit) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new TitledBorder("Conversions (bulk unit -> retail unit)"));
        p.add(new JScrollPane(convTable), BorderLayout.CENTER);

        JPanel f = new JPanel(new GridBagLayout());
        spnRatio.setPreferredSize(new Dimension(110, 28));
        cmbFrom.addActionListener(e -> updatePreview());
        cmbTo.addActionListener(e -> updatePreview());
        spnRatio.addChangeListener(e -> updatePreview());
        UiKit.addRow(f, 0, "Applies to:", cmbItem);
        UiKit.addRow(f, 1, "1 unit of:", cmbFrom);
        UiKit.addRow(f, 2, "equals (in):", cmbTo);
        UiKit.addRow(f, 3, "How many:", spnRatio);
        UiKit.addRow(f, 4, "", lblPreview);

        JButton add = new JButton("Add Conversion");
        add.addActionListener(e -> addConversion());
        JButton del = new JButton("Delete Selected");
        del.addActionListener(e -> deleteConversion());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.add(add);
        btns.add(del);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 5; c.gridx = 0; c.gridwidth = 2; c.anchor = GridBagConstraints.WEST;
        f.add(btns, c);
        p.add(f, BorderLayout.SOUTH);
        for (JComponent x : new JComponent[]{cmbItem, cmbFrom, cmbTo, spnRatio, add, del}) x.setEnabled(canEdit);
        return p;
    }

    @Override
    public void refreshData() {
        try {
            units.clear();
            unitModel.setRowCount(0);
            for (Unit u : service.getUnits()) {
                units.add(u);
                unitModel.addRow(new Object[]{u.getUnitName()});
            }
            conversions.clear();
            convModel.setRowCount(0);
            for (UnitConversion c : service.getConversions()) {
                conversions.add(c);
                convModel.addRow(new Object[]{c.getItemId() == null ? "All items" : c.getItemName(), c.describe()});
            }
            Unit f = (Unit) cmbFrom.getSelectedItem(), t = (Unit) cmbTo.getSelectedItem();
            cmbFrom.removeAllItems();
            cmbTo.removeAllItems();
            for (Unit u : units) {
                cmbFrom.addItem(u);
                cmbTo.addItem(u);
                if (f != null && f.getUnitId() == u.getUnitId()) cmbFrom.setSelectedItem(u);
                if (t != null && t.getUnitId() == u.getUnitId()) cmbTo.setSelectedItem(u);
            }
            Item sel = (Item) cmbItem.getSelectedItem();
            cmbItem.removeAllItems();
            cmbItem.addItem(Item.allItems("All items"));
            for (Item i : itemService.search(null)) {
                cmbItem.addItem(i);
                if (sel != null && sel.getItemId() == i.getItemId()) cmbItem.setSelectedItem(i);
            }
            updatePreview();
        } catch (SQLException e) {
            UiKit.error(this, "Could not load units: " + e.getMessage());
        }
    }

    private void updatePreview() {
        Unit f = (Unit) cmbFrom.getSelectedItem(), t = (Unit) cmbTo.getSelectedItem();
        lblPreview.setText(f == null || t == null ? " "
                : "1 " + f + " = " + UnitConversion.trim(((Number) spnRatio.getValue()).doubleValue()) + " " + t);
    }

    private void addUnit() {
        try {
            service.addUnit(txtUnit.getText(), false);
            txtUnit.setText("");
            refreshData();
        } catch (ValidationException e) {
            UiKit.warn(this, e.getMessage());
        } catch (SQLException e) {
            UiKit.error(this, e.getMessage());
        }
    }

    private void deleteUnit() {
        int v = unitTable.getSelectedRow();
        if (v < 0) { UiKit.warn(this, "Select a unit first."); return; }
        Unit u = units.get(unitTable.convertRowIndexToModel(v));
        if (!UiKit.confirm(this, "Delete unit \"" + u + "\"?")) return;
        try {
            service.deleteUnit(u);
            refreshData();
        } catch (ValidationException e) {
            UiKit.warn(this, e.getMessage());
        } catch (SQLException e) {
            UiKit.error(this, e.getMessage());
        }
    }

    private void addConversion() {
        try {
            Item item = (Item) cmbItem.getSelectedItem();
            service.addConversion(item == null || item.getItemId() == 0 ? null : item.getItemId(),
                    (Unit) cmbFrom.getSelectedItem(), (Unit) cmbTo.getSelectedItem(),
                    ((Number) spnRatio.getValue()).doubleValue());
            refreshData();
        } catch (ValidationException e) {
            UiKit.warn(this, e.getMessage());
        } catch (SQLException e) {
            UiKit.error(this, e.getMessage());
        }
    }

    private void deleteConversion() {
        int v = convTable.getSelectedRow();
        if (v < 0) { UiKit.warn(this, "Select a rule first."); return; }
        try {
            service.deleteConversion(conversions.get(convTable.convertRowIndexToModel(v)));
            refreshData();
        } catch (SQLException e) {
            UiKit.error(this, e.getMessage());
        }
    }
}
