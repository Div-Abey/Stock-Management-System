package com.stockmanagement.inventory.dao;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.inventory.model.Unit;
import com.stockmanagement.inventory.model.UnitConversion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Handles database operations for measurement units and unit conversions
public class UnitDAO {

    // units

    public List<Unit> findAll() throws SQLException {
        String sql = "SELECT unit_id, unit_name, is_base_unit FROM units ORDER BY unit_name";
        List<Unit> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Unit(rs.getInt(1), rs.getString(2), rs.getInt(3) == 1));
            }
        }
        return list;
    }

    public Unit findById(int unitId) throws SQLException {
        String sql = "SELECT unit_id, unit_name, is_base_unit FROM units WHERE unit_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, unitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Unit(rs.getInt(1), rs.getString(2), rs.getInt(3) == 1) : null;
            }
        }
    }

    public boolean nameExists(String name) throws SQLException {
        String sql = "SELECT 1 FROM units WHERE lower(unit_name) = lower(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(String name, boolean baseUnit) throws SQLException {
        String sql = "INSERT INTO units(unit_name, is_base_unit) VALUES(?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, baseUnit ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    // Counts how many records use this unit across items, conversions, and orders
    public int countUsage(int unitId) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM items WHERE base_unit_id = ?) " +
                "+ (SELECT COUNT(*) FROM unit_conversions WHERE from_unit_id = ? OR to_unit_id = ?) " +
                "+ (SELECT COUNT(*) FROM po_items WHERE unit_id = ?) " +
                "+ (SELECT COUNT(*) FROM grn_items WHERE unit_id = ?) " +
                "+ (SELECT COUNT(*) FROM invoice_items WHERE unit_id = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 6; i++) {
                ps.setInt(i, unitId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void delete(int unitId) throws SQLException {
        String sql = "DELETE FROM units WHERE unit_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, unitId);
            ps.executeUpdate();
        }
    }

    //conversions

    private static final String CONVERSION_SELECT =
            "SELECT uc.conversion_id, uc.item_id, i.item_name, uc.from_unit_id, uf.unit_name, " +
                    "       uc.to_unit_id, ut.unit_name, uc.conversion_ratio " +
                    "FROM unit_conversions uc " +
                    "JOIN units uf ON uf.unit_id = uc.from_unit_id " +
                    "JOIN units ut ON ut.unit_id = uc.to_unit_id " +
                    "LEFT JOIN items i ON i.item_id = uc.item_id ";

    public List<UnitConversion> findAllConversions() throws SQLException {
        String sql = CONVERSION_SELECT + "ORDER BY (uc.item_id IS NOT NULL), i.item_name, uf.unit_name";
        List<UnitConversion> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UnitConversion c = new UnitConversion();
                c.setConversionId(rs.getInt(1));
                int itemId = rs.getInt(2);
                c.setItemId(rs.wasNull() ? null : itemId);
                c.setItemName(rs.getString(3));
                c.setFromUnitId(rs.getInt(4));
                c.setFromUnitName(rs.getString(5));
                c.setToUnitId(rs.getInt(6));
                c.setToUnitName(rs.getString(7));
                c.setRatio(rs.getDouble(8));
                list.add(c);
            }
        }
        return list;
    }
    // Finds the exact conversion ratio (1 fromUnit = ratio toUnit); null itemId checks default rules
    public Double findDirectRatio(Integer itemId, int fromUnitId, int toUnitId) throws SQLException {
        String sql = "SELECT conversion_ratio FROM unit_conversions WHERE from_unit_id = ? AND to_unit_id = ? AND " +
                (itemId == null ? "item_id IS NULL" : "item_id = ?");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fromUnitId);
            ps.setInt(2, toUnitId);
            if (itemId != null) {
                ps.setInt(3, itemId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : null;
            }
        }
    }

    public boolean conversionExists(Integer itemId, int fromUnitId, int toUnitId) throws SQLException {
        return findDirectRatio(itemId, fromUnitId, toUnitId) != null;
    }

    public void insertConversion(Integer itemId, int fromUnitId, int toUnitId, double ratio) throws SQLException {
        String sql = "INSERT INTO unit_conversions(item_id, from_unit_id, to_unit_id, conversion_ratio) VALUES(?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (itemId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, itemId);
            }
            ps.setInt(2, fromUnitId);
            ps.setInt(3, toUnitId);
            ps.setDouble(4, ratio);
            ps.executeUpdate();
        }
    }

    public void deleteConversion(int conversionId) throws SQLException {
        String sql = "DELETE FROM unit_conversions WHERE conversion_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversionId);
            ps.executeUpdate();
        }
    }

    // Seeds a default conversion rule using unit names if it does not already exist
    public void insertConversionIfMissing(String fromUnit, String toUnit, double ratio) throws SQLException {
        String sql = "INSERT INTO unit_conversions(item_id, from_unit_id, to_unit_id, conversion_ratio) " +
                "SELECT NULL, f.unit_id, t.unit_id, ? FROM units f, units t " +
                "WHERE f.unit_name = ? AND t.unit_name = ? " +
                "AND NOT EXISTS (SELECT 1 FROM unit_conversions x WHERE x.item_id IS NULL " +
                "                AND x.from_unit_id = f.unit_id AND x.to_unit_id = t.unit_id)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, ratio);
            ps.setString(2, fromUnit);
            ps.setString(3, toUnit);
            ps.executeUpdate();
        }
    }
}
