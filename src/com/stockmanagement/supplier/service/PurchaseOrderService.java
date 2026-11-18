package com.stockmanagement.supplier.service;

import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.supplier.dao.PurchaseOrderDAO;
import com.stockmanagement.supplier.dao.PurchaseOrderDAO.ItemChoice;
import com.stockmanagement.supplier.model.PurchaseOrder;
import com.stockmanagement.supplier.model.PurchaseOrderItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** 3.2 Purchase orders: create and dispatch orders for goods ordered from suppliers. */
public class PurchaseOrderService {

    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();

    public List<ItemChoice> getItemChoices() throws SQLException {
        return poDAO.findItemChoices();
    }

    /** Saves the order and its lines in one transaction. Returns the new PO id. */
    public int createPurchaseOrder(int supplierId, List<PurchaseOrderItem> lines, int userId) throws SQLException {
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("Add at least one item to the purchase order.");
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int poId = poDAO.insertPo(conn, supplierId, DateUtil.format(LocalDate.now()), userId);
                for (PurchaseOrderItem line : lines) {
                    poDAO.insertLine(conn, poId, line);
                }
                conn.commit();
                return poId;
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Marks the order as dispatched today and returns it with its lines (ready to print / e-mail). */
    public PurchaseOrder dispatch(int poId) throws SQLException {
        PurchaseOrder po = getPurchaseOrder(poId);
        if ("CANCELLED".equals(po.getStatus())) {
            throw new ValidationException("A cancelled purchase order cannot be sent.");
        }
        poDAO.markSent(poId, DateUtil.format(LocalDate.now()));
        return getPurchaseOrder(poId);
    }

    public void cancel(int poId) throws SQLException {
        if (!poDAO.cancel(poId)) {
            throw new ValidationException("Only a purchase order that has not received any goods can be cancelled.");
        }
    }

    public List<PurchaseOrder> getAll() throws SQLException {
        return poDAO.findAll();
    }

    public List<PurchaseOrder> getOpenOrders() throws SQLException {
        return poDAO.findOpen();
    }

    public PurchaseOrder getPurchaseOrder(int poId) throws SQLException {
        PurchaseOrder po = poDAO.findById(poId);
        if (po == null) {
            throw new ValidationException("Purchase order not found.");
        }
        po.setLines(poDAO.findLines(poId));
        return po;
    }
}
