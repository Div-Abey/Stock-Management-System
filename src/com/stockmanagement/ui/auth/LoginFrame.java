package com.stockmanagement.ui.auth;

import com.stockmanagement.security.dao.UserDAO;
import com.stockmanagement.ui.dashboard.DashboardFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {

    private JComboBox<String> userRoleComboBox;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginFrame() {

        setTitle("Stock Management System - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Header Label
        JLabel headerLabel = new JLabel("System Login", JLabel.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 5, 30, 2);
        panel.add(headerLabel, gbc);

        // Standard fonts for components
        Font labelFont = new Font("Arial", Font.PLAIN, 14);
        Font fieldFont = new Font("Arial", Font.PLAIN, 14);

        // Username
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(labelFont);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.1;
        gbc.insets = new Insets(5, 5, 5, 0);
        panel.add(usernameLabel, gbc);

        String[] roles = {"Admin", "Manager", "Store Keeper", "Cashier"};

        userRoleComboBox = new JComboBox<>(roles);
        userRoleComboBox.setFont(fieldFont);
        userRoleComboBox.setPreferredSize(new Dimension(60, 25));

        gbc.gridx = 1;
        gbc.weightx = 0.9;
        gbc.insets = new Insets(5, 0, 5, 5);
        panel.add(userRoleComboBox, gbc);

        // Password
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(labelFont);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.1;
        gbc.insets = new Insets(5, 5, 5, 0);
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField();
        passwordField.setFont(fieldFont);
        passwordField.setPreferredSize(new Dimension(60, 25));

        gbc.gridx = 1;
        gbc.weightx = 0.9;
        gbc.insets = new Insets(5, 0, 5, 5);
        panel.add(passwordField, gbc);

        // Login Button
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 5, 10, 5);
        panel.add(loginButton, gbc);

        add(panel);

        loginButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                // Login logic
                String username = (String) userRoleComboBox.getSelectedItem();
                String password = new String(passwordField.getPassword());

                boolean isValidUser = false;
                String role = "";

                if ("Admin".equals(username) && "admin123".equals(password)) {

                    isValidUser = true;
                    role = "Admin";

                } else if ("Manager".equals(username) && "manager123".equals(password)) {

                    isValidUser = true;
                    role = "Manager";

                } else if ("Cashier".equals(username) && "cashier123".equals(password)) {

                    isValidUser = true;
                    role = "Cashier";

                } else if ("Store Keeper".equals(username) && "storekeeper123".equals(password)) {

                    isValidUser = true;
                    role = "Store Keeper";
                }

                if (isValidUser) {

                    JOptionPane.showMessageDialog(
                            LoginFrame.this,
                            "Login successful as " + role
                    );

                    // Pass selected role to Dashboard
                    new DashboardFrame(role).setVisible(true);

                    LoginFrame.this.setVisible(false);

                } else {

                    JOptionPane.showMessageDialog(
                            LoginFrame.this,
                            "Invalid username or password"
                    );
                }
            }
        });
    }
}