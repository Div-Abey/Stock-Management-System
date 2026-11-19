package com.stockmanagement.inventory.model;

// Model class representing a unit conversion rule (e.g., 1 Box = 12 Pieces)
public class UnitConversion {
    private int conversionId;
    private Integer itemId;
    private String itemName;
    private int fromUnitId;
    private String fromUnitName;
    private int toUnitId;
    private String toUnitName;
    private double ratio;

    public int getConversionId() { return conversionId; }
    public void setConversionId(int conversionId) { this.conversionId = conversionId; }
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getFromUnitId() { return fromUnitId; }
    public void setFromUnitId(int fromUnitId) { this.fromUnitId = fromUnitId; }
    public String getFromUnitName() { return fromUnitName; }
    public void setFromUnitName(String fromUnitName) { this.fromUnitName = fromUnitName; }
    public int getToUnitId() { return toUnitId; }
    public void setToUnitId(int toUnitId) { this.toUnitId = toUnitId; }
    public String getToUnitName() { return toUnitName; }
    public void setToUnitName(String toUnitName) { this.toUnitName = toUnitName; }
    public double getRatio() { return ratio; }
    public void setRatio(double ratio) { this.ratio = ratio; }

    public String describe() {
        return "1 " + fromUnitName + " = " + trim(ratio) + " " + toUnitName;
    }

    public static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
