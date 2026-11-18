package com.stockmanagement.inventory.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL for stock batches. Methods take a Connection so a sale can run in ONE database transaction. */
public class BatchDAO {

    public int insertBatch(Connection conn, int itemId, String trim, String trim1, int quantity, double purchasePrice) {
        return itemId;
    }

    public static class SellableBatch {
        private final int batchId;
        private final String batchNumber;
        private final int quantityRemaining;

        public SellableBatch(int batchId, String batchNumber, int quantityRemaining) {
            this.batchId = batchId;
            this.batchNumber = batchNumber;
            this.quantityRemaining = quantityRemaining;
        }

        public int getBatchId() { return batchId; }
        public String getBatchNumber() { return batchNumber; }
        public int getQuantityRemaining() { return quantityRemaining; }
    }

    /** Oldest batch first (FIFO). Expired and empty batches are skipped. */
    public List<SellableBatch> findSellableBatches(Connection conn, int itemId) throws SQLException {
        String sql = "SELECT batch_id, batch_number, quantity_remaining FROM batches " +
                "WHERE item_id = ? AND quantity_remaining > 0 AND expiry_date >= date('now','localtime') " +
                "ORDER BY batch_id ASC";
        List<SellableBatch> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new SellableBatch(rs.getInt(1), rs.getString(2), rs.getInt(3)));
                }
            }
        }
        return list;
    }

    public void deductQuantity(Connection conn, int batchId, int quantity) throws SQLException {
        String sql = "UPDATE batches SET quantity_remaining = quantity_remaining - ? " +
                "WHERE batch_id = ? AND quantity_remaining >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, batchId);
            pstmt.setInt(3, quantity);
            if (pstmt.executeUpdate() == 0) {
                throw new SQLException("Batch stock changed while billing (batch id " + batchId + "). Please try again.");
            }
        }
    }
}
