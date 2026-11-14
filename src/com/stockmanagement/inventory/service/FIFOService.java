package com.stockmanagement.inventory.service;

import com.stockmanagement.inventory.dao.BatchDAO;
import com.stockmanagement.inventory.dao.BatchDAO.SellableBatch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Picks batches oldest-first and deducts the stock. Must be called inside the sale's transaction. */
public class FIFOService {

    public static class Allocation {
        private final int batchId;
        private final String batchNumber;
        private final int quantity;

        public Allocation(int batchId, String batchNumber, int quantity) {
            this.batchId = batchId;
            this.batchNumber = batchNumber;
            this.quantity = quantity;
        }

        public int getBatchId() { return batchId; }
        public String getBatchNumber() { return batchNumber; }
        public int getQuantity() { return quantity; }
    }

    private final BatchDAO batchDAO = new BatchDAO();

    public List<Allocation> allocateAndDeduct(Connection conn, int itemId, String itemName, int quantity) throws SQLException {
        List<Allocation> allocations = new ArrayList<>();
        int remaining = quantity;

        for (SellableBatch batch : batchDAO.findSellableBatches(conn, itemId)) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(remaining, batch.getQuantityRemaining());
            batchDAO.deductQuantity(conn, batch.getBatchId(), take);
            allocations.add(new Allocation(batch.getBatchId(), batch.getBatchNumber(), take));
            remaining -= take;
        }

        if (remaining > 0) {
            throw new SQLException("Insufficient stock for " + itemName + " (short by " + remaining + ").");
        }
        return allocations;
    }
}
