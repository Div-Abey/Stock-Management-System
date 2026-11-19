package com.stockmanagement.sales.model;

/** One row of bulk_discounts: "buy at least minQuantity of this item and get a discount". */
public class BulkDiscount {

    private final int discountId;
    private final int itemId;
    private final int minQuantity;
    private final double discountPercentage;   // % of the line gross; used when > 0
    private final double discountAmount;       // Rs. off PER UNIT; used when percentage is 0

    public BulkDiscount(int discountId, int itemId, int minQuantity, double discountPercentage, double discountAmount) {
        this.discountId = discountId;
        this.itemId = itemId;
        this.minQuantity = minQuantity;
        this.discountPercentage = discountPercentage;
        this.discountAmount = discountAmount;
    }

    public int getDiscountId() { return discountId; }
    public int getItemId() { return itemId; }
    public int getMinQuantity() { return minQuantity; }
    public double getDiscountPercentage() { return discountPercentage; }
    public double getDiscountAmount() { return discountAmount; }
}
