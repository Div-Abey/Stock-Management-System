package com.stockmanagement.supplier.dao;

import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.supplier.model.PurchaseOrder;
import com.stockmanagement.supplier.model.PurchaseOrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL for purchase_orders and po_items. */
public class PurchaseOrderDAO {

    /** An item that can be ordered; price = the last price we paid (0 if never bought). */
    public static class ItemChoice {
        public final int itemId;
        public final String code;
        public final String name;
        public final int unitId;
        public final String unitName;
        public final double lastPrice;

        public ItemChoice(int itemId, String code, String name, int unitId, String unitName, double lastPrice) {
            this.itemId = itemId;
            this.code = code;
            this.name = name;
            this.unitId = unitId;
            this.unitName = unitName;
            this.lastPrice = lastPrice;
        }

        @Override
        public String toString() { return code + " - " + name; }
    }

    private static final String SELECT_PO =
            "SELECT po.po_id, po.supplier_id, s.supplier_name, po.order_date, po.sent_date, po.status, " +
                    "       COALESCE((SELECT SUM(quantity * unit_price) FROM po_items WHERE po_id = po.po_id), 0) AS total " +
                    "FROM purchase_orders po JOIN suppliers s ON s.supplier_id = po.supplier_id ";

    public List<ItemChoice> findItemChoices() throws SQLException {
        String sql = "SELECT i.item_id, i.item_code, i.item_name, i.base_unit_id, u.unit_name, " +
                "COALESCE((SELECT purchase_price FROM batches b WHERE b.item_id = i.item_id ORDER BY b.batch_id DESC LIMIT 1), 0) " +
                "FROM items i JOIN units u ON u.unit_id = i.base_unit_id ORDER BY i.item_name";
        List<ItemChoice> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ItemChoice(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getDouble(6)));
            }
        }
        return list;
    }

    public int insertPo(Connection conn, int supplierId, String orderDate, int createdBy) throws SQLException {
        String sql = "INSERT INTO purchase_orders(supplier_id, order_date, status, created_by) VALUES(?, ?, 'PENDING', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, supplierId);
            ps.setString(2, orderDate);
            ps.setInt(3, createdBy);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not create the purchase order.");
    }

    public void insertLine(Connection conn, int poId, PurchaseOrderItem line) throws SQLException {
        String sql = "INSERT INTO po_items(po_id, item_id, quantity, unit_id, unit_price) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, poId);
            ps.setInt(2, line.getItemId());
            ps.setInt(3, line.getQuantity());
            ps.setInt(4, line.getUnitId());
            ps.setDouble(5, line.getUnitPrice());
            ps.executeUpdate();
        }
    }

    public List<PurchaseOrder> findAll() throws SQLException {
        return query(SELECT_PO + "ORDER BY po.po_id DESC");
    }

    /** Orders that still wait for goods (PENDING or PARTIAL, and not cancelled). */
    public List<PurchaseOrder> findOpen() throws SQLException {
        return query(SELECT_PO + "WHERE po.status IN ('PENDING', 'PARTIAL') ORDER BY po.po_id DESC");
    }

    public PurchaseOrder findById(int poId) throws SQLException {
        List<PurchaseOrder> list = query(SELECT_PO + "WHERE po.po_id = " + poId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<PurchaseOrderItem> findLines(int poId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findLines(conn, poId);
        }
    }

    public List<PurchaseOrderItem> findLines(Connection conn, int poId) throws SQLException {
        String sql = "SELECT pi.po_item_id, pi.item_id, i.item_code, i.item_name, pi.unit_id, u.unit_name, " +
                "pi.quantity, pi.quantity_received, pi.unit_price " +
                "FROM po_items pi JOIN items i ON i.item_id = pi.item_id JOIN units u ON u.unit_id = pi.unit_id " +
                "WHERE pi.po_id = ? ORDER BY pi.po_item_id";
        List<PurchaseOrderItem> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, poId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrderItem l = new PurchaseOrderItem(rs.getInt(2), rs.getString(3), rs.getString(4),
                            rs.getInt(5), rs.getString(6), rs.getInt(7), rs.getDouble(9));
                    l.setPoItemId(rs.getInt(1));
                    l.setQuantityReceived(rs.getInt(8));
                    list.add(l);
                }
            }
        }
        return list;
    }

    public void markSent(int poId, String sentDate) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE purchase_orders SET sent_date = ? WHERE po_id = ?")) {
            ps.setString(1, sentDate);
            ps.setInt(2, poId);
            ps.executeUpdate();
        }
    }

    /** Cancels only an order that has not received anything yet. Returns false when it could not be cancelled. */
    public boolean cancel(int poId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE purchase_orders SET status = 'CANCELLED' WHERE po_id = ? AND status = 'PENDING'")) {
            ps.setInt(1, poId);
            return ps.executeUpdate() > 0;
        }
    }

    public void addReceived(Connection conn, int poItemId, int quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE po_items SET quantity_received = quantity_received + ? WHERE po_item_id = ?")) {
            ps.setInt(1, quantity);
            ps.setInt(2, poItemId);
            ps.executeUpdate();
        }
    }

    /** RECEIVED when every line is complete, PARTIAL when something arrived, otherwise unchanged. */
    public void refreshStatus(Connection conn, int poId) throws SQLException {
        String sql = "UPDATE purchase_orders SET status = CASE " +
                "WHEN NOT EXISTS (SELECT 1 FROM po_items WHERE po_id = ? AND quantity_received < quantity) THEN 'RECEIVED' " +
                "WHEN EXISTS (SELECT 1 FROM po_items WHERE po_id = ? AND quantity_received > 0) THEN 'PARTIAL' " +
                "ELSE status END WHERE po_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, poId);
            ps.setInt(2, poId);
            ps.setInt(3, poId);
            ps.executeUpdate();
        }
    }

    private List<PurchaseOrder> query(String sql) throws SQLException {
        List<PurchaseOrder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PurchaseOrder(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getDouble(7)));
            }
        }
        return list;
    }
}
