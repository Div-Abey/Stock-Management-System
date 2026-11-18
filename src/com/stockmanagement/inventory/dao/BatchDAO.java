package com.stockmanagement.inventory.dao;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.inventory.model.StockBatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** This code manages stock batches. It allows multiple operations to run together using one database connection. */
public class BatchDAO {

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

    // ------------------------------------------------------------------
    // FIFO (used by FIFOService during a sale)
    // ------------------------------------------------------------------

    /**
     * Picks batches expiring first (FEFO); older batch wins if dates match.
     * Skips expired and empty batches.
     */
    public List<SellableBatch> findSellableBatches(Connection conn, int itemId) throws SQLException {
        String sql = "SELECT batch_id, batch_number, quantity_remaining FROM batches " +
                "WHERE item_id = ? AND quantity_remaining > 0 AND expiry_date >= date('now','localtime') " +
                "ORDER BY expiry_date ASC, batch_id ASC";
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

    // ------------------------------------------------------------------
    // Inventory screens
    // ------------------------------------------------------------------

    private static final String BATCH_SELECT =
            "SELECT b.batch_id, b.item_id, i.item_code, i.item_name, u.unit_name, b.batch_number, b.expiry_date, " +
                    "       b.quantity_received, b.quantity_remaining, b.purchase_price, b.created_at " +
                    "FROM batches b " +
                    "JOIN items i ON i.item_id = b.item_id " +
                    "JOIN units u ON u.unit_id = i.base_unit_id ";

    /**
     * Gets batches sorted by earliest expiry date. Pass null to get all items.
     */    public List<StockBatch> findBatches(Integer itemId, boolean includeEmpty) throws SQLException {
        StringBuilder sql = new StringBuilder(BATCH_SELECT).append("WHERE 1=1 ");
        if (itemId != null) sql.append("AND b.item_id = ? ");
        if (!includeEmpty) sql.append("AND b.quantity_remaining > 0 ");
        sql.append("ORDER BY b.expiry_date ASC, b.batch_id ASC");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (itemId != null) ps.setInt(1, itemId);
            return readBatches(ps);
        }
    }

    /**
     * Gets available batches expiring within the given days (includes today, skips already expired).
     */    public List<StockBatch> findExpiringWithin(int days) throws SQLException {
        String sql = BATCH_SELECT +
                "WHERE b.quantity_remaining > 0 AND b.expiry_date >= date('now','localtime') " +
                "AND b.expiry_date <= date('now','localtime', ?) " +
                "ORDER BY b.expiry_date ASC, b.batch_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "+" + days + " days");
            return readBatches(ps);
        }
    }

    /**
     * Gets batches sorted by earliest expiry date. Pass null to get all items.
     */    public List<StockBatch> findExpiredWithStock() throws SQLException {
        return findExpiredWithStock(null);
    }

    public List<StockBatch> findExpiredWithStock(Connection existing) throws SQLException {
        String sql = BATCH_SELECT +
                "WHERE b.quantity_remaining > 0 AND b.expiry_date < date('now','localtime') " +
                "ORDER BY b.expiry_date ASC";
        if (existing != null) {
            try (PreparedStatement ps = existing.prepareStatement(sql)) {
                return readBatches(ps);
            }
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return readBatches(ps);
        }
    }

    public StockBatch findById(Connection conn, int batchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(BATCH_SELECT + "WHERE b.batch_id = ?")) {
            ps.setInt(1, batchId);
            List<StockBatch> list = readBatches(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public StockBatch findByItemAndNumber(Connection conn, int itemId, String batchNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(BATCH_SELECT + "WHERE b.item_id = ? AND lower(b.batch_number) = lower(?)")) {
            ps.setInt(1, itemId);
            ps.setString(2, batchNumber);
            List<StockBatch> list = readBatches(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /**
     * Adds a new batch (purchasePrice is per base unit) and returns its new ID.
     */    public int insert(Connection conn, int itemId, String batchNumber, String expiryDate,
                      int quantity, double purchasePrice) throws SQLException {
        String sql = "INSERT INTO batches(item_id, batch_number, expiry_date, quantity_received, quantity_remaining, purchase_price) " +
                "VALUES(?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemId);
            ps.setString(2, batchNumber);
            ps.setString(3, expiryDate);
            ps.setInt(4, quantity);
            ps.setInt(5, quantity);
            ps.setDouble(6, purchasePrice);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /**
     * Adds new stock to an existing batch, updates quantities, and saves the new average price.
     */    public void addToBatch(Connection conn, int batchId, int quantity, double newAveragePrice) throws SQLException {
        String sql = "UPDATE batches SET quantity_received = quantity_received + ?, " +
                "quantity_remaining = quantity_remaining + ?, purchase_price = ? WHERE batch_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, quantity);
            ps.setDouble(3, newAveragePrice);
            ps.setInt(4, batchId);
            ps.executeUpdate();
        }
    }

    /**
     * Updates remaining stock (for stock takes/damage). Increases received quantity if the new amount is higher.
     */    public void setRemaining(Connection conn, int batchId, int newRemaining) throws SQLException {
        String sql = "UPDATE batches SET quantity_remaining = ?, " +
                "quantity_received = CASE WHEN quantity_received < ? THEN ? ELSE quantity_received END WHERE batch_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newRemaining);
            ps.setInt(2, newRemaining);
            ps.setInt(3, newRemaining);
            ps.setInt(4, batchId);
            ps.executeUpdate();
        }
    }

    private List<StockBatch> readBatches(PreparedStatement ps) throws SQLException {
        List<StockBatch> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StockBatch b = new StockBatch();
                b.setBatchId(rs.getInt(1));
                b.setItemId(rs.getInt(2));
                b.setItemCode(rs.getString(3));
                b.setItemName(rs.getString(4));
                b.setBaseUnitName(rs.getString(5));
                b.setBatchNumber(rs.getString(6));
                b.setExpiryDate(rs.getString(7));
                b.setQuantityReceived(rs.getInt(8));
                b.setQuantityRemaining(rs.getInt(9));
                b.setPurchasePrice(rs.getDouble(10));
                b.setCreatedAt(rs.getString(11));
                list.add(b);
            }
        }
        return list;
    }
}
