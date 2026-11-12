package com.stockmanagement.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private static final String[] SCHEMA_SQL = {
            "CREATE TABLE IF NOT EXISTS roles (" +
                    "  role_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  role_name TEXT NOT NULL UNIQUE" +
                    ");",

            "CREATE TABLE IF NOT EXISTS users (" +
                    "  user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  username TEXT NOT NULL UNIQUE," +
                    "  password_hash TEXT NOT NULL," +
                    "  role_id INTEGER NOT NULL," +
                    "  full_name TEXT NOT NULL," +
                    "  is_active INTEGER DEFAULT 1," +
                    "  created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "  FOREIGN KEY (role_id) REFERENCES roles(role_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS categories (" +
                    "  category_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  category_name TEXT NOT NULL" +
                    ");",

            "CREATE TABLE IF NOT EXISTS units (" +
                    "  unit_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  unit_name TEXT NOT NULL UNIQUE," +
                    "  is_base_unit INTEGER DEFAULT 0" +
                    ");",

            "CREATE TABLE IF NOT EXISTS unit_conversions (" +
                    "  conversion_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  from_unit_id INTEGER NOT NULL," +
                    "  to_unit_id INTEGER NOT NULL," +
                    "  conversion_ratio NUMERIC NOT NULL," +
                    "  FOREIGN KEY (from_unit_id) REFERENCES units(unit_id)," +
                    "  FOREIGN KEY (to_unit_id) REFERENCES units(unit_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS items (" +
                    "  item_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  item_code TEXT NOT NULL UNIQUE," +
                    "  barcode TEXT UNIQUE," +
                    "  item_name TEXT NOT NULL," +
                    "  category_id INTEGER NOT NULL," +
                    "  base_unit_id INTEGER NOT NULL," +
                    "  reorder_level INTEGER NOT NULL DEFAULT 10," +
                    "  retail_price NUMERIC NOT NULL DEFAULT 0.00," +
                    "  wholesale_price NUMERIC NOT NULL DEFAULT 0.00," +
                    "  created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "  FOREIGN KEY (category_id) REFERENCES categories(category_id)," +
                    "  FOREIGN KEY (base_unit_id) REFERENCES units(unit_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS bulk_discounts (" +
                    "  discount_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  item_id INTEGER NOT NULL," +
                    "  min_quantity INTEGER NOT NULL," +
                    "  discount_percentage NUMERIC DEFAULT 0.00," +
                    "  discount_amount NUMERIC DEFAULT 0.00," +
                    "  FOREIGN KEY (item_id) REFERENCES items(item_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS customers (" +
                    "  customer_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  customer_name TEXT NOT NULL," +
                    "  phone TEXT," +
                    "  email TEXT," +
                    "  customer_type TEXT DEFAULT 'RETAIL' CHECK(customer_type IN ('RETAIL', 'WHOLESALE', 'SPECIAL'))," +
                    "  credit_limit NUMERIC NOT NULL DEFAULT 0.00," +
                    "  current_balance NUMERIC NOT NULL DEFAULT 0.00," +
                    "  created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ");",

            "CREATE TABLE IF NOT EXISTS customer_pricing (" +
                    "  pricing_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  customer_id INTEGER NOT NULL," +
                    "  item_id INTEGER NOT NULL," +
                    "  special_price NUMERIC NOT NULL," +
                    "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id)," +
                    "  FOREIGN KEY (item_id) REFERENCES items(item_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS suppliers (" +
                    "  supplier_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  supplier_name TEXT NOT NULL," +
                    "  phone TEXT," +
                    "  email TEXT," +
                    "  address TEXT," +
                    "  current_balance NUMERIC NOT NULL DEFAULT 0.00" +
                    ");",

            "CREATE TABLE IF NOT EXISTS purchase_orders (" +
                    "  po_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  supplier_id INTEGER NOT NULL," +
                    "  order_date TEXT NOT NULL," +
                    "  status TEXT DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'PARTIAL', 'RECEIVED', 'CANCELLED'))," +
                    "  created_by INTEGER NOT NULL," +
                    "  FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)," +
                    "  FOREIGN KEY (created_by) REFERENCES users(user_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS po_items (" +
                    "  po_item_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  po_id INTEGER NOT NULL," +
                    "  item_id INTEGER NOT NULL," +
                    "  quantity INTEGER NOT NULL," +
                    "  unit_id INTEGER NOT NULL," +
                    "  unit_price NUMERIC NOT NULL," +
                    "  FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id)," +
                    "  FOREIGN KEY (item_id) REFERENCES items(item_id)," +
                    "  FOREIGN KEY (unit_id) REFERENCES units(unit_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS grn (" +
                    "  grn_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  po_id INTEGER," +
                    "  supplier_id INTEGER NOT NULL," +
                    "  received_date TEXT NOT NULL," +
                    "  total_amount NUMERIC NOT NULL," +
                    "  received_by INTEGER NOT NULL," +
                    "  FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id)," +
                    "  FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)," +
                    "  FOREIGN KEY (received_by) REFERENCES users(user_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS batches (" +
                    "  batch_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  item_id INTEGER NOT NULL," +
                    "  batch_number TEXT NOT NULL," +
                    "  expiry_date TEXT NOT NULL," +
                    "  quantity_received INTEGER NOT NULL," +
                    "  quantity_remaining INTEGER NOT NULL," +
                    "  purchase_price NUMERIC NOT NULL," +
                    "  created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "  FOREIGN KEY (item_id) REFERENCES items(item_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS grn_items (" +
                    "  grn_item_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  grn_id INTEGER NOT NULL," +
                    "  batch_id INTEGER NOT NULL," +
                    "  item_id INTEGER NOT NULL," +
                    "  quantity_received INTEGER NOT NULL," +
                    "  unit_id INTEGER NOT NULL," +
                    "  purchase_price NUMERIC NOT NULL," +
                    "  FOREIGN KEY (grn_id) REFERENCES grn(grn_id)," +
                    "  FOREIGN KEY (batch_id) REFERENCES batches(batch_id)," +
                    "  FOREIGN KEY (item_id) REFERENCES items(item_id)," +
                    "  FOREIGN KEY (unit_id) REFERENCES units(unit_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS invoices (" +
                    "  invoice_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  invoice_number TEXT NOT NULL UNIQUE," +
                    "  customer_id INTEGER," +
                    "  cashier_id INTEGER NOT NULL," +
                    "  invoice_date TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "  payment_type TEXT NOT NULL CHECK(payment_type IN ('CASH', 'CREDIT', 'CHEQUE', 'CARD'))," +
                    "  total_amount NUMERIC NOT NULL," +
                    "  discount_amount NUMERIC DEFAULT 0.00," +
                    "  net_amount NUMERIC NOT NULL," +
                    "  status TEXT DEFAULT 'PAID' CHECK(status IN ('PAID', 'UNPAID', 'PARTIAL', 'CANCELLED'))," +
                    "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id)," +
                    "  FOREIGN KEY (cashier_id) REFERENCES users(user_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS invoice_items (" +
                    "  invoice_item_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  invoice_id INTEGER NOT NULL," +
                    "  item_id INTEGER NOT NULL," +
                    "  batch_id INTEGER NOT NULL," +
                    "  unit_id INTEGER NOT NULL," +
                    "  quantity INTEGER NOT NULL," +
                    "  unit_price NUMERIC NOT NULL," +
                    "  discount_applied NUMERIC DEFAULT 0.00," +
                    "  line_total NUMERIC NOT NULL," +
                    "  FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)," +
                    "  FOREIGN KEY (item_id) REFERENCES items(item_id)," +
                    "  FOREIGN KEY (batch_id) REFERENCES batches(batch_id)," +
                    "  FOREIGN KEY (unit_id) REFERENCES units(unit_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS customer_payments (" +
                    "  payment_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  customer_id INTEGER NOT NULL," +
                    "  invoice_id INTEGER," +
                    "  payment_date TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "  amount_paid NUMERIC NOT NULL," +
                    "  payment_method TEXT NOT NULL CHECK(payment_method IN ('CASH', 'CHEQUE', 'BANK_TRANSFER'))," +
                    "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id)," +
                    "  FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS cheques (" +
                    "  cheque_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  cheque_number TEXT NOT NULL," +
                    "  bank_name TEXT NOT NULL," +
                    "  amount NUMERIC NOT NULL," +
                    "  issue_date TEXT NOT NULL," +
                    "  realization_date TEXT NOT NULL," +
                    "  cheque_type TEXT NOT NULL CHECK(cheque_type IN ('RECEIVED', 'ISSUED'))," +
                    "  customer_id INTEGER," +
                    "  supplier_id INTEGER," +
                    "  status TEXT DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'CLEARED', 'BOUNCED', 'CANCELLED'))," +
                    "  reminder_sent INTEGER DEFAULT 0," +
                    "  FOREIGN KEY (customer_id) REFERENCES customers(customer_id)," +
                    "  FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS notification_logs (" +
                    "  notification_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  channel TEXT NOT NULL CHECK(channel IN ('DASHBOARD', 'EMAIL', 'SMS', 'WHATSAPP'))," +
                    "  recipient TEXT NOT NULL," +
                    "  message TEXT NOT NULL," +
                    "  status TEXT DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'SENT', 'FAILED'))," +
                    "  created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ");"
    };

    public static void initializeDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : SCHEMA_SQL) {
                stmt.execute(sql);
            }
            System.out.println("Database schema initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Schema Initialization Error: " + e.getMessage());
        }
    }
}