package com.stockmanagement.inventory.service;

import com.stockmanagement.common.enums.StockMovementType;
import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.inventory.dao.BatchDAO;
import com.stockmanagement.inventory.dao.ItemDAO;
import com.stockmanagement.inventory.dao.StockMovementDAO;
import com.stockmanagement.inventory.model.Item;
import com.stockmanagement.inventory.model.StockBatch;
import com.stockmanagement.inventory.model.StockMovement;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Stock entry (GRN-style receiving with batch + expiry), adjustments and write-offs.
 * Every change to stock also writes a stock_movements row inside the same transaction.
 */
public class StockService {

    /** Everything needed to receive one line of stock. */
    public static class ReceiveRequest {
        public int itemId;
        public String batchNumber;
        public LocalDate expiryDate;      // null = item does not expire
        public int quantity;              // in purchaseUnitId
        public int purchaseUnitId;        // Carton, Box, Piece ...
        public double pricePerPurchaseUnit;
        public String reference;          // supplier invoice / GRN number (optional)
        public Integer userId;
    }

    private final ItemDAO itemDAO = new ItemDAO();
    private final BatchDAO batchDAO = new BatchDAO();
    private final StockMovementDAO movementDAO = new StockMovementDAO();
    private final UnitConversionService conversionService = new UnitConversionService();

    /** Receives stock in its own transaction. Returns the batch that now holds it. */
    public StockBatch receiveStock(ReceiveRequest r) throws ValidationException, SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                StockBatch batch = receiveStock(conn, r);
                conn.commit();
                return batch;
            } catch (ValidationException | SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Same, but inside a transaction the caller controls - the Supplier module's GRN screen can call this once per
     * GRN line, then link grn_items.batch_id to the returned batch.
     */
    public StockBatch receiveStock(Connection conn, ReceiveRequest r) throws ValidationException, SQLException {
        Item item = itemDAO.findById(r.itemId);
        if (item == null) throw new ValidationException("Select an item.");
        if (r.batchNumber == null || r.batchNumber.isBlank()) throw new ValidationException("Batch number is required.");
        if (r.quantity <= 0) throw new ValidationException("Quantity must be greater than zero.");
        if (r.pricePerPurchaseUnit < 0) throw new ValidationException("Purchase price cannot be negative.");
        if (r.expiryDate != null && r.expiryDate.isBefore(LocalDate.now())) {
            throw new ValidationException("The expiry date is in the past - expired stock cannot be received.");
        }

        String batchNo = r.batchNumber.trim();
        String expiry = r.expiryDate == null ? DateUtil.NO_EXPIRY_DATE : r.expiryDate.toString();

        // Unit conversion: 5 Box -> 60 Piece. Price is also converted to price per base unit.
        int baseQty = conversionService.toBaseUnits(r.itemId, r.purchaseUnitId, r.quantity);
        double pricePerBase = r.pricePerPurchaseUnit * r.quantity / baseQty;

        StockBatch existing = batchDAO.findByItemAndNumber(conn, r.itemId, batchNo);
        int batchId;
        if (existing == null) {
            batchId = batchDAO.insert(conn, r.itemId, batchNo, expiry, baseQty, pricePerBase);
        } else {
            if (!existing.getExpiryDate().equals(expiry)) {
                throw new ValidationException("Batch \"" + batchNo + "\" already exists for this item with expiry "
                        + DateUtil.display(existing.getExpiryDate()) + ". Use a different batch number for a different expiry date.");
            }
            double oldValue = existing.getPurchasePrice() * existing.getQuantityReceived();
            double avg = (oldValue + pricePerBase * baseQty) / (existing.getQuantityReceived() + baseQty);
            batchDAO.addToBatch(conn, existing.getBatchId(), baseQty, avg);
            batchId = existing.getBatchId();
        }

        movementDAO.insert(conn, r.itemId, batchId, StockMovementType.STOCK_IN, baseQty,
                blankToNull(r.reference),
                "Received " + r.quantity + " x " + unitName(r.purchaseUnitId) + " = " + baseQty + " " + item.getBaseUnitName(),
                r.userId);
        return batchDAO.findById(conn, batchId);
    }

    /** Stock-take / damage correction: set the quantity really on the shelf for one batch. */
    public void adjustBatch(int batchId, int newRemaining, String reason, Integer userId) throws ValidationException, SQLException {
        if (newRemaining < 0) throw new ValidationException("Quantity cannot be negative.");
        if (reason == null || reason.isBlank()) throw new ValidationException("Please enter a reason for the adjustment.");
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                StockBatch b = batchDAO.findById(conn, batchId);
                if (b == null) throw new ValidationException("Batch not found.");
                int change = newRemaining - b.getQuantityRemaining();
                if (change == 0) throw new ValidationException("The quantity is unchanged.");
                batchDAO.setRemaining(conn, batchId, newRemaining);
                movementDAO.insert(conn, b.getItemId(), batchId, StockMovementType.ADJUSTMENT, change, null, reason.trim(), userId);
                conn.commit();
            } catch (ValidationException | SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Sets every expired batch that still has stock to zero and records it. Returns how many batches were written off. */
    public int writeOffExpired(Integer userId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<StockBatch> expired = batchDAO.findExpiredWithStock(conn);
                for (StockBatch b : expired) {
                    batchDAO.setRemaining(conn, b.getBatchId(), 0);
                    movementDAO.insert(conn, b.getItemId(), b.getBatchId(), StockMovementType.EXPIRED_WRITE_OFF,
                            -b.getQuantityRemaining(), null, "Expired on " + b.getExpiryDate(), userId);
                }
                conn.commit();
                return expired.size();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<StockBatch> getBatches(Integer itemId, boolean includeEmpty) throws SQLException {
        return batchDAO.findBatches(itemId, includeEmpty);
    }

    public List<StockMovement> getMovements(Integer itemId, int limit) throws SQLException {
        return movementDAO.findRecent(itemId, limit);
    }

    private String unitName(int unitId) throws SQLException {
        return new com.stockmanagement.inventory.dao.UnitDAO().findById(unitId).getUnitName();
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
