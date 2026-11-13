PRAGMA foreign_keys = ON;

-- ==========================================
-- 1. SECURITY & USER MANAGEMENT[cite: 4]
-- ==========================================

CREATE TABLE roles (
                       role_id INTEGER PRIMARY KEY AUTOINCREMENT,
                       role_name TEXT NOT NULL UNIQUE -- Cashier, Store Keeper, Manager, Admin[cite: 4]
);

CREATE TABLE users (
                       user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                       username TEXT NOT NULL UNIQUE,
                       password_hash TEXT NOT NULL,
                       role_id INTEGER NOT NULL,
                       full_name TEXT NOT NULL,
                       is_active INTEGER DEFAULT 1,
                       created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

-- ==========================================
-- 2. INVENTORY & ITEM MANAGEMENT[cite: 1, 2]
-- ==========================================

CREATE TABLE categories (
                            category_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            category_name TEXT NOT NULL
);

-- Unit Conversion Master Table
CREATE TABLE units (
                       unit_id INTEGER PRIMARY KEY AUTOINCREMENT,
                       unit_name TEXT NOT NULL UNIQUE, -- e.g., Carton, Box, Piece, Kg, Gram
                       is_base_unit INTEGER DEFAULT 0
);

CREATE TABLE unit_conversions (
                                  conversion_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                  from_unit_id INTEGER NOT NULL,
                                  to_unit_id INTEGER NOT NULL,
                                  conversion_ratio NUMERIC NOT NULL, -- e.g., 1 Box = 10 Pieces
                                  FOREIGN KEY (from_unit_id) REFERENCES units(unit_id),
                                  FOREIGN KEY (to_unit_id) REFERENCES units(unit_id)
);

-- Item Master Table[cite: 1, 2]
CREATE TABLE items (
                       item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                       item_code TEXT NOT NULL UNIQUE,
                       barcode TEXT UNIQUE, -- Generated or scanned barcode[cite: 1]
                       item_name TEXT NOT NULL,
                       category_id INTEGER NOT NULL,
                       base_unit_id INTEGER NOT NULL,
                       reorder_level INTEGER NOT NULL DEFAULT 10, -- Re-order alert threshold[cite: 1]
                       retail_price NUMERIC NOT NULL DEFAULT 0.00, -- Multi-tier price 1
                       wholesale_price NUMERIC NOT NULL DEFAULT 0.00, -- Multi-tier price 2[cite: 2]
                       created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (category_id) REFERENCES categories(category_id),
                       FOREIGN KEY (base_unit_id) REFERENCES units(unit_id)
);

-- Quantity / Bulk Discount Rules[cite: 2]
CREATE TABLE bulk_discounts (
                                discount_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                item_id INTEGER NOT NULL,
                                min_quantity INTEGER NOT NULL,
                                discount_percentage NUMERIC DEFAULT 0.00,
                                discount_amount NUMERIC DEFAULT 0.00,
                                FOREIGN KEY (item_id) REFERENCES items(item_id)
);

-- ==========================================
-- 3. CRM & SUPPLIER MANAGEMENT[cite: 2, 3]
-- ==========================================

CREATE TABLE customers (
                           customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
                           customer_name TEXT NOT NULL,
                           phone TEXT,
                           email TEXT,
                           customer_type TEXT DEFAULT 'RETAIL' CHECK(customer_type IN ('RETAIL', 'WHOLESALE', 'SPECIAL')), -- Pricing tier classifier[cite: 2]
                           credit_limit NUMERIC NOT NULL DEFAULT 0.00, -- Max credit allowed[cite: 2]
                           current_balance NUMERIC NOT NULL DEFAULT 0.00, -- Accounts Receivable[cite: 2]
                           created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- Customer-Specific Tier Pricing[cite: 2]
CREATE TABLE customer_pricing (
                                  pricing_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                  customer_id INTEGER NOT NULL,
                                  item_id INTEGER NOT NULL,
                                  special_price NUMERIC NOT NULL,
                                  FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
                                  FOREIGN KEY (item_id) REFERENCES items(item_id)
);

CREATE TABLE suppliers (
                           supplier_id INTEGER PRIMARY KEY AUTOINCREMENT,
                           supplier_name TEXT NOT NULL,
                           phone TEXT,
                           email TEXT,
                           address TEXT,
                           current_balance NUMERIC NOT NULL DEFAULT 0.00 -- Accounts Payable
);

-- ==========================================
-- 4. PROCUREMENT & BATCH TRACKING[cite: 1, 2, 3]
-- ==========================================

CREATE TABLE purchase_orders (
                                 po_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                 supplier_id INTEGER NOT NULL,
                                 order_date TEXT NOT NULL,
                                 status TEXT DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'PARTIAL', 'RECEIVED', 'CANCELLED')),
                                 created_by INTEGER NOT NULL,
                                 FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
                                 FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- Complete po_items definition
CREATE TABLE po_items (
                          po_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                          po_id INTEGER NOT NULL,
                          item_id INTEGER NOT NULL,
                          quantity INTEGER NOT NULL,
                          unit_id INTEGER NOT NULL,
                          unit_price NUMERIC NOT NULL,
                          FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id),
                          FOREIGN KEY (item_id) REFERENCES items(item_id),
                          FOREIGN KEY (unit_id) REFERENCES units(unit_id)
);

