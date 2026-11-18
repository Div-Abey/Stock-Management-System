package com.stockmanagement.inventory.dao;

import com.stockmanagement.common.enums.StockMovementType;
import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.inventory.model.StockMovement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// Handles database operations for stock movement history
public class StockMovementDAO {

    // Records a stock change using an existing connection (for transactions)
    public void insert(Connection conn, int itemId, Integer batchId, StockMovementType type,
                       int quantityChange, String reference, String note, Integer createdBy) throws SQLException {
        String sql = "INSERT INTO stock_movements(item_id, batch_id, movement_type, quantity_change, reference, note, created_by, created_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, datetime('now','localtime'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            if (batchId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, batchId);
            ps.setString(3, type.name());
            ps.setInt(4, quantityChange);
            ps.setString(5, reference);
            ps.setString(6, note);
            if (createdBy == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, createdBy);
            ps.executeUpdate();
        }
    }

    // Fetches recent stock movements, newest first (all items if itemId is null)
    public List<StockMovement> findRecent(Integer itemId, int limit) throws SQLException {
        String sql = "SELECT m.movement_id, m.item_id, i.item_name, m.batch_id, b.batch_number, m.movement_type, " +
                "       m.quantity_change, m.reference, m.note, u.username, m.created_at " +
                "FROM stock_movements m " +
                "JOIN items i ON i.item_id = m.item_id " +
                "LEFT JOIN batches b ON b.batch_id = m.batch_id " +
                "LEFT JOIN users u ON u.user_id = m.created_by " +
                (itemId == null ? "" : "WHERE m.item_id = ? ") +
                "ORDER BY m.movement_id DESC LIMIT ?";
        List<StockMovement> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int p = 1;
            if (itemId != null) ps.setInt(p++, itemId);
            ps.setInt(p, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement m = new StockMovement();
                    m.setMovementId(rs.getInt(1));
                    m.setItemId(rs.getInt(2));
                    m.setItemName(rs.getString(3));
                    int batchId = rs.getInt(4);
                    m.setBatchId(rs.wasNull() ? null : batchId);
                    m.setBatchNumber(rs.getString(5));
                    m.setType(StockMovementType.fromDb(rs.getString(6)));
                    m.setQuantityChange(rs.getInt(7));
                    m.setReference(rs.getString(8));
                    m.setNote(rs.getString(9));
                    m.setCreatedByName(rs.getString(10));
                    m.setCreatedAt(rs.getString(11));
                    list.add(m);
                }
            }
        }
        return list;
    }
}
