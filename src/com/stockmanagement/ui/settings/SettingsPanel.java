package com.stockmanagement.ui.settings;

import com.stockmanagement.security.dao.UserDAO;
import com.stockmanagement.security.dao.UserDAO.UserRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class SettingsPanel extends JPanel {

    private final String currentRole;
    private final UserDAO userDAO = new UserDAO();

    private JComboBox<UserRecord> userComboBox;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    public SettingsPanel(String currentRole) {
        this.currentRole = currentRole;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Settings");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(createPasswordPanel());

        add(content, BorderLayout.CENTER);

        loadUsers();
    }

    // ---------------- Password Panel ----------------

    private JPanel createPasswordPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Change User Password"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Select User:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(userLabel, gbc);

        userComboBox = new JComboBox<>();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(userComboBox, gbc);

        JLabel newLabel = new JLabel("New Password:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(newLabel, gbc);

        newPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(newPasswordField, gbc);

        JLabel confirmLabel = new JLabel("Confirm Password:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(confirmLabel, gbc);

        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(confirmPasswordField, gbc);

        JButton updateButton = new JButton("Update Password");
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(updateButton, gbc);

        updateButton.addActionListener(e -> updatePassword());

        return panel;
    }

    // ---------------- Load Users ----------------

    private void loadUsers() {
        try {
            List<UserRecord> users = userDAO.getManageableUsers(currentRole);
            userComboBox.removeAllItems();
            for (UserRecord user : users) {
                userComboBox.addItem(user);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load users: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ---------------- Update Password ----------------

    private void updatePassword() {
        UserRecord selectedUser = (UserRecord) userComboBox.getSelectedItem();

        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Please select a user.");
            return;
        }

        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both password fields.");
            return;
        }

        if (newPassword.length() < 8) {
            JOptionPane.showMessageDialog(this, "Password must contain at least 8 characters.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Change password for " + selectedUser.username + "?",
                "Confirm Password Change",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean updated = userDAO.updatePassword(
                    selectedUser.userId,
                    newPassword,
                    currentRole
            );

            if (updated) {
                JOptionPane.showMessageDialog(this, "Password updated successfully!");
                newPasswordField.setText("");
                confirmPasswordField.setText("");
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "You do not have permission to change this password.",
                        "Access Denied",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Password update failed: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}