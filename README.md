# Stock Management System

A desktop stock, sales and account-management application for a small retail / wholesale business.
Built with **Java 17**, **Swing** and an embedded **SQLite** database, using a layered
(UI → Service → DAO → Database) architecture.

---

## Table of contents
1. [Features](#features)
2. [Technology stack](#technology-stack)
3. [Project structure](#project-structure)
4. [Getting started](#getting-started)
5. [Default users and roles](#default-users-and-roles)
6. [Module guide: CRM & Supplier Management](#module-guide-crm--supplier-management)
7. [Database](#database)
8. [Git workflow](#git-workflow)
9. [Recommended .gitignore additions](#recommended-gitignore-additions)
10. [Team and module ownership](#team-and-module-ownership)
11. [Known limitations](#known-limitations)

---

## Features

| Module | What it does |
|---|---|
| **Login and roles** | Role-based login (Admin, Manager, Store Keeper, Cashier). Menu buttons are shown according to the role. |
| **Inventory** | Items, categories, units and unit conversions, stock batches with expiry dates, FIFO stock handling, low-stock / expiry alerts. |
| **Sales (POS)** | Point-of-sale billing, pricing, bulk discounts, invoices, cash / cheque / credit sales. |
| **Customers (CRM)** | Customer accounts with credit limits, customer ledger, payment recording, monthly account statements (PDF / print). |
| **Suppliers** | Supplier records, purchase orders (create, view, send as PDF / email, cancel). |
| **Cheques** | Post-dated cheques received from customers and issued to suppliers, status tracking, realization-date reminders. |
| **Reports** | Sales, stock and profit/loss reports. |
| **Settings** | User and password management (Admin / Manager). |
| **Dashboard** | Greeting and clock, sales summary, stock alerts, cheque reminders. |

## Technology stack

- Java 17 (JDK 17 or newer)
- Java Swing (UI)
- SQLite via `sqlite-jdbc` 3.45.1.0
- SLF4J 1.7.36 (`slf4j-api`, `slf4j-simple`)
- IntelliJ IDEA (project files included); a basic `pom.xml` is present for dependencies

The required `.jar` files are in the `lib/` folder.

## Project structure

```
Stock-Management-System/
├── lib/                       sqlite-jdbc and slf4j jars
├── pom.xml
├── console.sql                handy SQL for testing
├── stock_management.db        SQLite database (created on first run)
└── src/com/stockmanagement/
    ├── Main.java              start-up: database setup, seed data, opens the login window
    ├── database/              DatabaseConnection, DatabaseInitializer, DatabaseMigrator
    ├── common/                enums, exceptions, utilities (Date, Currency, Validation)
    ├── inventory/             model, dao, service  (items, batches, FIFO, alerts)
    ├── sales/                 model, dao, service  (billing, pricing, discounts, credit)
    ├── customer/              model, dao, service  (customers, ledger, credit)
    ├── supplier/              model, dao, service  (suppliers, purchase orders, cheques)
    ├── reports/               model, service, export (PDF, statement and PO formatters)
    ├── security/              UserDAO (users, roles)
    ├── integration/           notification and cloud-sync placeholders
    └── ui/                    Swing screens grouped by feature
        ├── auth/  dashboard/  inventory/  sales/  customer/
        ├── supplier/  cheque/  reports/  settings/  components/
```

**Layer rule:** UI classes never contain SQL. They call a **service** (business rules),
which calls a **DAO** (SQL), which uses `DatabaseConnection`.

## Getting started

### Prerequisites
- JDK 17 or newer
- IntelliJ IDEA (Community or Ultimate)
- Git

### Run in IntelliJ
1. Clone the repository:
   ```bash
(https://github.com/Div-Abey/Stock-Management-System.git)
   ```
2. Open the folder in IntelliJ IDEA (**File > Open**).
3. Set the project SDK to JDK 17+ (**File > Project Structure > Project**).
4. Make sure the jars in `lib/` are libraries of the module
   (**File > Project Structure > Libraries > + > Java**, select the three jars in `lib/`).
5. Mark `src` as **Sources Root** (right-click `src` > Mark Directory as > Sources Root).
6. Run `com.stockmanagement.Main`.

On the first run the application creates `stock_management.db` in the working directory,
creates all tables, upgrades older databases automatically, and adds demo data.

> The database file path is relative (`jdbc:sqlite:stock_management.db`).
> Always run from the project root, otherwise a new empty database is created somewhere else.

## Default users and roles

Seeded by `Main.java` for development and demonstration only.

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Manager | `manager` | `manager123` |
| Store Keeper | `storekeeper` | `storekeeper123` |
| Cashier | `cashier` | `cashier123` |

**Change or remove these before any real use.**

Menu access:

| Menu | Admin | Manager | Store Keeper | Cashier |
|---|:--:|:--:|:--:|:--:|
| Sales | ✔ | ✔ | | ✔ |
| Customers | ✔ | ✔ | | ✔ |
| Suppliers | ✔ | ✔ | ✔ | |
| Cheques | ✔ | ✔ | | |
| Inventory | ✔ | ✔ | ✔ | ✔ |
| Reports / Settings | ✔ | per role rules in `DashboardFrame` | | |

## Module guide: CRM & Supplier Management

### Customer ledger
- Create and update customers (name, phone, email, type: RETAIL / WHOLESALE / SPECIAL) with a credit limit.
- The ledger is built from real records: every invoice is a **debit**, every payment a **credit**.
  The closing balance always equals the customer's current balance.
- Record payments (cash, cheque, bank transfer). A payment settles the oldest unpaid invoices first and cannot exceed what the customer owes.
- Payment / settlement history and **monthly account statements** with opening, running and closing balance, exported as PDF or printed.

### Suppliers and purchase orders
- Create and update suppliers.
- Create purchase orders with several item lines; the last purchase price is suggested automatically.
- Send a purchase order: marks the sent date, saves a PDF and opens the supplier's email with the order text.
- Cancel a purchase order that is still pending.

### Cheque management
- Record post-dated cheques received from customers and issued to suppliers
  (cheque number, bank, amount, cheque date, realization date).
- Statuses: `PENDING`, `CLEARED`, `BOUNCED`, `CANCELLED`.
- Cheques that reached their realization date are highlighted, shown in the dashboard alerts and announced once by a pop-up.
- A bounced received cheque puts the amount back on the customer's balance (shown as *Bounced cheque* in the ledger).

## Database

- SQLite file: `stock_management.db`
- `DatabaseInitializer` creates the tables; `DatabaseMigrator` adds missing columns to an existing database
  so no data is lost when the schema grows. It is safe to run on every start.
- Main tables: `users`, `roles`, `items`, `categories`, `units`, `batches`, `invoices`, `invoice_items`,
  `customers`, `customer_payments`, `suppliers`, `purchase_orders`, `po_items`, `cheques`, `notification_logs`.

If you see an error such as `no such column`, the app is using an old database file.
Start the app once from `Main` (it runs the migrator), or delete your **local** `stock_management.db`
to let it be rebuilt from scratch.

## Git workflow

```bash
# 1. get the latest code
git checkout main
git pull

# 2. create a branch for your work
git checkout -b feature/<short-name>        # e.g. feature/cheque-reminders

# 3. work, then check what changed
git status
git diff

# 4. commit small, meaningful steps
git add <files>
git commit -m "Add cheque realization reminder pop-up"

# 5. publish and open a pull request
git push -u origin feature/<short-name>
```

Guidelines
- Never commit directly to `main`; use a branch and a pull request.
- Pull the latest `main` into your branch before opening a pull request (`git pull origin main`).
- One feature or fix per commit; write the message in the imperative ("Add…", "Fix…").
- Do not commit the database file, logs or IDE-generated files (see below).
- Keep package names and folder names exactly as in [Project structure](#project-structure) (for example `dao`, not `doa`),
  and make sure a public class has the same name as its file.

## Recommended .gitignore additions

The existing `.gitignore` does not exclude these files. Add them, then untrack any that were already committed
(`git rm --cached <file>`):

```gitignore
# Build output
target/
out/

# Local runtime data
stock_management.db
*.sqlite

# JVM crash logs
hs_err_pid*.log
replay_pid*.log

# IntelliJ
.idea/
*.iml
```

## Team and module ownership

| Area | Package(s) |
|---|---|
| CRM and supplier management (customer ledger, purchase orders, cheques) | `customer`, `supplier`, `ui/customer`, `ui/supplier`, `ui/cheque`, `reports/export` |
| Inventory | `inventory`, `ui/inventory` |
| Sales and billing | `sales`, `ui/sales` |
| Reports | `reports`, `ui/reports` |
| Login, security, settings | `security`, `ui/auth`, `ui/settings` |

Shared files (`Main.java`, `DashboardFrame.java`, `database/*`) should be changed in small commits
and announced to the team, because everyone's module is wired in there.

## Known limitations

- Login passwords for the seeded users are fixed demo values.
- The `integration` package (SMS, WhatsApp, cloud sync) contains placeholders only.
- Goods Received Notes and supplier accounts payable are not part of the current scope.
- `pom.xml` only declares the SQLite driver; the project is built and run through IntelliJ, not `mvn package`.
- Two older files, `config/DatabaseInitializer.java` and `config/DatabaseConnection.java`, are duplicates of the `database` package
  versions and should not be used.
