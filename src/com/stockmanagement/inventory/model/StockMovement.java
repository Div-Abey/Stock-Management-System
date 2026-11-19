package com.stockmanagement.inventory.model;

import com.stockmanagement.common.enums.StockMovementType;

// Model class representing a single stock movement history record
public class StockMovement {
    private int movementId;
    private int itemId;
    private String itemName;
    private Integer batchId;
    private String batchNumber;
    private StockMovementType type;
    private int quantityChange;
    private String reference;
    private String note;
    private String createdByName;
    private String createdAt;

    public int getMovementId() { return movementId; }
    public void setMovementId(int movementId) { this.movementId = movementId; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getBatchId() { return batchId; }
    public void setBatchId(Integer batchId) { this.batchId = batchId; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public StockMovementType getType() { return type; }
    public void setType(StockMovementType type) { this.type = type; }
    public int getQuantityChange() { return quantityChange; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
