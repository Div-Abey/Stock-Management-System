package com.stockmanagement.security.dao;

import com.stockmanagement.database.DatabaseConnection;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;

/**
 * Minimal user access for now. Login is still the fixed role/password check in LoginFrame,
 * but every invoice needs a real cashier row (invoices.cashier_id -> users.user_id),
 * so the four default users are created here.
 * Replace with real user management when you build the security module.
 */
public class UserDAO {

    /** Creates the user if the username does not exist yet. The password is stored as a SHA-256 hash. */
    public void insertIfMissing(String username, String plainPassword, String roleName, String fullName) throws SQLException {
        String sql = "INSERT OR IGNORE INTO users(username, password_hash, role_id, full_name) " +
                "SELECT ?, ?, role_id, ? FROM roles WHERE role_name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, sha256(plainPassword));
            pstmt.setString(3, fullName);
            pstmt.setString(4, roleName);
            pstmt.executeUpdate();
        }
    }

    /** user_id of the (first) active user having this role, or 1 if none is found. */
    public int findUserIdByRole(String roleName) {
        String sql = "SELECT u.user_id FROM users u JOIN roles r ON r.role_id = u.role_id " +
                "WHERE r.role_name = ? AND u.is_active = 1 ORDER BY u.user_id LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roleName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        return 1;
    }

    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
