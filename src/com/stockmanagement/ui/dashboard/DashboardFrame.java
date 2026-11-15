package com.stockmanagement.ui.dashboard;

import com.stockmanagement.ui.sales.POSPanel;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DashboardFrame extends JFrame {

    private static final int MENU_WIDTH = 180;

    private final int currentUserId;
    private final String currentUserName;
    private final String userRole;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainAreaPanel = new JPanel(cardLayout);

    private POSPanel posPanel;

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
                "Inventory",
                "Reports",
                "Settings"
        };

        for (String item : menuItems) {

            if (hasAccess(item)) {
                addMenuButton(menuPanel, item, labelFont);
            }
        }

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
    }

    // ---------------- Role Permissions ----------------

    private boolean hasAccess(String menuName) {

        // Home is accessible to every role
        if ("Home".equals(menuName)) {
            return true;
        }

        switch (userRole.toLowerCase()) {

            case "admin":

                return true;

            case "manager":

                return menuName.equals("Inventory")
                        || menuName.equals("Reports")
                        || menuName.equals("Settings");

            case "store keeper":

                return menuName.equals("Inventory");

            case "cashier":

                return menuName.equals("Sales")
                        || menuName.equals("Inventory");

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

    // ---------------- Main Dashboard Panel ----------------

    private JPanel createMainDashboardPanel(Font labelFont) {

        JPanel dashboardPanel = new JPanel(
                new GridLayout(1, 3, 10, 10)
        );

        dashboardPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        // Daily Tasks
        JPanel dailyTasksPanel = new JPanel(new BorderLayout());

        dailyTasksPanel.setBorder(
                BorderFactory.createTitledBorder("Daily Tasks")
        );

        dailyTasksPanel.add(
                new JLabel("Task list here...", JLabel.CENTER),
                BorderLayout.CENTER
        );

        // Alerts section in dashboard content
        JPanel alertsPanel = new JPanel(new BorderLayout());

        alertsPanel.setBorder(
                BorderFactory.createTitledBorder("Alerts")
        );

        alertsPanel.add(
                new JLabel("Critical alerts here...", JLabel.CENTER),
                BorderLayout.CENTER
        );

        // System Time
        JPanel timePanel = new JPanel(new BorderLayout());

        timePanel.setBorder(
                BorderFactory.createTitledBorder("System Time")
        );

        JLabel timeLabel = new JLabel("", JLabel.CENTER);

        timeLabel.setFont(labelFont);

        timePanel.add(
                timeLabel,
                BorderLayout.CENTER
        );

        SimpleDateFormat timeFormat =
                new SimpleDateFormat("HH:mm:ss");

        Timer timer = new Timer(
                1000,
                e -> timeLabel.setText(
                        timeFormat.format(new Date())
                )
        );

        timer.start();

        dashboardPanel.add(dailyTasksPanel);
        dashboardPanel.add(alertsPanel);
        dashboardPanel.add(timePanel);

        return dashboardPanel;
    }
}