-- Goods Received Note (GRN)[cite: 1, 3]
CREATE TABLE grn (
                     grn_id INTEGER PRIMARY KEY AUTOINCREMENT,
                     po_id INTEGER,
                     supplier_id INTEGER NOT NULL,
                     received_date TEXT NOT NULL,
                     total_amount NUMERIC NOT NULL,
                     received_by INTEGER NOT NULL,
                     FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id),
                     FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
                     FOREIGN KEY (received_by) REFERENCES users(user_id)
);

-- Batches for Expiry & FIFO Tracking[cite: 1]
CREATE TABLE batches (
                         batch_id INTEGER PRIMARY KEY AUTOINCREMENT,
                         item_id INTEGER NOT NULL,
                         batch_number TEXT NOT NULL,
                         expiry_date TEXT NOT NULL, -- Used to execute FIFO deduction[cite: 1]
                         quantity_received INTEGER NOT NULL,
                         quantity_remaining INTEGER NOT NULL,
                         purchase_price NUMERIC NOT NULL,
                         created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                         FOREIGN KEY (item_id) REFERENCES items(item_id)
);

CREATE TABLE grn_items (
                           grn_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                           grn_id INTEGER NOT NULL,
                           batch_id INTEGER NOT NULL,
                           item_id INTEGER NOT NULL,
                           quantity_received INTEGER NOT NULL,
                           unit_id INTEGER NOT NULL,
                           purchase_price NUMERIC NOT NULL,
                           FOREIGN KEY (grn_id) REFERENCES grn(grn_id),
                           FOREIGN KEY (batch_id) REFERENCES batches(batch_id),
                           FOREIGN KEY (item_id) REFERENCES items(item_id),
                           FOREIGN KEY (unit_id) REFERENCES units(unit_id)
);

-- ==========================================
-- 5. SALES & BILLING PROCESS[cite: 1, 2]
-- ==========================================

CREATE TABLE invoices (
                          invoice_id INTEGER PRIMARY KEY AUTOINCREMENT,
                          invoice_number TEXT NOT NULL UNIQUE,
                          customer_id INTEGER,
                          cashier_id INTEGER NOT NULL,
                          invoice_date TEXT DEFAULT CURRENT_TIMESTAMP,
                          payment_type TEXT NOT NULL CHECK(payment_type IN ('CASH', 'CREDIT', 'CHEQUE', 'CARD')),
                          total_amount NUMERIC NOT NULL,
                          discount_amount NUMERIC DEFAULT 0.00,
                          net_amount NUMERIC NOT NULL,
                          status TEXT DEFAULT 'PAID' CHECK(status IN ('PAID', 'UNPAID', 'PARTIAL', 'CANCELLED')),
                          FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
                          FOREIGN KEY (cashier_id) REFERENCES users(user_id)
);

CREATE TABLE invoice_items (
                               invoice_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                               invoice_id INTEGER NOT NULL,
                               item_id INTEGER NOT NULL,
                               batch_id INTEGER NOT NULL, -- FIFO batch reference[cite: 1]
                               unit_id INTEGER NOT NULL,
                               quantity INTEGER NOT NULL,
                               unit_price NUMERIC NOT NULL,
                               discount_applied NUMERIC DEFAULT 0.00,
                               line_total NUMERIC NOT NULL,
                               FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id),
                               FOREIGN KEY (item_id) REFERENCES items(item_id),
                               FOREIGN KEY (batch_id) REFERENCES batches(batch_id),
                               FOREIGN KEY (unit_id) REFERENCES units(unit_id)
);

CREATE TABLE customer_payments (
                                   payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                   customer_id INTEGER NOT NULL,
                                   invoice_id INTEGER,
                                   payment_date TEXT DEFAULT CURRENT_TIMESTAMP,
                                   amount_paid NUMERIC NOT NULL,
                                   payment_method TEXT NOT NULL CHECK(payment_method IN ('CASH', 'CHEQUE', 'BANK_TRANSFER')),
                                   FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
                                   FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
);

-- ==========================================
-- 6. CHEQUE MANAGEMENT & NOTIFICATIONS[cite: 3, 4]
-- ==========================================

-- Cheque Tracking (Received & Issued Post-Dated Cheques)[cite: 3]
CREATE TABLE cheques (
                         cheque_id INTEGER PRIMARY KEY AUTOINCREMENT,
                         cheque_number TEXT NOT NULL,
                         bank_name TEXT NOT NULL,
                         amount NUMERIC NOT NULL,
                         issue_date TEXT NOT NULL,
                         realization_date TEXT NOT NULL, -- Date to trigger alert reminder[cite: 3]
                         cheque_type TEXT NOT NULL CHECK(cheque_type IN ('RECEIVED', 'ISSUED')),
                         customer_id INTEGER,
                         supplier_id INTEGER,
                         status TEXT DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'CLEARED', 'BOUNCED', 'CANCELLED')),
                         reminder_sent INTEGER DEFAULT 0,
                         FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
                         FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
);

-- System Dashboard Alerts & Message Notification Logs[cite: 1, 4]
CREATE TABLE notification_logs (
                                   notification_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                   channel TEXT NOT NULL CHECK(channel IN ('DASHBOARD', 'EMAIL', 'SMS', 'WHATSAPP')), --[cite: 1, 4]
                                   recipient TEXT NOT NULL,
                                   message TEXT NOT NULL,
                                   status TEXT DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'SENT', 'FAILED')),
                                   created_at TEXT DEFAULT CURRENT_TIMESTAMP
);