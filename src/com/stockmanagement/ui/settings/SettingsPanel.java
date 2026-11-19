package com.stockmanagement.ui.settings;

import com.stockmanagement.security.dao.UserDAO;
import com.stockmanagement.security.dao.UserDAO.UserRecord;
import com.stockmanagement.settings.dao.SettingsDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class SettingsPanel extends JPanel {

    private final String currentRole;
    private final UserDAO userDAO = new UserDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    private JComboBox<UserRecord> userComboBox;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    private DefaultTableModel accountTableModel;
    private JTable accountTable;
    private JButton toggleStatusButton;

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

        if ("Admin".equalsIgnoreCase(currentRole)) {
            content.add(Box.createVerticalStrut(15));
            content.add(createAccountStatusPanel());
        }

        add(content, BorderLayout.CENTER);

        loadUsers();
        if ("Admin".equalsIgnoreCase(currentRole)) {
            loadAccountStatusTable();
        }
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

    // ---------------- Account Enable / Disable Panel (Admin Only) ----------------

    private JPanel createAccountStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("User Account Enable / Disable (Non-Admin)"));

        String[] columns = {"User ID", "Username", "Full Name", "Role", "Status"};
        accountTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        accountTable = new JTable(accountTableModel);
        accountTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountTable.getSelectionModel().addListSelectionListener(e -> updateToggleButtonText());

        JScrollPane scrollPane = new JScrollPane(accountTable);
        scrollPane.setPreferredSize(new Dimension(0, 180));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toggleStatusButton = new JButton("Enable / Disable Account");
        toggleStatusButton.setEnabled(false);
        toggleStatusButton.addActionListener(e -> toggleSelectedUserStatus());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadAccountStatusTable());

        buttonPanel.add(refreshButton);
        buttonPanel.add(toggleStatusButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateToggleButtonText() {
        int selectedRow = accountTable.getSelectedRow();
        if (selectedRow == -1) {
            toggleStatusButton.setEnabled(false);
            toggleStatusButton.setText("Enable / Disable Account");
            return;
        }

        String currentStatus = String.valueOf(accountTableModel.getValueAt(selectedRow, 4));
        toggleStatusButton.setEnabled(true);
        if ("Disabled".equalsIgnoreCase(currentStatus)) {
            toggleStatusButton.setText("Enable Account");
        } else {
            toggleStatusButton.setText("Disable Account");
        }
    }

    private void loadAccountStatusTable() {
        try {
            accountTableModel.setRowCount(0);
            List<Object[]> rows = settingsDAO.getNonAdminUsers();
            for (Object[] row : rows) {
                accountTableModel.addRow(row);
            }
            updateToggleButtonText();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load user accounts: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void toggleSelectedUserStatus() {
        if (!"Admin".equalsIgnoreCase(currentRole)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Only Admin can enable or disable user accounts.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int selectedRow = accountTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user account first.");
            return;
        }

        int userId = Integer.parseInt(String.valueOf(accountTableModel.getValueAt(selectedRow, 0)));
        String username = String.valueOf(accountTableModel.getValueAt(selectedRow, 1));
        String role = String.valueOf(accountTableModel.getValueAt(selectedRow, 3));
        String currentStatus = String.valueOf(accountTableModel.getValueAt(selectedRow, 4));

        if ("Admin".equalsIgnoreCase(role)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Admin accounts cannot be disabled.",
                    "Not Allowed",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        boolean enableAccount = "Disabled".equalsIgnoreCase(currentStatus);
        String actionWord = enableAccount ? "enable" : "disable";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + actionWord + " account '" + username + "' (" + role + ")?",
                "Confirm Account Status Change",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean updated = settingsDAO.updateUserStatus(userId, enableAccount);
            if (updated) {
                JOptionPane.showMessageDialog(
                        this,
                        "Account '" + username + "' has been " + (enableAccount ? "enabled." : "disabled.")
                );
                loadAccountStatusTable();
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to update account status.",
                        "Update Failed",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to update account status: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
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