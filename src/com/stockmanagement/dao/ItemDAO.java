package com.stockmanagement.dao;

import com.stockmanagement.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemDAO {

    public boolean addCategory(String categoryName) {
        String sql = "INSERT INTO categories(category_name) VALUES(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding category: " + e.getMessage());
            return false;
        }
    }

    public boolean addUnit(String unitName, boolean isBaseUnit) {
        String sql = "INSERT INTO units(unit_name, is_base_unit) VALUES(?, ?)";
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
        String sql = "INSERT INTO items(item_code, barcode, item_name, category_id, base_unit_id, " +
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