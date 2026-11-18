package com.stockmanagement.inventory.model;

public class Item {
    private int itemId;
    private String itemCode;
    private String barcode;
    private String itemName;
    private int categoryId;
    private String categoryName;
    private int baseUnitId;
    private String baseUnitName;
    private int reorderLevel;
    private double retailPrice;
    private double wholesalePrice;
// Available non-expired stock in base units (calculated dynamically, not stored in items table)
private int currentStock;

    // Creates a placeholder item for "All Items" in dropdowns (ID = 0)
    public static Item allItems(String label) {
        Item i = new Item();
        i.itemId = 0;
        i.itemName = label;
        return i;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public int getBaseUnitId() { return baseUnitId; }
    public void setBaseUnitId(int baseUnitId) { this.baseUnitId = baseUnitId; }
    public String getBaseUnitName() { return baseUnitName; }
    public void setBaseUnitName(String baseUnitName) { this.baseUnitName = baseUnitName; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public double getRetailPrice() { return retailPrice; }
    public void setRetailPrice(double retailPrice) { this.retailPrice = retailPrice; }
    public double getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(double wholesalePrice) { this.wholesalePrice = wholesalePrice; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }

    public boolean isLowStock() {
        return currentStock <= reorderLevel;
    }

    @Override
    public String toString() {
        return itemId == 0 ? itemName : itemCode + " - " + itemName;
    }
}
