package com.stockmanagement.ui.dashboard;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DashboardFrame extends JFrame {

    public DashboardFrame() {
        setTitle("Stock Management System - Dashboard");
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Standard fonts
        Font headerFont = new Font("Arial", Font.BOLD, 14);
        Font labelFont = new Font("Arial", Font.PLAIN, 14);

        // Main Transaction Area layout setup
        CardLayout cardLayout = new CardLayout();
        JPanel mainAreaPanel = new JPanel(cardLayout);
        mainAreaPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Content Area", 0, 0, headerFont));

        // Sidebar / Menu Area
        JPanel menuPanel = new JPanel();
        menuPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Menu", 0, 0, headerFont));
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

        // Add Main Button at the top
        JButton mainButton = new JButton("Home");
        mainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainButton.setFont(labelFont);
        mainButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainButton.getPreferredSize().height));
        mainButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainAreaPanel, "MainDashboard");
            }
        });
        menuPanel.add(mainButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Sidebar Buttons
        String[] menuItems = {"Alerts", "POS System", "Inventory", "Reports", "Settings"};
        for (String item : menuItems) {
            JButton button = new JButton(item);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setFont(labelFont);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
            menuPanel.add(button);
            menuPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(menuPanel, gbc);

        // Add placeholder POS panel to mainAreaPanel
        JPanel posPanel = new JPanel(new BorderLayout());
        JLabel mainPlaceholder = new JLabel("POS Transaction View Placeholder", JLabel.CENTER);
        mainPlaceholder.setFont(labelFont);
        posPanel.add(mainPlaceholder, BorderLayout.CENTER);
        mainAreaPanel.add(posPanel, "POS");

        // Add Main Dashboard Panel
        mainAreaPanel.add(createMainDashboardPanel(labelFont), "MainDashboard");

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.8;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(mainAreaPanel, gbc);

        // Initially show Main
        cardLayout.show(mainAreaPanel, "MainDashboard");

        // Status Bar
        JPanel statusBar = new JPanel();
        statusBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        statusBar.setLayout(new BorderLayout());
        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setFont(labelFont);
        statusBar.add(statusLabel, BorderLayout.WEST);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);
        add(statusBar, gbc);
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
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeLabel.setText(timeFormat.format(new Date()));
            }
        });
        timer.start();

        dashboardPanel.add(dailyTasksPanel);
        dashboardPanel.add(alertsPanel);
        dashboardPanel.add(timePanel);

        return dashboardPanel;
    }
}
