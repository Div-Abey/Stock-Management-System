package com.stockmanagement.inventory.dao;

import com.stockmanagement.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // ------------------------------------------------------------------
    // Light-weight read models used by the POS screen (ui/sales/POSFrame)
    // ------------------------------------------------------------------

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

    /** Item + how many units can be sold right now (non-expired batches only). */
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

    // SELECT shared by the two stock lookups below. Expired batches are NOT sellable.
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

    /** Matches item_code OR barcode. Returns null if no such item (stock may be 0). */
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
}
