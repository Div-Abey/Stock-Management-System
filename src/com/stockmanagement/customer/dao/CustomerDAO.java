package com.stockmanagement.customer.dao;

import com.stockmanagement.common.enums.CustomerType;
import com.stockmanagement.customer.model.Customer;
import com.stockmanagement.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL for the customers table. */
public class CustomerDAO {

    private static final String SELECT =
            "SELECT customer_id, customer_name, phone, email, customer_type, credit_limit, current_balance FROM customers ";

    public void insert(String name, String phone, String email, CustomerType type, double creditLimit) throws SQLException {
        String sql = "INSERT INTO customers(customer_name, phone, email, customer_type, credit_limit) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setString(4, type.name());
            ps.setDouble(5, creditLimit);
            ps.executeUpdate();
        }
    }

    public void update(int customerId, String name, String phone, String email, CustomerType type, double creditLimit) throws SQLException {
        String sql = "UPDATE customers SET customer_name = ?, phone = ?, email = ?, customer_type = ?, credit_limit = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setString(4, type.name());
            ps.setDouble(5, creditLimit);
            ps.setInt(6, customerId);
            ps.executeUpdate();
        }
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY customer_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Customer findById(int customerId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, customerId);
        }
    }

    public Customer findById(Connection conn, int customerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Adds (or, with a negative amount, removes) money from what the customer owes. */
    public void adjustBalance(Connection conn, int customerId, double delta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE customers SET current_balance = current_balance + ? WHERE customer_id = ?")) {
            ps.setDouble(1, delta);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        }
    }

    private Customer map(ResultSet rs) throws SQLException {
        return new Customer(rs.getInt("customer_id"), rs.getString("customer_name"), rs.getString("phone"),
                rs.getString("email"), CustomerType.valueOf(rs.getString("customer_type")),
                rs.getDouble("credit_limit"), rs.getDouble("current_balance"));
    }
}
