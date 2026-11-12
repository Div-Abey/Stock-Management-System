package com.stockmanagement;

import com.stockmanagement.config.DatabaseConnection;
import com.stockmanagement.config.DatabaseInitializer;
import com.stockmanagement.dao.ItemDAO;
import com.stockmanagement.ui.auth.LoginFrame;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        // Step 1: Initialize Database Tables
        DatabaseInitializer.initializeDatabase();

        // Step 2: Seed Initial Master Data
        seedRoles();

        ItemDAO itemDAO = new ItemDAO();
        itemDAO.addCategory("Beverages");
        itemDAO.addUnit("Piece", true);
        itemDAO.addUnit("Box", false);

        itemDAO.addItem("ITM-001", "8901234567890", "Orange Juice 1L", 1, 1, 15, 350.00, 300.00);

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
}