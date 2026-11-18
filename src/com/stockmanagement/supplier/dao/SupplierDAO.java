package com.stockmanagement.supplier.dao;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.supplier.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL for the suppliers table. */
public class SupplierDAO {

    private static final String SELECT = "SELECT supplier_id, supplier_name, phone, email, address, current_balance FROM suppliers ";

    public void insert(String name, String phone, String email, String address) throws SQLException {
        String sql = "INSERT INTO suppliers(supplier_name, phone, email, address) VALUES(?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setString(4, address);
            ps.executeUpdate();
        }
    }

    public void update(int id, String name, String phone, String email, String address) throws SQLException {
        String sql = "UPDATE suppliers SET supplier_name = ?, phone = ?, email = ?, address = ? WHERE supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setString(4, address);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    public List<Supplier> findAll() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY supplier_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Supplier findById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE supplier_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Positive = we owe more (GRN), negative = we paid. */
    public void adjustBalance(Connection conn, int supplierId, double delta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE suppliers SET current_balance = current_balance + ? WHERE supplier_id = ?")) {
            ps.setDouble(1, delta);
            ps.setInt(2, supplierId);
            ps.executeUpdate();
        }
    }

    private Supplier map(ResultSet rs) throws SQLException {
        return new Supplier(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getDouble(6));
    }
}
