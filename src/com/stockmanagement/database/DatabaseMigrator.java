package com.stockmanagement.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Upgrades an EXISTING stock_management.db without deleting it.
 * Safe to run on every start-up (each step checks first).
 */
public class DatabaseMigrator {

    public static void migrate() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Sales module
            addColumnIfMissing(conn, "invoices", "amount_paid", "NUMERIC NOT NULL DEFAULT 0.00");
            addColumnIfMissing(conn, "invoices", "due_date", "TEXT");

            // Section 3 (CRM & supplier)
            addColumnIfMissing(conn, "purchase_orders", "sent_date", "TEXT");
            addColumnIfMissing(conn, "po_items", "quantity_received", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(conn, "grn", "paid_amount", "NUMERIC NOT NULL DEFAULT 0.00");
            addColumnIfMissing(conn, "grn", "due_date", "TEXT");

            // supplier_payments is new in section 3; older databases don't have it
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS supplier_payments (" +
                        "  payment_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "  supplier_id INTEGER NOT NULL," +
                        "  grn_id INTEGER NOT NULL," +
                        "  payment_date TEXT NOT NULL," +
                        "  amount_paid NUMERIC NOT NULL," +
                        "  payment_method TEXT NOT NULL CHECK(payment_method IN ('CASH', 'CHEQUE', 'BANK_TRANSFER'))," +
                        "  reference TEXT," +
                        "  FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)," +
                        "  FOREIGN KEY (grn_id) REFERENCES grn(grn_id))");

                // remove duplicate categories that no item uses
                st.executeUpdate("DELETE FROM categories " +
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