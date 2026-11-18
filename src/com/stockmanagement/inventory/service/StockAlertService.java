package com.stockmanagement.inventory.service;

import com.stockmanagement.inventory.dao.BatchDAO;
import com.stockmanagement.inventory.dao.ItemDAO;
import com.stockmanagement.inventory.model.Item;
import com.stockmanagement.inventory.model.StockBatch;

import java.sql.SQLException;
import java.util.List;

/** Dashboard alerts: items at/below re-order level, batches close to expiry, and expired stock. */
public class StockAlertService {

    public static final int DEFAULT_EXPIRY_WARNING_DAYS = 30;

    public static class AlertSummary {
        public final List<Item> lowStock;
        public final List<StockBatch> expiringSoon;
        public final List<StockBatch> expired;

        AlertSummary(List<Item> lowStock, List<StockBatch> expiringSoon, List<StockBatch> expired) {
            this.lowStock = lowStock;
            this.expiringSoon = expiringSoon;
            this.expired = expired;
        }

        public int total() {
            return lowStock.size() + expiringSoon.size() + expired.size();
        }
    }

    private final ItemDAO itemDAO = new ItemDAO();
    private final BatchDAO batchDAO = new BatchDAO();

    public List<Item> getLowStockItems() throws SQLException {
        return itemDAO.findLowStockItems();
    }

    public List<StockBatch> getExpiringSoon(int days) throws SQLException {
        return batchDAO.findExpiringWithin(days);
    }

    public List<StockBatch> getExpired() throws SQLException {
        return batchDAO.findExpiredWithStock();
    }

    public AlertSummary getSummary(int expiryDays) throws SQLException {
        return new AlertSummary(getLowStockItems(), getExpiringSoon(expiryDays), getExpired());
    }
}
