package com.stockmanagement.ui.inventory;

import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.inventory.model.Category;
import com.stockmanagement.inventory.service.ItemService;
import com.stockmanagement.ui.components.InventoryNavBar;
import com.stockmanagement.ui.components.UiKit;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Item categories: add, rename, delete. Shown as a tab inside {@link InventoryPanel}. */
public class CategoryPanel extends JPanel implements InventoryNavBar.Refreshable {

    private final ItemService service = new ItemService();
    private final List<Category> rows = new ArrayList<>();
    private final DefaultTableModel model = UiKit.readOnlyModel("ID", "Category Name");
    private final JTable table = UiKit.table(model);
    private final JTextField txtName = new JTextField(20);

    public CategoryPanel(String role) {
        super(new BorderLayout(10, 10));
        boolean canEdit = InventoryNavBar.canEdit(role);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new TitledBorder("Categories"));
        add(sp, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            int v = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && v >= 0) txtName.setText(rows.get(table.convertRowIndexToModel(v)).getCategoryName());
        });

        JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton add = new JButton("Add");
        add.addActionListener(e -> run(() -> { service.addCategory(txtName.getText()); txtName.setText(""); }));
        JButton ren = new JButton("Rename Selected");
        ren.addActionListener(e -> run(() -> service.renameCategory(selected(), txtName.getText())));
        JButton del = new JButton("Delete Selected");
        del.addActionListener(e -> run(() -> {
            Category c = selected();
            if (UiKit.confirm(this, "Delete category \"" + c + "\"?")) { service.deleteCategory(c); txtName.setText(""); }
        }));
        f.add(new JLabel("Name:"));
        f.add(txtName);
        f.add(add);
        f.add(ren);
        f.add(del);
        for (JComponent c : new JComponent[]{txtName, add, ren, del}) c.setEnabled(canEdit);
        add(f, BorderLayout.SOUTH);
        // No data is loaded here: InventoryPanel calls refreshData() once this panel is on screen.
    }

    private interface Action {
        void run() throws ValidationException, SQLException;
    }

    private void run(Action a) {
        try {
            a.run();
            refreshData();
        } catch (ValidationException e) {
            UiKit.warn(this, e.getMessage());
        } catch (SQLException e) {
            UiKit.error(this, "Database error: " + e.getMessage());
        }
    }

    private Category selected() throws ValidationException {
        int v = table.getSelectedRow();
        if (v < 0) throw new ValidationException("Select a category first.");
        return rows.get(table.convertRowIndexToModel(v));
    }

    @Override
    public void refreshData() {
        try {
            rows.clear();
            model.setRowCount(0);
            for (Category c : service.getCategories()) {
                rows.add(c);
                model.addRow(new Object[]{c.getCategoryId(), c.getCategoryName()});
            }
        } catch (SQLException e) {
            UiKit.error(this, "Could not load categories: " + e.getMessage());
        }
    }
}
