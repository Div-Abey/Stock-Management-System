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

            // ---- Inventory module ----
            // Item-specific unit conversions (1 Box of juice = 12, 1 Box of biscuits = 24). NULL = default rule for all items.
            addColumnIfMissing(conn, "unit_conversions", "item_id", "INTEGER REFERENCES items(item_id)");

            try (Statement st = conn.createStatement()) {
                // Stock history: every stock in / sale / adjustment / write-off
                st.executeUpdate("CREATE TABLE IF NOT EXISTS stock_movements (" +
                        "movement_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "item_id INTEGER NOT NULL, " +
                        "batch_id INTEGER, " +
                        "movement_type TEXT NOT NULL, " +
                        "quantity_change INTEGER NOT NULL, " +
                        "reference TEXT, " +
                        "note TEXT, " +
                        "created_by INTEGER, " +
                        "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (item_id) REFERENCES items(item_id), " +
                        "FOREIGN KEY (batch_id) REFERENCES batches(batch_id))");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_movements_item ON stock_movements(item_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_batches_item_expiry ON batches(item_id, expiry_date)");
            }
            // These two can fail on old data that already contains duplicates -> skipped quietly, the services also check.
            tryExecute(conn, "CREATE UNIQUE INDEX IF NOT EXISTS ux_batches_item_number ON batches(item_id, batch_number)");
            tryExecute(conn, "CREATE UNIQUE INDEX IF NOT EXISTS ux_unit_conv ON unit_conversions(COALESCE(item_id, 0), from_unit_id, to_unit_id)");

            // Main.java used to insert "Beverages" on every start -> duplicate rows.
            // Only removes duplicates that no item uses, so it is safe.
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

    private static void tryExecute(Connection conn, String sql) {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Migration note (skipped): " + e.getMessage());
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