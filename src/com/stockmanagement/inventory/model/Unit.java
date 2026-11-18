package com.stockmanagement.inventory.model;

public class Unit {
    private int unitId;
    private String unitName;
    private boolean baseUnit;

    public Unit() {
    }

    public Unit(int unitId, String unitName, boolean baseUnit) {
        this.unitId = unitId;
        this.unitName = unitName;
        this.baseUnit = baseUnit;
    }

    public int getUnitId() { return unitId; }
    public void setUnitId(int unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public boolean isBaseUnit() { return baseUnit; }
    public void setBaseUnit(boolean baseUnit) { this.baseUnit = baseUnit; }

    @Override
    public String toString() {
        return unitName;
    }
}
