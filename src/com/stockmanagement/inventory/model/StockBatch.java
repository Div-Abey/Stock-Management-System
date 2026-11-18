package com.stockmanagement.inventory.model;

import com.stockmanagement.common.util.DateUtil;

/** One delivery of an item with its own batch number and expiry date. Quantities are in the item's BASE unit. */
public class StockBatch {
    private int batchId;
    private int itemId;
    private String itemCode;
    private String itemName;
    private String baseUnitName;
    private String batchNumber;
    private String expiryDate;      // yyyy-MM-dd, or DateUtil.NO_EXPIRY_DATE
    private int quantityReceived;
    private int quantityRemaining;
    private double purchasePrice;   // per BASE unit
    private String createdAt;

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getBaseUnitName() { return baseUnitName; }
    public void setBaseUnitName(String baseUnitName) { this.baseUnitName = baseUnitName; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public int getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(int quantityReceived) { this.quantityReceived = quantityReceived; }
    public int getQuantityRemaining() { return quantityRemaining; }
    public void setQuantityRemaining(int quantityRemaining) { this.quantityRemaining = quantityRemaining; }
    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public long getDaysToExpiry() {
        return DateUtil.daysUntil(expiryDate);
    }

    public boolean isExpired() {
        return getDaysToExpiry() < 0;
    }

    public String getStatus(int warnDays) {
        if (quantityRemaining <= 0) return "EMPTY";
        long d = getDaysToExpiry();
        if (d < 0) return "EXPIRED";
        if (d <= warnDays) return "EXPIRING SOON";
        return "OK";
    }

    @Override
    public String toString() {
        return batchNumber + " (exp " + DateUtil.display(expiryDate) + ", " + quantityRemaining + " left)";
    }
}
