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

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainAreaPanel = new JPanel(cardLayout);
    private POSPanel posPanel;   // created the first time "Sales" is clicked

    public DashboardFrame() {
        this(1, "System Admin");
    }

    /* userId / name of the logged-in user: the POS saves them as the cashier of each invoice. */
    public DashboardFrame(int userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;

        setTitle("Stock Management System - Dashboard");
        setSize(1100, 760);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // BorderLayout: the menu keeps its own fixed width, so whatever panel is opened in the
        // centre can never squeeze the menu names into "..."
        setLayout(new BorderLayout(5, 5));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(5, 5, 5, 5));

        Font headerFont = new Font("Arial", Font.BOLD, 14);
        Font labelFont = new Font("Arial", Font.PLAIN, 14);

        // ---------------- Sidebar----------------
        JPanel menuPanel = new JPanel();
        menuPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Menu", 0, 0, headerFont));
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setPreferredSize(new Dimension(MENU_WIDTH, 0));
        menuPanel.setMinimumSize(new Dimension(MENU_WIDTH, 0));

        JButton mainButton = new JButton("Home");
        mainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainButton.setFont(labelFont);
        mainButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainButton.getPreferredSize().height));
        mainButton.addActionListener(e -> cardLayout.show(mainAreaPanel, "MainDashboard"));
        menuPanel.add(mainButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        String[] menuItems = {"Sales", "Inventory", "Reports", "Settings"};
        for (String item : menuItems) {
            JButton button = new JButton(item);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setFont(labelFont);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
            button.addActionListener(e -> handleMenuClick(item));
            menuPanel.add(button);
            menuPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        add(menuPanel, BorderLayout.WEST);

        // ---------------- Content area ----------------
        mainAreaPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Content Area", 0, 0, headerFont));
        mainAreaPanel.add(createMainDashboardPanel(labelFont), "MainDashboard");
        add(mainAreaPanel, BorderLayout.CENTER);
        cardLayout.show(mainAreaPanel, "MainDashboard");

        // ---------------- Status bar ----------------
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        JLabel statusLabel = new JLabel("Ready  |  Logged in as: " + currentUserName);
        statusLabel.setFont(labelFont);
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
    }

    /** Sidebar clicks. "Sales" shows the POS (cart -> billing -> invoice) inside the content area. */
    private void handleMenuClick(String menuName) {
        if ("Sales".equals(menuName)) {
            if (posPanel == null) {
                posPanel = new POSPanel(currentUserId, currentUserName);
                mainAreaPanel.add(posPanel, "Sales");
            } else {
                posPanel.refreshMasterData();   // pick up stock / customers changed elsewhere
            }
            cardLayout.show(mainAreaPanel, "Sales");
        } else {
            JOptionPane.showMessageDialog(this, menuName + " module is not built yet.",
                    menuName, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel createMainDashboardPanel(Font labelFont) {
        JPanel dashboardPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel dailyTasksPanel = new JPanel(new BorderLayout());
        dailyTasksPanel.setBorder(BorderFactory.createTitledBorder("Daily Tasks"));
        dailyTasksPanel.add(new JLabel("Task list here...", JLabel.CENTER), BorderLayout.CENTER);

        JPanel alertsPanel = new JPanel(new BorderLayout());
        alertsPanel.setBorder(BorderFactory.createTitledBorder("Alerts"));
        alertsPanel.add(new JLabel("Critical alerts here...", JLabel.CENTER), BorderLayout.CENTER);

        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setBorder(BorderFactory.createTitledBorder("System Time"));
        JLabel timeLabel = new JLabel("", JLabel.CENTER);
        timeLabel.setFont(labelFont);
        timePanel.add(timeLabel, BorderLayout.CENTER);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Timer timer = new Timer(1000, e -> timeLabel.setText(timeFormat.format(new Date())));
        timer.start();

        dashboardPanel.add(dailyTasksPanel);
        dashboardPanel.add(alertsPanel);
        dashboardPanel.add(timePanel);

        return dashboardPanel;
    }
}
