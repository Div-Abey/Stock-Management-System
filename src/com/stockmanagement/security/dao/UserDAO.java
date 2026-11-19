package com.stockmanagement.security.dao;

import com.stockmanagement.config.DatabaseConnection;

import java.security.MessageDigest;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Existing SHA-256 method
    private static String sha256(String text) {

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();

            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // User information
    public static class UserRecord {

        public int userId;
        public String username;
        public String fullName;
        public String role;

        public UserRecord(int userId, String username,
                          String fullName, String role) {

            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
        }

        @Override
        public String toString() {
            return fullName + " (" + role + ")";
        }
    }

    // Authenticated user
    public UserRecord authenticateByRoleAndPassword(
            String role, String password) throws SQLException {

        String sql = """
                SELECT u.user_id, u.username, u.full_name,
                       r.role_name, u.password_hash
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                WHERE r.role_name = ?
                AND u.is_active = 1
                ORDER BY u.user_id
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    String storedHash = rs.getString("password_hash");
                    String enteredHash = sha256(password);

                    if (MessageDigest.isEqual(
                            storedHash.getBytes(StandardCharsets.UTF_8),
                            enteredHash.getBytes(StandardCharsets.UTF_8))) {

                        return new UserRecord(
                                rs.getInt("user_id"),
                                rs.getString("username"),
                                rs.getString("full_name"),
                                rs.getString("role_name")
                        );
                    }
                }
            }
        }

        return null;
    }

    // Get users that current user can manage
    public List<UserRecord> getManageableUsers(
            String actorRole) throws SQLException {

        List<UserRecord> users = new ArrayList<>();

        if (!actorRole.equalsIgnoreCase("Admin")
                && !actorRole.equalsIgnoreCase("Manager")) {
            return users;
        }

        String sql = """
                SELECT u.user_id, u.username, u.full_name,
                       r.role_name
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                WHERE u.is_active = 1
                AND (? = 'Admin' OR r.role_name <> 'Admin')
                ORDER BY u.username
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1,
                    actorRole.equalsIgnoreCase("Admin")
                            ? "Admin" : "Manager");

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    users.add(new UserRecord(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role_name")
                    ));
                }
            }
        }

        return users;
    }

    // Update selected user's password
    public boolean updatePassword(
            int targetUserId,
            String newPassword,
            String actorRole) throws SQLException {

        if (!actorRole.equalsIgnoreCase("Admin")
                && !actorRole.equalsIgnoreCase("Manager")) {
            return false;
        }

        String sql = """
                UPDATE users
                SET password_hash = ?
                WHERE user_id = ?
                AND is_active = 1
                AND (
                    ? = 'Admin'
                    OR EXISTS (
                        SELECT 1
                        FROM roles r
                        WHERE r.role_id = users.role_id
                        AND r.role_name <> 'Admin'
                    )
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sha256(newPassword));
            ps.setInt(2, targetUserId);

            ps.setString(3,
                    actorRole.equalsIgnoreCase("Admin")
                            ? "Admin" : "Manager");

            return ps.executeUpdate() > 0;
        }
    }

    // Existing method
    public void insertIfMissing(
            String username,
            String plainPassword,
            String roleName,
            String fullName) throws SQLException {

        String sql = """
                INSERT OR IGNORE INTO users
                (username, password_hash, role_id, full_name)
                SELECT ?, ?, role_id, ?
                FROM roles
                WHERE role_name = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, sha256(plainPassword));
            ps.setString(3, fullName);
            ps.setString(4, roleName);

            ps.executeUpdate();
        }
    }

    // Existing method
    public int findUserIdByRole(String roleName) {

        String sql = """
                SELECT u.user_id
                FROM users u
                JOIN roles r ON r.role_id = u.role_id
                WHERE r.role_name = ?
                AND u.is_active = 1
                ORDER BY u.user_id
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roleName);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 1;
    }
}