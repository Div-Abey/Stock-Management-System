package com.stockmanagement.ui.dashboard;

import com.stockmanagement.ui.auth.LoginFrame;
import com.stockmanagement.ui.components.AlertsPanel;
import com.stockmanagement.ui.components.GreetingClockPanel;
import com.stockmanagement.ui.components.SalesSummaryPanel;
import com.stockmanagement.ui.inventory.InventoryPanel;
import com.stockmanagement.supplier.service.ChequeService;
import com.stockmanagement.ui.cheque.ChequePanel;
import com.stockmanagement.ui.cheque.ChequeReminderDialog;
import com.stockmanagement.ui.customer.CustomerModulePanel;
import com.stockmanagement.ui.reports.ReportsPanel;
import com.stockmanagement.ui.sales.POSPanel;
import com.stockmanagement.ui.supplier.SupplierManagementPanel;
import com.stockmanagement.ui.settings.SettingsPanel;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardFrame extends JFrame {

    private static final int MENU_WIDTH = 180;

    private final int currentUserId;
    private final String currentUserName;
    private final String userRole;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainAreaPanel = new JPanel(cardLayout);

    private POSPanel posPanel;
    private CustomerModulePanel customerPanel;
    private SupplierManagementPanel supplierPanel;
    private ChequePanel chequePanel;
    private ReportsPanel reportsPanel;

    private final ChequeService chequeService = new ChequeService();
    private InventoryPanel inventoryPanel;
    private SettingsPanel settingsPanel;
    private AlertsPanel homeAlertsPanel;
    private SalesSummaryPanel todaySalesPanel;
    private SalesSummaryPanel lastMonthSalesPanel;

    public DashboardFrame() {
        this(1, "System Admin", "Admin");
    }

    public DashboardFrame(int userId, String userName) {
        this(userId, userName, "Admin");
    }

    public DashboardFrame(String role) {
        this(1, role, role);
    }

    public DashboardFrame(int userId, String userName, String role) {

        this.currentUserId = userId;
        this.currentUserName = userName;
        this.userRole = role.trim();

        setTitle("Stock Management System - Dashboard");
        setSize(1100, 760);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(5, 5));

        ((JComponent) getContentPane()).setBorder(
                new EmptyBorder(5, 5, 5, 5)
        );

        Font headerFont = new Font("Arial", Font.BOLD, 14);
        Font labelFont = new Font("Arial", Font.PLAIN, 14);

        // ---------------- Sidebar ----------------

        JPanel menuPanel = new JPanel();

        menuPanel.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        "Menu", 0, 0, headerFont
                )
        );

        menuPanel.setLayout(
                new BoxLayout(menuPanel, BoxLayout.Y_AXIS)
        );

        menuPanel.setPreferredSize(
                new Dimension(MENU_WIDTH, 0)
        );

        menuPanel.setMinimumSize(
                new Dimension(MENU_WIDTH, 0)
        );

        String[] menuItems = {
                "Home",
                "Sales",
                "Customers",
                "Suppliers",
                "Cheques",
                "Inventory",
                "Reports",
                "Settings"
        };

        for (String item : menuItems) {
            if (hasAccess(item)) {
                addMenuButton(menuPanel, item, labelFont);
            }
        }

        // Log Out button pinned to the bottom of the sidebar
        menuPanel.add(Box.createVerticalGlue());
        addMenuButton(menuPanel, "Log Out", labelFont);

        add(menuPanel, BorderLayout.WEST);

        // ---------------- Content Area ----------------

        mainAreaPanel.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        "Content Area", 0, 0, headerFont
                )
        );

        mainAreaPanel.add(
                createMainDashboardPanel(labelFont),
                "MainDashboard"
        );

        add(mainAreaPanel, BorderLayout.CENTER);

        cardLayout.show(mainAreaPanel, "MainDashboard");

        // ---------------- Status Bar ----------------

        JPanel statusBar = new JPanel(new BorderLayout());

        statusBar.setBorder(
                BorderFactory.createBevelBorder(
                        BevelBorder.LOWERED
                )
        );

        JLabel statusLabel = new JLabel(
                "Ready  |  Logged in as: " + currentUserName
                        + "  |  Role: " + userRole
        );

        statusLabel.setFont(labelFont);

        statusBar.add(statusLabel, BorderLayout.WEST);

        add(statusBar, BorderLayout.SOUTH);

        // Cheque reminders: pop-up once per cheque after the window opens (the Alerts box refreshes itself every minute)
        SwingUtilities.invokeLater(() -> {
            if (hasAccess("Cheques")) {
                ChequeReminderDialog.showIfNeeded(this, chequeService);
            }
        });
    }

    // ---------------- Role Permissions ----------------

    private boolean hasAccess(String menuName) {

        // Home and Log Out are accessible to every role
        if ("Home".equals(menuName) || "Log Out".equals(menuName)) {
            return true;
        }

        switch (userRole.toLowerCase()) {

            case "admin":
                return true;

            case "manager":
                return menuName.equals("Customers")
                        || menuName.equals("Suppliers")
                        || menuName.equals("Cheques")
                        || menuName.equals("Inventory")
                        || menuName.equals("Reports")
                        || menuName.equals("Settings");

            case "store keeper":
                return menuName.equals("Suppliers")
                        || menuName.equals("Inventory")
                        || menuName.equals("Customers")
                        || menuName.equals("Cheques")
                        || menuName.equals("Reports");

            case "cashier":
                return menuName.equals("Sales")
                        || menuName.equals("Customers")
                        || menuName.equals("Inventory")
                        || menuName.equals("Cheques");

            default:
                return false;
        }
    }

    // ---------------- Create Menu Buttons ----------------

    private void addMenuButton(
            JPanel menuPanel,
            String item,
            Font labelFont
    ) {

        JButton button = new JButton(item);

        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(labelFont);

        button.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        button.getPreferredSize().height
                )
        );

        button.addActionListener(e -> handleMenuClick(item));

        menuPanel.add(button);

        menuPanel.add(
                Box.createRigidArea(new Dimension(0, 5))
        );
    }

    // ---------------- Menu Click Handling ----------------

    private void handleMenuClick(String menuName) {

        if ("Log Out".equals(menuName)) {
            handleLogout();
            return;
        }

        if (!hasAccess(menuName)) {

            JOptionPane.showMessageDialog(
                    this,
                    "You do not have permission to access this module.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        // Home
        if ("Home".equals(menuName)) {

            if (homeAlertsPanel != null) {
                homeAlertsPanel.refreshAlerts();
            }
            if (todaySalesPanel != null) {
                todaySalesPanel.refresh();
            }
            if (lastMonthSalesPanel != null) {
                lastMonthSalesPanel.refresh();
            }

            cardLayout.show(mainAreaPanel, "MainDashboard");
        }

        // Sales
        else if ("Sales".equals(menuName)) {

            if (posPanel == null) {

                posPanel = new POSPanel(
                        currentUserId,
                        currentUserName
                );

                mainAreaPanel.add(posPanel, "Sales");

            } else {
                posPanel.refreshMasterData();
            }

            cardLayout.show(mainAreaPanel, "Sales");
        }

        // Inventory: loads inside the content area. Items / Categories / Units / Stock
        // are tabs inside InventoryPanel, so switching between them never opens a window.
        else if ("Inventory".equals(menuName)) {

            if (inventoryPanel == null) {

                inventoryPanel = new InventoryPanel(
                        currentUserId,
                        userRole
                );

                mainAreaPanel.add(inventoryPanel, "Inventory");
            }

            cardLayout.show(mainAreaPanel, "Inventory");

            // Items tab the first time, afterwards reloads the tab that was last used
            inventoryPanel.refresh();
        }

        // Settings
        else if ("Settings".equals(menuName)) {

            if (settingsPanel == null) {

                settingsPanel = new SettingsPanel(userRole);

                mainAreaPanel.add(
                        settingsPanel,
                        "Settings"
                );
            }

            cardLayout.show(
                    mainAreaPanel,
                    "Settings"
            );
        }

        // Customers (3.1): accounts + ledger / statements
        else if ("Customers".equals(menuName)) {

            if (customerPanel == null) {
                customerPanel = new CustomerModulePanel();
                mainAreaPanel.add(customerPanel, "Customers");
            } else {
                customerPanel.refresh();
            }
            cardLayout.show(mainAreaPanel, "Customers");
        }

        // Suppliers (3.2): suppliers, payables, purchase orders, GRN
        else if ("Suppliers".equals(menuName)) {

            if (supplierPanel == null) {
                supplierPanel = new SupplierManagementPanel(currentUserId);
                mainAreaPanel.add(supplierPanel, "Suppliers");
            } else {
                supplierPanel.refresh();
            }
            cardLayout.show(mainAreaPanel, "Suppliers");
        }

        // Cheques (3.3)
        else if ("Cheques".equals(menuName)) {

            if (chequePanel == null) {
                chequePanel = new ChequePanel();
                mainAreaPanel.add(chequePanel, "Cheques");
            } else {
                chequePanel.refresh();
            }
            cardLayout.show(mainAreaPanel, "Cheques");
        }

        // Reports: Sales / Stock / Profit & Loss / Aging tabs, CSV + PDF export
        else if ("Reports".equals(menuName)) {

            if (reportsPanel == null) {
                reportsPanel = new ReportsPanel();
                mainAreaPanel.add(reportsPanel, "Reports");
            } else {
                reportsPanel.refreshSelected();
            }
            cardLayout.show(mainAreaPanel, "Reports");
        }

        // Other Modules
        else {

            JOptionPane.showMessageDialog(
                    this,
                    menuName + " module is not built yet.",
                    menuName,
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    // ---------------- Log Out Handling ----------------

    private void handleLogout() {

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Log Out",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();

            new LoginFrame().setVisible(true);
        }
    }

    // ---------------- Main Dashboard Panel ----------------

    /**
     * Home page: greeting + live clock on top, Alerts (out of stock / low stock / expiry / cheques) on the left,
     * today's sales and last month's sales on the right.
     */
    private JPanel createMainDashboardPanel(Font labelFont) {

        JPanel dashboardPanel = new JPanel(new BorderLayout(10, 10));

        dashboardPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        dashboardPanel.add(
                new GreetingClockPanel(currentUserName),
                BorderLayout.NORTH
        );

        homeAlertsPanel = new AlertsPanel();
        todaySalesPanel = new SalesSummaryPanel(SalesSummaryPanel.Period.TODAY);
        lastMonthSalesPanel = new SalesSummaryPanel(SalesSummaryPanel.Period.LAST_MONTH);

        JPanel salesColumn = new JPanel(new GridLayout(2, 1, 10, 10));
        salesColumn.add(todaySalesPanel);
        salesColumn.add(lastMonthSalesPanel);

        JPanel body = new JPanel(new GridLayout(1, 2, 10, 10));
        body.add(homeAlertsPanel);
        body.add(salesColumn);

        dashboardPanel.add(body, BorderLayout.CENTER);

        return dashboardPanel;
    }
}
