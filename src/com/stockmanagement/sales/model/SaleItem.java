package com.stockmanagement.sales.model;

/** One line of a sale / cart. After checkout each line belongs to exactly one batch (FIFO split). */
public class SaleItem {

    private int itemId;
    private String itemCode;
    private String itemName;
    private int unitId;
    private String unitName;
    private int quantity;
    private double unitPrice;
    private double discountApplied;   // total bulk discount for THIS line (Rs.), not per unit
    private double lineTotal;         // unitPrice * quantity - discountApplied
    private int batchId;
    private String batchNumber;

    public SaleItem(int itemId, String itemCode, String itemName, int unitId, int quantity, double unitPrice) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.unitId = unitId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        recalculateLineTotal();
    }

    public void recalculateLineTotal() {
        double gross = Math.round(unitPrice * quantity * 100.0) / 100.0;
        this.lineTotal = Math.round((gross - discountApplied) * 100.0) / 100.0;
    }

    /** unitPrice * quantity, before discount */
    public double getGrossAmount() {
        return Math.round(unitPrice * quantity * 100.0) / 100.0;
    }

    public int getItemId() { return itemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public int getUnitId() { return unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; recalculateLineTotal(); }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; recalculateLineTotal(); }
    public double getDiscountApplied() { return discountApplied; }
    public void setDiscountApplied(double discountApplied) { this.discountApplied = discountApplied; recalculateLineTotal(); }
    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
}
