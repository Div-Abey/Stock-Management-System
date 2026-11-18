package com.stockmanagement.supplier.model;

/** One line of a purchase order. */
public class PurchaseOrderItem {
    private int poItemId;
    private final int itemId;
    private final String itemCode;
    private final String itemName;
    private final int unitId;
    private final String unitName;
    private final int quantity;
    private int quantityReceived;
    private final double unitPrice;

    public PurchaseOrderItem(int itemId, String itemCode, String itemName, int unitId, String unitName,
                             int quantity, double unitPrice) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.unitId = unitId;
        this.unitName = unitName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getPoItemId() { return poItemId; }
    public void setPoItemId(int poItemId) { this.poItemId = poItemId; }
    public int getItemId() { return itemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public int getUnitId() { return unitId; }
    public String getUnitName() { return unitName; }
    public int getQuantity() { return quantity; }
    public int getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(int q) { this.quantityReceived = q; }
    public int getOutstanding() { return Math.max(0, quantity - quantityReceived); }
    public double getUnitPrice() { return unitPrice; }
    public double getLineTotal() { return quantity * unitPrice; }
}
