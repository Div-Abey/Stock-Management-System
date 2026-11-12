package com.stockmanagement.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:stock_management.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);

        // Enforce SQLite Foreign Key constraints on every connection open
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }

        return conn;
    }
}