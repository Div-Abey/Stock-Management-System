package com.stockmanagement.inventory.service;

import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.inventory.dao.CategoryDAO;
import com.stockmanagement.inventory.dao.ItemDAO;
import com.stockmanagement.inventory.model.Category;
import com.stockmanagement.inventory.model.Item;

import java.sql.SQLException;
import java.util.List;

/** Rules for item master data (add / edit / delete) and categories. */
public class ItemService {

    private final ItemDAO itemDAO = new ItemDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // ------------------------------------------------------------- items

    public List<Item> search(String text) throws SQLException {
        return itemDAO.findAllItems(text);
    }

    public Item findById(int id) throws SQLException {
        return itemDAO.findById(id);
    }

    /** Exact match on item code or scanned barcode. */
    public Item findByCodeOrBarcode(String text) throws SQLException {
        return itemDAO.findByCodeOrBarcode(text.trim());
    }

    public String nextItemCode() throws SQLException {
        return itemDAO.nextItemCode();
    }

    public boolean hasStockHistory(int itemId) throws SQLException {
        return itemDAO.hasStockHistory(itemId);
    }

    public int addItem(Item item) throws ValidationException, SQLException {
        validate(item);
        return itemDAO.insertItem(item);
    }

    public void updateItem(Item item) throws ValidationException, SQLException {
        validate(item);
        Item old = itemDAO.findById(item.getItemId());
        if (old != null && old.getBaseUnitId() != item.getBaseUnitId() && itemDAO.hasStockHistory(item.getItemId())) {
            throw new ValidationException("The base unit cannot be changed because this item already has stock records.");
        }
        itemDAO.updateItem(item);
    }

    public void deleteItem(Item item) throws ValidationException, SQLException {
        if (itemDAO.hasStockHistory(item.getItemId())) {
            throw new ValidationException("\"" + item.getItemName() + "\" has stock or sales history and cannot be deleted.\n"
                    + "Set its re-order level / stop buying it instead.");
        }
        try {
            itemDAO.deleteItem(item.getItemId());
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toUpperCase().contains("FOREIGN KEY")) {
                throw new ValidationException("This item is used in purchase orders or other records and cannot be deleted.");
            }
            throw e;
        }
    }

    private void validate(Item item) throws ValidationException, SQLException {
        if (item.getItemCode() == null || item.getItemCode().isBlank()) throw new ValidationException("Item code is required.");
        if (item.getItemName() == null || item.getItemName().isBlank()) throw new ValidationException("Item name is required.");
        if (item.getCategoryId() <= 0) throw new ValidationException("Select a category (add one under Categories if the list is empty).");
        if (item.getBaseUnitId() <= 0) throw new ValidationException("Select a base unit (add one under Units & Conversions if the list is empty).");
        if (item.getReorderLevel() < 0) throw new ValidationException("Re-order level cannot be negative.");
        if (item.getRetailPrice() < 0 || item.getWholesalePrice() < 0) throw new ValidationException("Prices cannot be negative.");

        item.setItemCode(item.getItemCode().trim());
        item.setItemName(item.getItemName().trim());
        String bc = item.getBarcode() == null ? "" : item.getBarcode().trim();
        if (bc.matches(".*\\s.*")) throw new ValidationException("The barcode cannot contain spaces.");
        item.setBarcode(bc.isEmpty() ? null : bc);

        if (itemDAO.codeExists(item.getItemCode(), item.getItemId())) {
            throw new ValidationException("Item code \"" + item.getItemCode() + "\" is already used.");
        }
        if (item.getBarcode() != null && itemDAO.barcodeExists(item.getBarcode(), item.getItemId())) {
            throw new ValidationException("Barcode \"" + item.getBarcode() + "\" is already assigned to another item.");
        }
    }

    // -------------------------------------------------------- categories

    public List<Category> getCategories() throws SQLException {
        return categoryDAO.findAll();
    }

    public Category addCategory(String name) throws ValidationException, SQLException {
        if (name == null || name.isBlank()) throw new ValidationException("Category name is required.");
        name = name.trim();
        if (categoryDAO.nameExists(name, -1)) throw new ValidationException("Category \"" + name + "\" already exists.");
        return new Category(categoryDAO.insert(name), name);
    }

    public void renameCategory(Category c, String newName) throws ValidationException, SQLException {
        if (newName == null || newName.isBlank()) throw new ValidationException("Category name is required.");
        newName = newName.trim();
        if (categoryDAO.nameExists(newName, c.getCategoryId())) throw new ValidationException("Category \"" + newName + "\" already exists.");
        categoryDAO.update(c.getCategoryId(), newName);
    }

    public void deleteCategory(Category c) throws ValidationException, SQLException {
        int used = categoryDAO.countItems(c.getCategoryId());
        if (used > 0) throw new ValidationException("Category \"" + c + "\" is used by " + used + " item(s) and cannot be deleted.");
        categoryDAO.delete(c.getCategoryId());
    }
}
