package com.stockmanagement.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Upgrades an EXISTING stock_management.db without deleting it.
 * Safe to run on every start-up (each step checks first).
 * Call it right after DatabaseInitializer.initializeDatabase().
 */
public class DatabaseMigrator {

    public static void migrate() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Sales module needs to remember how much was paid and the credit due date
            addColumnIfMissing(conn, "invoices", "amount_paid", "NUMERIC NOT NULL DEFAULT 0.00");
            addColumnIfMissing(conn, "invoices", "due_date", "TEXT");

            // Main.java used to insert "Beverages" on every start -> duplicate rows.
            // Only removes duplicates that no item uses, so it is safe.
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                        "DELETE FROM categories " +
                                "WHERE category_id NOT IN (SELECT MIN(category_id) FROM categories GROUP BY category_name) " +
                                "AND category_id NOT IN (SELECT category_id FROM items)");
            }
            System.out.println("Database migration check completed.");

        } catch (SQLException e) {
            System.err.println("Database migration error: " + e.getMessage());
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition) throws SQLException {
        if (columnExists(conn, table, column)) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            System.out.println("Migration: added " + table + "." + column);
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
