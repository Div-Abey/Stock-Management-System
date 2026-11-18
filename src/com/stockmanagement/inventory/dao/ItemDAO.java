package com.stockmanagement.inventory.dao;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.inventory.model.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {


    // Light-weight read models used by the POS screen (ui/sales/POSFrame)

    /** Customer shown in the POS drop-down. customerId == null means the Walk-in customer. */
    public static class CustomerSummary {
        private final Integer customerId;
        private final String customerName;
        private final String customerType;      // RETAIL, WHOLESALE, SPECIAL
        private final double creditLimit;
        private final double currentBalance;

        public CustomerSummary(Integer customerId, String customerName, String customerType,
                               double creditLimit, double currentBalance) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.customerType = customerType;
            this.creditLimit = creditLimit;
            this.currentBalance = currentBalance;
        }

        public static CustomerSummary walkIn() {
            return new CustomerSummary(null, "Walk-in Customer", "RETAIL", 0, 0);
        }

        public Integer getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getCustomerType() { return customerType; }
        public double getCreditLimit() { return creditLimit; }
        public double getCurrentBalance() { return currentBalance; }

        @Override
        public String toString() {
            return customerId == null ? customerName : customerName + " (" + customerType + ")";
        }
    }
    // Holds item details along with sellable (non-expired) stock count
    public static class ItemStockSummary {
        private final int itemId;
        private final String itemCode;
        private final String barcode;
        private final String itemName;
        private final int baseUnitId;
        private final String baseUnitName;
        private final double retailPrice;
        private final double wholesalePrice;
        private final int availableStock;

        public ItemStockSummary(int itemId, String itemCode, String barcode, String itemName,
                                int baseUnitId, String baseUnitName,
                                double retailPrice, double wholesalePrice, int availableStock) {
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.barcode = barcode;
            this.itemName = itemName;
            this.baseUnitId = baseUnitId;
            this.baseUnitName = baseUnitName;
            this.retailPrice = retailPrice;
            this.wholesalePrice = wholesalePrice;
            this.availableStock = availableStock;
        }

        public int getItemId() { return itemId; }
        public String getItemCode() { return itemCode; }
        public String getBarcode() { return barcode; }
        public String getItemName() { return itemName; }
        public int getBaseUnitId() { return baseUnitId; }
        public String getBaseUnitName() { return baseUnitName; }
        public double getRetailPrice() { return retailPrice; }
        public double getWholesalePrice() { return wholesalePrice; }
        public int getAvailableStock() { return availableStock; }

        @Override
        public String toString() {
            return String.format("%s - %s  [Stock: %d %s]  Rs. %.2f",
                    itemCode, itemName, availableStock, baseUnitName, retailPrice);
        }
    }

    // Base query to fetch item details and sum only non-expired batch quantities
    private static final String STOCK_SELECT =
            "SELECT i.item_id, i.item_code, i.barcode, i.item_name, i.base_unit_id, u.unit_name, " +
                    "       i.retail_price, i.wholesale_price, " +
                    "       COALESCE((SELECT SUM(b.quantity_remaining) FROM batches b " +
                    "                  WHERE b.item_id = i.item_id AND b.quantity_remaining > 0 " +
                    "                    AND b.expiry_date >= date('now','localtime')), 0) AS available_stock " +
                    "FROM items i JOIN units u ON u.unit_id = i.base_unit_id ";

    public List<ItemStockSummary> findAvailableStockItems() throws SQLException {
        String sql = "SELECT * FROM (" + STOCK_SELECT + ") WHERE available_stock > 0 ORDER BY item_name";
        List<ItemStockSummary> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapStock(rs));
            }
        }
        return list;
    }
    // Finds stock summary by exact item code or barcode
    public ItemStockSummary findStockByCodeOrBarcode(String codeOrBarcode) throws SQLException {
        String sql = STOCK_SELECT + "WHERE i.item_code = ? OR i.barcode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codeOrBarcode);
            pstmt.setString(2, codeOrBarcode);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapStock(rs) : null;
            }
        }
    }

    /** Walk-in customer first, then all registered customers. */
    public List<CustomerSummary> findAllCustomerSummaries() throws SQLException {
        String sql = "SELECT customer_id, customer_name, customer_type, credit_limit, current_balance " +
                "FROM customers ORDER BY customer_name";
        List<CustomerSummary> list = new ArrayList<>();
        list.add(CustomerSummary.walkIn());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new CustomerSummary(
                        rs.getInt("customer_id"),
                        rs.getString("customer_name"),
                        rs.getString("customer_type"),
                        rs.getDouble("credit_limit"),
                        rs.getDouble("current_balance")));
            }
        }
        return list;
    }

    private ItemStockSummary mapStock(ResultSet rs) throws SQLException {
        return new ItemStockSummary(
                rs.getInt("item_id"),
                rs.getString("item_code"),
                rs.getString("barcode"),
                rs.getString("item_name"),
                rs.getInt("base_unit_id"),
                rs.getString("unit_name"),
                rs.getDouble("retail_price"),
                rs.getDouble("wholesale_price"),
                rs.getInt("available_stock"));
    }

    // ------------------------------------------------------------------
    // Your existing methods (unchanged, except addCategory now skips duplicates)
    // ------------------------------------------------------------------

    public boolean addCategory(String categoryName) {
        String exists = "SELECT 1 FROM categories WHERE category_name = ?";
        String sql = "INSERT INTO categories(category_name) VALUES(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement check = conn.prepareStatement(exists)) {
            check.setString(1, categoryName);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return false;   // already there
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, categoryName);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error adding category: " + e.getMessage());
            return false;
        }
    }

    public boolean addUnit(String unitName, boolean isBaseUnit) {
        String sql = "INSERT OR IGNORE INTO units(unit_name, is_base_unit) VALUES(?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, unitName);
            pstmt.setInt(2, isBaseUnit ? 1 : 0);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding unit: " + e.getMessage());
            return false;
        }
    }

    public boolean addItem(String itemCode, String barcode, String itemName,
                           int categoryId, int baseUnitId, int reorderLevel,
                           double retailPrice, double wholesalePrice) {
        String sql = "INSERT OR IGNORE INTO items(item_code, barcode, item_name, category_id, base_unit_id, " +
                "reorder_level, retail_price, wholesale_price) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemCode);
            pstmt.setString(2, barcode);
            pstmt.setString(3, itemName);
            pstmt.setInt(4, categoryId);
            pstmt.setInt(5, baseUnitId);
            pstmt.setInt(6, reorderLevel);
            pstmt.setDouble(7, retailPrice);
            pstmt.setDouble(8, wholesalePrice);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding item: " + e.getMessage());
            return false;
        }
    }

    public void listLowStockItems() {
        String sql = "SELECT i.item_name, i.reorder_level, COALESCE(SUM(b.quantity_remaining), 0) AS current_stock " +
                "FROM items i " +
                "LEFT JOIN batches b ON i.item_id = b.item_id " +
                "GROUP BY i.item_id " +
                "HAVING current_stock <= i.reorder_level";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("--- LOW STOCK ALERTS ---");
            while (rs.next()) {
                System.out.printf("Item: %s | Reorder Threshold: %d | Current Stock: %d%n",
                        rs.getString("item_name"),
                        rs.getInt("reorder_level"),
                        rs.getInt("current_stock"));
            }
        } catch (SQLException e) {
            System.err.println("Error listing low stock items: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Inventory screens: item master data (CRUD) + stock-aware lookups
    // ------------------------------------------------------------------

    // current_stock counts only NON-expired batches, same rule the POS uses.
    private static final String ITEM_SELECT =
            "SELECT i.item_id, i.item_code, i.barcode, i.item_name, i.category_id, c.category_name, " +
                    "       i.base_unit_id, u.unit_name, i.reorder_level, i.retail_price, i.wholesale_price, " +
                    "       COALESCE((SELECT SUM(b.quantity_remaining) FROM batches b " +
                    "                  WHERE b.item_id = i.item_id AND b.quantity_remaining > 0 " +
                    "                    AND b.expiry_date >= date('now','localtime')), 0) AS current_stock " +
                    "FROM items i " +
                    "JOIN categories c ON c.category_id = i.category_id " +
                    "JOIN units u ON u.unit_id = i.base_unit_id ";
    // Fetches all items, optionally filtered by code, name, or barcode
    public List<Item> findAllItems(String search) throws SQLException {
        boolean filter = search != null && !search.isBlank();
        String sql = ITEM_SELECT +
                (filter ? "WHERE i.item_code LIKE ? OR i.item_name LIKE ? OR i.barcode LIKE ? " : "") +
                "ORDER BY i.item_name";
        List<Item> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (filter) {
                String like = "%" + search.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapItem(rs));
                }
            }
        }
        return list;
    }

    public Item findById(int itemId) throws SQLException {
        String sql = ITEM_SELECT + "WHERE i.item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapItem(rs) : null;
            }
        }
    }
    // Finds an item by exact item code or barcode (used for barcode scanners)
    public Item findByCodeOrBarcode(String codeOrBarcode) throws SQLException {
        String sql = ITEM_SELECT + "WHERE i.item_code = ? OR i.barcode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codeOrBarcode);
            ps.setString(2, codeOrBarcode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapItem(rs) : null;
            }
        }
    }

    // Fetches items with stock at or below reorder level, sorted lowest first
    public List<Item> findLowStockItems() throws SQLException {
        String sql = "SELECT * FROM (" + ITEM_SELECT + ") WHERE current_stock <= reorder_level " +
                "ORDER BY current_stock ASC, item_name";
        List<Item> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapItem(rs));
            }
        }
        return list;
    }

    public boolean codeExists(String itemCode, int excludeItemId) throws SQLException {
        return exists("SELECT 1 FROM items WHERE lower(item_code) = lower(?) AND item_id <> ?", itemCode, excludeItemId);
    }

    public boolean barcodeExists(String barcode, int excludeItemId) throws SQLException {
        return exists("SELECT 1 FROM items WHERE barcode = ? AND item_id <> ?", barcode, excludeItemId);
    }

    private boolean exists(String sql, String value, int excludeItemId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, excludeItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Inserts a new Item object and returns its generated ID
    public int insertItem(Item item) throws SQLException {
        String sql = "INSERT INTO items(item_code, barcode, item_name, category_id, base_unit_id, " +
                "reorder_level, retail_price, wholesale_price) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindItem(ps, item);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public void updateItem(Item item) throws SQLException {
        String sql = "UPDATE items SET item_code = ?, barcode = ?, item_name = ?, category_id = ?, base_unit_id = ?, " +
                "reorder_level = ?, retail_price = ?, wholesale_price = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindItem(ps, item);
            ps.setInt(9, item.getItemId());
            ps.executeUpdate();
        }
    }

    private void bindItem(PreparedStatement ps, Item item) throws SQLException {
        ps.setString(1, item.getItemCode());
        if (item.getBarcode() == null || item.getBarcode().isBlank()) {
            ps.setNull(2, Types.VARCHAR);
        } else {
            ps.setString(2, item.getBarcode());
        }
        ps.setString(3, item.getItemName());
        ps.setInt(4, item.getCategoryId());
        ps.setInt(5, item.getBaseUnitId());
        ps.setInt(6, item.getReorderLevel());
        ps.setDouble(7, item.getRetailPrice());
        ps.setDouble(8, item.getWholesalePrice());
    }

    // Checks if an item has any batches, stock movements, or sales history
    public boolean hasStockHistory(int itemId) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM batches WHERE item_id = ?) " +
                "+ (SELECT COUNT(*) FROM stock_movements WHERE item_id = ?) " +
                "+ (SELECT COUNT(*) FROM invoice_items WHERE item_id = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, itemId);
            ps.setInt(3, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

// Safely deletes an item and its pricing/unit rules inside a transaction
public void deleteItem(int itemId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String table : new String[]{"bulk_discounts", "customer_pricing", "unit_conversions"}) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE item_id = ?")) {
                        ps.setInt(1, itemId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE item_id = ?")) {
                    ps.setInt(1, itemId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Generates the next sequential item code (e.g., ITM-001, ITM-002)
    public String nextItemCode() throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTR(item_code, 5) AS INTEGER)), 0) FROM items WHERE item_code LIKE 'ITM-%'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int next = rs.next() ? rs.getInt(1) + 1 : 1;
            return String.format("ITM-%03d", next);
        }
    }

    private Item mapItem(ResultSet rs) throws SQLException {
        Item i = new Item();
        i.setItemId(rs.getInt("item_id"));
        i.setItemCode(rs.getString("item_code"));
        i.setBarcode(rs.getString("barcode"));
        i.setItemName(rs.getString("item_name"));
        i.setCategoryId(rs.getInt("category_id"));
        i.setCategoryName(rs.getString("category_name"));
        i.setBaseUnitId(rs.getInt("base_unit_id"));
        i.setBaseUnitName(rs.getString("unit_name"));
        i.setReorderLevel(rs.getInt("reorder_level"));
        i.setRetailPrice(rs.getDouble("retail_price"));
        i.setWholesalePrice(rs.getDouble("wholesale_price"));
        i.setCurrentStock(rs.getInt("current_stock"));
        return i;
    }
}
