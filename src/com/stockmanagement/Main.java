package com.stockmanagement;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.database.DatabaseInitializer;
import com.stockmanagement.database.DatabaseMigrator;
import com.stockmanagement.inventory.dao.ItemDAO;
import com.stockmanagement.security.dao.UserDAO;
import com.stockmanagement.ui.auth.LoginFrame;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        // Step 1: Initialize Database Tables, then upgrade an existing database (adds invoices.amount_paid / due_date)
        DatabaseInitializer.initializeDatabase();
        DatabaseMigrator.migrate();

        // Step 2: Seed Initial Master Data
        seedRoles();
        seedDefaultUsers();

        ItemDAO itemDAO = new ItemDAO();
        itemDAO.addCategory("Beverages");
        itemDAO.addUnit("Piece", true);
        itemDAO.addUnit("Box", false);

        itemDAO.addItem("ITM-001", "8901234567890", "Orange Juice 1L", 1, 1, 15, 350.00, 300.00);
        seedDemoStock();

        // Step 3: Check Stock Alerts
        itemDAO.listLowStockItems();

        // Step 4: Launch UI on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }

    private static void seedRoles() {
        String[] roles = {"Admin", "Manager", "Store Keeper", "Cashier"};
        String sql = "INSERT OR IGNORE INTO roles(role_name) VALUES(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (String role : roles) {
                pstmt.setString(1, role);
                pstmt.executeUpdate();
            }
            System.out.println("Default user roles verified.");

        } catch (SQLException e) {
            System.err.println("Error seeding roles: " + e.getMessage());
        }
    }

    /**
     * Every invoice stores a cashier (users.user_id), so one user per role must exist.
     * Passwords match the fixed ones LoginFrame checks for now.
     */
    private static void seedDefaultUsers() {
        UserDAO userDAO = new UserDAO();
        try {
            userDAO.insertIfMissing("admin", "admin123", "Admin", "System Admin");
            userDAO.insertIfMissing("manager", "manager123", "Manager", "Store Manager");
            userDAO.insertIfMissing("storekeeper", "storekeeper123", "Store Keeper", "Store Keeper");
            userDAO.insertIfMissing("cashier", "cashier123", "Cashier", "Cashier");
            System.out.println("Default users verified.");
        } catch (SQLException e) {
            System.err.println("Error seeding users: " + e.getMessage());
        }
    }

    /**
     * TEMPORARY: gives ITM-001 some stock so the POS has something to sell while the GRN / purchase
     * screens are not built. Only inserts when NO batch exists for the item. Delete this method later.
     */
    private static void seedDemoStock() {
        String sql = "INSERT INTO batches(item_id, batch_number, expiry_date, quantity_received, quantity_remaining, purchase_price) " +
                "SELECT i.item_id, 'DEMO-001', date('now','+1 year'), 100, 100, 300.00 " +
                "FROM items i WHERE i.item_code = 'ITM-001' " +
                "AND NOT EXISTS (SELECT 1 FROM batches b WHERE b.item_id = i.item_id)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (pstmt.executeUpdate() > 0) {
                System.out.println("Demo stock added for ITM-001 (100 units).");
            }
        } catch (SQLException e) {
            System.err.println("Error seeding demo stock: " + e.getMessage());
        }
    }
}
